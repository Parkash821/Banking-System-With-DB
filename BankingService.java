// --- 10. service/BankingService.java ---
package service;

import db.DatabaseManager;
import model.*;
import util.PasswordHasher;
import util.LoanPriorityComparator;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter; // Added import
import java.sql.Connection; // Added import
import java.sql.Statement; // Added import
import java.sql.ResultSet; // Added import
import java.util.*;
import java.util.stream.Collectors;

// BankingService is the main business logic layer
public class BankingService {
    private DatabaseManager dbManager;
    private User currentUser;
    private PriorityQueue<LoanApplication> loanHeap; // Min-heap for loan prioritization

    // Transaction Graph: Adjacency list representation
    // Map<SenderUserId, Map<ReceiverUserId, List<Transaction>>>
    private Map<String, Map<String, List<Transaction>>> transactionGraph;

    
    public BankingService(String dbFilePath) {
        this.dbManager = new DatabaseManager(dbFilePath);
        try {
            dbManager.initializeDatabase(); // Ensure database tables exist
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            // Fatal error, propagate or handle appropriately
            throw new RuntimeException("Database initialization failed.", e);
        }
        this.loanHeap = new PriorityQueue<>(new LoanPriorityComparator());
        this.transactionGraph = new HashMap<>();
        loadLoansIntoHeap(); // Load pending loans when application starts
        loadTransactionsIntoGraph(); // Load transaction graph when application starts
    }

    // --- User Management ---
    public User registerUser(String username, String password, String fullName, boolean isAdmin) throws SQLException, IllegalArgumentException {
        if (dbManager.getUserByUsername(username) != null) {
            throw new IllegalArgumentException("Username already exists.");
        }
        if (password.length() < 6) { // Basic password policy
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }
        byte[] salt = PasswordHasher.generateSalt();
        String hashedPassword = PasswordHasher.hashPassword(password, salt);
        User newUser = new User(username, hashedPassword, fullName, isAdmin);
        dbManager.addUser(newUser, salt);
        return newUser;
    }

    public User loginUser(String username, String password) throws Exception {
        User user = dbManager.getUserByUsername(username);
        if (user == null) {
            throw new Exception("User not found.");
        }
        byte[] salt = dbManager.getUserSalt(username);
        if (salt == null || !PasswordHasher.verifyPassword(password, user.getPasswordHash(), salt)) {
            throw new Exception("Invalid username or password.");
        }
        currentUser = user;
        System.out.println("User logged in: " + currentUser.getUsername());
        return currentUser;
    }

    public void logoutUser() {
        currentUser = null;
        System.out.println("User logged out.");
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    // --- Account Management ---
    public Account createAccount(Account.AccountType type, BigDecimal initialBalance) throws SQLException, IllegalStateException {
        if (currentUser == null) {
            throw new IllegalStateException("No user is currently logged in.");
        }
        if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative.");
        }
        Account newAccount = new Account(currentUser.getId(), type, initialBalance);
        dbManager.addAccount(newAccount);
        return newAccount;
    }

    public List<Account> getUserAccounts() throws SQLException, IllegalStateException {
        if (currentUser == null) {
            throw new IllegalStateException("No user is currently logged in.");
        }
        return dbManager.getAccountsByUserId(currentUser.getId());
    }
    
    // New method to get all accounts
    public List<Account> getAllAccounts() throws SQLException {
        return dbManager.getAllAccounts();
    }

