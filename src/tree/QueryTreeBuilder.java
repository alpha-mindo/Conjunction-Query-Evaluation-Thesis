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
     * Build and return the root of a binary query tree for Loomis-Whitney.
     *
     * @param relations map of relation-name → Relation (must not be empty)
     * @return root TreeNode of the constructed join tree
     * @throws IllegalArgumentException if {@code relations} is empty
     */
    public static TreeNode buildForLoomisWhitney(Map<String, Relation> relations) {
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

    /**
     * Builds a query tree specifically for the General Recursive Join (Worst-Case Optimal Join).
     * 
     * @param relations map of relation-name → Relation
     * @return root TreeNode of the constructed join tree
     */
    public static TreeNode buildForRecursiveJoin(Map<String, Relation> relations) {
        if (relations == null || relations.isEmpty()) {
            throw new IllegalArgumentException("Cannot build a query tree from an empty relation set.");
        }
        
        // 1. Build Hypergraph
        List<Set<String>> hypergraph = buildHypergraph(relations);
        
        // 2. Compute Fractional Edge Cover
        Map<String, Double> weights = computeFractionalCover(relations);
        
        // 3. Construct Recursive Query Tree
        List<TreeNode> leaves = new ArrayList<>();
        for (Map.Entry<String, Relation> entry : relations.entrySet()) {
            TreeNode leaf = new TreeNode(entry.getKey());
            leaf.setUniverse(entry.getValue().getSchema());
            leaves.add(leaf);
        }
        
        TreeNode root = buildRecursiveTree(leaves, weights);
        return root;
    }

    private static List<Set<String>> buildHypergraph(Map<String, Relation> relations) {
        List<Set<String>> hypergraph = new ArrayList<>();
        for (Relation r : relations.values()) {
            hypergraph.add(new HashSet<>(r.getSchema()));
        }
        return hypergraph;
    }

    /**
     * Computes a rough greedy fractional edge cover.
     * Since Java lacks a built-in LP solver, this heuristic assigns weights 
     * based on attribute overlap frequency.
     */
    private static Map<String, Double> computeFractionalCover(Map<String, Relation> relations) {
        Map<String, Double> weights = new HashMap<>();
        Map<String, Integer> attributeFrequencies = new HashMap<>();
        
        // Count frequencies of each attribute
        for (Relation r : relations.values()) {
            for (String attr : r.getSchema()) {
                attributeFrequencies.put(attr, attributeFrequencies.getOrDefault(attr, 0) + 1);
            }
        }
        
        // Assign a fractional weight to each relation based on the max inverse frequency of its attributes
        for (Map.Entry<String, Relation> entry : relations.entrySet()) {
            double maxWeightNeeded = 0.0;
            for (String attr : entry.getValue().getSchema()) {
                double weightForAttr = 1.0 / attributeFrequencies.get(attr);
                if (weightForAttr > maxWeightNeeded) {
                    maxWeightNeeded = weightForAttr;
                }
            }
            // For general WCOJ, edge weight e_k is attached to the relation
            weights.put("e_" + entry.getKey(), maxWeightNeeded);
        }
        
        return weights;
    }

    /**
     * Recursively partitions relations into a binary tree structure.
     */
    private static TreeNode buildRecursiveTree(List<TreeNode> nodes, Map<String, Double> weights) {
        if (nodes.size() == 1) {
            return nodes.get(0);
        }

        // Greedy partition: pick two nodes with the highest overlap to merge
        int bestI = 0, bestJ = 1;
        int maxOverlap = -1;

        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                Set<String> attrsI = new HashSet<>(nodes.get(i).getUniverse()); // getUniverse needs to reflect current subtree attributes
                Set<String> attrsJ = new HashSet<>(nodes.get(j).getUniverse());
                attrsI.retainAll(attrsJ);
                if (attrsI.size() > maxOverlap) {
                    maxOverlap = attrsI.size();
                    bestI = i;
                    bestJ = j;
                }
            }
        }

        TreeNode left = nodes.remove(Math.max(bestI, bestJ));
        TreeNode right = nodes.remove(Math.min(bestI, bestJ));

        TreeNode internal = new TreeNode(left.getLabel() + "_" + right.getLabel());
        internal.setLeft(left);
        internal.setRight(right);
        
        // Define the universe for the new internal node (union of children's universes)
        Set<String> union = new HashSet<>();
        if (left.getUniverse() != null) union.addAll(left.getUniverse());
        if (right.getUniverse() != null) union.addAll(right.getUniverse());
        internal.setUniverse(new ArrayList<>(union));
        
        // Set edge attributes (separator attributes)
        Set<String> separator = new HashSet<>();
        if (left.getUniverse() != null) separator.addAll(left.getUniverse());
        if (right.getUniverse() != null) separator.retainAll(right.getUniverse());
        internal.setEdgeK(new ArrayList<>(separator));
        
        nodes.add(internal);

        return buildRecursiveTree(nodes, weights);
    }
    
    /**
     * Traverses the tree to generate a printable join plan.
     */
    public static String getJoinPlan(TreeNode node) {
        if (node.isLeaf()) {
            return "Scan(" + node.getLabel() + ")";
        }
        String leftPlan = getJoinPlan(node.leftChild());
        String rightPlan = getJoinPlan(node.rightChild());
        
        return "Join [" + String.join(",", node.getEdgeK()) + "] (" + leftPlan + " ⨝ " + rightPlan + ")";
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
