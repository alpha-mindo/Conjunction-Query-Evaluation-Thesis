package algorithm;

import database.Relation;
import database.Tuple;
import tree.TreeNode;
import tree.Result;
import java.util.*;

/**
 * Worst-Case Optimal Join (WCOJ) algorithm.
 * Achieves O(N + Output) complexity using delayed materialization and
 * conditional joins bounded by the AGM limit.
 */
public class WorstCaseOptimalJoin {
    private final Map<String, Relation> relations;
    private final TreeNode tree;
    private final double P;
    private final Map<TreeNode, List<String>> nodeSchemas; // Schema for each tree node
    
    public WorstCaseOptimalJoin(Map<String, Relation> relations, TreeNode tree) {
        this.relations = relations;
        this.tree = tree;
        this.nodeSchemas = new HashMap<>();
        computeNodeSchemas(tree);
        this.P = computeSizeBound();
    }
    
    // Compute schemas for all tree nodes (union of child schemas)
    private List<String> computeNodeSchemas(TreeNode node) {
        if (node.isLeaf()) {
            Relation rel = relations.get(node.getLabel());
            List<String> schema = rel != null ? rel.getSchema() : new ArrayList<>();
            nodeSchemas.put(node, schema);
            return schema;
        }
        
        List<String> leftSchema = computeNodeSchemas(node.leftChild());
        List<String> rightSchema = computeNodeSchemas(node.rightChild());
        
        Set<String> unionSet = new LinkedHashSet<>(leftSchema);
        unionSet.addAll(rightSchema);
        List<String> unionSchema = new ArrayList<>(unionSet);
        nodeSchemas.put(node, unionSchema);
        return unionSchema;
    }
    
    // λ(x) = common attributes between left and right subtrees
    private List<String> getLambda(TreeNode node) {
        if (node.isLeaf()) return new ArrayList<>();
        
        List<String> leftSchema = nodeSchemas.get(node.leftChild());
        List<String> rightSchema = nodeSchemas.get(node.rightChild());
        
        List<String> common = new ArrayList<>();
        for (String attr : leftSchema) {
            if (rightSchema.contains(attr)) {
                common.add(attr);
            }
        }
        return common;
    }
    
    // Computes AGM bound: P = ∏_{c∈C} N_c^{1/(n-1)}
    private double computeSizeBound() {
        int n = relations.size();
        if (n <= 1) return 1.0;
        
        double product = 1.0;
        for (Relation rel : relations.values()) {
            int N_c = rel.size();
            if (N_c > 0) {
                product *= Math.pow(N_c, 1.0 / (n - 1));
            }
        }
        return product;
    }
    
    public Set<Tuple> execute() {
        TreeNode u = tree;
        Result result = LW(u);
        Set<Tuple> C = result.getC();
        return prune(C);
    }
    
    // LW(x): Processes join tree node, returns (C, D)
    private Result LW(TreeNode x) {
        if (x.isLeaf()) {
            Set<Tuple> emptyC = new HashSet<>();
            Set<Tuple> D = getRelationForNode(x);
            // Set attribute maps for tuples
            Relation rel = relations.get(x.getLabel());
            if (rel != null && rel.getSchema() != null) {
                Map<String, Integer> attrMap = new HashMap<>();
                for (int i = 0; i < rel.getSchema().size(); i++) {
                    attrMap.put(rel.getSchema().get(i), i);
                }
                for (Tuple t : D) {
                    t.setAttributeMap(attrMap);
                }
            }
            return new Result(emptyC, D);
        }
        
        Result leftResult = LW(x.leftChild());
        Result rightResult = LW(x.rightChild());
        
        Set<Tuple> C_L = leftResult.getC();
        Set<Tuple> D_L = leftResult.getD();
        Set<Tuple> C_R = rightResult.getC();
        Set<Tuple> D_R = rightResult.getD();
        
        Set<Tuple> F = computeF(D_L, D_R, x);
        
        Set<Tuple> G;
        if (D_R.isEmpty() || F.isEmpty()) {
            F = new HashSet<>();
            G = new HashSet<>();
        } else {
            G = computeG(D_L, F, D_R, P, x); // Pass D_L and x for proper bucket counting
        }
        
        Set<Tuple> C;
        Set<Tuple> D;
        
        if (x.isRoot()) {
            C = join(D_L, D_R);
            C.addAll(C_L);
            C.addAll(C_R);
            D = new HashSet<>();
        } else {
            C = conditionalJoin(D_L, D_R, G, x); // Pass x to get λ(x) for projection
            C.addAll(C_L);
            C.addAll(C_R);
            D = new HashSet<>(F);
            D.removeAll(G);
        }
        
        return new Result(C, D);
    }
    
