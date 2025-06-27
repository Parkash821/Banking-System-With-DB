// --- 18. gui/TransactionGraphPanel.java ---
package gui;

import service.BankingService;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.lang.Runnable; 

// TransactionGraphPanel displays the representation of the transaction graph
public class TransactionGraphPanel extends JPanel {
    private BankingService bankingService;
    private Runnable refreshDashboardCallback;
    private User currentUser;

    private JTextArea graphDisplayArea;
    private JButton refreshGraphButton;

    // Define consistent colors and fonts
    private static final Color BG_DARK = new Color(45, 45, 45);
    private static final Color TEXT_LIGHT = new Color(230, 230, 230);
    private static final Color ACCENT_BLUE = new Color(70, 130, 180);
    private static final Color FIELD_BG = new Color(60, 60, 60);
    private static final Color BORDER_COLOR = new Color(90, 90, 90);
    private static final Font LABEL_FONT = new Font("Arial", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 14);
    private static final Font TITLE_BORDER_FONT = new Font("Arial", Font.BOLD, 16);
    private static final Font GRAPH_TEXT_FONT = new Font("Monospaced", Font.PLAIN, 12);


    // Constructor
    public TransactionGraphPanel(BankingService bankingService, Runnable refreshDashboardCallback) { // Changed to Runnable
        this.bankingService = bankingService;
        this.refreshDashboardCallback = refreshDashboardCallback;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(BG_DARK); // Set panel background

        initComponents();
        layoutComponents();
        addListeners();
        // Initial call to refreshGraph() is still needed
        refreshGraph();
    }
    
    // Sets the current user and refreshes the graph
    public void setCurrentUser(User user) {
        this.currentUser = user;
        refreshGraph();
    }

    // Initializes GUI components
    private void initComponents() {
        graphDisplayArea = new JTextArea();
        graphDisplayArea.setEditable(false);
        graphDisplayArea.setLineWrap(true);
        graphDisplayArea.setWrapStyleWord(true);
        
        refreshGraphButton = new JButton("Refresh Transaction Graph");

        // Apply consistent styles
        graphDisplayArea.setBackground(FIELD_BG);
        graphDisplayArea.setForeground(TEXT_LIGHT);
        graphDisplayArea.setFont(GRAPH_TEXT_FONT);
        graphDisplayArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        refreshGraphButton.setBackground(ACCENT_BLUE);
        refreshGraphButton.setForeground(Color.WHITE);
        refreshGraphButton.setFont(BUTTON_FONT);
        refreshGraphButton.setFocusPainted(false);
        refreshGraphButton.setBorderPainted(false);
        refreshGraphButton.setOpaque(true);
    }

    // Lays out components
    private void layoutComponents() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topPanel.setBackground(BG_DARK);
        topPanel.add(refreshGraphButton);
        
        add(topPanel, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(graphDisplayArea);
        scrollPane.getViewport().setBackground(FIELD_BG);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR), "Transaction Graph Display",
            TitledBorder.LEFT, TitledBorder.TOP, TITLE_BORDER_FONT, TEXT_LIGHT
        ));
        add(scrollPane, BorderLayout.CENTER);
    }

    // Adds listeners
    private void addListeners() {
        refreshGraphButton.addActionListener(e -> refreshGraph());
    }

    // Refreshes the graph
    public void refreshGraph() {
        graphDisplayArea.setText(""); // Clear previous content

        if (currentUser == null) {
            graphDisplayArea.setText("Please log in to view the transaction graph.");
            return;
        }

        try {
            Map<String, Map<String, List<String>>> summarizedGraph = bankingService.getSummarizedTransactionGraph();
            
            if (summarizedGraph.isEmpty()) {
                graphDisplayArea.setText("No transfer transactions found to build a graph.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("--- Transaction Graph (User Flow) ---\n\n");

            summarizedGraph.forEach((senderUsername, receiversMap) -> {
                sb.append("User: ").append(senderUsername).append(" has transferred to:\n");
                receiversMap.forEach((receiverUsername, transactions) -> {
                    sb.append("  -> ").append(receiverUsername).append(" (").append(transactions.size()).append(" transfers)\n");
                    transactions.forEach(t -> sb.append("    - ").append(t).append("\n"));
                });
                sb.append("\n");
            });
            graphDisplayArea.setText(sb.toString());

        } catch (SQLException e) {
            graphDisplayArea.setText("Error loading transaction graph: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error loading transaction graph: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        // Removed: refreshDashboardCallback.run(); // This caused the StackOverflowError
    }
}
