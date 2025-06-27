// --- 16. gui/LoanApplicationPanel.java ---
package gui;

import model.LoanApplication;
import service.BankingService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer; // Added for table styling
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.lang.Runnable; 

// LoanApplicationPanel is the JPanel where users can apply for new loans
public class LoanApplicationPanel extends JPanel {
    private BankingService bankingService;
    private Runnable refreshDashboardCallback;
    private model.User currentUser;

    private JTextField amountField;
    private JTextArea reasonArea;
    private JSpinner prioritySpinner;
    private JButton applyButton;

    private DefaultTableModel userLoansTableModel;
    private JTable userLoansTable;

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
    public LoanApplicationPanel(BankingService bankingService, Runnable refreshDashboardCallback) { // Changed to Runnable
        this.bankingService = bankingService;
        this.refreshDashboardCallback = refreshDashboardCallback;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(BG_DARK); // Set panel background

        initComponents();
        layoutComponents();
        addListeners();
        // Initial call to refreshUserLoans() is still needed
        refreshUserLoans();
    }
    
    // Sets the current user and refreshes user's loans
    public void setCurrentUser(model.User user) {
        this.currentUser = user;
        refreshUserLoans();
    }

    // Initializes GUI components
    private void initComponents() {
        amountField = new JTextField(15);
        reasonArea = new JTextArea(4, 20);
        reasonArea.setLineWrap(true);
        reasonArea.setWrapStyleWord(true);
        prioritySpinner = new JSpinner(new SpinnerNumberModel(5, 1, 10, 1)); // Priority 1 (highest) to 10 (lowest)
        applyButton = new JButton("Apply for Loan");

        userLoansTableModel = new DefaultTableModel(new Object[]{"ID", "Amount", "Date", "Status", "Priority"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        userLoansTable = new JTable(userLoansTableModel);
        userLoansTable.setFillsViewportHeight(true); // Fill the viewport height

        // Apply consistent styles
        amountField.setBackground(FIELD_BG);
        amountField.setForeground(TEXT_LIGHT);
        amountField.setCaretColor(TEXT_LIGHT);
        amountField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        amountField.setFont(LABEL_FONT);

        reasonArea.setBackground(FIELD_BG);
        reasonArea.setForeground(TEXT_LIGHT);
        reasonArea.setCaretColor(TEXT_LIGHT);
        reasonArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        reasonArea.setFont(LABEL_FONT);

        // Styling for JSpinner - explicitly setting colors for visibility
        JFormattedTextField spinnerTextField = ((JSpinner.DefaultEditor) prioritySpinner.getEditor()).getTextField();
        spinnerTextField.setBackground(Color.WHITE); // Set a clear background for the spinner text field
        spinnerTextField.setForeground(Color.BLACK); // Set black text for maximum contrast
        spinnerTextField.setCaretColor(Color.BLACK); // Make caret visible
        spinnerTextField.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        spinnerTextField.setFont(LABEL_FONT);
        prioritySpinner.setBorder(BorderFactory.createLineBorder(BORDER_COLOR)); // Overall spinner border

        applyButton.setBackground(ACCENT_BLUE);
        applyButton.setForeground(Color.WHITE);
        applyButton.setFont(BUTTON_FONT);
        applyButton.setFocusPainted(false);
        applyButton.setBorderPainted(false);
        applyButton.setOpaque(true);

        userLoansTable.setBackground(FIELD_BG);
        userLoansTable.setForeground(TEXT_LIGHT);
        userLoansTable.setFont(LABEL_FONT);
        userLoansTable.setGridColor(BORDER_COLOR.darker()); // Darker grid lines
        userLoansTable.setSelectionBackground(ACCENT_BLUE.darker().darker()); // Darker selection
        userLoansTable.setSelectionForeground(Color.WHITE);

        // Table Header Styling
        userLoansTable.getTableHeader().setBackground(new Color(60,60,60));
        userLoansTable.getTableHeader().setForeground(Color.BLACK); // Set header text to WHITE
        userLoansTable.getTableHeader().setFont(TABLE_HEADER_FONT);
        userLoansTable.getTableHeader().setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        // Custom Cell Renderer for rows to ensure consistent background
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(FIELD_BG);
                c.setForeground(TEXT_LIGHT);
                if (isSelected) {
                    c.setBackground(ACCENT_BLUE.darker().darker());
                }
                return c;
            }
        };
        for (int i = 0; i < userLoansTable.getColumnCount(); i++) {
            userLoansTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
    }

    // Lays out components
    private void layoutComponents() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(BG_DARK);
        formPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR), "New Loan Application",
            TitledBorder.LEFT, TitledBorder.TOP, TITLE_BORDER_FONT, TEXT_LIGHT
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        JLabel amountLabel = new JLabel("Loan Amount:");
        amountLabel.setForeground(TEXT_LIGHT);
        amountLabel.setFont(LABEL_FONT);
        gbc.gridx = 0; gbc.gridy = row++;
        formPanel.add(amountLabel, gbc);
        gbc.gridx = 1; gbc.gridy = row - 1; gbc.weightx = 1.0;
        formPanel.add(amountField, gbc);

