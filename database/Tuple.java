package database;

import java.util.*;

/**
 * Represents a single tuple (row) in a database relation.
 * Contains an ordered list of attribute values.
 */
public class Tuple {
    private final List<Object> values;
    
    /**
     * Creates a tuple with the given attribute values.
     * @param vals Variable number of attribute values
     */
    public Tuple(Object... vals) {
        this.values = Arrays.asList(vals);
    }
    
    /**
     * Gets the list of values in this tuple.
     * @return List of attribute values
     */
    public List<Object> getValues() {
        return values;
    }
    
    /**
     * Gets the value at a specific position.
     * @param index Position of the value (0-based)
     * @return The value at the specified position
     */
    public Object getValue(int index) {
        return values.get(index);
    }
    
    /**
     * Gets the number of attributes in this tuple.
     * @return Number of attributes
     */
    public int size() {
        return values.size();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tuple)) return false;
        Tuple tuple = (Tuple) o;
        return Objects.equals(values, tuple.values);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(values);
    }
    
    @Override
    public String toString() {
        return "Tuple" + values;
    }
}
