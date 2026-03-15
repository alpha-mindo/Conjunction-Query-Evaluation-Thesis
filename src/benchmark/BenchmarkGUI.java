package benchmark;

import database.Relation;
import tree.TreeNode;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;

/**
 * Swing-based GUI — sidebar layout, light main panel, teal sidebar.
 */
public class BenchmarkGUI extends JFrame {

    // ── Palette ────────────────────────────────────────────────────────────────
    // Sidebar
    private static final Color SIDE_BG      = new Color(22,  60,  80);   // deep teal
    private static final Color SIDE_TOP     = new Color(16,  44,  60);   // darker teal for header
    private static final Color SIDE_HOVER   = new Color(32,  80, 104);
    private static final Color SIDE_TEXT    = new Color(220, 240, 248);
    private static final Color SIDE_MUTED   = new Color(140, 190, 210);
    private static final Color SIDE_BORDER  = new Color(30,  72,  95);
    // Main
    private static final Color MAIN_BG      = new Color(245, 247, 250);
    private static final Color CARD_BG      = Color.WHITE;
    private static final Color BORDER_COL   = new Color(218, 224, 232);
    private static final Color HDR_BG       = new Color(235, 240, 246);
    private static final Color TEXT_PRI     = new Color(30,  40,  55);
    private static final Color TEXT_SEC     = new Color(100, 116, 139);
    private static final Color ROW_ALT      = new Color(250, 252, 255);
    private static final Color ROW_SEL_BG   = new Color(207, 232, 252);
    private static final Color ROW_SEL_FG   = new Color(10,  50,  90);
    private static final Color LOG_BG       = new Color(250, 250, 252);
    private static final Color LOG_FG       = new Color(40,  55,  75);
    private static final Color OK_COLOR     = new Color(22, 140,  80);
    private static final Color ERR_COLOR    = new Color(190,  40,  40);

