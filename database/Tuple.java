package database;

import java.util.*;

public class Tuple {
    private final List<Object> values;
    private Map<String, Integer> attributeMap; // Maps attribute name to index
    
    public Tuple(Object... vals) {
        this.values = Arrays.asList(vals);
        this.attributeMap = new HashMap<>();
    }
    
    public Tuple(List<Object> vals) {
        this.values = new ArrayList<>(vals);
        this.attributeMap = new HashMap<>();
    }
    
    public Tuple(Map<String, Object> attributeValues) {
        this.attributeMap = new HashMap<>();
        this.values = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Object> entry : attributeValues.entrySet()) {
            attributeMap.put(entry.getKey(), index++);
            values.add(entry.getValue());
        }
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
    
    public void setAttributeMap(Map<String, Integer> attributeMap) {
        this.attributeMap = new HashMap<>(attributeMap);
    }
    
    public Map<String, Integer> getAttributeMap() {
        return attributeMap;
    }
    
    public Object getValueByAttribute(String attribute) {
        Integer index = attributeMap.get(attribute);
        return index != null ? values.get(index) : null;
    }
    
    public boolean hasAttribute(String attribute) {
        return attributeMap.containsKey(attribute);
    }
    
    public Tuple projectOn(List<String> attributes) {
        Map<String, Object> projected = new LinkedHashMap<>();
        for (String attr : attributes) {
            if (attributeMap.containsKey(attr)) {
                projected.put(attr, getValueByAttribute(attr));
            }
        }
        return new Tuple(projected);
    }
    
    public boolean matchesOn(Tuple other, List<String> attributes) {
        for (String attr : attributes) {
            Object thisVal = this.getValueByAttribute(attr);
            Object otherVal = other.getValueByAttribute(attr);
            if (thisVal == null || otherVal == null) continue;
            if (!thisVal.equals(otherVal)) return false;
        }
        return true;
    }

    // Returns true if this tuple can join with other (all common attributes agree)
    public boolean canJoin(Tuple other) {
        for (String attr : this.attributeMap.keySet()) {
            if (other.hasAttribute(attr)) {
                Object thisVal = this.getValueByAttribute(attr);
                Object otherVal = other.getValueByAttribute(attr);
                if (thisVal != null && otherVal != null && !thisVal.equals(otherVal)) {
                    return false;
                }
            }
        }
        return true;
    }

    // Joins this tuple with other, merging all attributes (no duplicates)
    public Tuple join(Tuple other) {
        Map<String, Object> joined = new LinkedHashMap<>();
        for (String attr : this.attributeMap.keySet()) {
            joined.put(attr, this.getValueByAttribute(attr));
        }
        for (String attr : other.attributeMap.keySet()) {
            if (!joined.containsKey(attr)) {
                joined.put(attr, other.getValueByAttribute(attr));
            }
        }
        return new Tuple(joined);
    }

    // Projects this tuple onto the attributes it shares with other
    public Tuple projectCommon(Tuple other) {
        List<String> common = new ArrayList<>();
        for (String attr : this.attributeMap.keySet()) {
            if (other.hasAttribute(attr)) {
                common.add(attr);
            }
        }
        return projectOn(common);
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
