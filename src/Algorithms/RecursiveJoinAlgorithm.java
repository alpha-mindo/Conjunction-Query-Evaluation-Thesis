package Algorithms;

import database.Relation;
import database.Tuple;
import tree.TreeNode;

import java.util.*;

public class RecursiveJoinAlgorithm {

    // Input relations
    private final Map<String, Relation> relations;

    public RecursiveJoinAlgorithm(Map<String, Relation> relations) {
        this.relations = relations;
    }

    /** Top-level join execution */
    public Set<Tuple> execute(Map<String, Double> weights) {
        TreeNode root = tree.QueryTreeBuilder.build(relations);
        Tuple emptyBoundTuple = new Tuple(new HashMap<>());
        return recursiveJoin(root, weights, emptyBoundTuple);
    }

    /** Recursive Join procedure */
    public Set<Tuple> recursiveJoin(TreeNode node, Map<String, Double> weights, Tuple boundTuple) {
        Set<Tuple> results = new HashSet<>();

        // Base Case: Leaf node
        if (node.isLeaf()) {
            int numRelations = Integer.parseInt(node.getLabel());
            List<String> universe = node.getUniverse();

            // Pick relation with smallest projection
            int bestIndex = argminProjection(universe, numRelations, boundTuple);
            Relation bestRelation = relations.get("R" + bestIndex);

            // Intersect with remaining relations
            for (Tuple candidate : project(bestRelation, universe, boundTuple)) {
                boolean isValidCandidate = true;

                for (int i = 1; i <= numRelations; i++) {
                    if (i == bestIndex) continue; 
                    
                    Relation otherRelation = relations.get("R" + i);
                    if (!project(otherRelation, universe, boundTuple).contains(candidate)) {
                        isValidCandidate = false;
                        break;
                    }
                }
                
                if (isValidCandidate) {
                    results.add(boundTuple.join(candidate));
                }
            }
            return results;
        }

        // Recursive Case: Internal Node
        Set<Tuple> leftResults;
        if (node.leftChild() == null) {
            leftResults = new HashSet<>(Collections.singleton(boundTuple));
        } else {
            leftResults = recursiveJoin(node.leftChild(), weights, boundTuple);
        }

        List<String> universe = node.getUniverse();
        List<String> edgeAttributes = node.getEdgeK();

        List<String> intersectionAttributes = new ArrayList<>(edgeAttributes);
        intersectionAttributes.retainAll(universe);

        if (intersectionAttributes.isEmpty()) {
            return leftResults;
        }

        Relation relationK = relations.get("Rk");

        for (Tuple extendedTuple : leftResults) {
            double edgeWeight = weights.getOrDefault("e_k", 1.0);

            // Heavy branch: edgeWeight >= 1.0
            if (edgeWeight >= 1.0) {
                evaluateHeavyBranch(results, extendedTuple, relationK, intersectionAttributes);
                continue;
            }

            // Light branch: AGM bound inequality test
            double projectedCapacity = productProjections(extendedTuple, intersectionAttributes, weights);
            double relationKSize = project(relationK, intersectionAttributes, boundTuple).size();

            if (projectedCapacity < relationKSize) {
                // Scale weights and recurse
                Map<String, Double> scaledWeights = scaleWeights(weights, edgeWeight);
                Set<Tuple> rightResults = recursiveJoin(node.rightChild(), scaledWeights, extendedTuple);

                for (Tuple rightTuple : rightResults) {
                    Tuple subTuple = rightTuple.projectOn(intersectionAttributes);
                    if (project(relationK, intersectionAttributes, boundTuple).contains(subTuple)) {
                        results.add(rightTuple);
                    }
                }
            } else {
                evaluateHeavyBranch(results, extendedTuple, relationK, intersectionAttributes);
            }
        }

        return results;
    }

    // --- Helpers ---

    private void evaluateHeavyBranch(Set<Tuple> results, Tuple extendedTuple, 
                                     Relation relationK, List<String> intersectionAttributes) {
        for (Tuple intersectionTuple : project(relationK, intersectionAttributes, extendedTuple)) {
            if (checkMembership(extendedTuple, intersectionTuple, relations, intersectionAttributes)) {
                results.add(extendedTuple.join(intersectionTuple));
            }
        }
    }

    private int argminProjection(List<String> universe, int numRelations, Tuple boundTuple) {
        // TODO: implement logic to pick relation with smallest projection
        return 1;
    }

    private Set<Tuple> project(Relation rel, List<String> targetAttributes, Tuple boundTuple) {
        // TODO: implement projection
        return new HashSet<>();
    }

    private boolean checkMembership(Tuple extendedTuple, Tuple intersectionTuple, 
                                    Map<String, Relation> rels, List<String> intersectionAttributes) {
        // TODO: implement membership test
        return true;
    }

    private double productProjections(Tuple extendedTuple, List<String> intersectionAttributes, 
                                      Map<String, Double> weights) {
        // TODO: calculate AGM bound capacity
        return 0.0;
    }

    private Map<String, Double> scaleWeights(Map<String, Double> weights, double edgeWeight) {
        // TODO: calculate scaled weights
        return new HashMap<>(weights);
    }
}
