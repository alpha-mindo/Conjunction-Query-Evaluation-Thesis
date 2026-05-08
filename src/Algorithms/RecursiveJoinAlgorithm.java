package Algorithms;

import database.Relation;
import database.Tuple;
import tree.TreeNode;
import java.util.*;

public class RecursiveJoinAlgorithm {

    private final Map<String, Relation> relations;

    public RecursiveJoinAlgorithm(Map<String, Relation> relations) {
        this.relations = relations;
    }

    public Set<Tuple> recursiveJoin(TreeNode currentNode, Map<String, Double> weights, Tuple boundTuple) {
        Set<Tuple> results = new HashSet<>();

        // Base Case: Leaf node. Produce base relation tuples matching bound attributes.
        if (currentNode.isLeaf()) {
            int numRelations = Integer.parseInt(currentNode.getLabel());
            List<String> universe = currentNode.getUniverse();

            // Find the relation that has the smallest projection size to optimize intersection
            int bestRelationIndex = argminProjection(universe, numRelations, boundTuple);
            Relation bestRelation = relations.get("R" + bestRelationIndex);

            // Iterate over the smallest projection and intersect with remaining relations
            for (Tuple candidateTuple : project(bestRelation, universe, boundTuple)) {
                boolean isPresentInAll = true;

                for (int i = 1; i <= numRelations; i++) {
                    if (i == bestRelationIndex) continue;
                    
                    Relation otherRelation = relations.get("R" + i);
                    if (!project(otherRelation, universe, boundTuple).contains(candidateTuple)) {
                        isPresentInAll = false;
                        break;
                    }
                }
                
                if (isPresentInAll) {
                    results.add(boundTuple.join(candidateTuple));
                }
            }
            return results;
        }

        // Recursive Case: Internal node processing
        Set<Tuple> leftResults;
        if (currentNode.leftChild() == null) {
            leftResults = new HashSet<>();
            leftResults.add(boundTuple);
        } else {
            leftResults = recursiveJoin(currentNode.leftChild(), weights, boundTuple);
        }

        // Calculate attribute partitions based on the current edge (relation)
        List<String> universe = currentNode.getUniverse();
        List<String> currentEdgeAttrs = currentNode.getEdgeK();
        
        List<String> missingAttrs = new ArrayList<>(universe);
        missingAttrs.removeAll(currentEdgeAttrs);
        
        List<String> intersectionAttrs = new ArrayList<>(currentEdgeAttrs);
        intersectionAttrs.retainAll(universe);

        // If there are no intersecting attributes to join on, return the left results
        if (intersectionAttrs.isEmpty()) {
            return leftResults;
        }

        Relation relationK = relations.get("Rk");

        // Iterate over results from the left branch
        for (Tuple extendedTuple : leftResults) {
            double edgeWeight = weights.getOrDefault("e_k", 1.0);

            // Heavy relation branch: evaluate directly
            if (edgeWeight >= 1.0) {
                evaluateHeavyBranch(results, extendedTuple, boundTuple, relationK, intersectionAttrs);
                continue;
            }

            // Test inequalities to determine whether to process as a heavy or light branch
            double projectedCapacity = productProjections(extendedTuple, intersectionAttrs, weights);
            double relationKSize = project(relationK, intersectionAttrs, boundTuple).size();

            if (projectedCapacity < relationKSize) {
                // Light relation branch: scale weights and recurse on right child
                Map<String, Double> scaledWeights = scaleWeights(weights, edgeWeight);
                Set<Tuple> rightResults = recursiveJoin(currentNode.rightChild(), scaledWeights, extendedTuple);

                // Filter valid tuples that exist in relation K
                for (Tuple rightTuple : rightResults) {
                    Tuple subTuple = rightTuple.projectOn(intersectionAttrs);
                    if (project(relationK, intersectionAttrs, boundTuple).contains(subTuple)) {
                        results.add(rightTuple);
                    }
                }
            } else {
                // Treated as heavy branch fallback
                evaluateHeavyBranch(results, extendedTuple, boundTuple, relationK, intersectionAttrs);
            }
        }

        return results;
    }

    /**
     * Processes heavy edges by performing an immediate set intersection and adding to results.
     */
    private void evaluateHeavyBranch(Set<Tuple> results, Tuple extendedTuple, Tuple boundTuple, 
                                     Relation relationK, List<String> intersectionAttrs) {
        for (Tuple intersectionTuple : project(relationK, intersectionAttrs, boundTuple)) {
            if (checkMembership(extendedTuple, intersectionTuple, relations, intersectionAttrs)) {
                results.add(extendedTuple.join(intersectionTuple));
            }
        }
    }

    // --- Helper stubs corresponding to pseudocode operations ---
    private int argminProjection(List<String> universe, int numRelations, Tuple boundTuple) {
        return 0; 
    }

    private Set<Tuple> project(Relation rel, List<String> attrs, Tuple boundTuple) {
        return new HashSet<>();
    }

    private boolean checkMembership(Tuple extendedTuple, Tuple intersectionTuple, Map<String, Relation> rels, List<String> intersectionAttrs) {
        return true;
    }

    private double productProjections(Tuple extendedTuple, List<String> intersectionAttrs, Map<String, Double> weights) {
        return 0.0;
    }

    private Map<String, Double> scaleWeights(Map<String, Double> weights, double targetWeight) {
        return new HashMap<>();
    }
}
