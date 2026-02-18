package database;

import java.util.*;

public class Tuple {
    private final List<Object> values;
    
    public Tuple(Object... vals) {
        this.values = Arrays.asList(vals);
    }
    
    public List<Object> getValues() {
        return values;
    }
    
    public Object getValue(int index) {
        return values.get(index);
    }
    
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
