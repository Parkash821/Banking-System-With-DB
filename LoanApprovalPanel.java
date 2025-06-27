// --- 17. gui/LoanApprovalPanel.java ---
package gui;

import model.LoanApplication;
import model.Account;
import service.BankingService;
import util.LoanPriorityComparator; // Import the comparator

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer; // Added for table cell rendering
import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.lang.Runnable; 
import java.util.HashMap; // Added for accountIdMap
import java.util.Map; // Added for accountIdMap
import java.util.stream.Collectors; // Added for filtering accounts
import java.util.Collections; // Added for sorting

// LoanApprovalPanel is a JPanel for administrators to view and manage pending loan applications
public class LoanApprovalPanel extends JPanel {
    private BankingService bankingService;
    private Runnable refreshDashboardCallback;
    private model.User currentUser;

    private DefaultTableModel pendingLoansTableModel;
    private JTable pendingLoansTable;
    private JButton approveButton, rejectButton;

    // Components for recipient account selection
    private JDialog recipientAccountDialog;
    private JComboBox<String> recipientAccountCombo;
    private JButton confirmRecipientButton;
    private String selectedLoanUserId; // To store the userId of the loan being approved
    private Map<String, String> accountIdMap; // Map to store full account IDs

    // Define consistent colors and fonts
    private static final Color BG_DARK = new Color(45, 45, 45);
    private static final Color TEXT_LIGHT = new Color(230, 230, 230);
    private static final Color ACCENT_BLUE = new Color(70, 130, 180);
    private static final Color FIELD_BG = new Color(60, 60, 60);
    private static final Color BORDER_COLOR = new Color(90, 90, 90);
    private static final Font LABEL_FONT = new Font("Arial", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 14);
    private static final Font TITLE_BORDER_FONT = new Font("Arial", Font.BOLD, 16);
    private static final Font TABLE_HEADER_FONT = new Font("Arial", Font.BOLD, 14);


    // Constructor
    public LoanApprovalPanel(BankingService bankingService, Runnable refreshDashboardCallback) { // Changed to Runnable
        this.bankingService = bankingService;
        this.refreshDashboardCallback = refreshDashboardCallback;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(BG_DARK); // Set panel background

        this.accountIdMap = new HashMap<>(); // Initialize the map

        initComponents();
        layoutComponents();
        addListeners();
        // Initial call to refreshPendingLoans() is still needed
        refreshPendingLoans();
    }
    
    // Sets the current user and refreshes pending loans
    public void setCurrentUser(model.User user) {
        this.currentUser = user;
        refreshPendingLoans();
    }

