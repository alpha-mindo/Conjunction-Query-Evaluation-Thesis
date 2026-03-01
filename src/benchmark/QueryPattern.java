package benchmark;

/**
 * Enumeration of different query patterns for benchmarking.
 */
public enum QueryPattern {
    LINEAR("Linear Chain"),           // R(A,B) ⋈ S(B,C) ⋈ T(C,D)
    CYCLIC("Cyclic/Triangle"),        // R(A,B) ⋈ S(B,C) ⋈ T(C,A)
    STAR("Star"),                     // R(A,B) ⋈ S(A,C) ⋈ T(A,D)
    CLIQUE("Clique"),                 // Full mesh connections
    TWO_WAY("Two-Way Join"),          // Simple R(A,B) ⋈ S(B,C)
    FOUR_WAY_LINEAR("4-Way Linear"),  // R ⋈ S ⋈ T ⋈ U
    CROSS_PRODUCT("Cross Product");   // R(A) × S(B)
    
    private final String description;
    
    QueryPattern(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return description;
    }
}
