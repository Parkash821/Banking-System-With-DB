// --- 14. gui/AccountPanel.java ---
package gui;

import model.Account;
import service.BankingService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder; // Added for titled borders
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.lang.Runnable; 

// AccountPanel is a JPanel for creating new accounts and performing deposit/withdrawal operations
public class AccountPanel extends JPanel {
    private BankingService bankingService;
    private Runnable refreshDashboardCallback; // Callback to refresh the main frame
    private model.User currentUser; // Store current user for contextual operations

    private JComboBox<String> accountTypeCreateCombo;
    private JButton createAccountButton;

    private JComboBox<String> accountSelectDepositWithdrawCombo;
    private JTextField amountField;
    private JButton depositButton, withdrawButton;

    private DefaultListModel<String> userAccountsModel;
    private JList<String> userAccountsList;

    // Define consistent colors and fonts
    private static final Color BG_DARK = new Color(45, 45, 45);
    private static final Color TEXT_LIGHT = new Color(230, 230, 230);
    private static final Color ACCENT_BLUE = new Color(70, 130, 180);
    private static final Color FIELD_BG = new Color(60, 60, 60);
    private static final Color BORDER_COLOR = new Color(90, 90, 90);
    private static final Font LABEL_FONT = new Font("Arial", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 14);
    private static final Font TITLE_BORDER_FONT = new Font("Arial", Font.BOLD, 16);


