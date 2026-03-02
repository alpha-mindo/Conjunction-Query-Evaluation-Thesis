package tree;

import database.Relation;
import java.util.*;

/**
 * Automatically builds a binary query tree from a map of relations.
 *
 * <p>The algorithm is a greedy bottom-up merge:
 * <ol>
 *   <li>Start with one leaf node per relation.</li>
 *   <li>Repeatedly pick the pair of subtrees whose schemas share the most
 *       attributes (i.e. the richest join condition) and merge them under a
 *       fresh internal node.</li>
 *   <li>Repeat until a single root remains.</li>
 * </ol>
 *
 * <p>This heuristic keeps join columns high in the tree, which matches the
 * Loomis-Whitney / WCOJ requirement that every internal node's λ (the set of
 * attributes shared between its two child subtrees) is non-empty wherever
 * possible.
 *
 * <p>If two pairs tie on overlap count the one with the smaller combined size
 * is preferred (fewer rows to join eagerly).
 */
public class QueryTreeBuilder {

    /**
     * Build and return the root of a binary query tree for the given relations.
     *
     * @param relations map of relation-name → Relation (must not be empty)
     * @return root TreeNode of the constructed join tree
     * @throws IllegalArgumentException if {@code relations} is empty
     */
    public static TreeNode build(Map<String, Relation> relations) {
        if (relations == null || relations.isEmpty()) {
            throw new IllegalArgumentException("Cannot build a query tree from an empty relation set.");
        }

        // Each entry tracks: the subtree node + the union of all attribute names in that subtree
        List<SubtreeInfo> pool = new ArrayList<>();
        for (Map.Entry<String, Relation> entry : relations.entrySet()) {
            TreeNode leaf = new TreeNode(entry.getKey());
            Set<String> attrs = new LinkedHashSet<>(entry.getValue().getSchema());
            int rows = entry.getValue().size();
            pool.add(new SubtreeInfo(leaf, attrs, rows));
        }

        // Special case: single relation
        if (pool.size() == 1) {
            return pool.get(0).node;
        }

        // Greedy merge until one node remains
        while (pool.size() > 1) {
            int bestI = -1, bestJ = -1;
            int bestOverlap = -1;
            int bestSize = Integer.MAX_VALUE;

            for (int i = 0; i < pool.size(); i++) {
                for (int j = i + 1; j < pool.size(); j++) {
                    int overlap = countOverlap(pool.get(i).attrs, pool.get(j).attrs);
                    int combinedSize = pool.get(i).rows + pool.get(j).rows;
                    if (overlap > bestOverlap
                            || (overlap == bestOverlap && combinedSize < bestSize)) {
                        bestOverlap = overlap;
                        bestSize    = combinedSize;
                        bestI = i;
                        bestJ = j;
                    }
                }
            }

            SubtreeInfo left  = pool.get(bestI);
            SubtreeInfo right = pool.get(bestJ);

            // Create internal node labelled with the shared attributes for readability
            String label = joinLabel(left.node.getLabel(), right.node.getLabel());
            TreeNode internal = new TreeNode(label);
            internal.setLeft(left.node);
            internal.setRight(right.node);

            // Union of schemas; combined row count is an approximate upper bound
            Set<String> unionAttrs = new LinkedHashSet<>(left.attrs);
            unionAttrs.addAll(right.attrs);
            int unionRows = left.rows + right.rows; // rough estimate

            // Remove merged pair, add new internal node (remove higher index first)
            pool.remove(Math.max(bestI, bestJ));
            pool.remove(Math.min(bestI, bestJ));
            pool.add(new SubtreeInfo(internal, unionAttrs, unionRows));
        }

        return pool.get(0).node;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static int countOverlap(Set<String> a, Set<String> b) {
        int count = 0;
        for (String attr : a) if (b.contains(attr)) count++;
        return count;
    }

    private static String joinLabel(String left, String right) {
        return "(" + left + "⋈" + right + ")";
    }

    // ── internal record ───────────────────────────────────────────────────────

    private static class SubtreeInfo {
        final TreeNode   node;
        final Set<String> attrs;
        int               rows;

        SubtreeInfo(TreeNode node, Set<String> attrs, int rows) {
            this.node  = node;
            this.attrs = attrs;
            this.rows  = rows;
        }
    }
}
