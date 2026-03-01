package benchmark;

/**
 * Stores the results of a single benchmark run.
 */
public class BenchmarkResult {
    private final String algorithmName;
    private final String queryPattern;
    private final int databaseSize;
    private final long executionTimeNanos;
    private final long executionTimeMillis;
    private final int resultSize;
    private final double agmBound;
    private final long memoryUsedBytes;
    
    public BenchmarkResult(String algorithmName, String queryPattern, int databaseSize,
                          long executionTimeNanos, int resultSize, double agmBound, long memoryUsedBytes) {
        this.algorithmName = algorithmName;
        this.queryPattern = queryPattern;
        this.databaseSize = databaseSize;
        this.executionTimeNanos = executionTimeNanos;
        this.executionTimeMillis = executionTimeNanos / 1_000_000;
        this.resultSize = resultSize;
        this.agmBound = agmBound;
        this.memoryUsedBytes = memoryUsedBytes;
    }
    
    public String getAlgorithmName() {
        return algorithmName;
    }
    
    public String getQueryPattern() {
        return queryPattern;
    }
    
    public int getDatabaseSize() {
        return databaseSize;
    }
    
    public long getExecutionTimeNanos() {
        return executionTimeNanos;
    }
    
    public long getExecutionTimeMillis() {
        return executionTimeMillis;
    }
    
    public double getExecutionTimeSeconds() {
        return executionTimeNanos / 1_000_000_000.0;
    }
    
    public int getResultSize() {
        return resultSize;
    }
    
    public double getAgmBound() {
        return agmBound;
    }
    
    public long getMemoryUsedBytes() {
        return memoryUsedBytes;
    }
    
    public double getMemoryUsedMB() {
        return memoryUsedBytes / (1024.0 * 1024.0);
    }
    
    @Override
    public String toString() {
        return String.format("%s | %s | DB Size: %d | Time: %.3f ms | Results: %d | AGM: %.2f | Memory: %.2f MB",
            algorithmName, queryPattern, databaseSize, executionTimeMillis / 1000.0, 
            resultSize, agmBound, getMemoryUsedMB());
    }
}
