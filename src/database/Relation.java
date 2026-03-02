package database;

import java.util.*;

/**
 * A relation modelled as a SQL table: named columns (schema) and ordered rows.
 * Every row (Tuple) added to this relation is automatically stamped with the
 * column-to-index map so attribute-based lookups work without extra setup.
 */
public class Relation {

    private final String       name;
    private final List<String> columns; // ordered column names
    private final List<Tuple>  rows;    // ordered rows (insertion order)

    // ── Constructors ──────────────────────────────────────────────────────────

    public Relation(String name, List<String> columns) {
        this.name    = name;
        this.columns = new ArrayList<>(columns);
        this.rows    = new ArrayList<>();
    }

    // ── Schema / column info ──────────────────────────────────────────────────

    public String getName() { return name; }

    /** Ordered list of column names. */
    public List<String> getColumns() { return Collections.unmodifiableList(columns); }

    /** Alias kept for algorithm compatibility. */
    public List<String> getSchema() { return getColumns(); }

    public int columnCount() { return columns.size(); }

    /** Returns the 0-based index of a column, or -1 if not found. */
    public int columnIndex(String col) { return columns.indexOf(col); }

    // ── Row operations ────────────────────────────────────────────────────────

    /**
     * Add a pre-built Tuple as a new row.
     * The tuple's column-to-index map is stamped automatically.
     */
    public void addTuple(Tuple tuple) {
        tuple.setAttributeMap(buildAttributeMap());
        rows.add(tuple);
    }

    /**
     * Convenience method: add a row directly from column values.
     * The number of values must match the number of columns.
     */
    public void addRow(Object... values) {
        if (values.length != columns.size()) {
            throw new IllegalArgumentException(
                "Expected " + columns.size() + " column(s) but got " + values.length);
        }
        Tuple t = new Tuple(new ArrayList<>(Arrays.asList(values)));
        t.setAttributeMap(buildAttributeMap());
        rows.add(t);
    }

    public Tuple getRow(int index)  { return rows.get(index); }

    /** All rows in insertion order (read-only view). */
    public List<Tuple> getRows()    { return Collections.unmodifiableList(rows); }

    /** Alias kept for algorithm compatibility. */
    public List<Tuple> getTuples()  { return getRows(); }

    public int     size()    { return rows.size(); }
    public boolean isEmpty() { return rows.isEmpty(); }

    // ── Column projection ─────────────────────────────────────────────────────

    /** Returns every value in the named column across all rows. */
    public List<Object> getColumnValues(String col) {
        int idx = columnIndex(col);
        if (idx == -1) throw new IllegalArgumentException("Unknown column: " + col);
        List<Object> vals = new ArrayList<>(rows.size());
        for (Tuple row : rows) vals.add(row.getValue(idx));
        return vals;
    }

    // ── Display ───────────────────────────────────────────────────────────────

    /** Prints a formatted SQL-style table to stdout. */
    public void printTable() { System.out.println(toTableString()); }

    /** Returns a formatted SQL-style table as a String. */
    public String toTableString() {
        // Compute per-column widths (at least as wide as the header)
        int[] w = new int[columns.size()];
        for (int i = 0; i < columns.size(); i++) w[i] = columns.get(i).length();
        for (Tuple row : rows) {
            for (int i = 0; i < columns.size(); i++) {
                Object v = row.getValue(i);
                int len  = (v == null) ? 4 : v.toString().length();
                if (len > w[i]) w[i] = len;
            }
        }
        // Separator line
        StringBuilder sep = new StringBuilder("+");
        for (int wi : w) sep.append("-").append("-".repeat(wi)).append("-+");
        String separator = sep.toString();
        // Build output
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("\n");
        sb.append(separator).append("\n");
        // Header row
        sb.append("| ");
        for (int i = 0; i < columns.size(); i++) {
            sb.append(String.format("%-" + w[i] + "s", columns.get(i)));
            sb.append(i < columns.size() - 1 ? " | " : " |");
        }
        sb.append("\n").append(separator).append("\n");
        // Data rows
        for (Tuple row : rows) {
            sb.append("| ");
            for (int i = 0; i < columns.size(); i++) {
                Object v = row.getValue(i);
                sb.append(String.format("%-" + w[i] + "s", v == null ? "null" : v));
                sb.append(i < columns.size() - 1 ? " | " : " |");
            }
            sb.append("\n");
        }
        sb.append(separator);
        return sb.toString();
    }

    @Override
    public String toString() { return toTableString(); }

    // ── Internal ──────────────────────────────────────────────────────────────

    private Map<String, Integer> buildAttributeMap() {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) map.put(columns.get(i), i);
        return map;
    }
}
