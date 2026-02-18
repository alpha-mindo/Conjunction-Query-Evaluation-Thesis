import algorithm.WorstCaseOptimalJoin;
import database.Relation;
import database.Tuple;
import tree.TreeNode;
import java.util.*;

/**
 * Example usage of the Worst-Case Optimal Join algorithm.
 * Demonstrates how to set up relations, build a join tree, and execute queries.
 */
public class Main {
    
    public static void main(String[] args) {
        // Example 1: Simple two-way join
        System.out.println("=== Example 1: Two-way Join ===");
        simpleTwoWayJoin();
        
        // Example 2: Three-way cyclic join
        System.out.println("\n=== Example 2: Three-way Cyclic Join ===");
        threeWayCyclicJoin();
    }
    
    /**
     * Example: R(A,B) ⋈ S(B,C)
     */
    private static void simpleTwoWayJoin() {
        // Create relations
        Map<String, Relation> relations = new HashMap<>();
        
        // Relation R(A, B)
        Relation R = new Relation("R");
        R.addTuple(new Tuple("a1", "b1"));
        R.addTuple(new Tuple("a2", "b2"));
        R.addTuple(new Tuple("a3", "b1"));
        relations.put("R", R);
        
        // Relation S(B, C)
        Relation S = new Relation("S");
        S.addTuple(new Tuple("b1", "c1"));
        S.addTuple(new Tuple("b2", "c2"));
        S.addTuple(new Tuple("b1", "c3"));
        relations.put("S", S);
        
        // Build join tree: R ⋈ S
        TreeNode root = new TreeNode("root");
        TreeNode leftLeaf = new TreeNode("R");
        TreeNode rightLeaf = new TreeNode("S");
        root.setLeft(leftLeaf);
        root.setRight(rightLeaf);
        
        // Execute WCOJ
        WorstCaseOptimalJoin wcoj = new WorstCaseOptimalJoin(relations, root);
        System.out.println("AGM Size Bound: " + wcoj.getSizeBound());
        
        Set<Tuple> results = wcoj.execute();
        System.out.println("Results (" + results.size() + " tuples):");
        for (Tuple t : results) {
            System.out.println("  " + t);
        }
    }
    
    /**
     * Example: R(A,B) ⋈ S(B,C) ⋈ T(C,A)
     * This is a cyclic query (triangle join)
     */
    private static void threeWayCyclicJoin() {
        // Create relations
        Map<String, Relation> relations = new HashMap<>();
        
        // Relation R(A, B)
        Relation R = new Relation("R");
        R.addTuple(new Tuple("a1", "b1"));
        R.addTuple(new Tuple("a2", "b2"));
        R.addTuple(new Tuple("a3", "b3"));
        relations.put("R", R);
        
        // Relation S(B, C)
        Relation S = new Relation("S");
        S.addTuple(new Tuple("b1", "c1"));
        S.addTuple(new Tuple("b2", "c2"));
        S.addTuple(new Tuple("b3", "c3"));
        relations.put("S", S);
        
        // Relation T(C, A)
        Relation T = new Relation("T");
        T.addTuple(new Tuple("c1", "a1"));
        T.addTuple(new Tuple("c2", "a2"));
        T.addTuple(new Tuple("c3", "a3"));
        relations.put("T", T);
        
        // Build join tree: (R ⋈ S) ⋈ T
        TreeNode root = new TreeNode("root");
        TreeNode internal = new TreeNode("internal");
        TreeNode leafR = new TreeNode("R");
        TreeNode leafS = new TreeNode("S");
        TreeNode leafT = new TreeNode("T");
        
        root.setLeft(internal);
        root.setRight(leafT);
        internal.setLeft(leafR);
        internal.setRight(leafS);
        
        // Execute WCOJ
        WorstCaseOptimalJoin wcoj = new WorstCaseOptimalJoin(relations, root);
        System.out.println("AGM Size Bound: " + wcoj.getSizeBound());
        
        Set<Tuple> results = wcoj.execute();
        System.out.println("Results (" + results.size() + " tuples):");
        for (Tuple t : results) {
            System.out.println("  " + t);
        }
    }
}
