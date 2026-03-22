package benchmark;

import Algorithms.LoomisWhitneyInstance;
import database.Relation;
import database.Tuple;
import tree.TreeNode;
import java.util.*;

/**
 * Wrapper for Loomis-Whitney Join algorithm for benchmarking.
 */
public class WCOJAlgorithm implements JoinAlgorithm {

    @Override
    public Set<Tuple> execute(Map<String, Relation> relations, TreeNode queryTree) {
        LoomisWhitneyInstance wcoj = new LoomisWhitneyInstance(relations, queryTree);
        return wcoj.execute();
    }

    @Override
    public String getName() {
        return "Loomis-Whitney Algorithm";
    }
}
