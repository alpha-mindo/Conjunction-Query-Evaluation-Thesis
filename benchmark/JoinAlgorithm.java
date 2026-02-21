package benchmark;

import database.Relation;
import database.Tuple;
import tree.TreeNode;
import java.util.*;

/**
 * Interface for join algorithms to enable benchmarking multiple approaches.
 */
public interface JoinAlgorithm {
    
    /**
     * Executes the join algorithm on the given relations and query tree.
     * @param relations Map of relation names to Relation objects
     * @param queryTree The join tree structure
     * @return Set of result tuples
     */
    Set<Tuple> execute(Map<String, Relation> relations, TreeNode queryTree);
    
    /**
     * Returns the name of the algorithm.
     */
    String getName();
}