    // Initializes GUI components
    private void initComponents() {
        pendingLoansTableModel = new DefaultTableModel(new Object[]{"ID", "User ID", "Amount", "Date", "Reason", "Priority"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        pendingLoansTable = new JTable(pendingLoansTableModel);
        pendingLoansTable.setFillsViewportHeight(true);

        approveButton = new JButton("Approve Loan");
        rejectButton = new JButton("Reject Loan");
        
        // Initially disable buttons
        approveButton.setEnabled(false);
        rejectButton.setEnabled(false);

        // Apply consistent styles for main panel components
        pendingLoansTable.setBackground(FIELD_BG);
        pendingLoansTable.setForeground(TEXT_LIGHT);
        pendingLoansTable.setFont(LABEL_FONT);
        pendingLoansTable.setGridColor(BORDER_COLOR.darker()); // Darker grid lines
        pendingLoansTable.setSelectionBackground(ACCENT_BLUE.darker().darker()); // Darker selection
        pendingLoansTable.setSelectionForeground(Color.WHITE);

        // Table Header Styling
        // Changed header background to a lighter shade for better visibility
        pendingLoansTable.getTableHeader().setBackground(new Color(100, 100, 100)); 
        pendingLoansTable.getTableHeader().setForeground(new Color(255, 255, 255)); // White text for better contrast
        pendingLoansTable.getTableHeader().setFont(TABLE_HEADER_FONT);
        pendingLoansTable.getTableHeader().setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        // Custom Cell Renderer for rows to ensure consistent background
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(FIELD_BG);
                c.setForeground(Color.WHITE); 
                if (isSelected) {
                    c.setBackground(ACCENT_BLUE.darker().darker());
                }
                return c;
            }
        };
        for (int i = 0; i < pendingLoansTable.getColumnCount(); i++) {
            pendingLoansTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        approveButton.setBackground(ACCENT_BLUE);
        approveButton.setForeground(Color.BLACK);
        approveButton.setFont(BUTTON_FONT);
        approveButton.setFocusPainted(false);
        approveButton.setBorderPainted(false);
        approveButton.setOpaque(true);

        rejectButton.setBackground(new Color(180, 70, 70)); // Red for reject
        rejectButton.setForeground(Color.WHITE);
        rejectButton.setFont(BUTTON_FONT);
        rejectButton.setFocusPainted(false);
        rejectButton.setBorderPainted(false);
        rejectButton.setOpaque(true);


        // Recipient Account Dialog components styling
        recipientAccountDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Select Recipient Account", true);
        recipientAccountDialog.setSize(400, 150);
        recipientAccountDialog.setLocationRelativeTo(this);
        recipientAccountDialog.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        recipientAccountDialog.getContentPane().setBackground(BG_DARK); // Dialog background

        recipientAccountCombo = new JComboBox<>();
        recipientAccountCombo.setBackground(FIELD_BG);
        recipientAccountCombo.setForeground(TEXT_LIGHT);
        recipientAccountCombo.setFont(LABEL_FONT);
        recipientAccountCombo.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        confirmRecipientButton = new JButton("Confirm Deposit");
        confirmRecipientButton.setBackground(ACCENT_BLUE);
        confirmRecipientButton.setForeground(Color.WHITE);
        confirmRecipientButton.setFont(BUTTON_FONT);
        confirmRecipientButton.setFocusPainted(false);
        confirmRecipientButton.setBorderPainted(false);
        confirmRecipientButton.setOpaque(true);

        JLabel chooseAccountLabel = new JLabel("Choose account for deposit:");
        chooseAccountLabel.setForeground(TEXT_LIGHT);
        chooseAccountLabel.setFont(LABEL_FONT);
        recipientAccountDialog.add(chooseAccountLabel);
        recipientAccountDialog.add(recipientAccountCombo);
        recipientAccountDialog.add(confirmRecipientButton);
    }

    // Lays out components
    private void layoutComponents() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        buttonPanel.setBackground(BG_DARK);
        buttonPanel.add(approveButton);
        buttonPanel.add(rejectButton);

        JScrollPane scrollPane = new JScrollPane(pendingLoansTable);
        scrollPane.getViewport().setBackground(FIELD_BG); // Important for scroll pane background
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(BORDER_COLOR), "Pending Loan Applications",
                                            TitledBorder.LEFT, TitledBorder.TOP, TITLE_BORDER_FONT, TEXT_LIGHT),
            BorderFactory.createEmptyBorder(5,5,5,5)
        ));

        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    // Adds listeners
    private void addListeners() {
        approveButton.addActionListener(e -> openRecipientAccountSelection()); // Changed to open selection dialog
        rejectButton.addActionListener(e -> rejectLoan());

        pendingLoansTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = pendingLoansTable.getSelectedRow() != -1;
                approveButton.setEnabled(hasSelection);
                rejectButton.setEnabled(hasSelection);
            }
        });

        confirmRecipientButton.addActionListener(e -> approveLoan()); // This will now be called from the dialog
    }

    // Refreshes pending loans
    public void refreshPendingLoans() {
        pendingLoansTableModel.setRowCount(0); // Clear existing data

        if (currentUser == null || !currentUser.isAdmin()) {
            pendingLoansTableModel.addRow(new Object[]{"", "", "", "", "Admin login required.", ""});
            return;
        }

        try {
            List<LoanApplication> pendingLoans = bankingService.getPendingLoans();
            
            // Sort the loans by priority (using the existing comparator)
            Collections.sort(pendingLoans, new LoanPriorityComparator());

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            if (pendingLoans.isEmpty()) {
                pendingLoansTableModel.addRow(new Object[]{"", "", "", "", "No pending loan applications.", ""});
            } else {
                for (LoanApplication loan : pendingLoans) {
                    pendingLoansTableModel.addRow(new Object[]{
                        loan.getId().substring(0, 8),
                        loan.getUserId().substring(0, 8), // Display truncated User ID
                        String.format("$%.2f", loan.getAmount()),
                        loan.getApplicationDate().format(formatter),
                        loan.getReason(),
                        loan.getPriorityScore()
                    });
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading pending loans: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            pendingLoansTableModel.addRow(new Object[]{"", "", "", "", "Error loading loans.", ""});
        }
        // Disable buttons if no selection
        approveButton.setEnabled(false);
        rejectButton.setEnabled(false);
    }

    // New method to open recipient account selection dialog
    private void openRecipientAccountSelection() {
        int selectedRow = pendingLoansTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a loan to approve.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String loanIdDisplay = (String) pendingLoansTableModel.getValueAt(selectedRow, 0);
        selectedLoanUserId = (String) pendingLoansTableModel.getValueAt(selectedRow, 1); // Get truncated user ID

        LoanApplication selectedLoan = null;
        try {
            List<LoanApplication> currentPendingLoans = bankingService.getPendingLoans();
            for(LoanApplication loan : currentPendingLoans) {
                if (loan.getId().startsWith(loanIdDisplay)) { // Match by truncated ID
                    selectedLoan = loan;
                    break;
                }
            }
        } catch (Exception e) {
             JOptionPane.showMessageDialog(this, "Error finding loan: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); 
             return;
        }

        if (selectedLoan == null) {
            JOptionPane.showMessageDialog(this, "Selected loan not found in pending list.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Store the full userId of the selected loan for later validation
        final String fullLoanUserId = selectedLoan.getUserId();

        // Populate recipientAccountCombo with accounts belonging to the selected loan's user
        recipientAccountCombo.removeAllItems();
        accountIdMap.clear();

        try {
            List<Account> allAccounts = bankingService.getAllAccounts();
            // Filter accounts to show only those belonging to the loan applicant
            List<Account> userAccountsForLoan = allAccounts.stream()
                                                    .filter(acc -> acc.getUserId().equals(fullLoanUserId))
                                                    .collect(Collectors.toList());

            if (userAccountsForLoan.isEmpty()) {
                recipientAccountCombo.addItem("No accounts found for this user.");
                confirmRecipientButton.setEnabled(false);
            } else {
                for (Account acc : userAccountsForLoan) {
                    String display = acc.getType().name() + " (" + acc.getId().substring(0, 8) + ") - $" + acc.getBalance();
                    recipientAccountCombo.addItem(display);
                    accountIdMap.put(display, acc.getId()); // Store full ID
                }
                confirmRecipientButton.setEnabled(true);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading accounts for loan approval: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            recipientAccountCombo.addItem("Error loading accounts.");
            confirmRecipientButton.setEnabled(false);
        }

        // Set dialog location relative to the main window
        recipientAccountDialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        recipientAccountDialog.setVisible(true); // Show the modal dialog
    }

    // Approves a loan (now called after recipient account is selected)
    private void approveLoan() {
        int selectedRow = pendingLoansTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a loan to approve.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String loanIdDisplay = (String) pendingLoansTableModel.getValueAt(selectedRow, 0); 
        
        LoanApplication selectedLoan = null;
        try {
            List<LoanApplication> currentPendingLoans = bankingService.getPendingLoans();
            for(LoanApplication loan : currentPendingLoans) {
                if (loan.getId().startsWith(loanIdDisplay)) { 
                    selectedLoan = loan;
                    break;
                }
            }
        } catch (Exception e) {
             JOptionPane.showMessageDialog(this, "Error finding loan: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); 
             return;
        }

        if (selectedLoan == null) {
            JOptionPane.showMessageDialog(this, "Selected loan not found in pending list.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String selectedRecipientAccountDisplay = (String) recipientAccountCombo.getSelectedItem();
        if (selectedRecipientAccountDisplay == null || selectedRecipientAccountDisplay.startsWith("No accounts") || selectedRecipientAccountDisplay.startsWith("Error loading")) {
            JOptionPane.showMessageDialog(recipientAccountDialog, "Please select a valid recipient account.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String recipientAccountId = accountIdMap.get(selectedRecipientAccountDisplay);

        if (recipientAccountId == null) {
            JOptionPane.showMessageDialog(recipientAccountDialog, "Internal error: Could not retrieve full account ID.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            bankingService.approveLoan(selectedLoan.getId(), recipientAccountId);
            JOptionPane.showMessageDialog(this, "Loan approved successfully and deposited to account: " + selectedRecipientAccountDisplay + "!", "Success", JOptionPane.INFORMATION_MESSAGE);
            recipientAccountDialog.dispose(); // Close the selection dialog
            refreshPendingLoans(); // Refresh this panel's display
            if (refreshDashboardCallback != null) { // Call dashboard refresh to update balances, etc.
                refreshDashboardCallback.run();
            }
        } catch (IllegalArgumentException | IllegalStateException | SQLException ex) {
            JOptionPane.showMessageDialog(recipientAccountDialog, "Loan approval failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); 
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(recipientAccountDialog, "Unexpected error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Rejects a loan
    private void rejectLoan() {
        int selectedRow = pendingLoansTable.getSelectedRow();
        if (selectedRow == -1) return;

        String loanId = (String) pendingLoansTableModel.getValueAt(selectedRow, 0); // Get truncated ID
        LoanApplication selectedLoan = null;
        try {
            List<LoanApplication> currentPendingLoans = bankingService.getPendingLoans();
            for(LoanApplication loan : currentPendingLoans) {
                if (loan.getId().startsWith(loanId)) { // Match by truncated ID
                    selectedLoan = loan;
                    break;
                }
            }
        } catch (Exception e) {
             JOptionPane.showMessageDialog(this, "Error finding loan: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); 
             return;
        }

        if (selectedLoan == null) {
            JOptionPane.showMessageDialog(this, "Selected loan not found in pending list.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int response = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to reject loan " + selectedLoan.getId().substring(0,8) + " for user " + selectedLoan.getUserId().substring(0,8) + "?",
                "Confirm Rejection", JOptionPane.YES_NO_OPTION);

        if (response == JOptionPane.YES_OPTION) {
            try {
                bankingService.rejectLoan(selectedLoan.getId());
                JOptionPane.showMessageDialog(this, "Loan rejected successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshPendingLoans(); // Refresh this panel's display
            } catch (IllegalArgumentException | IllegalStateException | SQLException ex) {
                JOptionPane.showMessageDialog(this, "Loan rejection failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); 
            } catch (Exception ex) {
                 JOptionPane.showMessageDialog(this, "Unexpected error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}