    // Constructor
    public AccountPanel(BankingService bankingService, Runnable refreshDashboardCallback) { // Changed to Runnable
        this.bankingService = bankingService;
        this.refreshDashboardCallback = refreshDashboardCallback;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(BG_DARK); // Set panel background

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
        accountTypeCreateCombo = new JComboBox<>(AccountTypeEnumHelper.valuesToStrings()); // Correct helper class name
        createAccountButton = new JButton("Create New Account");

        accountSelectDepositWithdrawCombo = new JComboBox<>();
        amountField = new JTextField(15);
        depositButton = new JButton("Deposit");
        withdrawButton = new JButton("Withdraw");

        userAccountsModel = new DefaultListModel<>();
        userAccountsList = new JList<>(userAccountsModel);

        // Apply consistent styles
        accountTypeCreateCombo.setBackground(FIELD_BG);
        accountTypeCreateCombo.setForeground(TEXT_LIGHT);
        accountTypeCreateCombo.setFont(LABEL_FONT);
        accountTypeCreateCombo.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        createAccountButton.setBackground(ACCENT_BLUE);
        createAccountButton.setForeground(Color.WHITE);
        createAccountButton.setFont(BUTTON_FONT);
        createAccountButton.setFocusPainted(false);
        createAccountButton.setBorderPainted(false);
        createAccountButton.setOpaque(true);

        accountSelectDepositWithdrawCombo.setBackground(FIELD_BG);
        accountSelectDepositWithdrawCombo.setForeground(TEXT_LIGHT);
        accountSelectDepositWithdrawCombo.setFont(LABEL_FONT);
        accountSelectDepositWithdrawCombo.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        amountField.setBackground(FIELD_BG);
        amountField.setForeground(TEXT_LIGHT);
        amountField.setCaretColor(TEXT_LIGHT);
        amountField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        amountField.setFont(LABEL_FONT);

        depositButton.setBackground(ACCENT_BLUE);
        depositButton.setForeground(Color.WHITE);
        depositButton.setFont(BUTTON_FONT);
        depositButton.setFocusPainted(false);
        depositButton.setBorderPainted(false);
        depositButton.setOpaque(true);

        withdrawButton.setBackground(ACCENT_BLUE);
        withdrawButton.setForeground(Color.WHITE);
        withdrawButton.setFont(BUTTON_FONT);
        withdrawButton.setFocusPainted(false);
        withdrawButton.setBorderPainted(false);
        withdrawButton.setOpaque(true);

        userAccountsList.setBackground(FIELD_BG);
        userAccountsList.setForeground(TEXT_LIGHT);
        userAccountsList.setFont(LABEL_FONT);
        userAccountsList.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        userAccountsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(FIELD_BG);
                setForeground(TEXT_LIGHT);
                if (isSelected) {
                    setBackground(ACCENT_BLUE.darker());
                }
                return this;
            }
        });
    }

    // Lays out components
    private void layoutComponents() {
        // --- Create Account Panel ---
        JPanel createAccountPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        createAccountPanel.setBackground(BG_DARK);
        createAccountPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR), "Create New Account",
            TitledBorder.LEFT, TitledBorder.TOP, TITLE_BORDER_FONT, TEXT_LIGHT
        ));
        JLabel typeLabel = new JLabel("Account Type:");
        typeLabel.setForeground(TEXT_LIGHT);
        typeLabel.setFont(LABEL_FONT);
        createAccountPanel.add(typeLabel);
        createAccountPanel.add(accountTypeCreateCombo);
        createAccountPanel.add(createAccountButton);

        // --- Deposit/Withdraw Panel ---
        JPanel depositWithdrawPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        depositWithdrawPanel.setBackground(BG_DARK);
        depositWithdrawPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR), "Deposit / Withdraw Funds",
            TitledBorder.LEFT, TitledBorder.TOP, TITLE_BORDER_FONT, TEXT_LIGHT
        ));
        JLabel selectAccDWLabel = new JLabel("Select Account:");
        selectAccDWLabel.setForeground(TEXT_LIGHT);
        selectAccDWLabel.setFont(LABEL_FONT);
        depositWithdrawPanel.add(selectAccDWLabel);
        depositWithdrawPanel.add(accountSelectDepositWithdrawCombo);
        JLabel amountLabel = new JLabel("Amount:");
        amountLabel.setForeground(TEXT_LIGHT);
        amountLabel.setFont(LABEL_FONT);
        depositWithdrawPanel.add(amountLabel);
        depositWithdrawPanel.add(amountField);
        depositWithdrawPanel.add(depositButton);
        depositWithdrawPanel.add(withdrawButton);

        // --- All Accounts Display Panel ---
        JPanel accountsDisplayPanel = new JPanel(new BorderLayout());
        accountsDisplayPanel.setBackground(BG_DARK);
        accountsDisplayPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR), "Your Accounts",
            TitledBorder.LEFT, TitledBorder.TOP, TITLE_BORDER_FONT, TEXT_LIGHT
        ));
        JScrollPane scrollPane = new JScrollPane(userAccountsList);
        scrollPane.getViewport().setBackground(FIELD_BG);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR)); // Inner border for the list itself
        accountsDisplayPanel.add(scrollPane, BorderLayout.CENTER);

        // Combine panels
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        topPanel.setBackground(BG_DARK);
        topPanel.add(createAccountPanel);
        topPanel.add(depositWithdrawPanel);

        add(topPanel, BorderLayout.NORTH);
        add(accountsDisplayPanel, BorderLayout.CENTER);
    }

    // Adds listeners
    private void addListeners() {
        createAccountButton.addActionListener(e -> createAccount());
        depositButton.addActionListener(e -> handleTransaction(true)); // true for deposit
        withdrawButton.addActionListener(e -> handleTransaction(false)); // false for withdraw
    }

    // Refreshes accounts
    public void refreshAccounts() {
        userAccountsModel.clear();
        accountSelectDepositWithdrawCombo.removeAllItems();

        if (currentUser == null) {
            userAccountsModel.addElement("Please log in to view accounts.");
            return;
        }

        try {
            List<Account> accounts = bankingService.getUserAccounts();
            if (accounts.isEmpty()) {
                userAccountsModel.addElement("No accounts created yet.");
                accountSelectDepositWithdrawCombo.addItem("No accounts available");
            } else {
                for (Account acc : accounts) {
                    userAccountsModel.addElement(String.format("%s Account (ID: %s): $%.2f", acc.getType().name(), acc.getId().substring(0, 8), acc.getBalance()));
                    accountSelectDepositWithdrawCombo.addItem(acc.getType().name() + " (" + acc.getId().substring(0, 8) + ")");
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading accounts: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            userAccountsModel.addElement("Error loading accounts.");
            accountSelectDepositWithdrawCombo.addItem("Error loading accounts");
        }
        // Removed: refreshDashboardCallback.run(); // This caused the StackOverflowError
    }

    // Creates an account
    private void createAccount() {
        try {
            // Corrected: Get the selected string and convert to enum
            String selectedTypeString = (String) accountTypeCreateCombo.getSelectedItem();
            Account.AccountType selectedType = Account.AccountType.valueOf(selectedTypeString);

            // Initial balance for new account
            String initialBalanceStr = JOptionPane.showInputDialog(this, "Enter initial balance for " + selectedType.name() + " account:", "Initial Balance", JOptionPane.QUESTION_MESSAGE);
            if (initialBalanceStr == null || initialBalanceStr.trim().isEmpty()) {
                return; // User cancelled
            }
            BigDecimal initialBalance = new BigDecimal(initialBalanceStr);
            if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
                 JOptionPane.showMessageDialog(this, "Initial balance cannot be negative.", "Input Error", JOptionPane.ERROR_MESSAGE);
                 return;
            }

            bankingService.createAccount(selectedType, initialBalance);
            JOptionPane.showMessageDialog(this, selectedType.name() + " account created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshAccounts(); // Refresh this panel's accounts display
            if (refreshDashboardCallback != null) { // Call dashboard refresh to update balance, etc.
                refreshDashboardCallback.run();
            }
        }
        catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid amount format.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
        catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error creating account: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Handles transactions (deposit/withdrawal)
    private void handleTransaction(boolean isDeposit) {
        int selectedIndex = accountSelectDepositWithdrawCombo.getSelectedIndex();
        if (selectedIndex == -1 || accountSelectDepositWithdrawCombo.getSelectedItem().equals("No accounts available") || accountSelectDepositWithdrawCombo.getSelectedItem().equals("Error loading accounts")) {
            JOptionPane.showMessageDialog(this, "Please select an account first.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            List<Account> accounts = bankingService.getUserAccounts();
            Account selectedAccount = accounts.get(selectedIndex);
            String amountStr = amountField.getText().trim();
            BigDecimal amount = new BigDecimal(amountStr);

            if (isDeposit) {
                bankingService.deposit(selectedAccount.getId(), amount);
                JOptionPane.showMessageDialog(this, String.format("Deposited $%.2f to %s account.", amount, selectedAccount.getType().name()), "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                bankingService.withdraw(selectedAccount.getId(), amount);
                JOptionPane.showMessageDialog(this, String.format("Withdrew $%.2f from %s account.", amount, selectedAccount.getType().name()), "Success", JOptionPane.INFORMATION_MESSAGE);
            }
            amountField.setText(""); // Clear field
            refreshAccounts(); // Refresh this panel's accounts display
            if (refreshDashboardCallback != null) { // Call dashboard refresh to update balance, etc.
                refreshDashboardCallback.run();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid amount format.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException | SQLException ex) {
            JOptionPane.showMessageDialog(this, "Transaction failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
             JOptionPane.showMessageDialog(this, "Unexpected error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

// Helper class to get a String array from enum values (for JComboBox)
class AccountTypeEnumHelper {
    public static String[] valuesToStrings() {
        Account.AccountType[] types = Account.AccountType.values();
        String[] names = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            names[i] = types[i].name();
        }
        return names;
    }
}
