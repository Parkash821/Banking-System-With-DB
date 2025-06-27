// --- 12. gui/RegisterDialog.java ---
package gui;

import model.User;
import service.BankingService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;

// RegisterDialog is a dialog for registering a new user
public class RegisterDialog extends JDialog {
    private BankingService bankingService;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JTextField fullNameField;
    private JCheckBox isAdminCheckBox;
    private JButton registerButton;
    private LoginFrame parentFrame;

    // Define consistent colors and fonts (similar to LoginFrame)
    private static final Color BG_DARK = new Color(45, 45, 45); // Dark gray background
    private static final Color TEXT_LIGHT = new Color(230, 230, 230); // Light gray text
    private static final Color ACCENT_BLUE = new Color(70, 130, 180); // Steel blue for buttons
    private static final Color FIELD_BG = new Color(60, 60, 60); // Slightly lighter gray for fields
    private static final Font LABEL_FONT = new Font("Arial", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 14);
    private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 18);

    // Constructor
    public RegisterDialog(LoginFrame parent, BankingService bankingService) {
        super(parent, "Register New User", true); // Modal dialog
        this.parentFrame = parent;
        this.bankingService = bankingService;
        setSize(400, 350);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(BG_DARK); // Set dialog background

        initComponents();
        layoutComponents();
        addListeners();
    }

    // Initializes GUI components
    private void initComponents() {
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        confirmPasswordField = new JPasswordField(20);
        fullNameField = new JTextField(20);
        isAdminCheckBox = new JCheckBox("Register as Admin (first user should be admin)");
        registerButton = new JButton("Register");

        // Apply styles
        fullNameField.setBackground(FIELD_BG);
        fullNameField.setForeground(TEXT_LIGHT);
        fullNameField.setCaretColor(TEXT_LIGHT);
        fullNameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(90,90,90), 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        fullNameField.setFont(LABEL_FONT);

        usernameField.setBackground(FIELD_BG);
        usernameField.setForeground(TEXT_LIGHT);
        usernameField.setCaretColor(TEXT_LIGHT);
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(90,90,90), 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        usernameField.setFont(LABEL_FONT);

        passwordField.setBackground(FIELD_BG);
        passwordField.setForeground(TEXT_LIGHT);
        passwordField.setCaretColor(TEXT_LIGHT);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(90,90,90), 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        passwordField.setFont(LABEL_FONT);

        confirmPasswordField.setBackground(FIELD_BG);
        confirmPasswordField.setForeground(TEXT_LIGHT);
        confirmPasswordField.setCaretColor(TEXT_LIGHT);
        confirmPasswordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(90,90,90), 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        confirmPasswordField.setFont(LABEL_FONT);

        isAdminCheckBox.setBackground(BG_DARK);
        isAdminCheckBox.setForeground(TEXT_LIGHT);
        isAdminCheckBox.setFont(LABEL_FONT);

        registerButton.setBackground(ACCENT_BLUE);
        registerButton.setForeground(Color.WHITE);
        registerButton.setFont(BUTTON_FONT);
        registerButton.setFocusPainted(false);
        registerButton.setBorderPainted(false);
        registerButton.setOpaque(true);
    }

    // Lays out components
    private void layoutComponents() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        JLabel fullNameLabel = new JLabel("Full Name:");
        fullNameLabel.setForeground(TEXT_LIGHT);
        fullNameLabel.setFont(LABEL_FONT);
        gbc.gridx = 0; gbc.gridy = row++; gbc.anchor = GridBagConstraints.EAST;
        panel.add(fullNameLabel, gbc);
        gbc.gridx = 1; gbc.gridy = row - 1; gbc.weightx = 1.0;
        panel.add(fullNameField, gbc);

        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setForeground(TEXT_LIGHT);
        usernameLabel.setFont(LABEL_FONT);
        gbc.gridx = 0; gbc.gridy = row++; gbc.anchor = GridBagConstraints.EAST;
        panel.add(usernameLabel, gbc);
        gbc.gridx = 1; gbc.gridy = row - 1;
        panel.add(usernameField, gbc);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setForeground(TEXT_LIGHT);
        passwordLabel.setFont(LABEL_FONT);
        gbc.gridx = 0; gbc.gridy = row++; gbc.anchor = GridBagConstraints.EAST;
        panel.add(passwordLabel, gbc);
        gbc.gridx = 1; gbc.gridy = row - 1;
        panel.add(passwordField, gbc);

        JLabel confirmPasswordLabel = new JLabel("Confirm Password:");
        confirmPasswordLabel.setForeground(TEXT_LIGHT);
        confirmPasswordLabel.setFont(LABEL_FONT);
        gbc.gridx = 0; gbc.gridy = row++; gbc.anchor = GridBagConstraints.EAST;
        panel.add(confirmPasswordLabel, gbc);
        gbc.gridx = 1; gbc.gridy = row - 1;
        panel.add(confirmPasswordField, gbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.WEST;
        panel.add(isAdminCheckBox, gbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        panel.add(registerButton, gbc);

        add(panel, BorderLayout.CENTER);
    }

    // Adds listeners
    private void addListeners() {
        registerButton.addActionListener(e -> attemptRegistration());
    }

    // Attempts registration
    private void attemptRegistration() {
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        boolean isAdmin = isAdminCheckBox.isSelected();

        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            User newUser = bankingService.registerUser(username, password, fullName, isAdmin);
            parentFrame.notifyRegistrationSuccess(newUser.getUsername());
            dispose(); // Close dialog on success
        } catch (IllegalArgumentException | SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Registration Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}