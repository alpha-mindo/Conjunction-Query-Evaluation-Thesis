import java.util.*;

public class WorstCaseOptimalJoin {
    
    // Relation structure
    static class Relation {
        String name;
        Set<Tuple> tuples;
        
        public Relation(String name) {
            this.name = name;
            this.tuples = new HashSet<>();
        }
        
        public int size() {
            return tuples.size();
        }
    }
    
    // Tuple structure
    static class Tuple {
        List<Object> values;
        
        public Tuple(Object... vals) {
            this.values = Arrays.asList(vals);
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Tuple)) return false;
            Tuple tuple = (Tuple) o;
            return Objects.equals(values, tuple.values);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(values);
        }
    }
    
    // Tree node structure
    static class TreeNode {
        String label;
        TreeNode left;
        TreeNode right;
        TreeNode parent;
        
        public TreeNode(String label) {
            this.label = label;
        }
        
        public boolean isLeaf() {
            return left == null && right == null;
        }
        
        public boolean isRoot() {
            return parent == null;
        }
        
        public TreeNode leftChild() {
            return left;
        }
        
        public TreeNode rightChild() {
            return right;
        }
    }
    
    // Result structure containing C and D
    static class Result {
        Set<Tuple> C; // Complete results
        Set<Tuple> D; // Delayed results
        
        public Result(Set<Tuple> C, Set<Tuple> D) {
            this.C = C;
            this.D = D;
        }
    }
    
    // LW instance with relations
    private Map<String, Relation> relations;
    private TreeNode tree;
    private double P; // Size bound from LW inequality
    
    public WorstCaseOptimalJoin(Map<String, Relation> relations, TreeNode tree) {
        this.relations = relations;
        this.tree = tree;
        this.P = computeSizeBound();
    }
    
    /**
     * Compute P = ∏_{c∈C} N_c^{1/(n-1)}
     * where N_c = |R_c| for each relation
     */
    private double computeSizeBound() {
        int n = relations.size();
        if (n <= 1) return 1.0;
        
        double product = 1.0;
        for (Relation rel : relations.values()) {
            int N_c = rel.size();
            product *= Math.pow(N_c, 1.0 / (n - 1));
        }
        return product;
    }
    
    /**
     * Main LW algorithm execution
     * Returns the final result set after pruning
     */
    public Set<Tuple> execute() {
        TreeNode u = tree; // root(T)
        Result result = LW(u);
        
        // "Prune" C(u) and return
        Set<Tuple> C = result.C;
        return prune(C);
    }
    
    /**
     * LW(x) : x ∈ T returns (C, D)
     */
    private Result LW(TreeNode x) {
        // Line 6: if x is a leaf then
        if (x.isLeaf()) {
            // Line 7: return (∅, R_{L_aux(x)})
            Set<Tuple> emptyC = new HashSet<>();
            Set<Tuple> D = getRelationForNode(x);
            return new Result(emptyC, D);
        }
        
        // Line 8: (C_L, D_L) ← LW(cc(x)) and (C_R, D_R) ← LW(rc(x))
        Result leftResult = LW(x.leftChild());
        Result rightResult = LW(x.rightChild());
        
        Set<Tuple> C_L = leftResult.C;
        Set<Tuple> D_L = leftResult.D;
        Set<Tuple> C_R = rightResult.C;
        Set<Tuple> D_R = rightResult.D;
        
        // Line 9: F ← F_{L_aux(x)}(D_L) ∩ π_{L_aux(x)}(D_R)
        Set<Tuple> F = computeF(D_L, D_R, x);
        
        // Line 10: G ← {t ∈ [D_L[1]] + 1 ≤ [P/|D_R|]} // F = G = ∅ if |D_R| = 0
        Set<Tuple> G;
        if (D_R.isEmpty()) {
            F = new HashSet<>();
            G = new HashSet<>();
        } else {
            G = computeG(D_L, D_R, P);
        }
        
        Set<Tuple> C;
        Set<Tuple> D;
        
        // Line 11: if x is the root of T then
        if (x.isRoot()) {
            // Line 12: C ← (D_L ⋈ D_R) ∪ C_L ∪ C_R
            C = join(D_L, D_R);
            C.addAll(C_L);
            C.addAll(C_R);
            
            // Line 13: D ← ∅
            D = new HashSet<>();
        } else {
            // Line 15: C ← (D_L ⋈_G D_R) ∪ C_L ∪ C_R
            C = conditionalJoin(D_L, D_R, G);
            C.addAll(C_L);
            C.addAll(C_R);
            
            // Line 16: D ← F \ G
            D = new HashSet<>(F);
            D.removeAll(G);
        }
        
        // Line 17: return (C, D)
        return new Result(C, D);
    }
    
    /**
     * Get relation tuples for a leaf node
     */
    private Set<Tuple> getRelationForNode(TreeNode node) {
        Relation rel = relations.get(node.label);
        if (rel != null) {
            return new HashSet<>(rel.tuples);
        }
        return new HashSet<>();
    }
    
    /**
     * Compute F = project(D_L) ∩ project(D_R)
     */
    private Set<Tuple> computeF(Set<Tuple> D_L, Set<Tuple> D_R, TreeNode x) {
        Set<Tuple> projectedL = project(D_L);
        Set<Tuple> projectedR = project(D_R);
        
        Set<Tuple> F = new HashSet<>(projectedL);
        F.retainAll(projectedR);
        return F;
    }
    
    /**
     * Compute G based on the size bound
     */
    private Set<Tuple> computeG(Set<Tuple> D_L, Set<Tuple> D_R, double P) {
        Set<Tuple> G = new HashSet<>();
        int threshold = (int) Math.ceil(P / D_R.size());
        
        // Get tuples from D_L where index <= threshold
        int count = 0;
        for (Tuple t : D_L) {
            if (count >= threshold) break;
            G.add(t);
            count++;
        }
        
        return G;
    }
    
    /**
     * Standard join operation: D_L ⋈ D_R
     */
    private Set<Tuple> join(Set<Tuple> D_L, Set<Tuple> D_R) {
        Set<Tuple> result = new HashSet<>();
        
        for (Tuple left : D_L) {
            for (Tuple right : D_R) {
                Tuple joined = joinTuples(left, right);
                if (joined != null) {
                    result.add(joined);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Conditional join: D_L ⋈_G D_R
     */
    private Set<Tuple> conditionalJoin(Set<Tuple> D_L, Set<Tuple> D_R, Set<Tuple> G) {
        Set<Tuple> result = new HashSet<>();
        
        for (Tuple left : D_L) {
            if (!G.contains(left)) continue;
            
            for (Tuple right : D_R) {
                Tuple joined = joinTuples(left, right);
                if (joined != null) {
                    result.add(joined);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Join two tuples if they are compatible
     */
    private Tuple joinTuples(Tuple left, Tuple right) {
        // Simple concatenation - in practice, need attribute matching
        List<Object> combined = new ArrayList<>();
        combined.addAll(left.values);
        combined.addAll(right.values);
        return new Tuple(combined.toArray());
    }
    
    /**
     * Project tuples (simplified version)
     */
    private Set<Tuple> project(Set<Tuple> tuples) {
        // In practice, project on specific attributes
        return new HashSet<>(tuples);
    }
    
    /**
     * Prune C(u) before returning final result
     */
    private Set<Tuple> prune(Set<Tuple> C) {
        // Pruning logic depends on specific requirements
        // For now, return as-is
        return C;
    }
}
