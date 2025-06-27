// --- 11. gui/LoginFrame.java ---
package gui;

import model.User;
import service.BankingService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

// LoginFrame is the initial window for user login and registration
public class LoginFrame extends JFrame {
    private BankingService bankingService;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton, registerButton;
    private MainFrame mainFrame; // Reference to the main application frame

    // Define consistent colors and fonts
    private static final Color BG_DARK = new Color(45, 45, 45); // Dark gray background
    private static final Color TEXT_LIGHT = new Color(230, 230, 230); // Light gray text
    private static final Color ACCENT_BLUE = new Color(70, 130, 180); // Steel blue for buttons
    private static final Color HOVER_BLUE = new Color(90, 150, 200); // Lighter blue on hover
    private static final Color FIELD_BG = new Color(60, 60, 60); // Slightly lighter gray for fields
    private static final Font LABEL_FONT = new Font("Arial", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 14);
    private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 20);

    // Constructor
    public LoginFrame(BankingService bankingService) {
        this.bankingService = bankingService;
        setTitle("SecureBank - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 250);
        setLocationRelativeTo(null); // Center the window
        getContentPane().setBackground(BG_DARK); // Set frame background

        initComponents();
        layoutComponents();
        addListeners();
    }

    // Initializes GUI components
    private void initComponents() {
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        loginButton = new JButton("Login");
        registerButton = new JButton("Register");

        // Apply styles
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

        loginButton.setBackground(ACCENT_BLUE);
        loginButton.setForeground(Color.WHITE);
        loginButton.setFont(BUTTON_FONT);
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        loginButton.setOpaque(true);

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
        panel.setBackground(BG_DARK); // Set panel background
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Welcome to SecureBank", SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(ACCENT_BLUE); // Title color
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setForeground(TEXT_LIGHT);
        usernameLabel.setFont(LABEL_FONT);
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.EAST;
        panel.add(usernameLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        panel.add(usernameField, gbc);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setForeground(TEXT_LIGHT);
        passwordLabel.setFont(LABEL_FONT);
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
        panel.add(passwordLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        panel.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(BG_DARK); // Set button panel background
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        panel.add(buttonPanel, gbc);

        add(panel, BorderLayout.CENTER);
    }

    // Adds listeners
    private void addListeners() {
        loginButton.addActionListener(e -> attemptLogin());
        registerButton.addActionListener(e -> openRegisterDialog());

        // Allow pressing Enter in password field to login
        passwordField.addActionListener(e -> attemptLogin());
    }

    // Attempts login
    private void attemptLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        try {
            User user = bankingService.loginUser(username, password);
            JOptionPane.showMessageDialog(this, "Login successful for " + user.getUsername(), "Success", JOptionPane.INFORMATION_MESSAGE);
            
            // Open main application frame
            if (mainFrame == null) {
                mainFrame = new MainFrame(bankingService); // Pass the service instance
            }
            mainFrame.updateDashboard(); // Ensure dashboard reflects logged-in user
            mainFrame.setVisible(true);
            this.dispose(); // Close login frame
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Login Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Opens the registration dialog
    private void openRegisterDialog() {
        RegisterDialog dialog = new RegisterDialog(this, bankingService);
        dialog.setVisible(true);
    }

    // For RegisterDialog to notify LoginFrame after successful registration
    public void notifyRegistrationSuccess(String username) {
        usernameField.setText(username);
        passwordField.setText("");
        JOptionPane.showMessageDialog(this, "Registration successful for " + username + ". Please login.", "Registration Success", JOptionPane.INFORMATION_MESSAGE);
    }
}
