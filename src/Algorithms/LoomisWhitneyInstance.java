package Algorithms;
import java.util.*;
import database.Relation;
import database.Tuple;
import tree.*;

public class LoomisWhitneyInstance {

    // Input relations: each corresponds to an (n-1)-subset of attributes
    private final Map<String, Relation> relations;
    private final TreeNode queryTree;

    public LoomisWhitneyInstance(Map<String, Relation> relations, TreeNode queryTree) {
        this.relations = relations;
        this.queryTree = queryTree;
    }

    /** Line 2: Compute LW bound P = ∏ |R_e|^(1/(n-1)) */
    public double computeLWBound(int n) {
        double P = 1.0;
        for (Relation rel : relations.values()) {
            P *= Math.pow(rel.size(), 1.0 / (n - 1));
        }
        return P;
    }

    public double getSizeBound() {
        Set<String> allAttrs = new HashSet<>();
        for (Relation r : relations.values()) {
            allAttrs.addAll(r.getColumns());
        }
        return computeLWBound(allAttrs.size());
    }

    /** Line 3: Run recursive LW(u) */
    public Set<Tuple> execute() {
        Result res = lw(queryTree);

        // Collect all attributes involved in the query
        List<String> allAttrs = new ArrayList<>(getContextAttributes(queryTree));

        // Filter the output C to only contain full tuples (with all attributes)
        return prune(res.getC(), allAttrs);
    }

    /** Recursive LW(u) procedure */
    private Result lw(TreeNode node) {
        if (node.isLeaf()) {
            Relation rel = relations.get(node.getLabel());
            Set<Tuple> D = new HashSet<>(rel.getTuples());
            return new Result(new HashSet<>(), D);
        }

        Result left  = lw(node.leftChild());
        Result right = lw(node.rightChild());

        Set<Tuple> C_L = left.getC(), D_L = left.getD();
        Set<Tuple> C_R = right.getC(), D_R = right.getD();

        List<String> lambda = getSeparator(node);

        // F = π_λ(D_L) ∩ π_λ(D_R)
        Set<Tuple> F = project(D_L, lambda);
        F.retainAll(project(D_R, lambda));

        Set<Tuple> G = new HashSet<>();
        if (D_R.isEmpty()) {
            F.clear(); // F = G = ∅ if |DR| = 0
        } else {
            double p = getSizeBound();
            int threshold = (int) Math.ceil(p / D_R.size()); // ⌈P / |DR|⌉
            
            for (Tuple t : F) {
                // Calculate |D_L[t]|: count tuples in D_L that match t on lambda
                long dl_t_size = 0;
                for (Tuple dl : D_L) {
                    if (dl.projectOn(lambda).equals(t)) {
                        dl_t_size++;
                    }
                }
                
                if (dl_t_size + 1 <= threshold) {
                    G.add(t);
                }
            }
        }

        Set<Tuple> C, D;
        if (node.isRoot()) {
            C = join(D_L, D_R);
            C.addAll(C_L);
            C.addAll(C_R);
            D = new HashSet<>();
        } else {
            C = conditionalJoin(D_L, D_R, G, lambda);
            C.addAll(C_L);
            C.addAll(C_R);
 
            Set<Tuple> lightKeys = new HashSet<>(F);
            lightKeys.removeAll(G); //returns a boolean not a set
            D = conditionalJoin(D_L, D_R, lightKeys, lambda);
        }

        return new Result(C, D);
    }

    // --- Helpers (projection, join, pruning) ---
    private List<String> getSeparator(TreeNode node) {
        Set<String> leftAttrs = getContextAttributes(node.leftChild());
        Set<String> rightAttrs = getContextAttributes(node.rightChild());
        leftAttrs.retainAll(rightAttrs);
        return new ArrayList<>(leftAttrs);
    }

    private Set<String> getContextAttributes(TreeNode n) {
        if (n == null) return new HashSet<>();
        if (n.isLeaf()) {
            Relation r = relations.get(n.getLabel());
            return r != null ? new HashSet<>(r.getColumns()) : new HashSet<>();
        }
        Set<String> attrs = getContextAttributes(n.leftChild());
        attrs.addAll(getContextAttributes(n.rightChild()));
        return attrs;
    }

    private Set<Tuple> project(Collection<Tuple> tuples, List<String> attrs) {
        Set<Tuple> projected = new HashSet<>();
        if (attrs == null || attrs.isEmpty()) return projected;
        for (Tuple t : tuples) {
            projected.add(t.projectOn(attrs));
        }
        return projected;
    }

    private Set<Tuple> join(Set<Tuple> left, Set<Tuple> right) {
        Set<Tuple> result = new HashSet<>();
        for (Tuple l : left) {
            for (Tuple r : right) {
                if (l.canJoin(r)) {
                    result.add(l.join(r));
                }
            }
        }
        return result;
    }

    private Set<Tuple> conditionalJoin(Set<Tuple> left, Set<Tuple> right,
                                       Set<Tuple> keys, List<String> lambda) {
        if (keys.isEmpty() || left.isEmpty() || right.isEmpty()) return new HashSet<>();
        Set<Tuple> result = new HashSet<>();
        for (Tuple l : left) {
            if (!keys.contains(l.projectOn(lambda))) continue;
            for (Tuple r : right) {
                if (l.canJoin(r)) {
                    result.add(l.join(r));
                }
            }
        }
        return result;
    }

    private Set<Tuple> prune(Set<Tuple> candidates, List<String> allAttrs) {
        Set<Tuple> finalResult = new HashSet<>();
        for (Tuple t : candidates) {
            if (t.getAttributeMap().keySet().containsAll(allAttrs)) {
                // Ensure the tuple is valid across all base relations
                boolean valid = true;
                for (Relation rel : relations.values()) {
                    List<String> relCols = rel.getColumns();
                    Tuple projected = t.projectOn(relCols);
                    if (!rel.getTuples().contains(projected)) {
                        valid = false;
                        break;
                    }
                }
                if (valid) {
                    finalResult.add(t);
                }
            }
        }
        return finalResult;
    }

}
