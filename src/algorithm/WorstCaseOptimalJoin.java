package algorithm;

import database.Relation;
import database.Tuple;
import tree.TreeNode;
import tree.Result;
import java.util.*;

/**
 * Worst-Case Optimal Join (WCOJ) — Loomis-Whitney skew-aware algorithm.
 *
 * Each internal node of the query tree partitions join-key values into:
 *   G  (heavy hitters) — joined eagerly, added to the complete output C
 *   F\G (light hitters) — fully joined within the subtree but deferred to D,
 *                         materialized at the root
 *
 * The threshold that separates heavy from light is:
 *   threshold = |F| / |D_R|
 * where F = π_λ(D_L) ∩ π_λ(D_R) is the set of matching join-key projections.
 */
public class WorstCaseOptimalJoin {

    private final Map<String, Relation> relations;
    private final TreeNode tree;
    /** Precomputed schema (ordered attribute list) for every subtree node. */
    private final Map<TreeNode, List<String>> nodeSchemas;

    public WorstCaseOptimalJoin(Map<String, Relation> relations, TreeNode tree) {
        this.relations   = relations;
        this.tree        = tree;
        this.nodeSchemas = new HashMap<>();
        computeNodeSchemas(tree);
    }

    // ── Schema utilities ───────────────────────────────────────────────────────

    /** Post-order: cache ordered attribute list for every subtree. */
    private List<String> computeNodeSchemas(TreeNode node) {
        if (node.isLeaf()) {
            Relation rel = relations.get(node.getLabel());
            List<String> schema = rel != null ? new ArrayList<>(rel.getSchema()) : new ArrayList<>();
            nodeSchemas.put(node, schema);
            return schema;
        }
        List<String> leftSchema  = computeNodeSchemas(node.leftChild());
        List<String> rightSchema = computeNodeSchemas(node.rightChild());
        Set<String>  unionSet    = new LinkedHashSet<>(leftSchema);
        unionSet.addAll(rightSchema);
        List<String> unionSchema = new ArrayList<>(unionSet);
        nodeSchemas.put(node, unionSchema);
        return unionSchema;
    }

    /**
     * λ(x) = attributes shared between x's left and right child subtrees.
     * Used as the join separator at this tree node.
     */
    private List<String> getLambda(TreeNode node) {
        if (node.isLeaf()) return new ArrayList<>();
        List<String> leftSchema  = nodeSchemas.get(node.leftChild());
        List<String> rightSchema = nodeSchemas.get(node.rightChild());
        List<String> common = new ArrayList<>();
        for (String attr : leftSchema) {
            if (rightSchema.contains(attr)) common.add(attr);
        }
        return common;
    }

    // ── AGM bound ─────────────────────────────────────────────────────────────

