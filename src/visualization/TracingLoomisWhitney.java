package visualization;

import database.Relation;
import database.Tuple;
import tree.QueryTreeBuilder;
import tree.Result;
import tree.TreeNode;

import java.util.*;

/**
 * Instrumented version of {@link Algorithms.LoomisWhitneyInstance} that records
 * an {@link AlgorithmStep} for every recursive call so the visualizer can
 * replay the execution step by step.
 *
 * The algorithmic logic is identical to the original WCOJ; only tracing
 * instrumentation has been added.
 */
public class TracingLoomisWhitney {

    private final Map<String, Relation>         relations;
    private final TreeNode                       tree;
    private final Map<TreeNode, List<String>>    nodeSchemas = new HashMap<>();
    private final List<AlgorithmStep>            steps       = new ArrayList<>();

    // ── Construction ──────────────────────────────────────────────────────────

    public TracingLoomisWhitney(Map<String, Relation> relations) {
        this.relations = relations;
        this.tree      = QueryTreeBuilder.build(relations);
        computeNodeSchemas(tree);
    }

    public TreeNode           getTree()  { return tree; }
    public List<AlgorithmStep> getSteps() { return Collections.unmodifiableList(steps); }

    // ── Public API ─────────────────────────────────────────────────────────────

    /** Run the algorithm, populate the step list, return the final result. */
    public Set<Tuple> execute() {
        steps.clear();
        return loomisWhitney(tree).getC();
    }

    // ── Schema helpers ─────────────────────────────────────────────────────────

    private List<String> computeNodeSchemas(TreeNode node) {
        if (node.isLeaf()) {
            Relation rel = relations.get(node.getLabel());
            List<String> sc = rel != null ? new ArrayList<>(rel.getSchema()) : new ArrayList<>();
            nodeSchemas.put(node, sc);
            return sc;
        }
        List<String> L = computeNodeSchemas(node.leftChild());
        List<String> R = computeNodeSchemas(node.rightChild());
        Set<String> u  = new LinkedHashSet<>(L);
        u.addAll(R);
        List<String> union = new ArrayList<>(u);
        nodeSchemas.put(node, union);
        return union;
    }

    private List<String> getLambda(TreeNode node) {
        if (node.isLeaf()) return new ArrayList<>();
        List<String> L = nodeSchemas.get(node.leftChild());
        List<String> R = nodeSchemas.get(node.rightChild());
        List<String> common = new ArrayList<>();
        for (String a : L) if (R.contains(a)) common.add(a);
        return common;
    }

    // ── Core recursive algorithm (with step recording) ─────────────────────────

