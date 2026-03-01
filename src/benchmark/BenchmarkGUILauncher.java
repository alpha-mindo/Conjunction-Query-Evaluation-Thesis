package benchmark;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point for the Swing benchmark GUI.
 */
public class BenchmarkGUILauncher {

    public static void main(String[] args) {
        // Metal L&F so custom colors are not overridden by Windows native renderer
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            // Main area — light
            UIManager.put("Table.background",           new java.awt.Color(255, 255, 255));
            UIManager.put("Table.foreground",           new java.awt.Color(30,  40,  55));
            UIManager.put("Table.selectionBackground",  new java.awt.Color(207, 232, 252));
            UIManager.put("Table.selectionForeground",  new java.awt.Color(10,  50,  90));
            UIManager.put("TableHeader.background",     new java.awt.Color(235, 240, 246));
            UIManager.put("TableHeader.foreground",     new java.awt.Color(100, 116, 139));
            UIManager.put("Panel.background",           new java.awt.Color(245, 247, 250));
            UIManager.put("ScrollPane.background",      new java.awt.Color(245, 247, 250));
            UIManager.put("SplitPane.background",       new java.awt.Color(245, 247, 250));
            UIManager.put("Label.foreground",           new java.awt.Color(30,  40,  55));
            // Sidebar controls — dark teal
            UIManager.put("ComboBox.background",        new java.awt.Color(32,  80, 104));
            UIManager.put("ComboBox.foreground",        new java.awt.Color(220, 240, 248));
            UIManager.put("TextField.background",       new java.awt.Color(32,  80, 104));
            UIManager.put("TextField.foreground",       new java.awt.Color(255, 255, 255));
            UIManager.put("TextField.caretForeground",  new java.awt.Color(255, 255, 255));
            UIManager.put("CheckBox.background",        new java.awt.Color(22,  60,  80));
            UIManager.put("CheckBox.foreground",        new java.awt.Color(220, 240, 248));
        } catch (Exception ignored) {
            // Fall back silently
        }

        SwingUtilities.invokeLater(() -> {
            BenchmarkGUI gui = new BenchmarkGUI();
            gui.setVisible(true);
        });
    }
}