    // ── Fonts ──────────────────────────────────────────────────────────────────
    private static final Font FONT_APPNAME = new Font("Segoe UI", Font.BOLD,  15);
    private static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD,  11);
    private static final Font FONT_LABEL   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD    = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FONT_MONO    = new Font("Consolas",  Font.PLAIN, 12);
    private static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 11);

    // ── Columns ────────────────────────────────────────────────────────────────
    private static final String[] COLUMNS   = {
        "Query Pattern", "DB Size", "Time (ms)", "Result Size", "AGM Bound", "Memory (MB)"
    };
    private static final int[]    COL_WIDTHS = { 170, 85, 100, 110, 110, 110 };

    // ── Components ─────────────────────────────────────────────────────────────
    private final DefaultTableModel         tableModel;
    private final JTable                     resultTable;
    private final List<Map<String,Relation>> storedRelations = new ArrayList<>();
    private final JTextArea                  logArea;
    private final JButton           runBtn;
    private final JButton           clearBtn;
    private final JComboBox<String> patternBox;
    private final JTextField        sizeField;
    private final JCheckBox         allPatternsBox;
    private final JLabel            statusLabel;
    private final JProgressBar      progressBar;

    // ── Constructor ────────────────────────────────────────────────────────────
    public BenchmarkGUI() {
        super("CQE Benchmark");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(880, 560));
        setLocationRelativeTo(null);

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        resultTable = buildTable();
        resultTable.getSelectionModel().addListSelectionListener(evt -> {
            if (evt.getValueIsAdjusting()) return;
            int sel = resultTable.getSelectedRow();
            if (sel >= 0 && sel < storedRelations.size())
                showInspection(storedRelations.get(sel));
        });

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(FONT_MONO);
        logArea.setBackground(LOG_BG);
        logArea.setForeground(LOG_FG);
        logArea.setBorder(new EmptyBorder(8, 12, 8, 12));
        logArea.setLineWrap(true);

        String[] patternNames = Arrays.stream(QueryPattern.values())
                                      .map(QueryPattern::name)
                                      .toArray(String[]::new);
        patternBox     = sideCombo(patternNames);
        sizeField      = sideField("500");
        allPatternsBox = sideCheckbox("Run all patterns");
        runBtn         = sideRunButton("▶  Run Benchmark");
        clearBtn       = sideClearButton("Clear Results");

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(TEXT_SEC);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setForeground(new Color(22, 150, 100));
        progressBar.setBackground(BORDER_COL);
        progressBar.setBorderPainted(false);
        progressBar.setPreferredSize(new Dimension(0, 3));

        allPatternsBox.addActionListener(e -> {
            boolean all = allPatternsBox.isSelected();
            patternBox.setEnabled(!all);
            sizeField.setEnabled(!all);
        });
        runBtn.addActionListener(this::onRun);
        clearBtn.addActionListener(e -> {
            tableModel.setRowCount(0);
            storedRelations.clear();
            logArea.setText("");
            setStatus("Cleared.", TEXT_SEC);
        });

        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);
        add(buildMain(),    BorderLayout.CENTER);
    }

    // ── Sidebar ────────────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(SIDE_BG);
        side.setPreferredSize(new Dimension(240, 0));
        side.setBorder(new MatteBorder(0, 0, 0, 1, SIDE_BORDER));

        // App name block
        JPanel appBlock = new JPanel(new BorderLayout());
        appBlock.setOpaque(true);
        appBlock.setBackground(SIDE_TOP);
        appBlock.setBorder(new EmptyBorder(22, 20, 20, 20));
        JLabel appName = new JLabel("CQE Benchmark");
        appName.setFont(FONT_APPNAME);
        appName.setForeground(Color.WHITE);
        JLabel appSub = new JLabel("Worst-Case Optimal Join");
        appSub.setFont(FONT_SMALL);
        appSub.setForeground(new Color(160, 210, 230));
        JPanel nameStack = new JPanel();
        nameStack.setLayout(new BoxLayout(nameStack, BoxLayout.Y_AXIS));
        nameStack.setOpaque(false);
        nameStack.add(appName);
        nameStack.add(Box.createVerticalStrut(3));
        nameStack.add(appSub);
        appBlock.add(nameStack);
        appBlock.setMaximumSize(new Dimension(240, 100));
        appBlock.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(appBlock);

        side.add(Box.createVerticalStrut(16));
        side.add(sectionLabel("CONFIGURATION"));
        side.add(Box.createVerticalStrut(10));
        side.add(sideFormRow("Pattern", patternBox));
        side.add(Box.createVerticalStrut(10));
        side.add(sideFormRow("DB Size", sizeField));
        side.add(Box.createVerticalStrut(12));

        JPanel cbRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        cbRow.setOpaque(false);
        cbRow.setMaximumSize(new Dimension(240, 30));
        cbRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbRow.add(allPatternsBox);
        side.add(cbRow);

        side.add(Box.createVerticalStrut(24));
        side.add(sectionLabel("ACTIONS"));
        side.add(Box.createVerticalStrut(10));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        btnRow.setOpaque(false);
        btnRow.setMaximumSize(new Dimension(240, 44));
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.add(runBtn);
        side.add(btnRow);

        side.add(Box.createVerticalStrut(8));
        JPanel clrRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        clrRow.setOpaque(false);
        clrRow.setMaximumSize(new Dimension(240, 36));
        clrRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        clrRow.add(clearBtn);
        side.add(clrRow);

        side.add(Box.createVerticalStrut(24));
        side.add(sectionLabel("CUSTOM TABLES"));
        side.add(Box.createVerticalStrut(10));

        JButton customBtn = sideRunButton("⊞  Custom Table Builder");
        customBtn.addActionListener(e -> new TableBuilderDialog(this).setVisible(true));
        JPanel customRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        customRow.setOpaque(false);
        customRow.setMaximumSize(new Dimension(240, 44));
        customRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        customRow.add(customBtn);
        side.add(customRow);

        side.add(Box.createVerticalStrut(8));
        JButton vizBtn = sideRunButton("▶  Algorithm Visualizer");
        vizBtn.addActionListener(e -> launchVisualizer());
        JPanel vizRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        vizRow.setOpaque(false);
        vizRow.setMaximumSize(new Dimension(240, 44));
        vizRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        vizRow.add(vizBtn);
        side.add(vizRow);

        side.add(Box.createVerticalGlue());
        return side;
    }

    // ── Main panel ─────────────────────────────────────────────────────────────
    private JPanel buildMain() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(MAIN_BG);

        // Table card
        JScrollPane tableScroll = new JScrollPane(resultTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(CARD_BG);
        tableScroll.getVerticalScrollBar().setUI(new SlimScrollBarUI(false));

        JPanel tableCard = mainCard("Results", tableScroll);

        // Log card
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createEmptyBorder());
        logScroll.getViewport().setBackground(LOG_BG);
        logScroll.getVerticalScrollBar().setUI(new SlimScrollBarUI(false));

        JPanel logCard = mainCard("Output Log", logScroll);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableCard, logCard);
        split.setResizeWeight(0.70);
        split.setDividerSize(5);
        split.setBorder(new EmptyBorder(14, 14, 0, 14));
        split.setBackground(MAIN_BG);

        main.add(split, BorderLayout.CENTER);
        main.add(buildStatusBar(), BorderLayout.SOUTH);
        return main;
    }

    // ── Status bar ─────────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(CARD_BG);
        bar.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER_COL),
            new EmptyBorder(4, 14, 4, 14)
        ));
        bar.add(statusLabel,  BorderLayout.WEST);
        bar.add(progressBar,  BorderLayout.SOUTH);
        return bar;
    }

    // ── Table ──────────────────────────────────────────────────────────────────
    private JTable buildTable() {
        JTable t = new JTable(tableModel) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (isRowSelected(row)) {
                    c.setBackground(ROW_SEL_BG);
                    c.setForeground(ROW_SEL_FG);
                } else {
                    c.setBackground(row % 2 == 0 ? CARD_BG : ROW_ALT);
                    c.setForeground(TEXT_PRI);
                }
                ((JComponent) c).setBorder(new EmptyBorder(0, 10, 0, 10));
                return c;
            }
        };
        t.setFont(FONT_LABEL);
        t.setRowHeight(30);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setBackground(CARD_BG);
        t.setForeground(TEXT_PRI);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setFillsViewportHeight(true);

        JTableHeader hdr = t.getTableHeader();
        hdr.setFont(FONT_BOLD);
        hdr.setBackground(HDR_BG);
        hdr.setForeground(TEXT_SEC);
        hdr.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_COL));
        hdr.setPreferredSize(new Dimension(0, 36));
        hdr.setReorderingAllowed(false);

        // Shared centered cell renderer
        DefaultTableCellRenderer centerCell = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(table, value, sel, foc, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return this;
            }
        };

        // Shared centered header renderer
        DefaultTableCellRenderer centerHdr = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(table, value, sel, foc, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBackground(HDR_BG);
                setForeground(TEXT_SEC);
                setFont(FONT_BOLD);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return this;
            }
        };

        TableColumnModel cm = t.getColumnModel();
        for (int i = 0; i < COLUMNS.length; i++) {
            cm.getColumn(i).setPreferredWidth(COL_WIDTHS[i]);
            cm.getColumn(i).setHeaderRenderer(centerHdr);
            cm.getColumn(i).setCellRenderer(centerCell);
        }
        return t;
    }

    // ── Run action ─────────────────────────────────────────────────────────────
    private void onRun(ActionEvent e) {
        runBtn.setEnabled(false);
        progressBar.setIndeterminate(true);
        setStatus("Running…", new Color(22, 100, 160));

        SwingWorker<List<RunRecord>, String> worker = new SwingWorker<>() {
            @Override
            protected List<RunRecord> doInBackground() {
                List<RunRecord> all = new ArrayList<>();
                if (allPatternsBox.isSelected()) {
                    int[] small  = {10, 50, 100};
                    int[] medium = {100, 500, 1000};
                    QueryPattern[] patterns = {
                        QueryPattern.TWO_WAY, QueryPattern.LINEAR,
                        QueryPattern.CYCLIC,  QueryPattern.STAR,
                        QueryPattern.FOUR_WAY_LINEAR
                    };
                    for (QueryPattern pat : patterns) {
                        int[] sizes = (pat == QueryPattern.CYCLIC) ? small : medium;
                        for (int sz : sizes) {
                            RunRecord r = runOne(pat, sz);
                            if (r != null) all.add(r);
                        }
                    }
                } else {
                    String patName = (String) patternBox.getSelectedItem();
                    int size;
                    try {
                        size = Integer.parseInt(sizeField.getText().trim());
                    } catch (NumberFormatException ex) {
                        publish("[WARN] Invalid DB size — using 100");
                        size = 100;
                    }
                    RunRecord r = runOne(QueryPattern.valueOf(patName), size);
                    if (r != null) all.add(r);
                }
                return all;
            }

            private RunRecord runOne(QueryPattern pat, int size) {
                publish("[ RUN ]  " + pat.getDescription() + "  (size=" + size + ")");
                try {
                    Object[] gen = DatabaseGenerator.generate(pat, size);
                    @SuppressWarnings("unchecked")
                    Map<String, Relation> rels = (Map<String, Relation>) gen[0];
                    TreeNode tree = (TreeNode) gen[1];
                    algorithm.WorstCaseOptimalJoin wcoj =
                        new algorithm.WorstCaseOptimalJoin(rels, tree);
                    WCOJBenchmarkAdapter adapter = new WCOJBenchmarkAdapter(wcoj);
                    BenchmarkResult r = BenchmarkRunner.runBenchmark(adapter, pat, size);
                    publish("[  OK ]  " + String.format("%.3f ms", r.getExecutionTimeMillis() / 1000.0)
                        + "  |  " + r.getResultSize() + " tuples  |  " +
                        String.format("%.2f MB", r.getMemoryUsedMB()));
                    return new RunRecord(r, rels);
                } catch (Exception ex) {
                    publish("[ ERR ]  " + ex.getMessage());
                    return null;
                }
            }

            @Override
            protected void process(List<String> chunks) {
                for (String line : chunks) logArea.append(line + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                runBtn.setEnabled(true);
                try {
                    List<RunRecord> results = get();
                    for (RunRecord rec : results) {
                        BenchmarkResult r = rec.result;
                        tableModel.addRow(new Object[]{
                            r.getQueryPattern(),
                            r.getDatabaseSize(),
                            String.format("%.3f", r.getExecutionTimeMillis() / 1000.0),
                            r.getResultSize(),
                            String.format("%.2f", r.getAgmBound()),
                            String.format("%.2f", r.getMemoryUsedMB())
                        });
                        storedRelations.add(rec.relations);
                    }
                    logArea.append("── Done: " + results.size() + " result(s) ──\n\n");
                    setStatus("Done — " + results.size() + " result(s).", OK_COLOR);
                } catch (Exception ex) {
                    setStatus("Error: " + ex.getMessage(), ERR_COLOR);
                }
            }
        };
        worker.execute();
    }

    // ── Inspection ─────────────────────────────────────────────────────────────
    private void showInspection(Map<String, Relation> relations) {
        StringBuilder sb = new StringBuilder();
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("INSPECTION  —  ").append(relations.size()).append(" relation(s)\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        for (Relation rel : relations.values()) {
            sb.append(rel.toTableString()).append("\n\n");
        }
        logArea.setText(sb.toString());
        logArea.setCaretPosition(0);
    }

    private void launchVisualizer() {
        int sel = resultTable.getSelectedRow();
        if (sel < 0 || sel >= storedRelations.size()) {
            JOptionPane.showMessageDialog(this, "Please select a benchmark result first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Map<String, Relation> relations = storedRelations.get(sel);
        new visualization.VisualizationGUI(this, relations).setVisible(true);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    private JPanel mainCard(String title, JComponent content) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(new LineBorder(BORDER_COL, 1));
        JLabel lbl = new JLabel("  " + title);
        lbl.setFont(FONT_BOLD);
        lbl.setForeground(TEXT_SEC);
        lbl.setOpaque(true);
        lbl.setBackground(HDR_BG);
        lbl.setPreferredSize(new Dimension(0, 34));
        lbl.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_COL),
            new EmptyBorder(0, 10, 0, 10)
        ));
        card.add(lbl,     BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_SECTION);
        l.setForeground(SIDE_MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, SIDE_BORDER),
            new EmptyBorder(8, 20, 4, 0)
        ));
        l.setMinimumSize(new Dimension(240, 28));
        l.setPreferredSize(new Dimension(240, 28));
        l.setMaximumSize(new Dimension(240, 28));
        return l;
    }

    private JPanel sideFormRow(String labelText, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(0, 4));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(240, 58));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(new EmptyBorder(0, 20, 0, 20));
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(SIDE_MUTED);
        row.add(lbl,   BorderLayout.NORTH);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private JButton sideRunButton(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? new Color(16, 100, 70)
                           : getModel().isRollover() ? new Color(20, 130, 88)
                           : new Color(22, 160, 100));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(FONT_BOLD);
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(196, 36));
        return b;
    }

    private JButton sideClearButton(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = getModel().isRollover() || getModel().isPressed()
                           ? new Color(40, 90, 115) : SIDE_HOVER;
                g2.setColor(base);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(FONT_LABEL);
        b.setForeground(SIDE_TEXT);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(196, 32));
        return b;
    }

    private JComboBox<String> sideCombo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(FONT_LABEL);
        cb.setBackground(SIDE_HOVER);
        cb.setForeground(SIDE_TEXT);
        cb.setMaximumSize(new Dimension(200, 30));
        return cb;
    }

    private JTextField sideField(String value) {
        JTextField f = new JTextField(value);
        f.setFont(FONT_LABEL);
        f.setBackground(SIDE_HOVER);
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);
        f.setBorder(new CompoundBorder(
            new LineBorder(SIDE_BORDER, 1, true),
            new EmptyBorder(4, 8, 4, 8)
        ));
        f.setMaximumSize(new Dimension(200, 30));
        return f;
    }

    private JCheckBox sideCheckbox(String text) {
        JCheckBox cb = new JCheckBox(text);
        cb.setFont(FONT_LABEL);
        cb.setForeground(SIDE_TEXT);
        cb.setOpaque(false);
        cb.setFocusPainted(false);
        return cb;
    }

    // ── Slim scroll bar ────────────────────────────────────────────────────────
    private static class SlimScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        private final boolean dark;
        SlimScrollBarUI(boolean dark) { this.dark = dark; }
        @Override protected void configureScrollBarColors() {
            thumbColor = dark ? new Color(80, 80, 110) : new Color(190, 200, 215);
            trackColor = dark ? new Color(22, 22, 32)  : new Color(240, 242, 246);
        }
        @Override protected JButton createDecreaseButton(int o) { return zeroButton(); }
        @Override protected JButton createIncreaseButton(int o) { return zeroButton(); }
        private JButton zeroButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            return b;
        }
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isDragging ? thumbColor.darker() : thumbColor);
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 6, 6);
            g2.dispose();
        }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(trackColor);
            g.fillRect(r.x, r.y, r.width, r.height);
        }
    }

    // ── RunRecord — pairs a result with its input relations ─────────────────────
    private static class RunRecord {
        final BenchmarkResult       result;
        final Map<String, Relation> relations;
        RunRecord(BenchmarkResult result, Map<String, Relation> relations) {
            this.result    = result;
            this.relations = relations;
        }
    }

    // ── ResultRow (binary compatibility) ──────────────────────────────────────
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
