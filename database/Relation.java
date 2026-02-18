package database;

import java.util.*;

public class Relation {
    private final String name;
    private final Set<Tuple> tuples;
    
    public Relation(String name) {
        this.name = name;
        this.tuples = new HashSet<>();
    }
    
    public String getName() {
        return name;
    }
    
    public Set<Tuple> getTuples() {
        return tuples;
    }
    
    public void addTuple(Tuple tuple) {
        tuples.add(tuple);
    }
    
    public int size() {
        return tuples.size();
    }
    
    public boolean isEmpty() {
        return tuples.isEmpty();
    }
    
    @Override
    public String toString() {
        return "Relation{" + name + ", size=" + size() + "}";
    }
}