        JLabel reasonLabel = new JLabel("Reason:");
        reasonLabel.setForeground(TEXT_LIGHT);
        reasonLabel.setFont(LABEL_FONT);
        gbc.gridx = 0; gbc.gridy = row++; gbc.anchor = GridBagConstraints.NORTHEAST;
        formPanel.add(reasonLabel, gbc);
        gbc.gridx = 1; gbc.gridy = row - 1; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        JScrollPane reasonScrollPane = new JScrollPane(reasonArea);
        reasonScrollPane.getViewport().setBackground(FIELD_BG);
        reasonScrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        formPanel.add(reasonScrollPane, gbc);

        JLabel priorityLabel = new JLabel("Priority (1-10, 1=Highest):");
        priorityLabel.setForeground(TEXT_LIGHT);
        priorityLabel.setFont(LABEL_FONT);
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weighty = 0.0; gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(priorityLabel, gbc);
        gbc.gridx = 1; gbc.gridy = row - 1;
        formPanel.add(prioritySpinner, gbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(applyButton, gbc);

        add(formPanel, BorderLayout.NORTH);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(BG_DARK);
        tablePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR), "Your Loan History",
            TitledBorder.LEFT, TitledBorder.TOP, TITLE_BORDER_FONT, TEXT_LIGHT
        ));
        JScrollPane loansScrollPane = new JScrollPane(userLoansTable);
        loansScrollPane.getViewport().setBackground(FIELD_BG);
        loansScrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        tablePanel.add(loansScrollPane, BorderLayout.CENTER);
        add(tablePanel, BorderLayout.CENTER);
    }

    // Adds listeners
    private void addListeners() {
        applyButton.addActionListener(e -> applyForLoan());
    }

    // Refreshes user's loans
    public void refreshUserLoans() {
        userLoansTableModel.setRowCount(0); // Clear existing data

        if (currentUser == null) {
            userLoansTableModel.addRow(new Object[]{"", "", "", "Please log in to view loans.", ""});
            return;
        }

        try {
            List<LoanApplication> loans = bankingService.getLoansByUserId(currentUser.getId());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            if (loans.isEmpty()) {
                userLoansTableModel.addRow(new Object[]{"", "", "", "No loan applications found.", ""});
            } else {
                for (LoanApplication loan : loans) {
                    userLoansTableModel.addRow(new Object[]{
                        loan.getId().substring(0, 8),
                        String.format("$%.2f", loan.getAmount()),
                        loan.getApplicationDate().format(formatter),
                        loan.getStatus().name(),
                        loan.getPriorityScore()
                    });
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading your loans: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            userLoansTableModel.addRow(new Object[]{"", "", "", "Error loading loans.", ""});
        }
        // Removed: refreshDashboardCallback.run(); // This caused the StackOverflowError
    }

    // Applies for a loan
    private void applyForLoan() {
        try {
            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            String reason = reasonArea.getText().trim();
            int priority = (Integer) prioritySpinner.getValue();

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                JOptionPane.showMessageDialog(this, "Loan amount must be positive.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (reason.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Reason for loan is required.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            bankingService.applyForLoan(amount, reason, priority);
            JOptionPane.showMessageDialog(this, "Loan application submitted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            amountField.setText("");
            reasonArea.setText("");
            prioritySpinner.setValue(5); // Reset to default
            refreshUserLoans(); // Refresh this panel's display
            if (refreshDashboardCallback != null) { // Call dashboard refresh to update counts, etc.
                refreshDashboardCallback.run();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid amount format.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException | SQLException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, "Loan application failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unexpected error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}