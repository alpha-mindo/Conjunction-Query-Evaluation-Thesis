package visualization;

import tree.TreeNode;
import java.util.*;

/**
 * Snapshot of one recursive call inside the WCOJ algorithm.
 * Produced post-order by {@link TracingWCOJ}.
 */
public class AlgorithmStep {

    /** Lightweight table snapshot (column list + rows as strings). */
    public static class TableSnap {
        public final List<String>       cols;
        public final List<List<String>> rows;

        public TableSnap(List<String> cols, List<List<String>> rows) {
            this.cols = Collections.unmodifiableList(cols);
            this.rows = Collections.unmodifiableList(rows);
        }

        public boolean isEmpty() { return rows.isEmpty(); }
    }

    // ── Identification ────────────────────────────────────────────────────────
    /** The tree node being processed in this step. */
    public final TreeNode node;
    /** Short heading, e.g. "Leaf: R1" or "Internal ⋈ {A, B}". */
    public final String   heading;

    // ── Structural info ───────────────────────────────────────────────────────
    public final List<String> lambda;       // join attributes (empty for leaves)

    // ── Set sizes ─────────────────────────────────────────────────────────────
    public final int DL_size, DR_size;      // inputs from child D-sets
    public final int CL_size, CR_size;      // inputs from child C-sets
    public final int F_size;                // |π_λ(D_L) ∩ π_λ(D_R)|
    public final int G_size;                // heavy hitters
    public final int light_size;            // light hitters (F \ G)
    public final int C_out_size;            // output C
    public final int D_out_size;            // output D

    // ── Data snapshots ────────────────────────────────────────────────────────
    public final TableSnap F_snap;          // matching join-key projections
    public final TableSnap G_snap;          // heavy hitters
    public final TableSnap light_snap;      // light hitters
    public final TableSnap C_out_snap;      // complete (eager) output tuples
    public final TableSnap D_out_snap;      // deferred output tuples

    // ── Narrative ─────────────────────────────────────────────────────────────
    public final String narrative;

    public AlgorithmStep(
            TreeNode node, String heading, List<String> lambda,
            int DL_size, int DR_size, int CL_size, int CR_size,
            int F_size, int G_size, int light_size,
            int C_out_size, int D_out_size,
            TableSnap F_snap, TableSnap G_snap, TableSnap light_snap,
            TableSnap C_out_snap, TableSnap D_out_snap,
            String narrative) {

        this.node      = node;
        this.heading   = heading;
        this.lambda    = Collections.unmodifiableList(new ArrayList<>(lambda));

        this.DL_size   = DL_size;    this.DR_size   = DR_size;
        this.CL_size   = CL_size;    this.CR_size   = CR_size;
        this.F_size    = F_size;     this.G_size    = G_size;
        this.light_size = light_size;
        this.C_out_size = C_out_size; this.D_out_size = D_out_size;

        this.F_snap     = F_snap;
        this.G_snap     = G_snap;
        this.light_snap = light_snap;
        this.C_out_snap = C_out_snap;
        this.D_out_snap = D_out_snap;
        this.narrative  = narrative;
    }
}