    private Result loomisWhitney(TreeNode node) {

        // ── Leaf ──────────────────────────────────────────────────────────────
        if (node.isLeaf()) {
            Relation rel = relations.get(node.getLabel());
            Set<Tuple> D = rel != null ? new HashSet<>(rel.getTuples()) : new HashSet<>();

            List<String> sc    = nodeSchemas.get(node);
            AlgorithmStep.TableSnap empty = snap(Set.of(), sc);
            AlgorithmStep.TableSnap dSnap = snap(D, sc);

            String narrative = String.format(
                "Leaf node  \"%s\"\n\n" +
                "All %d tuple(s) from relation %s are placed into D (the deferred set).\n" +
                "C starts empty — leaf nodes have nothing to join yet.\n\n" +
                "D will be passed up to the parent node, which will decide\n" +
                "how to split the tuples into heavy (G) and light (F\\G) hitters.",
                node.getLabel(), D.size(), node.getLabel());

            steps.add(new AlgorithmStep(
                node, "Leaf: " + node.getLabel(), new ArrayList<>(),
                0, 0, 0, 0,
                0, 0, 0,
                0, D.size(),
                empty, empty, empty, empty, dSnap,
                narrative));

            return new Result(new HashSet<>(), D);
        }

        // ── Internal / root ───────────────────────────────────────────────────
        Result leftRes  = loomisWhitney(node.leftChild());
        Result rightRes = loomisWhitney(node.rightChild());

        Set<Tuple> C_L = leftRes.getC(),  D_L = leftRes.getD();
        Set<Tuple> C_R = rightRes.getC(), D_R = rightRes.getD();

        List<String> lambda    = getLambda(node);
        List<String> nodeSchema = nodeSchemas.get(node);

        // F = π_λ(D_L) ∩ π_λ(D_R)
        Set<Tuple> F = project(D_L, lambda);
        F.retainAll(project(D_R, lambda));

        int P         = Math.max(1, F.size());
        int threshold = Math.max(1, P / Math.max(1, D_R.size()));
        Set<Tuple> G         = selectTop(F, threshold);
        Set<Tuple> lightKeys = new HashSet<>(F);
        lightKeys.removeAll(G);

        Set<Tuple> C, D;
        if (node.isRoot()) {
            C = join(D_L, D_R);
            C.addAll(C_L); C.addAll(C_R);
            D = new HashSet<>();
        } else {
            C = conditionalJoin(D_L, D_R, G, lambda);
            C.addAll(C_L); C.addAll(C_R);
            D = conditionalJoin(D_L, D_R, lightKeys, lambda);
        }

        // ── Record step ───────────────────────────────────────────────────────
        String lambdaStr = lambda.isEmpty() ? "(none)" : String.join(", ", lambda);
        String type      = node.isRoot() ? "Root" : "Internal";

        String narrative;
        if (node.isRoot()) {
            narrative = String.format(
                "%s node — materialising all results\n\n" +
                "λ (join attributes):  %s\n\n" +
                "Left child  → D_L = %d tuple(s),  C_L = %d tuple(s)\n" +
                "Right child → D_R = %d tuple(s),  C_R = %d tuple(s)\n\n" +
                "F = π_λ(D_L) ∩ π_λ(D_R)  =  %d matching key(s)\n" +
                "Threshold = |F| / |D_R| = %d / %d  =  %d\n" +
                "G (heavy hitters):  %d key(s)\n" +
                "F\\G (light hitters): %d key(s)\n\n" +
                "Root materialises everything:\n" +
                "  C = join(D_L, D_R) ∪ C_L ∪ C_R  →  %d tuple(s)\n" +
                "  D = ∅  (root produces the final answer)",
                type, lambdaStr,
                D_L.size(), C_L.size(), D_R.size(), C_R.size(),
                F.size(), P, D_R.size(), threshold,
                G.size(), lightKeys.size(), C.size());
        } else {
            narrative = String.format(
                "%s node — joining children on  λ = {%s}\n\n" +
                "Left child  → D_L = %d tuple(s),  C_L = %d tuple(s)\n" +
                "Right child → D_R = %d tuple(s),  C_R = %d tuple(s)\n\n" +
                "F = π_λ(D_L) ∩ π_λ(D_R)  =  %d matching key(s)\n" +
                "Threshold = |F| / |D_R| = %d / %d  =  %d\n\n" +
                "G  (heavy, eager join → C):  %d key(s)\n" +
                "F\\G (light, deferred → D):   %d key(s)\n\n" +
                "C_out = condJoin(D_L, D_R, G)  ∪ C_L ∪ C_R  →  %d tuple(s)\n" +
                "D_out = condJoin(D_L, D_R, F\\G)              →  %d tuple(s)",
                type, lambdaStr,
                D_L.size(), C_L.size(), D_R.size(), C_R.size(),
                F.size(), P, D_R.size(), threshold,
                G.size(), lightKeys.size(),
                C.size(), D.size());
        }

        String heading = type + " ⋈ {" + lambdaStr + "}";
        steps.add(new AlgorithmStep(
            node, heading, lambda,
            D_L.size(), D_R.size(), C_L.size(), C_R.size(),
            F.size(), G.size(), lightKeys.size(),
            C.size(), D.size(),
            snap(F, lambda), snap(G, lambda), snap(lightKeys, lambda),
            snap(C, nodeSchema), snap(D, nodeSchema),
            narrative));

        return new Result(C, D);
    }

    // ── Join helpers (mirrors LoomisWhitneyInstance) ────────────────────────────

    private Set<Tuple> join(Set<Tuple> left, Set<Tuple> right) {
        Set<Tuple> result = new HashSet<>();
        for (Tuple l : left)
            for (Tuple r : right)
                if (l.canJoin(r)) result.add(l.join(r));
        return result;
    }

    private Set<Tuple> conditionalJoin(Set<Tuple> left, Set<Tuple> right,
                                        Set<Tuple> keys, List<String> lambda) {
        if (keys.isEmpty() || left.isEmpty() || right.isEmpty()) return new HashSet<>();
        Set<Tuple> result = new HashSet<>();
        for (Tuple l : left) {
            if (!keys.contains(l.projectOn(lambda))) continue;
            for (Tuple r : right)
                if (l.canJoin(r)) result.add(l.join(r));
        }
        return result;
    }

    private Set<Tuple> project(Set<Tuple> tuples, List<String> attrs) {
        Set<Tuple> projected = new HashSet<>();
        if (attrs.isEmpty()) return projected;
        for (Tuple t : tuples) projected.add(t.projectOn(attrs));
        return projected;
    }

    private Set<Tuple> selectTop(Set<Tuple> tuples, int k) {
        Set<Tuple> selected = new HashSet<>();
        int count = 0;
        for (Tuple t : tuples) { if (count++ >= k) break; selected.add(t); }
        return selected;
    }

    // ── Snapshot builder ───────────────────────────────────────────────────────

    static AlgorithmStep.TableSnap snap(Set<Tuple> tuples, List<String> cols) {
        if (cols == null || cols.isEmpty()) {
            return new AlgorithmStep.TableSnap(List.of("(value)"),
                tuples.stream().map(t -> List.of(t.toString())).collect(java.util.stream.Collectors.toList()));
        }
        List<List<String>> rows = new ArrayList<>();
        for (Tuple t : tuples) {
            List<String> row = new ArrayList<>();
            for (String col : cols) {
                Object v = t.getValueByAttribute(col);
                row.add(v == null ? "—" : v.toString());
            }
            rows.add(row);
        }
        return new AlgorithmStep.TableSnap(new ArrayList<>(cols), rows);
    }
}
