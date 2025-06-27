
package db;

import model.*;
import util.PasswordHasher; // For Base64 decoding of salt if stored directly as string
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;


public class DatabaseManager {
    private String dbFilePath;
    private Connection connection;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    
    public DatabaseManager(String dbFilePath) {
        this.dbFilePath = dbFilePath;
      
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found.", e);
        }
    }

    // Opens a database connection
    public Connection openConnection() throws SQLException { // Made public for external use
        if (connection == null || connection.isClosed()) {
            String url = "jdbc:sqlite:" + dbFilePath;
            connection = DriverManager.getConnection(url);
            connection.setAutoCommit(true); // Auto-commit for simplicity
            // System.out.println("Opened database connection: " + dbFilePath); // Debug
        }
        return connection;
    }

    // Closes the database connection
    public void closeConnection() { // Made public for external use
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null; // Clear connection to allow reopening
                // System.out.println("Closed database connection: " + dbFilePath); // Debug
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }

    // Initializes database tables (creates them if they don't exist)
    public void initializeDatabase() throws SQLException {
        try (Connection conn = openConnection();
             Statement stmt = conn.createStatement()) {
            // Users table
            String createUserTableSql = "CREATE TABLE IF NOT EXISTS users (" +
                                        "id TEXT PRIMARY KEY NOT NULL," +
                                        "username TEXT UNIQUE NOT NULL," +
                                        "password_hash TEXT NOT NULL," +
                                        "salt TEXT NOT NULL," + // Salt stored for each user
                                        "full_name TEXT NOT NULL," +
                                        "is_admin INTEGER NOT NULL" + // 0 for false, 1 for true
                                        ");";
            stmt.execute(createUserTableSql);

            // Accounts table
            String createAccountTableSql = "CREATE TABLE IF NOT EXISTS accounts (" +
                                           "id TEXT PRIMARY KEY NOT NULL," +
                                           "user_id TEXT NOT NULL," +
                                           "type TEXT NOT NULL," + // CHECKING or SAVINGS
                                           "balance REAL NOT NULL," +
                                           "FOREIGN KEY (user_id) REFERENCES users(id)" +
                                           ");";
            stmt.execute(createAccountTableSql);

            // Transactions table
            String createTransactionTableSql = "CREATE TABLE IF NOT EXISTS transactions (" +
                                               "id TEXT PRIMARY KEY NOT NULL," +
                                               "account_id TEXT NOT NULL," +
                                               "counterparty_account_id TEXT," + // Null for deposit/withdrawal
                                               "amount REAL NOT NULL," +
                                               "type TEXT NOT NULL," +
                                               "timestamp TEXT NOT NULL," + // Stored as ISO 8601 string
                                               "description TEXT," +
                                               "FOREIGN KEY (account_id) REFERENCES accounts(id)" +
                                               ");";
            stmt.execute(createTransactionTableSql);

            // LoanApplications table
            String createLoanApplicationsTableSql = "CREATE TABLE IF NOT EXISTS loan_applications (" +
                                                    "id TEXT PRIMARY KEY NOT NULL," +
                                                    "user_id TEXT NOT NULL," +
                                                    "amount REAL NOT NULL," +
                                                    "status TEXT NOT NULL," + // PENDING, APPROVED, REJECTED
                                                    "application_date TEXT NOT NULL," +
                                                    "reason TEXT," +
                                                    "priority_score INTEGER NOT NULL," +
                                                    "FOREIGN KEY (user_id) REFERENCES users(id)" +
                                                    ");";
            stmt.execute(createLoanApplicationsTableSql);

            System.out.println("Database initialized.");
        } finally {
            closeConnection();
        }
    }

    // --- User CRUD ---
    public void addUser(User user, byte[] salt) throws SQLException {
        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO users (id, username, password_hash, salt, full_name, is_admin) VALUES (?, ?, ?, ?, ?, ?)")) {
            pstmt.setString(1, user.getId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setString(4, Base64.getEncoder().encodeToString(salt)); // Store salt as Base64 string
            pstmt.setString(5, user.getFullName());
            pstmt.setInt(6, user.isAdmin() ? 1 : 0);
            pstmt.executeUpdate();
            System.out.println("User added: " + user.getUsername());
        } finally {
            closeConnection();
        }
    }

    public User getUserByUsername(String username) throws SQLException {
        User user = null;
        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE username = ?")) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    user = new User(
                            rs.getString("id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("full_name"),
                            rs.getInt("is_admin") == 1
                    );
                }
            }
        } finally {
            closeConnection();
        }
        return user;
    }

    public byte[] getUserSalt(String username) throws SQLException {
        byte[] salt = null;
        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT salt FROM users WHERE username = ?")) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String saltBase64 = rs.getString("salt");
                    if (saltBase64 != null) {
                        salt = Base64.getDecoder().decode(saltBase64);
                    }
                }
            }
        } finally {
            closeConnection();
        }
        return salt;
    }

    // --- Account CRUD ---
    public void addAccount(Account account) throws SQLException {
        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO accounts (id, user_id, type, balance) VALUES (?, ?, ?, ?)")) {
            pstmt.setString(1, account.getId());
            pstmt.setString(2, account.getUserId());
            pstmt.setString(3, account.getType().name());
            pstmt.setBigDecimal(4, account.getBalance());
            pstmt.executeUpdate();
            System.out.println("Account added: " + account.getId());
        } finally {
            closeConnection();
        }
    }

    public void updateAccountBalance(String accountId, BigDecimal newBalance) throws SQLException {
        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement("UPDATE accounts SET balance = ? WHERE id = ?")) {
            pstmt.setBigDecimal(1, newBalance);
            pstmt.setString(2, accountId);
            pstmt.executeUpdate();
            System.out.println("Account balance updated for: " + accountId);
        } finally {
            closeConnection();
        }
    }

    public Account getAccountById(String accountId) throws SQLException {
        Account account = null;
        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM accounts WHERE id = ?")) {
            pstmt.setString(1, accountId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    account = new Account(
                            rs.getString("id"),
                            rs.getString("user_id"),
                            Account.AccountType.valueOf(rs.getString("type")),
                            rs.getBigDecimal("balance")
                    );
                }
            }
        } finally {
            closeConnection();
        }
        return account;
    }

    public List<Account> getAccountsByUserId(String userId) throws SQLException {
        List<Account> accounts = new ArrayList<>();
        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM accounts WHERE user_id = ?")) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    accounts.add(new Account(
                            rs.getString("id"),
                            rs.getString("user_id"),
                            Account.AccountType.valueOf(rs.getString("type")),
                            rs.getBigDecimal("balance")
                    ));
                }
            }
        } finally {
            closeConnection();
        }
        return accounts;
    }
    
    // New method to get all accounts
    public List<Account> getAllAccounts() throws SQLException {
        List<Account> accounts = new ArrayList<>();
        try (Connection conn = openConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM accounts")) {
            while (rs.next()) {
                accounts.add(new Account(
                        rs.getString("id"),
                        rs.getString("user_id"),
                        Account.AccountType.valueOf(rs.getString("type")),
                        rs.getBigDecimal("balance")
                ));
            }
        } finally {
            closeConnection();
        }
        return accounts;
    }

    // --- Transaction CRUD ---
    public void addTransaction(Transaction transaction) throws SQLException {
        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO transactions (id, account_id, counterparty_account_id, amount, type, timestamp, description) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            pstmt.setString(1, transaction.getId());
            pstmt.setString(2, transaction.getAccountId());
            pstmt.setString(3, transaction.getCounterpartyAccountId());
            pstmt.setBigDecimal(4, transaction.getAmount());
            pstmt.setString(5, transaction.getType().name());
            pstmt.setString(6, transaction.getTimestamp().format(FORMATTER));
            pstmt.setString(7, transaction.getDescription());
            pstmt.executeUpdate();
            System.out.println("Transaction added: " + transaction.getId());
        } finally {
            closeConnection();
        }
    }

    public List<Transaction> getTransactionsByAccountId(String accountId) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM transactions WHERE account_id = ? ORDER BY timestamp DESC")) {
            pstmt.setString(1, accountId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(new Transaction(
                            rs.getString("id"),
                            rs.getString("account_id"),
                            rs.getString("counterparty_account_id"),
                            rs.getBigDecimal("amount"),
                            Transaction.TransactionType.valueOf(rs.getString("type")),
                            LocalDateTime.parse(rs.getString("timestamp"), FORMATTER),
                            rs.getString("description")
                    ));
                }
            }
        } finally {
            closeConnection();
        }
        return transactions;
    }

    public List<Transaction> getAllTransactions() throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        try (Connection conn = openConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM transactions ORDER BY timestamp DESC")) {
            while (rs.next()) {
                transactions.add(new Transaction(
                        rs.getString("id"),
                        rs.getString("account_id"),
                        rs.getString("counterparty_account_id"),
                        rs.getBigDecimal("amount"),
                        Transaction.TransactionType.valueOf(rs.getString("type")),
                        LocalDateTime.parse(rs.getString("timestamp"), FORMATTER),
                        rs.getString("description")
                ));
            }
        } finally {
            closeConnection();
        }
        return transactions;
    }


    // --- Loan Application CRUD ---
    public void addLoanApplication(LoanApplication loan) throws SQLException {
        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO loan_applications (id, user_id, amount, status, application_date, reason, priority_score) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            pstmt.setString(1, loan.getId());
            pstmt.setString(2, loan.getUserId());
            pstmt.setBigDecimal(3, loan.getAmount());
            pstmt.setString(4, loan.getStatus().name());
            pstmt.setString(5, loan.getApplicationDate().format(FORMATTER));
            pstmt.setString(6, loan.getReason());
            pstmt.setInt(7, loan.getPriorityScore());
            pstmt.executeUpdate();
            System.out.println("Loan application added for user: " + loan.getUserId());
        } finally {
            closeConnection();
        }
    }

    public void updateLoanApplicationStatus(String loanId, LoanApplication.LoanStatus newStatus) throws SQLException {
        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement("UPDATE loan_applications SET status = ? WHERE id = ?")) {
            pstmt.setString(1, newStatus.name());
            pstmt.setString(2, loanId);
            pstmt.executeUpdate();
            System.out.println("Loan application status updated for: " + loanId);
        } finally {
            closeConnection();
        }
    }

    public List<LoanApplication> getLoanApplicationsByStatus(LoanApplication.LoanStatus status) throws SQLException {
        List<LoanApplication> loans = new ArrayList<>();
        try (Connection conn = openConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM loan_applications WHERE status = ? ORDER BY priority_score ASC, application_date ASC")) {
            pstmt.setString(1, status.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    loans.add(new LoanApplication(
                            rs.getString("id"),
                            rs.getString("user_id"),
                            rs.getBigDecimal("amount"),
                            LoanApplication.LoanStatus.valueOf(rs.getString("status")),
                            LocalDateTime.parse(rs.getString("application_date"), FORMATTER),
                            rs.getString("reason"),
                            rs.getInt("priority_score")
                    ));
                }
            }
        } finally {
            closeConnection();
        }
        return loans;
    }
}