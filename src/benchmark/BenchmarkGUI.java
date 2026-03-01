package benchmark;

import database.Relation;
import tree.TreeNode;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Swing-based GUI for running and visualising benchmark results.
 */
public class BenchmarkGUI extends JFrame {

    // ── table columns ──────────────────────────────────────────────────────────
    private static final String[] COLUMNS = {
        "Query Pattern", "DB Size", "Avg Time (ms)", "Result Size", "AGM Bound", "Memory (MB)"
    };

    // ── UI components ──────────────────────────────────────────────────────────
    private final DefaultTableModel tableModel;
    private final JTable            resultTable;
    private final JTextArea         logArea;
    private final JButton           runBtn;
    private final JButton           clearBtn;
    private final JComboBox<String> patternBox;
    private final JTextField        sizeField;
    private final JCheckBox         allPatternsBox;
    private final JLabel            statusLabel;

    // ── constructor ────────────────────────────────────────────────────────────
    public BenchmarkGUI() {
        super("Conjunction Query Evaluation – Benchmark");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setMinimumSize(new Dimension(800, 550));
        setLocationRelativeTo(null);

        // ── table ──
        tableModel  = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        resultTable = new JTable(tableModel);
        resultTable.setFont(new Font("Monospaced", Font.PLAIN, 12));
        resultTable.setRowHeight(22);
        resultTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        styleTable();

        // ── log ──
        logArea = new JTextArea(6, 60);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(200, 255, 200));

        // ── controls ──
        String[] patternNames = Arrays.stream(QueryPattern.values())
                                      .map(QueryPattern::name)
                                      .toArray(String[]::new);
        patternBox     = new JComboBox<>(patternNames);
        sizeField      = new JTextField("500", 7);
        allPatternsBox = new JCheckBox("All patterns (comprehensive)", false);
        runBtn         = new JButton("▶  Run Benchmark");
        clearBtn       = new JButton("✕  Clear");
        statusLabel    = new JLabel("Ready.");

        runBtn.setBackground(new Color(70, 130, 180));
        runBtn.setForeground(Color.WHITE);
        runBtn.setFont(runBtn.getFont().deriveFont(Font.BOLD));
        runBtn.setFocusPainted(false);

        allPatternsBox.addActionListener(e -> {
            patternBox.setEnabled(!allPatternsBox.isSelected());
            sizeField.setEnabled(!allPatternsBox.isSelected());
        });

        runBtn.addActionListener(this::onRun);
        clearBtn.addActionListener(e -> {
            tableModel.setRowCount(0);
            logArea.setText("");
            statusLabel.setText("Cleared.");
        });

