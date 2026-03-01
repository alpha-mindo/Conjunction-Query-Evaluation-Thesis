import benchmark.*;
import database.Relation;
import database.Tuple;
import tree.TreeNode;
import java.util.*;

/**
 * Example demonstrating how to use the benchmarking framework
 * to test join algorithms on various query patterns and database sizes.
 */
public class BenchmarkExample {
    
    public static void main(String[] args) {
        System.out.println("=== Benchmark Framework Usage Examples ===\n");
        
        // Example 1: Single Pattern, Single Size
        System.out.println("Example 1: Testing TWO_WAY join with 100 tuples");
        System.out.println("-------------------------------------------------");
        
        Object[] generated = DatabaseGenerator.generate(QueryPattern.TWO_WAY, 100);
        @SuppressWarnings("unchecked")
        Map<String, Relation> relations = (Map<String, Relation>) generated[0];
        TreeNode tree = (TreeNode) generated[1];
        
        // Time the algorithm
        long startTime = System.nanoTime();
        algorithm.WorstCaseOptimalJoin wcoj = new algorithm.WorstCaseOptimalJoin(relations, tree);
        Set<Tuple> results = wcoj.execute();
        long endTime = System.nanoTime();
        
        double timeMs = (endTime - startTime) / 1_000_000.0;
        System.out.println("Results: " + results.size() + " tuples");
        System.out.println("Time: " + timeMs + " ms");
        System.out.println("AGM Bound: " + wcoj.getSizeBound());
        System.out.println();
        
        // Example 2: Single Pattern, Multiple Sizes
        System.out.println("Example 2: Testing LINEAR pattern with multiple sizes");
        System.out.println("-------------------------------------------------------");
        
        int[] sizes = {50, 100, 200};
        for (int size : sizes) {
            Object[] gen = DatabaseGenerator.generate(QueryPattern.LINEAR, size);
            @SuppressWarnings("unchecked")
            Map<String, Relation> rels = (Map<String, Relation>) gen[0];
            TreeNode t = (TreeNode) gen[1];
            
            startTime = System.nanoTime();
            wcoj = new algorithm.WorstCaseOptimalJoin(rels, t);
            results = wcoj.execute();
            endTime = System.nanoTime();
            
            timeMs = (endTime - startTime) / 1_000_000.0;
            System.out.printf("Size: %4d | Time: %8.3f ms | Results: %6d | AGM: %10.2f%n",
                size, timeMs, results.size(), wcoj.getSizeBound());
        }
        System.out.println();
        
        // Example 3: Using the BenchmarkRunner
        System.out.println("Example 3: Using BenchmarkRunner for comprehensive testing");
        System.out.println("-----------------------------------------------------------");
        System.out.println("Run from command line:");
        System.out.println("  java -cp bin benchmark.BenchmarkRunner                # Full benchmark");
        System.out.println("  java -cp bin benchmark.BenchmarkRunner CYCLIC 50     # Specific test");
        System.out.println();
        
        // Example 4: All available query patterns
        System.out.println("Example 4: Available Query Patterns");
        System.out.println("------------------------------------");
        for (QueryPattern pattern : QueryPattern.values()) {
            System.out.println("  " + pattern.name() + " - " + pattern.getDescription());
        }
        System.out.println();
        
        System.out.println("For more details, see benchmark/README.md");
    }
}
