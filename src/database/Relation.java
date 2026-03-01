package database;

import java.util.*;

public class Relation {
    private final String name;
    private final Set<Tuple> tuples;
    private List<String> schema; // Attribute names
    
    public Relation(String name) {
        this.name = name;
        this.tuples = new HashSet<>();
        this.schema = new ArrayList<>();
    }
    
    public Relation(String name, List<String> schema) {
        this.name = name;
        this.tuples = new HashSet<>();
        this.schema = new ArrayList<>(schema);
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
    
    public List<String> getSchema() {
        return schema;
    }
    
    public void setSchema(List<String> schema) {
        this.schema = new ArrayList<>(schema);
    }
    
    @Override
    public String toString() {
        return "Relation{" + name + ", size=" + size() + "}";
    }
}
