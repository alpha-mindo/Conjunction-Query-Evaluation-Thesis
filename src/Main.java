import algorithm.WorstCaseOptimalJoin;
import database.Relation;
import database.Tuple;
import tree.QueryTreeBuilder;
import tree.TreeNode;
import java.util.*;

public class Main {

    private static final Random RNG = new Random();

    /**
     * Returns a random ASCII lowercase letter from a domain of {@code domain} distinct
     * values ('a' through 'a' + domain - 1).  Domain is capped at 26.
     */
    private static String rc(int domain) {
        int d = Math.max(1, Math.min(domain, 26));
        return String.valueOf((char)('a' + RNG.nextInt(d)));
    }

    public static void main(String[] args) {
        System.out.println("=== Example 1: Two-way Join ===");
        simpleTwoWayJoin();

        System.out.println("\n=== Example 2: Three-way Cyclic Join ===");
        threeWayCyclicJoin();
    }

    // R(A,B) ⋈ S(B,C)
    // B is the join column — both relations draw from the same 3-value pool
    // so that joins produce visible matches in the small demo tables.
    private static void simpleTwoWayJoin() {
        Map<String, Relation> relations = new HashMap<>();
        // Shared B-domain: 3 possible chars picked randomly from a-z
        String[] bPool = randomPool(3);

        Relation R = new Relation("R", Arrays.asList("A", "B"));
        for (int i = 0; i < 5; i++) R.addRow(rc(26), bPool[RNG.nextInt(bPool.length)]);
        relations.put("R", R);

        Relation S = new Relation("S", Arrays.asList("B", "C"));
        for (int i = 0; i < 5; i++) S.addRow(bPool[RNG.nextInt(bPool.length)], rc(26));
        relations.put("S", S);

        System.out.println();
        R.printTable();
        System.out.println();
        S.printTable();

        TreeNode root = QueryTreeBuilder.build(relations);
        System.out.println("\nQuery tree: " + root.getLabel());

        WorstCaseOptimalJoin wcoj = new WorstCaseOptimalJoin(relations, root);
        System.out.println("\nAGM Size Bound: " + wcoj.getSizeBound());

        Set<Tuple> results = wcoj.execute();
        System.out.println("\nResult — R ⋈ S (" + results.size() + " rows):");
        printResultTable(results, Arrays.asList("A", "B", "C"));
    }

    // R(A,B) ⋈ S(B,C) ⋈ T(C,A) — Triangle join
    // All three join columns share bounded pools for a non-trivial result.
    private static void threeWayCyclicJoin() {
        Map<String, Relation> relations = new HashMap<>();
        String[] aPool = randomPool(4);
        String[] bPool = randomPool(4);
        String[] cPool = randomPool(4);

        Relation R = new Relation("R", Arrays.asList("A", "B"));
        for (int i = 0; i < 5; i++) R.addRow(aPool[RNG.nextInt(aPool.length)], bPool[RNG.nextInt(bPool.length)]);
        relations.put("R", R);

        Relation S = new Relation("S", Arrays.asList("B", "C"));
        for (int i = 0; i < 5; i++) S.addRow(bPool[RNG.nextInt(bPool.length)], cPool[RNG.nextInt(cPool.length)]);
        relations.put("S", S);

        Relation T = new Relation("T", Arrays.asList("C", "A"));
        for (int i = 0; i < 5; i++) T.addRow(cPool[RNG.nextInt(cPool.length)], aPool[RNG.nextInt(aPool.length)]);
        relations.put("T", T);

        System.out.println();
        R.printTable();
        System.out.println();
        S.printTable();
        System.out.println();
        T.printTable();

        TreeNode root = QueryTreeBuilder.build(relations);
        System.out.println("\nQuery tree: " + root.getLabel());

        WorstCaseOptimalJoin wcoj = new WorstCaseOptimalJoin(relations, root);
        System.out.println("\nAGM Size Bound: " + wcoj.getSizeBound());

        Set<Tuple> results = wcoj.execute();
        System.out.println("\nResult — R ⋈ S ⋈ T (" + results.size() + " rows):");
        printResultTable(results, Arrays.asList("A", "B", "C"));
    }

    /**
     * Builds a pool of {@code size} distinct random lowercase letters (ASCII a–z).
     * These are used as shared join-column domains so that queries produce matches.
     */
    private static String[] randomPool(int size) {
        int d = Math.max(1, Math.min(size, 26));
        // Start with all letters, shuffle, take first d
        List<String> all = new ArrayList<>();
        for (int i = 0; i < 26; i++) all.add(String.valueOf((char)('a' + i)));
        Collections.shuffle(all, RNG);
        String[] pool = new String[d];
        for (int i = 0; i < d; i++) pool[i] = all.get(i);
        return pool;
    }

    /** Print a set of result tuples as a formatted table given the expected column order. */
    private static void printResultTable(Set<Tuple> tuples, List<String> columns) {
        // Compute column widths
        int[] w = new int[columns.size()];
        for (int i = 0; i < columns.size(); i++) w[i] = columns.get(i).length();
        for (Tuple t : tuples) {
            for (int i = 0; i < columns.size(); i++) {
                Object v = t.getValueByAttribute(columns.get(i));
                int len = (v == null) ? 4 : v.toString().length();
                if (len > w[i]) w[i] = len;
            }
        }
        StringBuilder sep = new StringBuilder("+");
        for (int wi : w) sep.append("-").append("-".repeat(wi)).append("-+");
        String separator = sep.toString();
        System.out.println(separator);
        StringBuilder header = new StringBuilder("| ");
        for (int i = 0; i < columns.size(); i++) {
            header.append(String.format("%-" + w[i] + "s", columns.get(i)));
            header.append(i < columns.size() - 1 ? " | " : " |");
        }
        System.out.println(header);
        System.out.println(separator);
        for (Tuple t : tuples) {
            StringBuilder row = new StringBuilder("| ");
            for (int i = 0; i < columns.size(); i++) {
                Object v = t.getValueByAttribute(columns.get(i));
                row.append(String.format("%-" + w[i] + "s", v == null ? "null" : v));
                row.append(i < columns.size() - 1 ? " | " : " |");
            }
            System.out.println(row);
        }
        System.out.println(separator);
    }
}
