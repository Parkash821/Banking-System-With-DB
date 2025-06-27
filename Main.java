
//  19. DSA/Main.java 
package DSA; 

import gui.LoginFrame;
import service.BankingService;

import javax.swing.SwingUtilities; // Used to run GUI operations on the Event Dispatch Thread (EDT)
import javax.swing.UIManager; // Added for Look and Feel


public class Main {
    public static void main(String[] args) {
       
        SwingUtilities.invokeLater(() -> {
            try {
              
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception e) {
                System.err.println("Failed to set Nimbus Look and Feel: " + e);
               
            }

           
            String dbFilePath = "secure_bank.db";
            BankingService bankingService = new BankingService(dbFilePath);
            
            LoginFrame loginFrame = new LoginFrame(bankingService);
            loginFrame.setVisible(true);
        });
    }
}