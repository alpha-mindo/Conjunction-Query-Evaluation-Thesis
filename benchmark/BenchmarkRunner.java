package benchmark;

import algorithm.WorstCaseOptimalJoin;
import database.Relation;
import database.Tuple;
import tree.TreeNode;
import java.util.*;

/**
 * Benchmarking framework for testing join algorithms.
 */
public class BenchmarkRunner {
    
    private static final int WARMUP_RUNS = 3;
    private static final int BENCHMARK_RUNS = 5;
    
    /**
     * Run a single benchmark test.
     */
    public static BenchmarkResult runBenchmark(AlgorithmBenchmark algorithm, 
                                               QueryPattern pattern, 
                                               int databaseSize) {
        // Warmup runs
        for (int i = 0; i < WARMUP_RUNS; i++) {
            algorithm.execute();
        }
        
        // Force garbage collection before benchmark
        System.gc();
        
        // Measure memory before
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        // Benchmark runs
        long totalTime = 0;
        Set<Tuple> results = null;
        
        for (int i = 0; i < BENCHMARK_RUNS; i++) {
            long startTime = System.nanoTime();
            results = algorithm.execute();
            long endTime = System.nanoTime();
            totalTime += (endTime - startTime);
        }
        
        // Measure memory after
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = Math.max(0, memoryAfter - memoryBefore);
        
        // Calculate average time
        long avgTime = totalTime / BENCHMARK_RUNS;
        
        return new BenchmarkResult(
            algorithm.getName(),
            pattern.toString(),
            databaseSize,
            avgTime,
            results != null ? results.size() : 0,
            algorithm.getSizeBound(),
            memoryUsed
        );
    }
    
    /**
     * Run benchmarks for multiple database sizes.
     */
    public static List<BenchmarkResult> runBenchmarkSuite(QueryPattern pattern, int[] sizes) {
        List<BenchmarkResult> results = new ArrayList<>();
        
        System.out.println("=".repeat(100));
        System.out.println("Running Benchmark Suite: " + pattern.getDescription());
        System.out.println("=".repeat(100));
        
        for (int size : sizes) {
            System.out.println("\nDatabase size: " + size + " tuples per relation");
            System.out.println("-".repeat(100));
            
            // Generate database for this size
            Object[] generated = DatabaseGenerator.generate(pattern, size);
            @SuppressWarnings("unchecked")
            Map<String, Relation> relations = (Map<String, Relation>) generated[0];
            TreeNode tree = (TreeNode) generated[1];
            
            // Test WCOJ algorithm
            WorstCaseOptimalJoin wcoj = new WorstCaseOptimalJoin(relations, tree);
            WCOJBenchmarkAdapter adapter = new WCOJBenchmarkAdapter(wcoj);
            
            BenchmarkResult result = runBenchmark(adapter, pattern, size);
            results.add(result);
            
            System.out.println(result);
        }
        
        return results;
    }
    
    /**
     * Run comprehensive benchmark across all patterns.
     */
    public static void runComprehensiveBenchmark() {
        System.out.println("\n");
        System.out.println("╔═══════════════════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                          CONJUNCTION QUERY EVALUATION BENCHMARK                               ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        List<BenchmarkResult> allResults = new ArrayList<>();
        
        // Test different database sizes
        int[] smallSizes = {10, 50, 100};
        int[] mediumSizes = {100, 500, 1000};
        
        // Test each query pattern
        QueryPattern[] patterns = {
            QueryPattern.TWO_WAY,
            QueryPattern.LINEAR,
            QueryPattern.CYCLIC,
            QueryPattern.STAR,
            QueryPattern.FOUR_WAY_LINEAR
        };
        
        for (QueryPattern pattern : patterns) {
            int[] sizes = (pattern == QueryPattern.CYCLIC) ? smallSizes : mediumSizes;
            List<BenchmarkResult> results = runBenchmarkSuite(pattern, sizes);
            allResults.addAll(results);
            System.out.println();
        }
        
        // Print summary
        printSummary(allResults);
    }
    
    /**
     * Print benchmark summary.
     */
    private static void printSummary(List<BenchmarkResult> results) {
        System.out.println("\n");
        System.out.println("=".repeat(100));
        System.out.println("BENCHMARK SUMMARY");
        System.out.println("=".repeat(100));
        System.out.println();
        
        System.out.printf("%-20s | %-20s | %-12s | %-15s | %-12s | %-10s%n",
            "Query Pattern", "DB Size", "Avg Time (ms)", "Result Size", "AGM Bound", "Memory (MB)");
        System.out.println("-".repeat(100));
        
        for (BenchmarkResult result : results) {
            System.out.printf("%-20s | %-20d | %-15.3f | %-12d | %-10.2f | %-10.2f%n",
                result.getQueryPattern(),
                result.getDatabaseSize(),
                result.getExecutionTimeMillis() / 1000.0,
                result.getResultSize(),
                result.getAgmBound(),
                result.getMemoryUsedMB());
        }
        
        System.out.println("=".repeat(100));
    }
    
    public static void main(String[] args) {
        if (args.length == 0) {
            // Run comprehensive benchmark
            runComprehensiveBenchmark();
        } else if (args.length == 2) {
            // Run specific pattern and size
            QueryPattern pattern = QueryPattern.valueOf(args[0].toUpperCase());
            int size = Integer.parseInt(args[1]);
            runBenchmarkSuite(pattern, new int[] { size });
        } else {
            System.out.println("Usage:");
            System.out.println("  java benchmark.BenchmarkRunner                    # Run comprehensive benchmark");
            System.out.println("  java benchmark.BenchmarkRunner <pattern> <size>   # Run specific benchmark");
            System.out.println();
            System.out.println("Available patterns: TWO_WAY, LINEAR, CYCLIC, STAR, FOUR_WAY_LINEAR, CROSS_PRODUCT");
        }
    }
}

/**
 * Adapter to wrap WorstCaseOptimalJoin for benchmarking.
 */
class WCOJBenchmarkAdapter implements AlgorithmBenchmark {
    private final WorstCaseOptimalJoin wcoj;
    
    public WCOJBenchmarkAdapter(WorstCaseOptimalJoin wcoj) {
        this.wcoj = wcoj;
    }
    
    @Override
    public Set<Tuple> execute() {
        return wcoj.execute();
    }
    
    @Override
    public String getName() {
        return "WCOJ";
    }
    
    @Override
    public double getSizeBound() {
        return wcoj.getSizeBound();
    }
}
