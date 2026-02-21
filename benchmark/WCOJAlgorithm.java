package benchmark;

import algorithm.WorstCaseOptimalJoin;
import database.Relation;
import database.Tuple;
import tree.TreeNode;
import java.util.*;

/**
 * Wrapper for Worst-Case Optimal Join algorithm for benchmarking.
 */
public class WCOJAlgorithm implements JoinAlgorithm {
    
    @Override
    public Set<Tuple> execute(Map<String, Relation> relations, TreeNode queryTree) {
        WorstCaseOptimalJoin wcoj = new WorstCaseOptimalJoin(relations, queryTree);
        return wcoj.execute();
    }
    
    @Override
    public String getName() {
        return "Worst-Case Optimal Join";
    }
}
