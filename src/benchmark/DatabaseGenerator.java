package benchmark;

import database.Relation;
import tree.QueryTreeBuilder;
import tree.TreeNode;
import java.util.*;

/**
 * Generates test databases and query trees for benchmarking.
 * All cell values are random single characters derived from ASCII codes:
 *   value = (char)('a' + rng.nextInt(domain))
 * where domain is sized so join columns across relations share enough
 * common values to produce non-trivial join results.
 */
public class DatabaseGenerator {

    private static final Random RNG = new Random();

    /**
     * Returns a random lowercase letter from a domain of {@code domain} values
     * (ASCII 'a' through 'a' + domain - 1).  Domain is capped at 26.
     */
    private static String rc(int domain) {
        int d = Math.max(1, Math.min(domain, 26));
        return String.valueOf((char)('a' + RNG.nextInt(d)));
    }

    /**
     * Builds a pre-filled String[] pool of {@code domain} random chars so that
     * join columns can be picked from the same pool (guaranteeing overlap).
     */
    private static String[] pool(int domain) {
        int d = Math.max(1, Math.min(domain, 26));
        String[] p = new String[d];
        for (int i = 0; i < d; i++) p[i] = String.valueOf((char)('a' + i));
        // Shuffle for extra randomness
        for (int i = d - 1; i > 0; i--) {
            int j = RNG.nextInt(i + 1);
            String tmp = p[i]; p[i] = p[j]; p[j] = tmp;
        }
        return p;
    }

    /**
     * Generate a database and query tree for a specific pattern.
     * @param pattern The query pattern to generate.
     * @param size The number of tuples per relation.
     * @return A pair of [relations map, query tree].
     */
    public static Object[] generate(QueryPattern pattern, int size) {
        switch (pattern) {
            case TWO_WAY:       return generateTwoWayJoin(size);
            case LINEAR:        return generateLinearChain(size);
            case CYCLIC:        return generateCyclicQuery(size);
            case STAR:          return generateStarQuery(size);
            case FOUR_WAY_LINEAR: return generateFourWayLinear(size);
            case CROSS_PRODUCT: return generateCrossProduct(size);
            default:            return generateTwoWayJoin(size);
        }
    }

    // R(A,B) ⋈ S(B,C)
    private static Object[] generateTwoWayJoin(int size) {
        Map<String, Relation> relations = new HashMap<>();
        // B is shared — pick from a bounded pool so joins produce results
        String[] bPool = pool(Math.max(2, size / 2));

        Relation R = new Relation("R", Arrays.asList("A", "B"));
        Relation S = new Relation("S", Arrays.asList("B", "C"));
        for (int i = 0; i < size; i++) {
            R.addRow(rc(26), bPool[RNG.nextInt(bPool.length)]);
            S.addRow(bPool[RNG.nextInt(bPool.length)], rc(26));
        }
        relations.put("R", R);
        relations.put("S", S);

        TreeNode root = QueryTreeBuilder.build(relations);
        return new Object[] { relations, root };
    }

    // R(A,B) ⋈ S(B,C) ⋈ T(C,D)
    private static Object[] generateLinearChain(int size) {
        Map<String, Relation> relations = new HashMap<>();
        String[] bPool = pool(Math.max(2, size / 2));
        String[] cPool = pool(Math.max(2, size / 3));

        Relation R = new Relation("R", Arrays.asList("A", "B"));
        Relation S = new Relation("S", Arrays.asList("B", "C"));
        Relation T = new Relation("T", Arrays.asList("C", "D"));
        for (int i = 0; i < size; i++) {
            R.addRow(rc(26), bPool[RNG.nextInt(bPool.length)]);
            S.addRow(bPool[RNG.nextInt(bPool.length)], cPool[RNG.nextInt(cPool.length)]);
            T.addRow(cPool[RNG.nextInt(cPool.length)], rc(26));
        }
        relations.put("R", R);
        relations.put("S", S);
        relations.put("T", T);

        TreeNode root = QueryTreeBuilder.build(relations);
        return new Object[] { relations, root };
    }

