package benchmark;

import database.Tuple;
import java.util.*;

/**
 * Interface for join algorithms to implement for benchmarking.
 */
public interface AlgorithmBenchmark {
    /**
     * Execute the join algorithm.
     * @return The set of result tuples.
     */
    Set<Tuple> execute();
    
    /**
     * Get the name of the algorithm.
     * @return Algorithm name.
     */
    String getName();
    
    /**
     * Get the AGM bound or size estimate if applicable.
     * @return Size bound, or -1 if not applicable.
     */
    double getSizeBound();
}
