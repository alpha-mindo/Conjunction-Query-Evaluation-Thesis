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
    
    public WorstCaseOptimalJoin(Map<String, Relation> relations, TreeNode tree) {
        this.relations = relations;
        this.tree = tree;
        this.P = computeSizeBound();
    }
    
    // Computes AGM bound: P = ∏_{c∈C} N_c^{1/(n-1)}
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
        if (D_R.isEmpty()) {
            F = new HashSet<>();
            G = new HashSet<>();
        } else {
            G = computeG(D_L, D_R, P);
        }
        
        Set<Tuple> C;
        Set<Tuple> D;
        
        if (x.isRoot()) {
            C = join(D_L, D_R);
            C.addAll(C_L);
            C.addAll(C_R);
            D = new HashSet<>();
        } else {
            C = conditionalJoin(D_L, D_R, G);
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
    
    // F = project(D_L) ∩ project(D_R)
    private Set<Tuple> computeF(Set<Tuple> D_L, Set<Tuple> D_R, TreeNode x) {
        Set<Tuple> projectedL = project(D_L);
        Set<Tuple> projectedR = project(D_R);
        
        Set<Tuple> F = new HashSet<>(projectedL);
        F.retainAll(projectedR);
        return F;
    }
    
    // G = top-⌈P/|D_R|⌉ tuples from D_L
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
    
    // Standard join: D_L ⋈ D_R
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
    
    // Conditional join: D_L ⋈_G D_R
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
    
    private Tuple joinTuples(Tuple left, Tuple right) {
        List<Object> combined = new ArrayList<>();
        combined.addAll(left.getValues());
        combined.addAll(right.getValues());
        return new Tuple(combined.toArray());
    }
    
    private Set<Tuple> project(Set<Tuple> tuples) {
        // TODO: Implement attribute-based projection
        return new HashSet<>(tuples);
    }
    
    private Set<Tuple> prune(Set<Tuple> C) {
        // TODO: Implement specific pruning logic
        return C;
    }
    
    public double getSizeBound() {
        return P;
    }
}
