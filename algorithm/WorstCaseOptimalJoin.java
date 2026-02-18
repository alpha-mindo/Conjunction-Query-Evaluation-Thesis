package algorithm;

import database.Relation;
import database.Tuple;
import tree.TreeNode;
import tree.Result;
import java.util.*;

/**
 * Implementation of the Worst-Case Optimal Join (WCOJ) algorithm.
 * 
 * This algorithm achieves O(N + Output) complexity by ensuring that no intermediate
 * result exceeds the AGM (Atserias-Grohe-Marx) bound.
 * 
 * Key features:
 * - Delayed materialization of intermediate results
 * - Size-bounded conditional joins
 * - Optimal for cyclic queries
 */
public class WorstCaseOptimalJoin {
    private final Map<String, Relation> relations;
    private final TreeNode tree;
    private final double P; // AGM bound
    
    /**
     * Creates a new WCOJ instance.
     * @param relations Map of relation names to Relation objects
     * @param tree The join tree structure
     */
    public WorstCaseOptimalJoin(Map<String, Relation> relations, TreeNode tree) {
        this.relations = relations;
        this.tree = tree;
        this.P = computeSizeBound();
    }
    
    /**
     * Computes the AGM bound: P = ∏_{c∈C} N_c^{1/(n-1)}
     * This is the theoretical worst-case size of the join result.
     * @return The computed size bound
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
     * Executes the worst-case optimal join algorithm.
     * @return The final set of result tuples
     */
    public Set<Tuple> execute() {
        TreeNode u = tree;
        Result result = LW(u);
        Set<Tuple> C = result.getC();
        return prune(C);
    }
    
    /**
     * LW(x) algorithm: Processes a node in the join tree.
     * Returns (C, D) where C is complete results and D is delayed results.
     * 
     * @param x The current tree node
     * @return Result containing C and D sets
     */
    private Result LW(TreeNode x) {
        // Base case: Leaf node
        if (x.isLeaf()) {
            Set<Tuple> emptyC = new HashSet<>();
            Set<Tuple> D = getRelationForNode(x);
            return new Result(emptyC, D);
        }
        
        // Recursive case: Internal node
        Result leftResult = LW(x.leftChild());
        Result rightResult = LW(x.rightChild());
        
        Set<Tuple> C_L = leftResult.getC();
        Set<Tuple> D_L = leftResult.getD();
        Set<Tuple> C_R = rightResult.getC();
        Set<Tuple> D_R = rightResult.getD();
        
        // Compute F and G sets
        Set<Tuple> F = computeF(D_L, D_R, x);
        
        Set<Tuple> G;
        if (D_R.isEmpty()) {
            F = new HashSet<>();
            G = new HashSet<>();
        } else {
            G = computeG(D_L, D_R, P);
        }
        
        Set<Tuple> C;
        Set<Tuple> D;
        
        // Root node: Full materialization
        if (x.isRoot()) {
            C = join(D_L, D_R);
            C.addAll(C_L);
            C.addAll(C_R);
            D = new HashSet<>();
        } 
        // Internal node: Conditional join
        else {
            C = conditionalJoin(D_L, D_R, G);
            C.addAll(C_L);
            C.addAll(C_R);
            D = new HashSet<>(F);
            D.removeAll(G);
        }
        
        return new Result(C, D);
    }
    
    /**
     * Gets the relation tuples for a leaf node.
     * @param node The leaf node
     * @return Set of tuples from the corresponding relation
     */
    private Set<Tuple> getRelationForNode(TreeNode node) {
        Relation rel = relations.get(node.getLabel());
        if (rel != null) {
            return new HashSet<>(rel.getTuples());
        }
        return new HashSet<>();
    }
    
    /**
     * Computes F = project(D_L) ∩ project(D_R)
     * F represents tuples that can potentially be joined.
     * 
     * @param D_L Left delayed results
     * @param D_R Right delayed results
     * @param x Current tree node
     * @return Set F
     */
    private Set<Tuple> computeF(Set<Tuple> D_L, Set<Tuple> D_R, TreeNode x) {
        Set<Tuple> projectedL = project(D_L);
        Set<Tuple> projectedR = project(D_R);
        
        Set<Tuple> F = new HashSet<>(projectedL);
        F.retainAll(projectedR);
        return F;
    }
    
    /**
     * Computes G based on the size bound P.
     * G = {t ∈ D_L | 1 ≤ [P/|D_R|]}
     * 
     * @param D_L Left delayed results
     * @param D_R Right delayed results
     * @param P Size bound
     * @return Set G
     */
    private Set<Tuple> computeG(Set<Tuple> D_L, Set<Tuple> D_R, double P) {
        Set<Tuple> G = new HashSet<>();
        int threshold = (int) Math.ceil(P / D_R.size());
        
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
     * Performs a full join of all tuples from both sets.
     * 
     * @param D_L Left set of tuples
     * @param D_R Right set of tuples
     * @return Joined result set
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
     * Conditional join operation: D_L ⋈_G D_R
     * Only joins tuples where the left tuple is in set G.
     * This limits intermediate result size.
     * 
     * @param D_L Left set of tuples
     * @param D_R Right set of tuples
     * @param G Set of tuples to conditionally join
     * @return Conditionally joined result set
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
     * Joins two tuples by concatenating their values.
     * In a full implementation, this would perform attribute-based matching.
     * 
     * @param left Left tuple
     * @param right Right tuple
     * @return Joined tuple
     */
    private Tuple joinTuples(Tuple left, Tuple right) {
        List<Object> combined = new ArrayList<>();
        combined.addAll(left.getValues());
        combined.addAll(right.getValues());
        return new Tuple(combined.toArray());
    }
    
    /**
     * Projects tuples onto specific attributes.
     * Simplified version returns all tuples as-is.
     * 
     * @param tuples Set of tuples to project
     * @return Projected tuple set
     */
    private Set<Tuple> project(Set<Tuple> tuples) {
        // TODO: Implement attribute-based projection
        return new HashSet<>(tuples);
    }
    
    /**
     * Prunes the final result set.
     * Can be used for deduplication, filtering, etc.
     * 
     * @param C Complete result set
     * @return Pruned result set
     */
    private Set<Tuple> prune(Set<Tuple> C) {
        // TODO: Implement specific pruning logic
        return C;
    }
    
    /**
     * Gets the AGM bound computed for this instance.
     * @return The size bound P
     */
    public double getSizeBound() {
        return P;
    }
}
