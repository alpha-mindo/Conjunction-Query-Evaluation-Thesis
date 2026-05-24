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
        
        int m = relations.size();
        Set<String> V = new HashSet<>();
        List<Set<String>> edges = new ArrayList<>();
        
        // Use 1-based indexing for edges to match Algorithm 3
        edges.add(new HashSet<>()); // dummy at index 0
        List<String> orderedKeys = new ArrayList<>(relations.keySet());
        for (int i = 1; i <= m; i++) {
            Relation r = relations.get("R" + i);
            if (r != null) {
                edges.add(new HashSet<>(r.getSchema()));
                V.addAll(r.getSchema());
            } else {
                r = relations.get(orderedKeys.get(i - 1));
                edges.add(new HashSet<>(r.getSchema()));
                V.addAll(r.getSchema());
            }
        }
        
        return buildTreeAlg3(V, m, edges);
    }

    /**
     * Algorithm 3: Constructing the query plan tree T
     * 1: if ei ∩ U = ∅, ∀i ∈ [k] then return nil
     * 2: Create a node u with label(u) ← k and univ(u) = U
     * 3: if k > 1 and ∄ i ∈ [k] such that U ⊆ ei then
     * 4:   lc(u) ← build-tree(U \ ek, k − 1)
     * 5:   rc(u) ← build-tree(U ∩ ek, k − 1)
     * 6: return u
     */
    private static TreeNode buildTreeAlg3(Set<String> U, int k, List<Set<String>> edges) {
        // Condition 1: if ei \cap U = \emptyset for all i \in [k]
        boolean allEmpty = true;
        for (int i = 1; i <= k; i++) {
            Set<String> intersection = new HashSet<>(edges.get(i));
            intersection.retainAll(U);
            if (!intersection.isEmpty()) {
                allEmpty = false;
                break;
            }
        }
        
        if (allEmpty) {
            return null;
        }

        // Condition 2: Create node u
        TreeNode u = new TreeNode(String.valueOf(k));
        u.setUniverse(new ArrayList<>(U));
        // Set edgeK = ek for use in the recursive join algorithm
        u.setEdgeK(new ArrayList<>(edges.get(k)));

        // Condition 3: if k > 1 and there is NO i in [k] such that U is fully covered by e_i
        boolean isCoveredBySingleEdge = false;
        for (int i = 1; i <= k; i++) {
            if (edges.get(i).containsAll(U)) {
                isCoveredBySingleEdge = true;
                break;
            }
        }

        if (k > 1 && !isCoveredBySingleEdge) {
            // lc(u) <- buildTree(U \ ek, k - 1)
            Set<String> U_minus_ek = new HashSet<>(U);
            U_minus_ek.removeAll(edges.get(k));
            u.setLeft(buildTreeAlg3(U_minus_ek, k - 1, edges));

            // rc(u) <- buildTree(U \cap ek, k - 1)
            Set<String> U_cap_ek = new HashSet<>(U);
            U_cap_ek.retainAll(edges.get(k));
            u.setRight(buildTreeAlg3(U_cap_ek, k - 1, edges));
        }

        return u;
    }

    /**
     * Algorithm 4: Computing a total order of attributes in V
     * 1: Let T be the query plan tree with root node u, where univ(u) = V
     * 2: return getAttributesOrder(u)
     */
    public static List<String> computeTotalAttributeOrder(TreeNode root) {
        List<String> order = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        printAttribs(root, order, visited);
        return order;
    }

    private static void printAttribs(TreeNode u, List<String> order, Set<String> visited) {
        if (u == null) return;

        if (u.isLeaf()) {
            // print all attributes in univ(u) in an arbitrary order
            for (String attr : u.getUniverse()) {
                if (visited.add(attr)) {
                    order.add(attr);
                }
            }
        } else if (u.leftChild() == null) {
            printAttribs(u.rightChild(), order, visited);
        } else if (u.rightChild() == null) {
            printAttribs(u.leftChild(), order, visited);
            // print all attributes in univ(u) \ univ(lc(u)) in an arbitrary order
            Set<String> diff = new HashSet<>(u.getUniverse());
            if (u.leftChild() != null && u.leftChild().getUniverse() != null) {
                diff.removeAll(u.leftChild().getUniverse());
            }
            for (String attr : diff) {
                if (visited.add(attr)) {
                    order.add(attr);
                }
            }
        } else {
            printAttribs(u.leftChild(), order, visited);
            printAttribs(u.rightChild(), order, visited);
        }
    }

    /**
     * Computes a rough greedy fractional edge cover.
     * Since Java lacks a built-in LP solver, this heuristic assigns weights 
     * based on attribute overlap frequency.
     * 
     * This is made public so the executor algorithm (like RecursiveJoinAlgorithm)
     * can utilize these weights during the recursive join phase.
     */
    public static Map<String, Double> computeFractionalCover(Map<String, Relation> relations) {
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
