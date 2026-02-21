package benchmark;

import database.Relation;
import database.Tuple;
import tree.TreeNode;
import java.util.*;

/**
 * Generates test databases and query trees for benchmarking.
 */
public class DatabaseGenerator {
    
    /**
     * Generate a database and query tree for a specific pattern.
     * @param pattern The query pattern to generate.
     * @param size The number of tuples per relation.
     * @return A pair of [relations map, query tree].
     */
    public static Object[] generate(QueryPattern pattern, int size) {
        switch (pattern) {
            case TWO_WAY:
                return generateTwoWayJoin(size);
            case LINEAR:
                return generateLinearChain(size);
            case CYCLIC:
                return generateCyclicQuery(size);
            case STAR:
                return generateStarQuery(size);
            case FOUR_WAY_LINEAR:
                return generateFourWayLinear(size);
            case CROSS_PRODUCT:
                return generateCrossProduct(size);
            default:
                return generateTwoWayJoin(size);
        }
    }
    
    // R(A,B) ⋈ S(B,C)
    private static Object[] generateTwoWayJoin(int size) {
        Map<String, Relation> relations = new HashMap<>();
        
        Relation R = new Relation("R");
        Relation S = new Relation("S");
        
        for (int i = 0; i < size; i++) {
            R.addTuple(new Tuple("a" + i, "b" + (i % (size / 2))));
            S.addTuple(new Tuple("b" + (i % (size / 2)), "c" + i));
        }
        
        relations.put("R", R);
        relations.put("S", S);
        
        TreeNode root = new TreeNode("root");
        root.setLeft(new TreeNode("R"));
        root.setRight(new TreeNode("S"));
        
        return new Object[] { relations, root };
    }
    
    // R(A,B) ⋈ S(B,C) ⋈ T(C,D)
    private static Object[] generateLinearChain(int size) {
        Map<String, Relation> relations = new HashMap<>();
        
        Relation R = new Relation("R");
        Relation S = new Relation("S");
        Relation T = new Relation("T");
        
        int halfSize = size / 2;
        int thirdSize = size / 3;
        
        for (int i = 0; i < size; i++) {
            R.addTuple(new Tuple("a" + i, "b" + (i % halfSize)));
            S.addTuple(new Tuple("b" + (i % halfSize), "c" + (i % thirdSize)));
            T.addTuple(new Tuple("c" + (i % thirdSize), "d" + i));
        }
        
        relations.put("R", R);
        relations.put("S", S);
        relations.put("T", T);
        
        TreeNode root = new TreeNode("root");
        TreeNode internal = new TreeNode("internal");
        root.setLeft(internal);
        root.setRight(new TreeNode("T"));
        internal.setLeft(new TreeNode("R"));
        internal.setRight(new TreeNode("S"));
        
        return new Object[] { relations, root };
    }
    
    // Triangle: R(A,B) ⋈ S(B,C) ⋈ T(C,A)
    private static Object[] generateCyclicQuery(int size) {
        Map<String, Relation> relations = new HashMap<>();
        
        Relation R = new Relation("R");
        Relation S = new Relation("S");
        Relation T = new Relation("T");
        
        int cycleSize = Math.min(size, 100); // Limit cycle size to avoid huge results
        
        for (int i = 0; i < size; i++) {
            R.addTuple(new Tuple("a" + (i % cycleSize), "b" + (i % cycleSize)));
            S.addTuple(new Tuple("b" + (i % cycleSize), "c" + (i % cycleSize)));
            T.addTuple(new Tuple("c" + (i % cycleSize), "a" + (i % cycleSize)));
        }
        
        relations.put("R", R);
        relations.put("S", S);
        relations.put("T", T);
        
        TreeNode root = new TreeNode("root");
        TreeNode internal = new TreeNode("internal");
        root.setLeft(internal);
        root.setRight(new TreeNode("T"));
        internal.setLeft(new TreeNode("R"));
        internal.setRight(new TreeNode("S"));
        
        return new Object[] { relations, root };
    }
    
    // Star: R(A,B) ⋈ S(A,C) ⋈ T(A,D)
    private static Object[] generateStarQuery(int size) {
        Map<String, Relation> relations = new HashMap<>();
        
        Relation R = new Relation("R");
        Relation S = new Relation("S");
        Relation T = new Relation("T");
        
        int centerSize = Math.min(size / 10, 50); // Central join attribute has fewer values
        
        for (int i = 0; i < size; i++) {
            R.addTuple(new Tuple("a" + (i % centerSize), "b" + i));
            S.addTuple(new Tuple("a" + (i % centerSize), "c" + i));
            T.addTuple(new Tuple("a" + (i % centerSize), "d" + i));
        }
        
        relations.put("R", R);
        relations.put("S", S);
        relations.put("T", T);
        
        TreeNode root = new TreeNode("root");
        TreeNode internal = new TreeNode("internal");
        root.setLeft(internal);
        root.setRight(new TreeNode("T"));
        internal.setLeft(new TreeNode("R"));
        internal.setRight(new TreeNode("S"));
        
        return new Object[] { relations, root };
    }
    
    // R(A,B) ⋈ S(B,C) ⋈ T(C,D) ⋈ U(D,E)
    private static Object[] generateFourWayLinear(int size) {
        Map<String, Relation> relations = new HashMap<>();
        
        Relation R = new Relation("R");
        Relation S = new Relation("S");
        Relation T = new Relation("T");
        Relation U = new Relation("U");
        
        int halfSize = size / 2;
        int thirdSize = size / 3;
        int quarterSize = size / 4;
        
        for (int i = 0; i < size; i++) {
            R.addTuple(new Tuple("a" + i, "b" + (i % halfSize)));
            S.addTuple(new Tuple("b" + (i % halfSize), "c" + (i % thirdSize)));
            T.addTuple(new Tuple("c" + (i % thirdSize), "d" + (i % quarterSize)));
            U.addTuple(new Tuple("d" + (i % quarterSize), "e" + i));
        }
        
        relations.put("R", R);
        relations.put("S", S);
        relations.put("T", T);
        relations.put("U", U);
        
        TreeNode root = new TreeNode("root");
        TreeNode internal1 = new TreeNode("internal1");
        TreeNode internal2 = new TreeNode("internal2");
        root.setLeft(internal1);
        root.setRight(new TreeNode("U"));
        internal1.setLeft(internal2);
        internal1.setRight(new TreeNode("T"));
        internal2.setLeft(new TreeNode("R"));
        internal2.setRight(new TreeNode("S"));
        
        return new Object[] { relations, root };
    }
    
    // R(A) × S(B) - Cross product with no common attributes
    private static Object[] generateCrossProduct(int size) {
        Map<String, Relation> relations = new HashMap<>();
        
        Relation R = new Relation("R");
        Relation S = new Relation("S");
        
        int smallSize = Math.min(size, 50); // Limit cross product size
        
        for (int i = 0; i < smallSize; i++) {
            R.addTuple(new Tuple("a" + i));
            S.addTuple(new Tuple("b" + i));
        }
        
        relations.put("R", R);
        relations.put("S", S);
        
        TreeNode root = new TreeNode("root");
        root.setLeft(new TreeNode("R"));
        root.setRight(new TreeNode("S"));
        
        return new Object[] { relations, root };
    }
}