    /**
     * Global AGM output-size bound: ∏_R |R|^{1/d(R)}
     * where d(R) = max degree of any attribute in R's schema.
     */
    public double getSizeBound() {
        if (relations.isEmpty()) return 1.0;
        Map<String, Integer> degree = new HashMap<>();
        for (Relation rel : relations.values()) {
            for (String a : rel.getSchema()) degree.merge(a, 1, Integer::sum);
        }
        double bound = 1.0;
        for (Relation rel : relations.values()) {
            if (rel.isEmpty()) return 0.0;
            int maxDeg = rel.getSchema().stream()
                            .mapToInt(a -> degree.getOrDefault(a, 1))
                            .max().orElse(1);
            bound *= Math.pow(rel.size(), 1.0 / maxDeg);
        }
        return Math.max(bound, 1.0);
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    public Set<Tuple> execute() {
        Result result = loomisWhitney(tree);
        return result.getC();
    }

    // ── Core recursive algorithm ───────────────────────────────────────────────

    /**
     * loomisWhitney(x) returns Result(C, D) where:
     *   C = tuples fully joined (heavy-hitter groups + children's C sets)
     *   D = fully-joined tuples for light-hitter groups, deferred to be
     *       materialized at the root or joined at a higher level
     *
     * Invariant: every tuple in D carries the complete schema of the subtree
     * rooted at x, so it can be joined with x's sibling at the parent.
     */
    private Result loomisWhitney(TreeNode node) {

        // ── Base case: leaf node ──────────────────────────────────────────────
        if (node.isLeaf()) {
            Relation rel = relations.get(node.getLabel());
            Set<Tuple> D = new HashSet<>();
            if (rel != null) {
                // Stamp attribute maps so projectOn / join work correctly
                Map<String, Integer> attrMap = new HashMap<>();
                List<String> schema = rel.getSchema();
                for (int i = 0; i < schema.size(); i++) attrMap.put(schema.get(i), i);
                for (Tuple t : rel.getTuples()) {
                    if (t.getAttributeMap().isEmpty()) t.setAttributeMap(attrMap);
                    D.add(t);
                }
            }
            return new Result(new HashSet<>(), D);
        }

        // ── Recursive case ────────────────────────────────────────────────────
        Result left  = loomisWhitney(node.leftChild());
        Result right = loomisWhitney(node.rightChild());

        Set<Tuple> C_L = left.getC(),  D_L = left.getD();
        Set<Tuple> C_R = right.getC(), D_R = right.getD();

        List<String> lambda = getLambda(node);

        // F = π_λ(D_L) ∩ π_λ(D_R) — matching join-key values
        Set<Tuple> F = project(D_L, lambda);
        F.retainAll(project(D_R, lambda));

        // threshold = |F| / |D_R|;  G = selectTop(F, threshold)
        int P         = Math.max(1, F.size());
        int threshold = Math.max(1, P / Math.max(1, D_R.size()));
        Set<Tuple> G  = selectTop(F, threshold);

        Set<Tuple> C;
        Set<Tuple> D;

        if (node.isRoot()) {
            // Materialise everything at the root
            C = join(D_L, D_R);
            C.addAll(C_L);
            C.addAll(C_R);
            D = new HashSet<>();
        } else {
            // Heavy hitters (G) → join eagerly → C
            C = conditionalJoin(D_L, D_R, G, lambda);
            C.addAll(C_L);
            C.addAll(C_R);

            // Light hitters (F \ G) → full join within subtree → D
            // D carries complete-schema tuples so the parent can use them directly
            Set<Tuple> lightKeys = new HashSet<>(F);
            lightKeys.removeAll(G);
            D = conditionalJoin(D_L, D_R, lightKeys, lambda);
        }

        return new Result(C, D);
    }

    // ── Join helpers ───────────────────────────────────────────────────────────

    /**
     * Standard nested-loop join: left ⋈ right on all common attributes.
     */
    private Set<Tuple> join(Set<Tuple> left, Set<Tuple> right) {
        Set<Tuple> result = new HashSet<>();
        for (Tuple l : left) {
            for (Tuple r : right) {
                if (l.canJoin(r)) result.add(l.join(r));
            }
        }
        return result;
    }

    /**
     * Conditional join: left ⋈ right restricted to tuples whose λ-projection
     * is contained in {@code keys}.
     */
    private Set<Tuple> conditionalJoin(Set<Tuple> left, Set<Tuple> right,
                                        Set<Tuple> keys, List<String> lambda) {
        if (keys.isEmpty() || left.isEmpty() || right.isEmpty()) return new HashSet<>();
        Set<Tuple> result = new HashSet<>();
        for (Tuple l : left) {
            if (!keys.contains(l.projectOn(lambda))) continue;
            for (Tuple r : right) {
                if (l.canJoin(r)) result.add(l.join(r));
            }
        }
        return result;
    }

    // ── Projection / selection helpers ────────────────────────────────────────

    /**
     * Project all tuples in {@code tuples} onto {@code attrs}.
     * Returns a fresh mutable set (so callers can call retainAll / removeAll).
     */
    private Set<Tuple> project(Set<Tuple> tuples, List<String> attrs) {
        Set<Tuple> projected = new HashSet<>();
        if (attrs.isEmpty()) return projected;
        for (Tuple t : tuples) projected.add(t.projectOn(attrs));
        return projected;
    }

    /**
     * selectTop(F, k) — returns the first k elements of F.
     * These are the "heavy" join-key values that trigger an eager join.
     */
    private Set<Tuple> selectTop(Set<Tuple> tuples, int k) {
        Set<Tuple> selected = new HashSet<>();
        int count = 0;
        for (Tuple t : tuples) {
            if (count++ >= k) break;
            selected.add(t);
        }
        return selected;
    }
}