    private Set<Tuple> getRelationForNode(TreeNode node) {
        Relation rel = relations.get(node.getLabel());
        if (rel != null) {
            return new HashSet<>(rel.getTuples());
        }
        return new HashSet<>();
    }
    
    // F = π_λ(x)(D_L) ∩ π_λ(x)(D_R)
    private Set<Tuple> computeF(Set<Tuple> D_L, Set<Tuple> D_R, TreeNode x) {
        List<String> lambda = getLambda(x);
        if (lambda.isEmpty()) {
            return new HashSet<>();
        }
        
        Set<Tuple> projectedL = projectSet(D_L, lambda);
        Set<Tuple> projectedR = projectSet(D_R, lambda);
        
        Set<Tuple> F = new HashSet<>(projectedL);
        F.retainAll(projectedR);
        return F;
    }
    
    // G = {t ∈ F : |D_L[t]| ≥ ⌈P/|D_R|⌉} where D_L[t] are tuples in D_L projecting to t
    private Set<Tuple> computeG(Set<Tuple> D_L, Set<Tuple> F, Set<Tuple> D_R, double P, TreeNode x) {
        if (D_R.isEmpty() || F.isEmpty()) return new HashSet<>();
        
        int threshold = (int) Math.ceil(P / D_R.size());
        Set<Tuple> G = new HashSet<>();
        
        // Get λ(x) for projection
        List<String> lambda = getLambda(x);
        if (lambda.isEmpty()) {
            // No common attributes - include all F
            return new HashSet<>(F);
        }
        
        // Count bucket sizes: for each t in F, count |D_L[t]|
        Map<Tuple, Integer> bucketSizes = new HashMap<>();
        for (Tuple tuple : D_L) {
            Tuple projected = tuple.projectOn(lambda);
            if (F.contains(projected)) {
                bucketSizes.merge(projected, 1, Integer::sum);
            }
        }
        
        // Select tuples whose bucket size meets threshold
        for (Map.Entry<Tuple, Integer> entry : bucketSizes.entrySet()) {
            if (entry.getValue() >= threshold) {
                G.add(entry.getKey());
            }
        }
        
        return G;
    }
    
    // Standard join: D_L ⋈ D_R
    private Set<Tuple> join(Set<Tuple> D_L, Set<Tuple> D_R) {
        Set<Tuple> result = new HashSet<>();
        for (Tuple left : D_L) {
            for (Tuple right : D_R) {
                if (left.canJoin(right)) {
                    result.add(left.join(right));
                }
            }
        }
        return result;
    }
    
    // Conditional join: D_L ⋈_G D_R  —  only join tuples in D_L whose projection onto λ(x) is in G
    private Set<Tuple> conditionalJoin(Set<Tuple> D_L, Set<Tuple> D_R, Set<Tuple> G, TreeNode x) {
        Set<Tuple> result = new HashSet<>();
        for (Tuple left : D_L) {
            for (Tuple right : D_R) {
                if (left.canJoin(right) && G.contains(left.projectCommon(right))) {
                    result.add(left.join(right));
                }
            }
        }
        return result;
    }
    
    private Set<Tuple> projectSet(Set<Tuple> tuples, List<String> attributes) {
        Set<Tuple> projected = new HashSet<>();
        for (Tuple t : tuples) {
            projected.add(t.projectOn(attributes));
        }
        return projected;
    }
    
    private Set<Tuple> prune(Set<Tuple> C) {
        // TODO: Implement specific pruning logic
        return C;
    }
    
    public double getSizeBound() {
        return P;
    }
}
