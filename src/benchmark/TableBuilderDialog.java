package benchmark;

import algorithm.WorstCaseOptimalJoin;
import database.Relation;
import database.Tuple;
import tree.QueryTreeBuilder;
import tree.TreeNode;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;

/**
 * Modal dialog for building custom relations and running the WCOJ on them.
 *
 * Layout
 * ───────────────────────────────────────────────────────
 * │ Relation list │  Column / row editor  │ Result log │
 * ───────────────────────────────────────────────────────
 *
 * Workflow:
 *  1. Click "+" to create a relation; give it a name.
 *  2. Type comma-separated column names and click "Apply Columns".
 *  3. Add rows manually or use a fill preset (All-Match, No-Match, Skewed …).
 *  4. Click "Run WCOJ" — the result is shown in the log on the right.
 */
public class TableBuilderDialog extends JDialog {

    // ── Palette (matches BenchmarkGUI) ────────────────────────────────────────
    private static final Color SIDE_BG     = new Color(22,  60,  80);
    private static final Color SIDE_TOP    = new Color(16,  44,  60);
    private static final Color SIDE_HOVER  = new Color(32,  80, 104);
    private static final Color SIDE_TEXT   = new Color(220, 240, 248);
    private static final Color SIDE_BORDER = new Color(30,  72,  95);
    private static final Color MAIN_BG     = new Color(245, 247, 250);
    private static final Color CARD_BG     = Color.WHITE;
    private static final Color BORDER_COL  = new Color(218, 224, 232);
    private static final Color HDR_BG      = new Color(235, 240, 246);
    private static final Color TEXT_PRI    = new Color(30,  40,  55);
    private static final Color TEXT_SEC    = new Color(100, 116, 139);
    private static final Color ROW_ALT     = new Color(250, 252, 255);
    private static final Color OK_COLOR    = new Color(22, 140,  80);
    private static final Color ERR_COLOR   = new Color(190,  40,  40);
    private static final Color WARN_COLOR  = new Color(180, 110,  20);