    // Triangle: R(A,B) ⋈ S(B,C) ⋈ T(C,A)
    private static Object[] generateCyclicQuery(int size) {
        Map<String, Relation> relations = new HashMap<>();
        int domainSize = Math.max(2, Math.min(size, 26));
        String[] aPool = pool(domainSize);
        String[] bPool = pool(domainSize);
        String[] cPool = pool(domainSize);

        Relation R = new Relation("R", Arrays.asList("A", "B"));
        Relation S = new Relation("S", Arrays.asList("B", "C"));
        Relation T = new Relation("T", Arrays.asList("C", "A"));
        for (int i = 0; i < size; i++) {
            R.addRow(aPool[RNG.nextInt(aPool.length)], bPool[RNG.nextInt(bPool.length)]);
            S.addRow(bPool[RNG.nextInt(bPool.length)], cPool[RNG.nextInt(cPool.length)]);
            T.addRow(cPool[RNG.nextInt(cPool.length)], aPool[RNG.nextInt(aPool.length)]);
        }
        relations.put("R", R);
        relations.put("S", S);
        relations.put("T", T);

        TreeNode root = QueryTreeBuilder.build(relations);
        return new Object[] { relations, root };
    }

    // Star: R(A,B) ⋈ S(A,C) ⋈ T(A,D)
    private static Object[] generateStarQuery(int size) {
        Map<String, Relation> relations = new HashMap<>();
        // Small centre domain → more join hits
        String[] aPool = pool(Math.max(2, Math.min(size / 10, 26)));

        Relation R = new Relation("R", Arrays.asList("A", "B"));
        Relation S = new Relation("S", Arrays.asList("A", "C"));
        Relation T = new Relation("T", Arrays.asList("A", "D"));
        for (int i = 0; i < size; i++) {
            R.addRow(aPool[RNG.nextInt(aPool.length)], rc(26));
            S.addRow(aPool[RNG.nextInt(aPool.length)], rc(26));
            T.addRow(aPool[RNG.nextInt(aPool.length)], rc(26));
        }
        relations.put("R", R);
        relations.put("S", S);
        relations.put("T", T);

        TreeNode root = QueryTreeBuilder.build(relations);
        return new Object[] { relations, root };
    }

    // R(A,B) ⋈ S(B,C) ⋈ T(C,D) ⋈ U(D,E)
    private static Object[] generateFourWayLinear(int size) {
        Map<String, Relation> relations = new HashMap<>();
        String[] bPool = pool(Math.max(2, size / 2));
        String[] cPool = pool(Math.max(2, size / 3));
        String[] dPool = pool(Math.max(2, size / 4));

        Relation R = new Relation("R", Arrays.asList("A", "B"));
        Relation S = new Relation("S", Arrays.asList("B", "C"));
        Relation T = new Relation("T", Arrays.asList("C", "D"));
        Relation U = new Relation("U", Arrays.asList("D", "E"));
        for (int i = 0; i < size; i++) {
            R.addRow(rc(26), bPool[RNG.nextInt(bPool.length)]);
            S.addRow(bPool[RNG.nextInt(bPool.length)], cPool[RNG.nextInt(cPool.length)]);
            T.addRow(cPool[RNG.nextInt(cPool.length)], dPool[RNG.nextInt(dPool.length)]);
            U.addRow(dPool[RNG.nextInt(dPool.length)], rc(26));
        }
        relations.put("R", R);
        relations.put("S", S);
        relations.put("T", T);
        relations.put("U", U);

        TreeNode root = QueryTreeBuilder.build(relations);
        return new Object[] { relations, root };
    }

    // R(A) × S(B) — cross product, no common attributes
    private static Object[] generateCrossProduct(int size) {
        Map<String, Relation> relations = new HashMap<>();
        int smallSize = Math.min(size, 26); // cap so the cross product stays printable

        Relation R = new Relation("R", Arrays.asList("A"));
        Relation S = new Relation("S", Arrays.asList("B"));
        for (int i = 0; i < smallSize; i++) {
            R.addRow(rc(26));
            S.addRow(rc(26));
        }
        relations.put("R", R);
        relations.put("S", S);

        TreeNode root = QueryTreeBuilder.build(relations);
        return new Object[] { relations, root };
    }
}
