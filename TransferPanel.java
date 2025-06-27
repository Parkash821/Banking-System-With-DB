// --- 15. gui/TransferPanel.java ---
package gui;

import model.Account;
import service.BankingService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import java.lang.Runnable;
import java.sql.SQLException; // Added import for SQLException
import java.util.HashMap; // Added import
import java.util.Map; // Added import

// TransferPanel is a JPanel for facilitating fund transfers
public class TransferPanel extends JPanel {
    private BankingService bankingService;
    private Runnable refreshDashboardCallback;
    private model.User currentUser;

    private JComboBox<String> fromAccountCombo;
    private JComboBox<String> toAccountCombo; // Changed from JTextField to JComboBox
    private JTextField amountField;
    private JButton transferButton;

    // Map to store full account IDs for retrieval from JComboBox selection
    private Map<String, String> accountIdMap;

    // Define consistent colors and fonts
    private static final Color BG_DARK = new Color(45, 45, 45);
    private static final Color TEXT_LIGHT = new Color(230, 230, 230);
    private static final Color ACCENT_BLUE = new Color(70, 130, 180);
    private static final Color FIELD_BG = new Color(60, 60, 60);
    private static final Color BORDER_COLOR = new Color(90, 90, 90);
    private static final Font LABEL_FONT = new Font("Arial", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 14);
    private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 18);


    // Constructor
    public TransferPanel(BankingService bankingService, Runnable refreshDashboardCallback) { // Changed to Runnable
        this.bankingService = bankingService;
        this.refreshDashboardCallback = refreshDashboardCallback;
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(BG_DARK); // Set panel background

        this.accountIdMap = new HashMap<>(); // Initialize the map

        initComponents();
        layoutComponents();
        addListeners();
        // Initial call to refreshAccounts() is still needed
        refreshAccounts();
    }
    
    // Sets the current user and refreshes accounts
    public void setCurrentUser(model.User user) {
        this.currentUser = user;
        refreshAccounts();
    }

    // Initializes GUI components
    private void initComponents() {
        fromAccountCombo = new JComboBox<>();
        toAccountCombo = new JComboBox<>(); // Changed to JComboBox
        amountField = new JTextField(15);
        transferButton = new JButton("Transfer Funds");

        // Apply consistent styles
        fromAccountCombo.setBackground(FIELD_BG);
        fromAccountCombo.setForeground(TEXT_LIGHT);
        fromAccountCombo.setFont(LABEL_FONT);
        fromAccountCombo.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        toAccountCombo.setBackground(FIELD_BG);
        toAccountCombo.setForeground(TEXT_LIGHT);
        toAccountCombo.setFont(LABEL_FONT);
        toAccountCombo.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        amountField.setBackground(FIELD_BG);
        amountField.setForeground(TEXT_LIGHT);
        amountField.setCaretColor(TEXT_LIGHT);
        amountField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        amountField.setFont(LABEL_FONT);

        transferButton.setBackground(ACCENT_BLUE);
        transferButton.setForeground(Color.WHITE);
        transferButton.setFont(BUTTON_FONT);
        transferButton.setFocusPainted(false);
        transferButton.setBorderPainted(false);
        transferButton.setOpaque(true);
    }

    // Lays out components
    private void layoutComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("Fund Transfer", SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(ACCENT_BLUE);
        add(titleLabel, gbc);

        JLabel fromAccLabel = new JLabel("From Account:");
        fromAccLabel.setForeground(TEXT_LIGHT);
        fromAccLabel.setFont(LABEL_FONT);
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 1;
        add(fromAccLabel, gbc);
        gbc.gridx = 1; gbc.gridy = row - 1; gbc.weightx = 1.0;
        add(fromAccountCombo, gbc);

        JLabel toAccLabel = new JLabel("To Account:");
        toAccLabel.setForeground(TEXT_LIGHT);
        toAccLabel.setFont(LABEL_FONT);
        gbc.gridx = 0; gbc.gridy = row++;
        add(toAccLabel, gbc); // Label changed
        gbc.gridx = 1; gbc.gridy = row - 1;
        add(toAccountCombo, gbc); // Changed to JComboBox

        JLabel amountLabel = new JLabel("Amount:");
        amountLabel.setForeground(TEXT_LIGHT);
        amountLabel.setFont(LABEL_FONT);
        gbc.gridx = 0; gbc.gridy = row++;
        add(amountLabel, gbc);
        gbc.gridx = 1; gbc.gridy = row - 1;
        add(amountField, gbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        add(transferButton, gbc);
    }

    // Adds listeners
    private void addListeners() {
        transferButton.addActionListener(e -> performTransfer());
    }

    // Refreshes accounts in both combo boxes
    public void refreshAccounts() {
        fromAccountCombo.removeAllItems();
        toAccountCombo.removeAllItems(); // Clear toAccountCombo as well
        accountIdMap.clear(); // Clear the map

        if (currentUser == null) {
            fromAccountCombo.addItem("Please log in.");
            toAccountCombo.addItem("Please log in.");
            transferButton.setEnabled(false);
            return;
        }

        try {
            // Populate 'From Account' combo box with current user's accounts
            List<Account> userAccounts = bankingService.getUserAccounts();
            if (userAccounts.isEmpty()) {
                fromAccountCombo.addItem("No accounts to transfer from");
                transferButton.setEnabled(false);
            } else {
                for (Account acc : userAccounts) {
                    String display = acc.getType().name() + " (" + acc.getId().substring(0, 8) + ") - $" + acc.getBalance();
                    fromAccountCombo.addItem(display);
                    accountIdMap.put(display, acc.getId()); // Store full ID
                }
                transferButton.setEnabled(true);
            }

            // Populate 'To Account' combo box with ALL accounts
            List<Account> allAccounts = bankingService.getAllAccounts();
            if (allAccounts.isEmpty()) {
                toAccountCombo.addItem("No accounts available");
            } else {
                for (Account acc : allAccounts) {
                    String display = acc.getType().name() + " (" + acc.getId().substring(0, 8) + ") - User ID: " + acc.getUserId().substring(0,8);
                    toAccountCombo.addItem(display);
                    accountIdMap.put(display, acc.getId()); // Store full ID
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading accounts for transfer: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            fromAccountCombo.addItem("Error loading accounts");
            toAccountCombo.addItem("Error loading accounts");
            transferButton.setEnabled(false);
        }
    }

    // Performs the transfer
    private void performTransfer() {
        int fromSelectedIndex = fromAccountCombo.getSelectedIndex();
        int toSelectedIndex = toAccountCombo.getSelectedIndex();

        if (fromSelectedIndex == -1 || fromAccountCombo.getSelectedItem().toString().startsWith("No accounts") || fromAccountCombo.getSelectedItem().toString().startsWith("Error loading")) {
            JOptionPane.showMessageDialog(this, "Please select a source account.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (toSelectedIndex == -1 || toAccountCombo.getSelectedItem().toString().startsWith("No accounts") || toAccountCombo.getSelectedItem().toString().startsWith("Error loading")) {
            JOptionPane.showMessageDialog(this, "Please select a destination account.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Retrieve full account IDs using the map
            String fromAccountDisplay = fromAccountCombo.getSelectedItem().toString();
            String fromAccountId = accountIdMap.get(fromAccountDisplay);

            String toAccountDisplay = toAccountCombo.getSelectedItem().toString();
            String toAccountId = accountIdMap.get(toAccountDisplay);

            if (fromAccountId == null || toAccountId == null) {
                JOptionPane.showMessageDialog(this, "Could not retrieve full account IDs. Please refresh and try again.", "Internal Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            BigDecimal amount = new BigDecimal(amountField.getText().trim());

            bankingService.transferFunds(fromAccountId, toAccountId, amount);
            JOptionPane.showMessageDialog(this, String.format("Successfully transferred $%.2f from %s to %s.", amount, fromAccountDisplay, toAccountDisplay), "Transfer Success", JOptionPane.INFORMATION_MESSAGE);
            
            amountField.setText(""); // Clear fields
            if (refreshDashboardCallback != null) { // Call dashboard refresh to update balance, etc.
                refreshDashboardCallback.run(); 
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid amount format.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException | IllegalStateException | SQLException ex) {
            JOptionPane.showMessageDialog(this, "Transfer failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
             JOptionPane.showMessageDialog(this, "Unexpected error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}