    private static final Font FONT_APPNAME = new Font("Segoe UI", Font.BOLD,  14);
    private static final Font FONT_LABEL   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD    = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FONT_MONO    = new Font("Consolas",  Font.PLAIN, 12);
    private static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 11);

    // ── State ─────────────────────────────────────────────────────────────────
    /** Ordered map: relation-name → list of column names */
    private final LinkedHashMap<String, List<String>> schemas  = new LinkedHashMap<>();
    /** Ordered map: relation-name → list of row data (each row = list of String values) */
    private final LinkedHashMap<String, List<List<String>>> data = new LinkedHashMap<>();
    private String selected = null;  // currently selected relation name

    // ── UI components ─────────────────────────────────────────────────────────
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String>            relList   = new JList<>(listModel);
    private final JTextField               nameField = darkField("");
    private final JTextField               colsField = darkField("A, B");
    private       DefaultTableModel        gridModel = new DefaultTableModel();
    private final JTable                   grid      = buildGrid();
    private final JTextArea                logArea   = buildLog();
    private final JLabel                   statusLbl = new JLabel("No relations defined.");

    // ── Constructor ───────────────────────────────────────────────────────────
    public TableBuilderDialog(Frame owner) {
        super(owner, "Custom Table Builder", false);
        setSize(1160, 680);
        setMinimumSize(new Dimension(900, 520));
        setLocationRelativeTo(owner);

        setLayout(new BorderLayout());
        add(buildRelationPanel(), BorderLayout.WEST);
        add(buildEditorPanel(),   BorderLayout.CENTER);
        add(buildResultPanel(),   BorderLayout.EAST);
        add(buildStatusBar(),     BorderLayout.SOUTH);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LEFT — Relation list
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel buildRelationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SIDE_BG);
        panel.setPreferredSize(new Dimension(180, 0));
        panel.setBorder(new MatteBorder(0, 0, 0, 1, SIDE_BORDER));

        // Header
        JLabel hdr = new JLabel("  Relations");
        hdr.setFont(FONT_APPNAME);
        hdr.setForeground(Color.WHITE);
        hdr.setOpaque(true);
        hdr.setBackground(SIDE_TOP);
        hdr.setPreferredSize(new Dimension(0, 48));
        panel.add(hdr, BorderLayout.NORTH);

        // List
        relList.setFont(FONT_LABEL);
        relList.setBackground(SIDE_BG);
        relList.setForeground(SIDE_TEXT);
        relList.setSelectionBackground(SIDE_HOVER);
        relList.setSelectionForeground(Color.WHITE);
        relList.setFixedCellHeight(30);
        relList.setBorder(new EmptyBorder(4, 0, 4, 0));
        relList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) selectRelation(relList.getSelectedValue());
        });
        JScrollPane scroll = new JScrollPane(relList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(SIDE_BG);
        panel.add(scroll, BorderLayout.CENTER);

        // Buttons
        JButton addBtn = smallDarkBtn("+  New");
        JButton delBtn = smallDarkBtn("−  Delete");
        addBtn.addActionListener(e -> addRelation());
        delBtn.addActionListener(e -> deleteRelation());

        JPanel btns = new JPanel(new GridLayout(2, 1, 0, 4));
        btns.setOpaque(false);
        btns.setBorder(new EmptyBorder(8, 12, 12, 12));
        btns.add(addBtn);
        btns.add(delBtn);
        panel.add(btns, BorderLayout.SOUTH);

        return panel;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CENTER — Column / row editor
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel buildEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(MAIN_BG);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // ── Top toolbar: name + columns ──────────────────────────────────────
        nameField.setPreferredSize(new Dimension(100, 28));
        colsField.setPreferredSize(new Dimension(220, 28));

        JButton applyColsBtn = greenBtn("Apply Columns");
        applyColsBtn.addActionListener(e -> applyColumns());

        JLabel nameLbl = label("Relation name:");
        JLabel colsLbl = label("Columns (comma-separated):");

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.setOpaque(false);
        toolbar.add(nameLbl);   toolbar.add(nameField);
        toolbar.add(Box.createHorizontalStrut(16));
        toolbar.add(colsLbl);   toolbar.add(colsField);
        toolbar.add(applyColsBtn);

        JPanel toolCard = card("Schema", toolbar);

        // ── Grid ─────────────────────────────────────────────────────────────
        JScrollPane gridScroll = new JScrollPane(grid);
        gridScroll.setBorder(BorderFactory.createEmptyBorder());
        gridScroll.getViewport().setBackground(CARD_BG);
        JPanel gridCard = card("Rows", gridScroll);

        // ── Row buttons + fill presets ────────────────────────────────────────
        JButton addRowBtn = tealBtn("+ Add Row");
        JButton delRowBtn = tealBtn("− Delete Row");
        JButton clearBtn  = tealBtn("Clear All");

        addRowBtn.addActionListener(e -> addRow());
        delRowBtn.addActionListener(e -> deleteRow());
        clearBtn .addActionListener(e -> clearRows());

        String[] presets = {
            "── Fill Preset ──",
            "All-Match (same key in all rels)",
            "No-Match  (unique keys, no joins)",
            "Skewed    (90 % one key, 10 % random)",
            "Dense     (every key appears ≥ 3×)",
            "Sparse    (each key appears once)"
        };
        JComboBox<String> presetBox = new JComboBox<>(presets);
        presetBox.setFont(FONT_LABEL);
        presetBox.setBackground(CARD_BG);

        JTextField rowCountField = new JTextField("20", 5);
        rowCountField.setFont(FONT_LABEL);

        JButton fillBtn = greenBtn("Fill");
        fillBtn.addActionListener(e -> {
            int n;
            try { n = Integer.parseInt(rowCountField.getText().trim()); }
            catch (NumberFormatException ex) { n = 20; }
            fillPreset(presetBox.getSelectedIndex(), n);
        });

        JPanel rowBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        rowBar.setBackground(HDR_BG);
        rowBar.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_COL));
        rowBar.add(addRowBtn); rowBar.add(delRowBtn); rowBar.add(clearBtn);
        rowBar.add(Box.createHorizontalStrut(20));
        rowBar.add(new JLabel("Rows:"));
        rowBar.add(rowCountField);
        rowBar.add(presetBox);
        rowBar.add(fillBtn);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, toolCard, gridCard);
        split.setResizeWeight(0.0);
        split.setDividerSize(4);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setBackground(MAIN_BG);

        panel.add(split,   BorderLayout.CENTER);
        panel.add(rowBar,  BorderLayout.SOUTH);
        return panel;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  RIGHT — Result log + Run button
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel buildResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(330, 0));
        panel.setBackground(MAIN_BG);
        panel.setBorder(new CompoundBorder(
            new MatteBorder(0, 1, 0, 0, BORDER_COL),
            new EmptyBorder(12, 12, 12, 12)
        ));

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createEmptyBorder());
        logScroll.getViewport().setBackground(logArea.getBackground());
        JPanel logCard = card("Output", logScroll);

        JButton runBtn   = bigGreenBtn("▶  Run WCOJ on Custom Tables");
        JButton clearLog = tealBtn("Clear Log");
        runBtn  .addActionListener(e -> runWCOJ());
        clearLog.addActionListener(e -> logArea.setText(""));

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnBar.setBackground(HDR_BG);
        btnBar.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_COL));
        btnBar.add(runBtn);
        btnBar.add(clearLog);

        panel.add(logCard, BorderLayout.CENTER);
        panel.add(btnBar,  BorderLayout.SOUTH);
        return panel;
    }

    // ── Status bar ─────────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        statusLbl.setFont(FONT_SMALL);
        statusLbl.setForeground(TEXT_SEC);
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(CARD_BG);
        bar.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER_COL),
            new EmptyBorder(4, 14, 4, 14)
        ));
        bar.add(statusLbl, BorderLayout.WEST);
        return bar;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LOGIC — Relation management
    // ══════════════════════════════════════════════════════════════════════════
    private void addRelation() {
        String base = "R";
        int n = 1;
        while (schemas.containsKey(base + n)) n++;
        String name = base + n;
        schemas.put(name, new ArrayList<>(Arrays.asList("A", "B")));
        data   .put(name, new ArrayList<>());
        listModel.addElement(name);
        relList.setSelectedValue(name, true);
        refreshStatus();
    }

    private void deleteRelation() {
        if (selected == null) return;
        schemas.remove(selected);
        data   .remove(selected);
        listModel.removeElement(selected);
        selected = null;
        clearGrid(new String[]{});
        nameField.setText("");
        colsField.setText("");
        refreshStatus();
    }

    private void selectRelation(String name) {
        if (name == null) return;
        // Flush current grid edits before switching
        if (selected != null && grid.isEditing()) grid.getCellEditor().stopCellEditing();
        if (selected != null) saveGridToData(selected);
        selected = name;
        nameField.setText(name);
        List<String> cols = schemas.get(name);
        colsField.setText(String.join(", ", cols));
        loadGridFromData(name);
        refreshStatus();
    }

    private void applyColumns() {
        if (selected == null) { status("Select a relation first.", WARN_COLOR); return; }
        String newName = nameField.getText().trim();
        if (newName.isEmpty()) { status("Relation name cannot be empty.", ERR_COLOR); return; }
        String colText = colsField.getText().trim();
        if (colText.isEmpty()) { status("Enter at least one column.", ERR_COLOR); return; }
        List<String> cols = parseColumns(colText);
        if (cols.isEmpty()) { status("Invalid column definition.", ERR_COLOR); return; }

        // Rename if changed
        if (!newName.equals(selected)) {
            if (schemas.containsKey(newName)) {
                status("A relation named \"" + newName + "\" already exists.", ERR_COLOR); return;
            }
            // Transfer data under new name
            List<String>        oldSchema = schemas.remove(selected);
            List<List<String>>  oldData   = data   .remove(selected);
            schemas.put(newName, oldSchema);
            data   .put(newName, oldData);
            int idx = listModel.indexOf(selected);
            listModel.set(idx, newName);
            selected = newName;
        }

        schemas.put(selected, cols);
        data.put(selected, new ArrayList<>()); // reset rows when columns change
        loadGridFromData(selected);
        status("Columns applied to " + selected + " — rows cleared.", OK_COLOR);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LOGIC — Row operations
    // ══════════════════════════════════════════════════════════════════════════
    private void addRow() {
        if (selected == null) return;
        int cols = schemas.get(selected).size();
        Object[] blank = new String[cols];
        Arrays.fill(blank, "");
        gridModel.addRow(blank);
    }

    private void deleteRow() {
        if (selected == null) return;
        int row = grid.getSelectedRow();
        if (row >= 0) gridModel.removeRow(row);
    }

    private void clearRows() {
        if (selected == null) return;
        gridModel.setRowCount(0);
        data.get(selected).clear();
    }

    /**
     * Fill presets — generates rows with specific extreme-case patterns across
     * ALL currently defined relations so that the join behaviour is predictable.
     */
    private void fillPreset(int presetIndex, int rowCount) {
        if (schemas.isEmpty()) { status("Define at least one relation first.", WARN_COLOR); return; }
        if (presetIndex == 0)  { status("Choose a fill preset first.", WARN_COLOR); return; }
        Random rng = new Random();

        // Collect all shared column names (join keys) across all relations
        List<String> allCols = new ArrayList<>();
        for (List<String> cols : schemas.values()) for (String c : cols) if (!allCols.contains(c)) allCols.add(c);

        // Domain pools: column → possible values used in this fill
        Map<String, String[]> pools = new LinkedHashMap<>();
        for (String col : allCols) {
            switch (presetIndex) {
                case 1: // All-Match: single value per join col
                    pools.put(col, new String[]{ col.toLowerCase() });
                    break;
                case 2: // No-Match: unique values per row (filled at generation time)
                    pools.put(col, null); // handled inline
                    break;
                case 3: // Skewed: one dominant + a few others
                    String[] sk = new String[10];
                    sk[0] = col.toLowerCase();
                    for (int i = 1; i < 10; i++) sk[i] = String.valueOf((char)('a' + i));
                    pools.put(col, sk);
                    break;
                case 4: // Dense: small domain so lots of repeats
                    String[] dn = new String[Math.max(2, rowCount / 5)];
                    for (int i = 0; i < dn.length; i++) dn[i] = String.valueOf((char)('a' + (i % 26)));
                    pools.put(col, dn);
                    break;
                case 5: // Sparse: large domain, each key once
                    pools.put(col, null); // handled inline (unique counter)
                    break;
            }
        }

        // Counter for unique-value presets (No-Match, Sparse)
        Map<String, Integer> counters = new HashMap<>();
        for (String col : allCols) counters.put(col, 0);

        for (Map.Entry<String, List<String>> entry : schemas.entrySet()) {
            String relName  = entry.getKey();
            List<String> cols = entry.getValue();
            List<List<String>> rows = new ArrayList<>();

            for (int i = 0; i < rowCount; i++) {
                List<String> row = new ArrayList<>();
                for (String col : cols) {
                    String[] pool = pools.get(col);
                    String val;
                    if (pool == null) {
                        // Unique value: counter across all relations for this col
                        int cnt = counters.get(col);
                        val = String.valueOf((char)('a' + (cnt % 26))) + (cnt / 26 == 0 ? "" : cnt / 26);
                        counters.put(col, cnt + 1);
                    } else if (presetIndex == 3 && col.equals(cols.get(0))) {
                        // Skewed: 90 % dominant
                        val = rng.nextDouble() < 0.9 ? pool[0] : pool[1 + rng.nextInt(pool.length - 1)];
                    } else {
                        val = pool[rng.nextInt(pool.length)];
                    }
                    row.add(val);
                }
                rows.add(row);
            }
            data.put(relName, rows);
        }

        // Refresh grid for the currently selected relation
        if (selected != null) loadGridFromData(selected);
        status("Filled " + schemas.size() + " relation(s) with preset #" + presetIndex
               + " (" + rowCount + " rows each).", OK_COLOR);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LOGIC — Run WCOJ
    // ══════════════════════════════════════════════════════════════════════════
    private void runWCOJ() {
        // Flush edits
        if (selected != null && grid.isEditing()) grid.getCellEditor().stopCellEditing();
        if (selected != null) saveGridToData(selected);

        if (schemas.isEmpty()) { status("Define at least one relation.", ERR_COLOR); return; }

        // Validate: all relations must have ≥ 1 row
        for (Map.Entry<String, List<List<String>>> e : data.entrySet()) {
            if (e.getValue().isEmpty()) {
                status("Relation \"" + e.getKey() + "\" has no rows — add rows or use a fill preset.", WARN_COLOR);
                return;
            }
        }

        // Build Relation objects
        Map<String, Relation> relations = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : schemas.entrySet()) {
            String relName = e.getKey();
            List<String> cols = e.getValue();
            Relation rel = new Relation(relName, cols);
            for (List<String> row : data.get(relName)) {
                // Pad or trim row to match column count
                Object[] vals = new Object[cols.size()];
                for (int i = 0; i < cols.size(); i++)
                    vals[i] = i < row.size() ? row.get(i) : "";
                rel.addRow(vals);
            }
            relations.put(relName, rel);
        }

        // Build query tree
        TreeNode root;
        try {
            root = QueryTreeBuilder.build(relations);
        } catch (Exception ex) {
            status("Tree build failed: " + ex.getMessage(), ERR_COLOR);
            return;
        }

        // Run WCOJ in background
        status("Running WCOJ…", new Color(22, 100, 160));
        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override protected Void doInBackground() {
                publish("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                publish("Custom WCOJ  —  " + relations.size() + " relation(s)\n");
                publish("Query tree : " + root.getLabel() + "\n");
                publish("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

                // Print each input table
                for (Relation rel : relations.values()) {
                    publish(rel.toTableString() + "\n\n");
                }

                try {
                    WorstCaseOptimalJoin wcoj = new WorstCaseOptimalJoin(relations, root);
                    double agm = wcoj.getSizeBound();
                    publish(String.format("AGM bound  : %.4f%n%n", agm));

                    long t0 = System.nanoTime();
                    Set<Tuple> result = wcoj.execute();
                    long ms = (System.nanoTime() - t0) / 1_000_000;

                    publish("Result — " + result.size() + " row(s)  [" + ms + " ms]\n");

                    if (result.isEmpty()) {
                        publish("(empty result — no matching tuples)\n");
                    } else {
                        // Collect all output column names (union of all schemas)
                        List<String> outCols = new ArrayList<>();
                        for (List<String> cols : schemas.values())
                            for (String c : cols) if (!outCols.contains(c)) outCols.add(c);
                        publish(buildResultTable(result, outCols) + "\n");
                    }
                } catch (Exception ex) {
                    publish("[ERROR] " + ex.getMessage() + "\n");
                }
                return null;
            }
            @Override protected void process(List<String> chunks) {
                for (String s : chunks) logArea.append(s);
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
            @Override protected void done() {
                status("Done.", OK_COLOR);
            }
        };
        worker.execute();
    }

    /** Renders result tuples as a plain-text box table (same style as Relation.toTableString). */
    private String buildResultTable(Set<Tuple> tuples, List<String>  cols) {
        int[] w = new int[cols.size()];
        for (int i = 0; i < cols.size(); i++) w[i] = cols.get(i).length();
        for (Tuple t : tuples)
            for (int i = 0; i < cols.size(); i++) {
                Object v = t.getValueByAttribute(cols.get(i));
                int len = v == null ? 4 : v.toString().length();
                if (len > w[i]) w[i] = len;
            }
        StringBuilder sep = new StringBuilder("+");
        for (int wi : w) sep.append("-").append("-".repeat(wi)).append("-+");
        String separator = sep.toString();
        StringBuilder sb = new StringBuilder();
        sb.append(separator).append("\n| ");
        for (int i = 0; i < cols.size(); i++) {
            sb.append(String.format("%-" + w[i] + "s", cols.get(i)));
            sb.append(i < cols.size() - 1 ? " | " : " |\n");
        }
        sb.append(separator).append("\n");
        for (Tuple t : tuples) {
            sb.append("| ");
            for (int i = 0; i < cols.size(); i++) {
                Object v = t.getValueByAttribute(cols.get(i));
                sb.append(String.format("%-" + w[i] + "s", v == null ? "null" : v));
                sb.append(i < cols.size() - 1 ? " | " : " |\n");
            }
        }
        sb.append(separator);
        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GRID SYNC
    // ══════════════════════════════════════════════════════════════════════════
    private void saveGridToData(String relName) {
        if (!data.containsKey(relName)) return;
        List<List<String>> rows = new ArrayList<>();
        for (int r = 0; r < gridModel.getRowCount(); r++) {
            List<String> row = new ArrayList<>();
            for (int c = 0; c < gridModel.getColumnCount(); c++) {
                Object v = gridModel.getValueAt(r, c);
                row.add(v == null ? "" : v.toString());
            }
            rows.add(row);
        }
        data.put(relName, rows);
    }

    private void loadGridFromData(String relName) {
        List<String> cols = schemas.get(relName);
        clearGrid(cols.toArray(new String[0]));
        for (List<String> row : data.get(relName)) {
            Object[] vals = row.stream().limit(cols.size()).toArray();
            // Pad if needed
            if (vals.length < cols.size()) {
                Object[] padded = Arrays.copyOf(vals, cols.size());
                Arrays.fill(padded, vals.length, cols.size(), "");
                vals = padded;
            }
            gridModel.addRow(vals);
        }
    }

    private void clearGrid(String[] cols) {
        gridModel = new DefaultTableModel(cols, 0);
        grid.setModel(gridModel);
        styleGridHeader();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  COMPONENT BUILDERS
    // ══════════════════════════════════════════════════════════════════════════
    private JTable buildGrid() {
        JTable t = new JTable(gridModel) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (isRowSelected(row)) {
                    c.setBackground(new Color(207, 232, 252));
                    c.setForeground(new Color(10, 50, 90));
                } else {
                    c.setBackground(row % 2 == 0 ? CARD_BG : ROW_ALT);
                    c.setForeground(TEXT_PRI);
                }
                ((JComponent) c).setBorder(new EmptyBorder(0, 8, 0, 8));
                return c;
            }
        };
        t.setFont(FONT_MONO);
        t.setRowHeight(26);
        t.setShowGrid(true);
        t.setGridColor(BORDER_COL);
        t.setBackground(CARD_BG);
        t.setForeground(TEXT_PRI);
        t.setFillsViewportHeight(true);
        styleGridHeader(t);
        return t;
    }

    private void styleGridHeader() { styleGridHeader(grid); }
    private void styleGridHeader(JTable t) {
        JTableHeader hdr = t.getTableHeader();
        hdr.setFont(FONT_BOLD);
        hdr.setBackground(HDR_BG);
        hdr.setForeground(TEXT_SEC);
        hdr.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_COL));
        hdr.setPreferredSize(new Dimension(0, 32));
        hdr.setReorderingAllowed(false);
    }

    private JTextArea buildLog() {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setFont(FONT_MONO);
        a.setBackground(new Color(250, 250, 252));
        a.setForeground(new Color(40, 55, 75));
        a.setBorder(new EmptyBorder(8, 10, 8, 10));
        a.setLineWrap(false);
        return a;
    }

    private JPanel card(String title, JComponent content) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(new LineBorder(BORDER_COL, 1));
        JLabel lbl = new JLabel("  " + title);
        lbl.setFont(FONT_BOLD);
        lbl.setForeground(TEXT_SEC);
        lbl.setOpaque(true);
        lbl.setBackground(HDR_BG);
        lbl.setPreferredSize(new Dimension(0, 32));
        lbl.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_COL),
            new EmptyBorder(0, 8, 0, 8)
        ));
        card.add(lbl, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JTextField darkField(String val) {
        JTextField f = new JTextField(val);
        f.setFont(FONT_LABEL);
        f.setBackground(SIDE_HOVER);
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);
        f.setBorder(new CompoundBorder(
            new LineBorder(SIDE_BORDER, 1, true),
            new EmptyBorder(3, 7, 3, 7)
        ));
        return f;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_LABEL);
        l.setForeground(TEXT_PRI);
        return l;
    }

    private JButton greenBtn(String text) {
        return styledBtn(text, new Color(22, 160, 100), new Color(16, 100, 70), Color.WHITE, false);
    }

    private JButton bigGreenBtn(String text) {
        JButton b = greenBtn(text);
        b.setFont(FONT_BOLD);
        b.setPreferredSize(new Dimension(220, 34));
        return b;
    }

    private JButton tealBtn(String text) {
        return styledBtn(text, SIDE_HOVER, new Color(40, 90, 115), SIDE_TEXT, false);
    }

    private JButton smallDarkBtn(String text) {
        return styledBtn(text, SIDE_HOVER, new Color(40, 90, 115), SIDE_TEXT, true);
    }

    private JButton styledBtn(String text, Color base, Color hover, Color fg, boolean small) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() || getModel().isPressed() ? hover : base);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 7, 7));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(small ? FONT_SMALL : FONT_LABEL);
        b.setForeground(fg);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (small) b.setPreferredSize(new Dimension(150, 28));
        return b;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────
    private List<String> parseColumns(String text) {
        List<String> cols = new ArrayList<>();
        for (String part : text.split(",")) {
            String col = part.trim().toUpperCase();
            if (!col.isEmpty()) cols.add(col);
        }
        return cols;
    }

    private void status(String msg, Color color) {
        statusLbl.setText(msg);
        statusLbl.setForeground(color);
    }

    private void refreshStatus() {
        int rels = schemas.size();
        int rows = selected != null && data.get(selected) != null ? data.get(selected).size() : 0;
        String sel = selected != null ? "  |  Editing: " + selected + " (" + rows + " rows)" : "";
        status(rels + " relation(s) defined" + sel, TEXT_SEC);
    }
}