    public void deposit(String accountId, BigDecimal amount) throws SQLException, IllegalArgumentException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }
        Account account = dbManager.getAccountById(accountId);
        if (account == null) {
            throw new IllegalArgumentException("Account not found.");
        }
        if (!account.getUserId().equals(currentUser.getId())) {
             throw new IllegalArgumentException("Account does not belong to the current user.");
        }

        BigDecimal newBalance = account.getBalance().add(amount);
        dbManager.updateAccountBalance(accountId, newBalance);
        dbManager.addTransaction(new Transaction(accountId, null, amount, Transaction.TransactionType.DEPOSIT, "Deposit"));
        System.out.println("Deposited " + amount + " to account " + accountId);
    }

    public void withdraw(String accountId, BigDecimal amount) throws SQLException, IllegalArgumentException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive.");
        }
        Account account = dbManager.getAccountById(accountId);
        if (account == null) {
            throw new IllegalArgumentException("Account not found.");
        }
        if (!account.getUserId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Account does not belong to the current user.");
        }
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds.");
        }

        BigDecimal newBalance = account.getBalance().subtract(amount);
        dbManager.updateAccountBalance(accountId, newBalance);
        dbManager.addTransaction(new Transaction(accountId, null, amount, Transaction.TransactionType.WITHDRAWAL, "Withdrawal"));
        System.out.println("Withdrew " + amount + " from account " + accountId);
    }

    public void transferFunds(String fromAccountId, String toAccountId, BigDecimal amount) throws SQLException, IllegalArgumentException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive.");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account.");
        }

        Account fromAccount = dbManager.getAccountById(fromAccountId);
        Account toAccount = dbManager.getAccountById(toAccountId);

        if (fromAccount == null || toAccount == null) {
            throw new IllegalArgumentException("One or both accounts not found.");
        }
        if (!fromAccount.getUserId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Source account does not belong to the current user.");
        }
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds in source account.");
        }

        // Perform transfers
        dbManager.updateAccountBalance(fromAccountId, fromAccount.getBalance().subtract(amount));
        dbManager.updateAccountBalance(toAccountId, toAccount.getBalance().add(amount));

        // Log transactions
        Transaction outgoing = new Transaction(fromAccountId, toAccountId, amount, Transaction.TransactionType.TRANSFER_OUT, "Transfer to " + toAccount.getId());
        Transaction incoming = new Transaction(toAccountId, fromAccountId, amount, Transaction.TransactionType.TRANSFER_IN, "Transfer from " + fromAccount.getId());
        dbManager.addTransaction(outgoing);
        dbManager.addTransaction(incoming);

        // Add to graph
        addToTransactionGraph(fromAccount.getUserId(), toAccount.getUserId(), amount, outgoing.getTimestamp());

        System.out.println("Transferred " + amount + " from " + fromAccountId + " to " + toAccountId);
    }

    public List<Transaction> getAccountTransactions(String accountId) throws SQLException {
        return dbManager.getTransactionsByAccountId(accountId);
    }

    // --- Loan Prioritization (Heap) ---
    private void loadLoansIntoHeap() {
        try {
            List<LoanApplication> pendingLoans = dbManager.getLoanApplicationsByStatus(LoanApplication.LoanStatus.PENDING);
            loanHeap.clear();
            for (LoanApplication loan : pendingLoans) {
                loanHeap.offer(loan); // Add to min-heap
            }
            System.out.println("Loaded " + loanHeap.size() + " pending loans into heap.");
        } catch (SQLException e) {
            System.err.println("Error loading pending loans into heap: " + e.getMessage());
        }
    }

    public LoanApplication applyForLoan(BigDecimal amount, String reason, int priorityScore) throws SQLException, IllegalStateException {
        if (currentUser == null) {
            throw new IllegalStateException("No user is currently logged in.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Loan amount must be positive.");
        }
        LoanApplication newLoan = new LoanApplication(currentUser.getId(), amount, reason, priorityScore);
        dbManager.addLoanApplication(newLoan);
        loanHeap.offer(newLoan); // Add to heap immediately
        return newLoan;
    }

    public LoanApplication getNextLoanForApproval() throws IllegalStateException {
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new IllegalStateException("Only administrators can approve loans.");
        }
        return loanHeap.peek(); // Get highest priority (lowest score) without removing
    }

    public LoanApplication approveLoan(String loanId, String recipientAccountId) throws SQLException, IllegalStateException, IllegalArgumentException {
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new IllegalStateException("Only administrators can approve loans.");
        }

        LoanApplication loanToApprove = null;
        // Find and remove the loan from the heap
        Iterator<LoanApplication> it = loanHeap.iterator();
        while (it.hasNext()) {
            LoanApplication loan = it.next();
            if (loan.getId().equals(loanId)) {
                loanToApprove = loan;
                it.remove(); // Remove from heap
                break;
            }
        }

        if (loanToApprove == null) {
            throw new IllegalArgumentException("Loan application not found in pending queue or already processed.");
        }
        
        Account recipientAccount = dbManager.getAccountById(recipientAccountId);
        if (recipientAccount == null) {
            throw new IllegalArgumentException("Recipient account for loan approval not found.");
        }
        if (!recipientAccount.getUserId().equals(loanToApprove.getUserId())) {
            throw new IllegalArgumentException("Recipient account does not belong to the loan applicant.");
        }

        // Update loan status in DB
        dbManager.updateLoanApplicationStatus(loanId, LoanApplication.LoanStatus.APPROVED);

        // Deposit loan amount to user's account
        BigDecimal newBalance = recipientAccount.getBalance().add(loanToApprove.getAmount());
        dbManager.updateAccountBalance(recipientAccountId, newBalance);
        dbManager.addTransaction(new Transaction(recipientAccountId, null, loanToApprove.getAmount(), Transaction.TransactionType.DEPOSIT, "Loan Approved: " + loanToApprove.getId()));

        System.out.println("Loan " + loanId + " approved for user " + loanToApprove.getUserId());
        return loanToApprove;
    }

    public LoanApplication rejectLoan(String loanId) throws SQLException, IllegalStateException, IllegalArgumentException {
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new IllegalStateException("Only administrators can reject loans.");
        }

        LoanApplication loanToReject = null;
        // Find and remove the loan from the heap
        Iterator<LoanApplication> it = loanHeap.iterator();
        while (it.hasNext()) {
            LoanApplication loan = it.next();
            if (loan.getId().equals(loanId)) {
                loanToReject = loan;
                it.remove(); // Remove from heap
                break;
            }
        }

        if (loanToReject == null) {
            throw new IllegalArgumentException("Loan application not found in pending queue or already processed.");
        }

        // Update loan status in DB
        dbManager.updateLoanApplicationStatus(loanId, LoanApplication.LoanStatus.REJECTED);
        System.out.println("Loan " + loanId + " rejected for user " + loanToReject.getUserId());
        return loanToReject;
    }

    public List<LoanApplication> getPendingLoans() {
        // Return a copy of the heap's elements for display without affecting the heap itself
        return new ArrayList<>(loanHeap);
    }
    
    public List<LoanApplication> getLoansByUserId(String userId) throws SQLException {
        try {
            List<LoanApplication> allLoans = new ArrayList<>();
            allLoans.addAll(dbManager.getLoanApplicationsByStatus(LoanApplication.LoanStatus.PENDING));
            allLoans.addAll(dbManager.getLoanApplicationsByStatus(LoanApplication.LoanStatus.APPROVED));
            allLoans.addAll(dbManager.getLoanApplicationsByStatus(LoanApplication.LoanStatus.REJECTED));
            
            return allLoans.stream()
                            .filter(loan -> loan.getUserId().equals(userId))
                            .sorted(Comparator.comparing(LoanApplication::getApplicationDate).reversed())
                            .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new SQLException("Error retrieving loans for user: " + e.getMessage(), e);
        }
    }


    // --- Transaction Graph ---
    private void loadTransactionsIntoGraph() {
        transactionGraph.clear();
        try {
            List<Transaction> allTransactions = dbManager.getAllTransactions();
            for (Transaction t : allTransactions) {
                if (t.getType() == Transaction.TransactionType.TRANSFER_OUT && t.getCounterpartyAccountId() != null) {
                    Account senderAccount = dbManager.getAccountById(t.getAccountId());
                    Account receiverAccount = dbManager.getAccountById(t.getCounterpartyAccountId());

                    if (senderAccount != null && receiverAccount != null) {
                        addToTransactionGraph(senderAccount.getUserId(), receiverAccount.getUserId(), t.getAmount(), t.getTimestamp());
                    }
                }
            }
            System.out.println("Transaction graph loaded with " + transactionGraph.size() + " unique sender/receiver pairs.");
        } catch (SQLException e) {
            System.err.println("Error loading transactions into graph: " + e.getMessage());
        }
    }

    private void addToTransactionGraph(String senderUserId, String receiverUserId, BigDecimal amount, LocalDateTime timestamp) {
        transactionGraph
            .computeIfAbsent(senderUserId, k -> new HashMap<>())
            .computeIfAbsent(receiverUserId, k -> new ArrayList<>())
            .add(new Transaction(senderUserId, receiverUserId, amount, Transaction.TransactionType.TRANSFER_OUT, "Graph Transfer")); // Simplified for graph, actual transaction details stored in DB
    }
    
    // Retrieves a summarized transaction graph for display
    // Returns Map<SenderUsername, Map<ReceiverUsername, List<String>>>
    public Map<String, Map<String, List<String>>> getSummarizedTransactionGraph() throws SQLException {
        Map<String, Map<String, List<String>>> summarizedGraph = new HashMap<>();
        
        // Fetch all users to map IDs to usernames
        Map<String, String> userIdToUsernameMap = new HashMap<>();
        try (Connection conn = dbManager.openConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, username FROM users")) {
            while (rs.next()) {
                userIdToUsernameMap.put(rs.getString("id"), rs.getString("username"));
            }
        } finally {
            dbManager.closeConnection(); // Ensure connection is closed
        }

        for (Map.Entry<String, Map<String, List<Transaction>>> senderEntry : transactionGraph.entrySet()) {
            String senderUserId = senderEntry.getKey();
            String senderUsername = userIdToUsernameMap.getOrDefault(senderUserId, "Unknown User (ID: " + senderUserId + ")");

            for (Map.Entry<String, List<Transaction>> receiverEntry : senderEntry.getValue().entrySet()) {
                String receiverUserId = receiverEntry.getKey();
                String receiverUsername = userIdToUsernameMap.getOrDefault(receiverUserId, "Unknown User (ID: " + receiverUserId + ")");
                
                List<String> transactionStrings = new ArrayList<>();
                for (Transaction t : receiverEntry.getValue()) {
                    transactionStrings.add(String.format("Amount: %.2f (at %s)", t.getAmount(), t.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm"))));
                }

                summarizedGraph
                    .computeIfAbsent(senderUsername, k -> new HashMap<>())
                    .put(receiverUsername, transactionStrings);
            }
        }
        return summarizedGraph;
    }
}
