// --- 13. gui/MainFrame.java ---
package gui;

import model.Account;
import model.LoanApplication;
import model.Transaction;
import model.User;
import service.BankingService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer; // Added for table styling
import javax.swing.border.TitledBorder; // Import TitledBorder

import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer; // Added for completeness, although now using Runnable

// MainFrame is the main application window
public class MainFrame extends JFrame {
    private BankingService bankingService;
    private JLabel welcomeLabel;
    private JTabbedPane tabbedPane;

    // Panels
    private JPanel dashboardPanel;
    private AccountPanel accountPanel;
    private TransferPanel transferPanel;
    private LoanApplicationPanel loanApplicationPanel;
    private LoanApprovalPanel loanApprovalPanel;
    private TransactionGraphPanel transactionGraphPanel;

    // Dashboard components
    private JLabel currentBalanceLabel;
    private JComboBox<String> accountSelector;
    private DefaultListModel<String> transactionHistoryModel;
    private JList<String> transactionHistoryList;
    private JButton logoutButton;

    // Define consistent colors and fonts
    private static final Color BG_DARK = new Color(45, 45, 45); // Dark gray background
    private static final Color TEXT_LIGHT = new Color(230, 230, 230); // Light gray text
    private static final Color ACCENT_BLUE = new Color(70, 130, 180); // Steel blue for primary actions/titles
    private static final Color FIELD_BG = new Color(60, 60, 60); // Slightly lighter gray for fields
    private static final Color BORDER_COLOR = new Color(90, 90, 90); // Border color for components
    private static final Font LABEL_FONT = new Font("Arial", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 14);
    private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 18);
    private static final Font DASHBOARD_BALANCE_FONT = new Font("Arial", Font.BOLD, 16);


    // Constructor
    public MainFrame(BankingService bankingService) {
        this.bankingService = bankingService;
        setTitle("SecureBank - Banking System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null); // Center the window
        getContentPane().setBackground(BG_DARK); // Set frame background

        initComponents();
        layoutComponents();
        addListeners();
    }

    // Initializes GUI components
    private void initComponents() {
        welcomeLabel = new JLabel("Welcome, User!");
        welcomeLabel.setFont(TITLE_FONT);
        welcomeLabel.setForeground(ACCENT_BLUE);
        
        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(BG_DARK);
        tabbedPane.setForeground(TEXT_LIGHT);
        tabbedPane.setFont(LABEL_FONT);

        // Dashboard Panel
        dashboardPanel = new JPanel(new BorderLayout(10, 10));
        dashboardPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        dashboardPanel.setBackground(BG_DARK);
        
        currentBalanceLabel = new JLabel("Select an account to view balance.");
        currentBalanceLabel.setFont(DASHBOARD_BALANCE_FONT);
        currentBalanceLabel.setForeground(TEXT_LIGHT);

        accountSelector = new JComboBox<>();
        accountSelector.setBackground(FIELD_BG);
        accountSelector.setForeground(TEXT_LIGHT);
        accountSelector.setFont(LABEL_FONT);
        accountSelector.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        transactionHistoryModel = new DefaultListModel<>();
        transactionHistoryList = new JList<>(transactionHistoryModel);
        transactionHistoryList.setBackground(FIELD_BG);
        transactionHistoryList.setForeground(TEXT_LIGHT);
        transactionHistoryList.setFont(LABEL_FONT);
        transactionHistoryList.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        // Custom renderer for list items to ensure proper background/foreground
        transactionHistoryList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(FIELD_BG);
                setForeground(TEXT_LIGHT);
                if (isSelected) {
                    setBackground(ACCENT_BLUE.darker()); // Darker accent for selection
                }
                return this;
            }
        });

        logoutButton = new JButton("Logout");
        logoutButton.setBackground(new Color(180, 70, 70)); // Reddish for logout
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setFont(BUTTON_FONT);
        logoutButton.setFocusPainted(false);
        logoutButton.setBorderPainted(false);
        logoutButton.setOpaque(true);

        // Initialize other panels with styling
        accountPanel = new AccountPanel(bankingService, this::refreshDashboard);
        transferPanel = new TransferPanel(bankingService, this::refreshDashboard);
        loanApplicationPanel = new LoanApplicationPanel(bankingService, this::refreshDashboard);
        loanApprovalPanel = new LoanApprovalPanel(bankingService, this::refreshDashboard);
        transactionGraphPanel = new TransactionGraphPanel(bankingService, this::refreshDashboard);
    }

    // Lays out components
    private void layoutComponents() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BG_DARK);
        headerPanel.setBorder(new EmptyBorder(10, 15, 5, 15));
        headerPanel.add(welcomeLabel, BorderLayout.WEST);
        headerPanel.add(logoutButton, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // --- Dashboard Panel Layout ---
        JPanel dashboardInfoPanel = new JPanel(new GridBagLayout());
        dashboardInfoPanel.setBackground(BG_DARK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel selectAccLabel = new JLabel("Select Account:");
        selectAccLabel.setForeground(TEXT_LIGHT);
        selectAccLabel.setFont(LABEL_FONT);
        dashboardInfoPanel.add(selectAccLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        dashboardInfoPanel.add(accountSelector, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        dashboardInfoPanel.add(currentBalanceLabel, gbc);
        
        dashboardPanel.add(dashboardInfoPanel, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(transactionHistoryList);
        scrollPane.getViewport().setBackground(FIELD_BG); // Important for scroll pane background
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(BORDER_COLOR), "Transaction History",
                                            TitledBorder.LEFT, TitledBorder.TOP, LABEL_FONT, TEXT_LIGHT),
            BorderFactory.createEmptyBorder(5,5,5,5)
        ));
        dashboardPanel.add(scrollPane, BorderLayout.CENTER);
        
        // --- Add Panels to Tabbed Pane ---
        tabbedPane.addTab("Dashboard", dashboardPanel);
        tabbedPane.addTab("Deposit/Withdraw", accountPanel);
        tabbedPane.addTab("Transfer Funds", transferPanel);
        tabbedPane.addTab("Apply for Loan", loanApplicationPanel);
        tabbedPane.addTab("Transaction Graph", transactionGraphPanel); // New tab for graph

        // Admin-only panel
        User currentUser = bankingService.getCurrentUser();
        if (currentUser != null && currentUser.isAdmin()) {
            tabbedPane.addTab("Loan Approvals", loanApprovalPanel);
        }

        add(tabbedPane, BorderLayout.CENTER);
    }

    // Adds listeners
    private void addListeners() {
        logoutButton.addActionListener(e -> {
            bankingService.logoutUser();
            JOptionPane.showMessageDialog(this, "Logged out successfully.", "Logout", JOptionPane.INFORMATION_MESSAGE);
            new LoginFrame(bankingService).setVisible(true); // Return to login screen
            dispose();
        });

        accountSelector.addActionListener(e -> {
            refreshAccountDetails();
        });
        
        // Listener for tab changes to refresh relevant panels
        tabbedPane.addChangeListener(e -> {
            Component selectedComponent = tabbedPane.getSelectedComponent();
            if (selectedComponent == dashboardPanel) {
                updateDashboard(); // Use updateDashboard here
            } else if (selectedComponent == loanApprovalPanel) {
                loanApprovalPanel.refreshPendingLoans();
            } else if (selectedComponent == transactionGraphPanel) {
                transactionGraphPanel.refreshGraph();
            }
        });
    }

    // Updates the dashboard
    public void updateDashboard() {
        User currentUser = bankingService.getCurrentUser();
        if (currentUser != null) {
            welcomeLabel.setText("Welcome, " + currentUser.getFullName() + "!");
            populateAccountSelector();
            refreshAccountDetails(); // Refresh initial account details
            
            // Re-add loan approval tab if user becomes admin or was already admin
            if (currentUser.isAdmin() && tabbedPane.indexOfTab("Loan Approvals") == -1) { 
                tabbedPane.addTab("Loan Approvals", loanApprovalPanel);
            } else if (!currentUser.isAdmin() && tabbedPane.indexOfTab("Loan Approvals") != -1) { 
                tabbedPane.remove(loanApprovalPanel);
            }

            // Also update other panels' user context if needed
            accountPanel.setCurrentUser(currentUser);
            transferPanel.setCurrentUser(currentUser);
            loanApplicationPanel.setCurrentUser(currentUser);
            loanApprovalPanel.setCurrentUser(currentUser);
            transactionGraphPanel.setCurrentUser(currentUser); // Update graph panel user
        } else {
            // User logged out
            welcomeLabel.setText("Welcome, Guest!");
            accountSelector.removeAllItems();
            transactionHistoryModel.clear();
            currentBalanceLabel.setText("Please log in.");
            if (tabbedPane.indexOfTab("Loan Approvals") != -1) { 
                tabbedPane.remove(loanApprovalPanel);
            }
        }
    }

    // Populates the account selector
    private void populateAccountSelector() {
        accountSelector.removeAllItems();
        try {
            List<Account> accounts = bankingService.getUserAccounts();
            if (accounts.isEmpty()) {
                accountSelector.addItem("No accounts available");
            } else {
                for (Account acc : accounts) {
                    accountSelector.addItem(acc.getType().name() + " (" + acc.getId().substring(0, 8) + ")");
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading accounts: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            accountSelector.addItem("Error loading accounts");
        }
    }

    // Refreshes account details
    private void refreshAccountDetails() {
        int selectedIndex = accountSelector.getSelectedIndex();
        if (selectedIndex == -1 || accountSelector.getSelectedItem().equals("No accounts available") || accountSelector.getSelectedItem().equals("Error loading accounts")) {
            currentBalanceLabel.setText("No account selected.");
            transactionHistoryModel.clear();
            return;
        }

        try {
            List<Account> userAccounts = bankingService.getUserAccounts();
            Account selectedAccount = userAccounts.get(selectedIndex); // Assumes order matches
            
            currentBalanceLabel.setText(String.format("Current Balance: $%.2f (%s)", selectedAccount.getBalance(), selectedAccount.getType().name()));

            transactionHistoryModel.clear();
            List<Transaction> transactions = bankingService.getAccountTransactions(selectedAccount.getId());
            if (transactions.isEmpty()) {
                transactionHistoryModel.addElement("No transactions found for this account.");
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                for (Transaction t : transactions) {
                    String desc = t.getDescription();
                    if (t.getType() == Transaction.TransactionType.TRANSFER_OUT && t.getCounterpartyAccountId() != null) {
                        desc = String.format("Transferred to: %s", t.getCounterpartyAccountId().substring(0,8));
                    } else if (t.getType() == Transaction.TransactionType.TRANSFER_IN && t.getCounterpartyAccountId() != null) {
                         desc = String.format("Received from: %s", t.getCounterpartyAccountId().substring(0,8));
                    }
                    transactionHistoryModel.addElement(String.format("%s - %s: $%.2f (%s)",
                                                                     t.getTimestamp().format(formatter),
                                                                     t.getType().name(),
                                                                     t.getAmount(),
                                                                     desc));
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error refreshing account details: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            currentBalanceLabel.setText("Error loading details.");
            transactionHistoryModel.clear();
        }
    }
    
    // Callback method for panels to trigger a dashboard refresh
    public void refreshDashboard() {
        populateAccountSelector();
        refreshAccountDetails();
        // Also refresh other dynamic panels if they are open
        accountPanel.refreshAccounts();
        transferPanel.refreshAccounts();
        loanApplicationPanel.refreshUserLoans();
        loanApprovalPanel.refreshPendingLoans();
        transactionGraphPanel.refreshGraph();
    }
}