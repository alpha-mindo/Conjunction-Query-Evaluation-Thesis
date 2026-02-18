package database;

import java.util.*;

/**
 * Represents a database relation (table) containing a set of tuples.
 */
public class Relation {
    private final String name;
    private final Set<Tuple> tuples;
    
    /**
     * Creates a new relation with the given name.
     * @param name The name of the relation
     */
    public Relation(String name) {
        this.name = name;
        this.tuples = new HashSet<>();
    }
    
    /**
     * Gets the name of this relation.
     * @return The relation name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the set of tuples in this relation.
     * @return Set of tuples
     */
    public Set<Tuple> getTuples() {
        return tuples;
    }
    
    /**
     * Adds a tuple to this relation.
     * @param tuple The tuple to add
     */
    public void addTuple(Tuple tuple) {
        tuples.add(tuple);
    }
    
    /**
     * Gets the number of tuples in this relation.
     * @return The cardinality of the relation
     */
    public int size() {
        return tuples.size();
    }
    
    /**
     * Checks if this relation is empty.
     * @return true if the relation has no tuples
     */
    public boolean isEmpty() {
        return tuples.isEmpty();
    }
    
    @Override
    public String toString() {
        return "Relation{" + name + ", size=" + size() + "}";
    }
}
