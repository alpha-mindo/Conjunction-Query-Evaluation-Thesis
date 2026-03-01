package benchmark;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point for the Swing benchmark GUI.
 */
public class BenchmarkGUILauncher {

    public static void main(String[] args) {
        // Use the system look-and-feel for a native appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to the default Swing L&F
        }

        SwingUtilities.invokeLater(() -> {
            BenchmarkGUI gui = new BenchmarkGUI();
            gui.setVisible(true);
        });
    }
}