        // ── layout ──
        setLayout(new BorderLayout(6, 6));
        add(buildControlPanel(), BorderLayout.NORTH);
        add(buildCentrePanel(),  BorderLayout.CENTER);
        add(buildStatusBar(),    BorderLayout.SOUTH);
    }

    // ── panel builders ─────────────────────────────────────────────────────────
    private JPanel buildControlPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        p.setBorder(BorderFactory.createTitledBorder("Configuration"));
        p.add(allPatternsBox);
        p.add(new JLabel("Pattern:"));
        p.add(patternBox);
        p.add(new JLabel("DB Size:"));
        p.add(sizeField);
        p.add(runBtn);
        p.add(clearBtn);
        return p;
    }

    private JSplitPane buildCentrePanel() {
        JScrollPane tableScroll = new JScrollPane(resultTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Results"));

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log"));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, logScroll);
        split.setResizeWeight(0.75);
        split.setBorder(null);
        return split;
    }

    private JPanel buildStatusBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        p.add(statusLabel);
        return p;
    }

    // ── style helper ───────────────────────────────────────────────────────────
    private void styleTable() {
        TableColumnModel cm = resultTable.getColumnModel();
        int[] widths = {130, 80, 110, 100, 100, 110};
        for (int i = 0; i < widths.length; i++) {
            cm.getColumn(i).setPreferredWidth(widths[i]);
        }
        // right-align numeric columns
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 1; i < COLUMNS.length; i++) {
            cm.getColumn(i).setCellRenderer(right);
        }
    }

    // ── run action ─────────────────────────────────────────────────────────────
    private void onRun(ActionEvent e) {
        runBtn.setEnabled(false);
        statusLabel.setText("Running…");

        SwingWorker<List<BenchmarkResult>, String> worker = new SwingWorker<>() {
            @Override
            protected List<BenchmarkResult> doInBackground() {
                List<BenchmarkResult> all = new ArrayList<>();

                if (allPatternsBox.isSelected()) {
                    // Comprehensive run mirrors BenchmarkRunner.runComprehensiveBenchmark()
                    int[] small  = {10, 50, 100};
                    int[] medium = {100, 500, 1000};
                    QueryPattern[] patterns = {
                        QueryPattern.TWO_WAY,
                        QueryPattern.LINEAR,
                        QueryPattern.CYCLIC,
                        QueryPattern.STAR,
                        QueryPattern.FOUR_WAY_LINEAR
                    };
                    for (QueryPattern pat : patterns) {
                        int[] sizes = (pat == QueryPattern.CYCLIC) ? small : medium;
                        for (int sz : sizes) {
                            BenchmarkResult r = runOne(pat, sz);
                            if (r != null) all.add(r);
                        }
                    }
                } else {
                    String patName = (String) patternBox.getSelectedItem();
                    int size;
                    try {
                        size = Integer.parseInt(sizeField.getText().trim());
                    } catch (NumberFormatException ex) {
                        publish("ERROR: invalid DB size – using 100");
                        size = 100;
                    }
                    BenchmarkResult r = runOne(QueryPattern.valueOf(patName), size);
                    if (r != null) all.add(r);
                }
                return all;
            }

            private BenchmarkResult runOne(QueryPattern pat, int size) {
                publish("Running " + pat.getDescription() + " | size=" + size + " …");
                try {
                    Object[] gen = DatabaseGenerator.generate(pat, size);
                    @SuppressWarnings("unchecked")
                    Map<String, Relation> rels = (Map<String, Relation>) gen[0];
                    TreeNode tree = (TreeNode) gen[1];
                    algorithm.WorstCaseOptimalJoin wcoj =
                        new algorithm.WorstCaseOptimalJoin(rels, tree);
                    WCOJBenchmarkAdapter adapter = new WCOJBenchmarkAdapter(wcoj);
                    BenchmarkResult r = BenchmarkRunner.runBenchmark(adapter, pat, size);
                    publish("  → " + r);
                    return r;
                } catch (Exception ex) {
                    publish("  ERROR: " + ex.getMessage());
                    return null;
                }
            }

            @Override
            protected void process(List<String> chunks) {
                for (String line : chunks) {
                    logArea.append(line + "\n");
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                }
            }

            @Override
            protected void done() {
                try {
                    List<BenchmarkResult> results = get();
                    for (BenchmarkResult r : results) {
                        tableModel.addRow(new Object[]{
                            r.getQueryPattern(),
                            r.getDatabaseSize(),
                            String.format("%.3f", r.getExecutionTimeMillis() / 1000.0),
                            r.getResultSize(),
                            String.format("%.2f", r.getAgmBound()),
                            String.format("%.2f", r.getMemoryUsedMB())
                        });
                    }
                    statusLabel.setText("Done – " + results.size() + " result(s).");
                } catch (Exception ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                } finally {
                    runBtn.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    // ── inner record for table rows (kept for binary compatibility) ─────────────
    static class ResultRow {
        final String pattern;
        final int    dbSize;
        final double timeMs;
        final int    resultSize;
        final double agmBound;
        final double memoryMB;

        ResultRow(BenchmarkResult r) {
            this.pattern    = r.getQueryPattern();
            this.dbSize     = r.getDatabaseSize();
            this.timeMs     = r.getExecutionTimeMillis() / 1000.0;
            this.resultSize = r.getResultSize();
            this.agmBound   = r.getAgmBound();
            this.memoryMB   = r.getMemoryUsedMB();
        }
    }
}
