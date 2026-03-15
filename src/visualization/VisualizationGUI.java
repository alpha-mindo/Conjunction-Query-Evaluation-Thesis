package visualization;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Swing GUI for visualizing WCOJ algorithm execution.
 * Shows F, G, F\G, C, D tables at each recursive step with narrative explanation.
 */
public class VisualizationGUI extends JDialog {
    private final Map<String, database.Relation> relations;
    private final TracingWCOJ tracer;
    private final List<AlgorithmStep> steps;
    private int currentStep = 0;

    private static final Color SIDE_TOP = new Color(240, 244, 250);
    private static final Color TEXT_PRI = new Color(20, 30, 50);
    private static final Color TEXT_SEC = new Color(100, 120, 150);
    private static final Color ACCENT = new Color(76, 175, 80);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 11);

    private JLabel stepLabel;
    private JLabel headingLabel;
    private JTextArea narrativeArea;
    private JTable fTable, gTable, lightTable, cTable, dTable;
    private DefaultTableModel fModel, gModel, lightModel, cModel, dModel;
    private JButton prevBtn, nextBtn, playBtn;
    private JProgressBar stepProgress;
    private Timer autoPlayTimer;

    public VisualizationGUI(Frame owner, Map<String, database.Relation> rels) {
        super(owner, "WCOJ Algorithm Visualizer", false);
        this.relations = rels;
        this.tracer = new TracingWCOJ(rels);
        
        setSize(1400, 850);
        setMinimumSize(new Dimension(1000, 600));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        // Execute algorithm to populate steps
        tracer.execute();
        this.steps = tracer.getSteps();

        // ── UI Components (initialize early so they're always ready) ────────

        stepLabel = new JLabel("Step 0 / " + (Math.max(0, steps.size() - 1)));
        stepLabel.setFont(FONT_LABEL);
        stepLabel.setForeground(TEXT_SEC);

        headingLabel = new JLabel("");
        headingLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        headingLabel.setForeground(TEXT_PRI);

        narrativeArea = new JTextArea();
        narrativeArea.setEditable(false);
        narrativeArea.setFont(FONT_MONO);
        narrativeArea.setLineWrap(true);
        narrativeArea.setWrapStyleWord(true);
        narrativeArea.setBackground(new Color(250, 250, 252));
        narrativeArea.setForeground(new Color(40, 55, 75));
        narrativeArea.setBorder(new EmptyBorder(8, 10, 8, 10));

        fModel = new DefaultTableModel(new String[]{}, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        gModel = new DefaultTableModel(new String[]{}, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        lightModel = new DefaultTableModel(new String[]{}, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        cModel = new DefaultTableModel(new String[]{}, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        dModel = new DefaultTableModel(new String[]{}, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };

        fTable = buildTable(fModel);
        gTable = buildTable(gModel);
        lightTable = buildTable(lightModel);
        cTable = buildTable(cModel);
        dTable = buildTable(dModel);

        prevBtn = greenBtn("◀ Previous");
        nextBtn = greenBtn("Next ▶");
        playBtn = greenBtn("▶ Auto-play");

        prevBtn.addActionListener(e -> previousStep());
        nextBtn.addActionListener(e -> nextStep());
        playBtn.addActionListener(e -> autoPlay());

        stepProgress = new JProgressBar(0, Math.max(1, steps.size() - 1));
        stepProgress.setPreferredSize(new Dimension(0, 8));

        // Check if empty
        if (steps.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No relations to visualize.", "Empty", JOptionPane.WARNING_MESSAGE);
            setVisible(false);
            return;
        }

        // ── Layout ──────────────────────────────────────────────────────────

        add(buildTopBar(),       BorderLayout.NORTH);
        add(buildMainContent(),  BorderLayout.CENTER);
        add(buildButtonBar(),    BorderLayout.SOUTH);

        updateStep(0);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(SIDE_TOP);
        bar.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        left.add(headingLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(stepLabel);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildMainContent() {
        JPanel main = new JPanel(new BorderLayout(0, 12));
        main.setBorder(new EmptyBorder(16, 16, 16, 16));
        main.setBackground(Color.WHITE);

        // Narrative box
        JScrollPane narrativeScroll = new JScrollPane(narrativeArea);
        narrativeScroll.setPreferredSize(new Dimension(0, 80));
        main.add(narrativeScroll, BorderLayout.NORTH);

        // 5 tables in a grid
        JPanel grid = new JPanel(new GridLayout(1, 5, 12, 0));
        grid.setBackground(Color.WHITE);
        grid.add(buildTablePanel("F (Matching Keys)", fTable));
        grid.add(buildTablePanel("G (Heavy Hitters)", gTable));
        grid.add(buildTablePanel("F\\G (Light Hitters)", lightTable));
        grid.add(buildTablePanel("C (Complete)", cTable));
        grid.add(buildTablePanel("D (Deferred)", dTable));

        main.add(grid, BorderLayout.CENTER);
        return main;
    }

    private JPanel buildTablePanel(String title, JTable table) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(Color.WHITE);
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(TEXT_PRI);
        p.add(lbl, BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(SIDE_TOP);
        bar.setBorder(new EmptyBorder(12, 16, 12, 16));

        bar.add(stepProgress, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btns.setOpaque(false);
        btns.add(prevBtn);
        btns.add(playBtn);
        btns.add(nextBtn);

        bar.add(btns, BorderLayout.EAST);
        return bar;
    }

    private void updateStep(int idx) {
        if (idx < 0 || idx >= steps.size()) return;
        currentStep = idx;
        AlgorithmStep step = steps.get(idx);

        // Update tables
        updateTableModel(fModel, step.F_snap);
        updateTableModel(gModel, step.G_snap);
        updateTableModel(lightModel, step.light_snap);
        updateTableModel(cModel, step.C_out_snap);
        updateTableModel(dModel, step.D_out_snap);

        // Update labels
        stepLabel.setText("Step " + idx + " / " + (steps.size() - 1));
        headingLabel.setText(step.heading);
        narrativeArea.setText(step.narrative);
        stepProgress.setValue(idx);
    }

    private void updateTableModel(DefaultTableModel model, AlgorithmStep.TableSnap snap) {
        model.setRowCount(0);
        model.setColumnCount(0);
        if (snap.isEmpty()) return;

        // Set columns
        for (String col : snap.cols) {
            model.addColumn(col);
        }

        // Add rows
        for (java.util.List<String> row : snap.rows) {
            model.addRow(row.toArray());
        }
    }

    private JTable buildTable(DefaultTableModel model) {
        JTable tbl = new JTable(model);
        tbl.setRowHeight(24);
        tbl.setFont(FONT_MONO);
        tbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tbl.getTableHeader().setBackground(SIDE_TOP);
        tbl.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        return tbl;
    }

    private JButton greenBtn(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void previousStep() {
        if (currentStep > 0) updateStep(currentStep - 1);
    }

    private void nextStep() {
        if (currentStep < steps.size() - 1) updateStep(currentStep + 1);
    }

    private void autoPlay() {
        if (autoPlayTimer != null && autoPlayTimer.isRunning()) {
            autoPlayTimer.stop();
            playBtn.setText("▶ Auto-play");
            return;
        }

        playBtn.setText("⏸ Pause");
        autoPlayTimer = new Timer(800, e -> {
            if (currentStep < steps.size() - 1) {
                nextStep();
            } else {
                autoPlayTimer.stop();
                playBtn.setText("▶ Auto-play");
            }
        });
        autoPlayTimer.start();
    }
}
