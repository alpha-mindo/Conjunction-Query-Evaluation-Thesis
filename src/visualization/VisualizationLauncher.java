package visualization;

/**
 * Launcher for the WCOJ Algorithm Visualizer.
 * Run as: java visualization.VisualizationLauncher
 */
public class VisualizationLauncher {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            // Create a hidden parent frame
            javax.swing.JFrame parent = new javax.swing.JFrame();
            parent.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            parent.setVisible(false);

            // Create sample relations for demonstration
            java.util.Map<String, database.Relation> relations = new java.util.HashMap<>();
            
            // R(A, B)
            database.Relation R = new database.Relation("R", java.util.Arrays.asList("A", "B"));
            R.addTuple(new database.Tuple(R.getColumns(), java.util.Arrays.asList("1", "10")));
            R.addTuple(new database.Tuple(R.getColumns(), java.util.Arrays.asList("1", "20")));
            R.addTuple(new database.Tuple(R.getColumns(), java.util.Arrays.asList("2", "10")));
            R.addTuple(new database.Tuple(R.getColumns(), java.util.Arrays.asList("2", "30")));
            relations.put("R", R);
            
            // S(B, C)
            database.Relation S = new database.Relation("S", java.util.Arrays.asList("B", "C"));
            S.addTuple(new database.Tuple(S.getColumns(), java.util.Arrays.asList("10", "100")));
            S.addTuple(new database.Tuple(S.getColumns(), java.util.Arrays.asList("10", "200")));
            S.addTuple(new database.Tuple(S.getColumns(), java.util.Arrays.asList("20", "100")));
            S.addTuple(new database.Tuple(S.getColumns(), java.util.Arrays.asList("30", "300")));
            relations.put("S", S);
            
            // T(C, D)
            database.Relation T = new database.Relation("T", java.util.Arrays.asList("C", "D"));
            T.addTuple(new database.Tuple(T.getColumns(), java.util.Arrays.asList("100", "X")));
            T.addTuple(new database.Tuple(T.getColumns(), java.util.Arrays.asList("100", "Y")));
            T.addTuple(new database.Tuple(T.getColumns(), java.util.Arrays.asList("200", "X")));
            T.addTuple(new database.Tuple(T.getColumns(), java.util.Arrays.asList("300", "Z")));
            relations.put("T", T);

            // Open visualizer
            VisualizationGUI viz = new VisualizationGUI(parent, relations);
            viz.setDefaultCloseOperation(javax.swing.JDialog.DISPOSE_ON_CLOSE);
            viz.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    System.exit(0);
                }
            });
            viz.setVisible(true);
        });
    }
}
