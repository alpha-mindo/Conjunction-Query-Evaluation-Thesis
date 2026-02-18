package tree;

import database.Tuple;
import java.util.*;

/**
 * Container for the result of the LW algorithm at each tree node.
 * Contains two sets:
 * - C: Complete results that have been fully materialized
 * - D: Delayed results that are deferred for conditional processing
 */
public class Result {
    private final Set<Tuple> C; // Complete results
    private final Set<Tuple> D; // Delayed results
    
    /**
     * Creates a new result with the given C and D sets.
     * @param C Set of complete tuples
     * @param D Set of delayed tuples
     */
    public Result(Set<Tuple> C, Set<Tuple> D) {
        this.C = C;
        this.D = D;
    }
    
    /**
     * Gets the set of complete results.
     * @return Complete result set C
     */
    public Set<Tuple> getC() {
        return C;
    }
    
    /**
     * Gets the set of delayed results.
     * @return Delayed result set D
     */
    public Set<Tuple> getD() {
        return D;
    }
    
    /**
     * Gets the total number of tuples (C ∪ D).
     * @return Total tuple count
     */
    public int totalSize() {
        return C.size() + D.size();
    }
    
    @Override
    public String toString() {
        return "Result{C.size=" + C.size() + ", D.size=" + D.size() + "}";
    }
}
