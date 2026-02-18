import algorithm.WorstCaseOptimalJoin;
import database.Relation;
import database.Tuple;
import tree.TreeNode;
import java.util.*;

public class Main {
    
    public static void main(String[] args) {
        System.out.println("=== Example 1: Two-way Join ===");
        simpleTwoWayJoin();
        
        System.out.println("\n=== Example 2: Three-way Cyclic Join ===");
        threeWayCyclicJoin();
    }
    
    // R(A,B) ⋈ S(B,C)
    private static void simpleTwoWayJoin() {
        Map<String, Relation> relations = new HashMap<>();
        
        Relation R = new Relation("R");
        R.addTuple(new Tuple("a1", "b1"));
        R.addTuple(new Tuple("a2", "b2"));
        R.addTuple(new Tuple("a3", "b1"));
        relations.put("R", R);
        
        Relation S = new Relation("S");
        S.addTuple(new Tuple("b1", "c1"));
        S.addTuple(new Tuple("b2", "c2"));
        S.addTuple(new Tuple("b1", "c3"));
        relations.put("S", S);
        
        TreeNode root = new TreeNode("root");
        TreeNode leftLeaf = new TreeNode("R");
        TreeNode rightLeaf = new TreeNode("S");
        root.setLeft(leftLeaf);
        root.setRight(rightLeaf);
        
        WorstCaseOptimalJoin wcoj = new WorstCaseOptimalJoin(relations, root);
        System.out.println("AGM Size Bound: " + wcoj.getSizeBound());
        
        Set<Tuple> results = wcoj.execute();
        System.out.println("Results (" + results.size() + " tuples):");
        for (Tuple t : results) {
            System.out.println("  " + t);
        }
    }
    
    // R(A,B) ⋈ S(B,C) ⋈ T(C,A) - Triangle join
    private static void threeWayCyclicJoin() {
        Map<String, Relation> relations = new HashMap<>();
        
        Relation R = new Relation("R");
        R.addTuple(new Tuple("a1", "b1"));
        R.addTuple(new Tuple("a2", "b2"));
        R.addTuple(new Tuple("a3", "b3"));
        relations.put("R", R);
        
        Relation S = new Relation("S");
        S.addTuple(new Tuple("b1", "c1"));
        S.addTuple(new Tuple("b2", "c2"));
        S.addTuple(new Tuple("b3", "c3"));
        relations.put("S", S);
        
        Relation T = new Relation("T");
        T.addTuple(new Tuple("c1", "a1"));
        T.addTuple(new Tuple("c2", "a2"));
        T.addTuple(new Tuple("c3", "a3"));
        relations.put("T", T);
        
        TreeNode root = new TreeNode("root");
        TreeNode internal = new TreeNode("internal");
        TreeNode leafR = new TreeNode("R");
        TreeNode leafS = new TreeNode("S");
        TreeNode leafT = new TreeNode("T");
        
        root.setLeft(internal);
        root.setRight(leafT);
        internal.setLeft(leafR);
        internal.setRight(leafS);
        
        WorstCaseOptimalJoin wcoj = new WorstCaseOptimalJoin(relations, root);
        System.out.println("AGM Size Bound: " + wcoj.getSizeBound());
        
        Set<Tuple> results = wcoj.execute();
        System.out.println("Results (" + results.size() + " tuples):");
        for (Tuple t : results) {
            System.out.println("  " + t);
        }
    }
}
