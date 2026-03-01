package tree;

import database.Tuple;
import java.util.*;

/**
 * Container for LW algorithm results.
 * C: Complete results, D: Delayed results
 */
public class Result {
    private final Set<Tuple> C;
    private final Set<Tuple> D;
    
    public Result(Set<Tuple> C, Set<Tuple> D) {
        this.C = C;
        this.D = D;
    }
    
    public Set<Tuple> getC() {
        return C;
    }
    
    public Set<Tuple> getD() {
        return D;
    }
    
    public int totalSize() {
        return C.size() + D.size();
    }
    
    @Override
    public String toString() {
        return "Result{C.size=" + C.size() + ", D.size=" + D.size() + "}";
    }
}
