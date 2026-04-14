package benchmark;

import javax.swing.*;
import java.awt.*;

public class MainGUI extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainContentPanel;

    public MainGUI() {
        super("CQE Benchmark & Visualizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Sophisticated Flat/Modern Look
        setupLookAndFeel();

        // Layout: Sidebar (WEST) + Main Content (CENTER)
        setLayout(new BorderLayout());

        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);

        // TODO: Build and add the new sophisticated panels:
        // 1. Dashboard (Overview)
        // 2. Table Builder / Custom Relations
        // 3. Query Execution Panel
        // 4. Algorithm Visualizer

        add(buildSidebar(), BorderLayout.WEST);
        add(mainContentPanel, BorderLayout.CENTER);
    }

    private void setupLookAndFeel() {
        try {
            // Using Nimbus or a modern-looking cross-platform built-in L&F
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                // Fallback to cross-platform
            }
        }
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(30, 35, 45));
        sidebar.setPreferredSize(new Dimension(200, 0));

        // Add navigation buttons
        // TODO: Implement styling and action listeners for navigation
        
        return sidebar;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainGUI().setVisible(true);
        });
    }
}
