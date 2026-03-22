# Conjunction Query Evaluation — WCOJ Benchmark

A Java implementation of the **Loomis-Whitney skew-aware Worst-Case Optimal Join** algorithm for conjunctive query evaluation, with a Swing-based benchmarking GUI and a CLI benchmark runner.

## 📁 Project Structure

```
.
├── src/
│   ├── database/
│   │   ├── Tuple.java                  # Tuple (row) with attribute map
│   │   └── Relation.java               # Relation (table) with schema
│   │
│   ├── tree/
│   │   ├── TreeNode.java               # Binary join-tree node
│   │   └── Result.java                 # C / D result-set container
│   │
│   ├── Algorithms/
│   │   └── LoomisWhitneyInstance.java   # Loomis-Whitney implementation
│   │
│   ├── benchmark/
│   │   ├── BenchmarkRunner.java        # CLI benchmark entry point
│   │   ├── BenchmarkResult.java        # Result POJO
│   │   ├── DatabaseGenerator.java      # Synthetic database generator
│   │   ├── QueryPattern.java           # Query-pattern enum
│   │   ├── AlgorithmBenchmark.java     # Algorithm interface
│   │   ├── WCOJAlgorithm.java          # WCOJ adapter
│   │   ├── AlgorithmBenchmark.java     # Benchmark interface
│   │   ├── BenchmarkGUI.java           # Swing GUI window
│   │   └── BenchmarkGUILauncher.java   # GUI entry point (sets L&F)
│   │
│   ├── Main.java                       # Quick-start demos
│   └── BenchmarkExample.java           # Programmatic benchmark examples
│
├── bin/                                # Compiled classes (javac output)
├── .vscode/
│   ├── tasks.json                      # "Compile Java Sources" build task
│   └── launch.json                     # GUI + CLI launch configs
└── README.md
```

## 🔬 Implemented Algorithm

### Worst-Case Optimal Join (WCOJ) — Loomis-Whitney skew-aware
**Status**: ✅ Implemented  
**File**: `src/Algorithms/LoomisWhitneyInstance.java`  
**Complexity**: O(N · IN^ρ* + OUT) — worst-case optimal in the AGM sense  
**Best For**: Cyclic queries, star queries, multi-way joins with potential intermediate blowup

The algorithm recursively processes a binary join tree, partitioning join-key values at each internal node into **heavy hitters** (joined eagerly) and **light hitters** (fully joined within the subtree but deferred to D until the root). The threshold separating heavy from light is `⌈|F| / |D_R|⌉`, which ensures the total work is bounded by the AGM output-size bound.

## 🎯 Package Overview

### `database` package
- **`Tuple`** — row with a `List<Object> values` and a `Map<String,Integer> attributeMap`; supports `projectOn()`, `canJoin()`, `join()`, `projectCommon()`
- **`Relation`** — table with an ordered schema (`List<String>`) and a `Set<Tuple>`

### `tree` package
- **`TreeNode`** — binary join-tree node; leaves correspond to base relations, internal nodes to join operations
- **`Result`** — wraps two sets: **C** (complete, materialized results) and **D** (fully-joined but deferred results)

### `algorithm` package
- **`LoomisWhitneyInstance`** — core WCOJ implementation; exposes `execute()` and `getSizeBound()` (fractional-edge-cover AGM bound)

### `benchmark` package
- **`BenchmarkRunner`** — CLI runner; runs warm-up + timed iterations, prints formatted tables
- **`DatabaseGenerator`** — generates synthetic relations and join trees for all six query patterns
- **`BenchmarkGUI`** — Swing GUI with teal sidebar (configuration), light main panel (results table + log)
- **`BenchmarkGUILauncher`** — sets Metal L&F with custom colour overrides, then opens `BenchmarkGUI` on the EDT

## 🚀 Quick Start

### Build

```bash
# From project root
javac -d bin src\database\*.java src\tree\*.java src\algorithm\*.java src\benchmark\*.java
```

Or use the VS Code task: **Terminal → Run Build Task → Compile Java Sources**.

### Run the GUI

```bash
java -cp bin benchmark.BenchmarkGUILauncher
```

Or use the VS Code launch config **BenchmarkGUI**.

### Run the CLI benchmark

```bash
# All patterns, multiple sizes
java -cp bin benchmark.BenchmarkRunner

# Specific pattern + size
java -cp bin benchmark.BenchmarkRunner LINEAR 1000
java -cp bin benchmark.BenchmarkRunner CYCLIC 100
```

### Basic API usage — WCOJ Algorithm

```java
import algorithm.LoomisWhitneyInstance;
import database.Relation;
import database.Tuple;
import tree.TreeNode;
import java.util.*;

// 1. Create relations with explicit schemas
Map<String, Relation> relations = new HashMap<>();

Relation R = new Relation("R", Arrays.asList("A", "B"));
R.addTuple(new Tuple("a1", "b1"));
R.addTuple(new Tuple("a2", "b2"));
relations.put("R", R);

Relation S = new Relation("S", Arrays.asList("B", "C"));
S.addTuple(new Tuple("b1", "c1"));
S.addTuple(new Tuple("b2", "c2"));
relations.put("S", S);

// 2. Build binary join tree  (leaves = relation names)
TreeNode root = new TreeNode("root");
root.setLeft(new TreeNode("R"));
root.setRight(new TreeNode("S"));

// 3. Execute WCOJ
LoomisWhitneyInstance wcoj = new LoomisWhitneyInstance(relations, root);
Set<Tuple> results = wcoj.execute();
System.out.println("AGM bound: " + wcoj.getSizeBound());

// 4. Process results
for (Tuple t : results) {
    System.out.println(t);
}
```

## 📊 Query Patterns Supported

| Pattern | Query | Relations | Notes |
|---------|-------|-----------|-------|
| `TWO_WAY` | R(A,B) ⋈ S(B,C) | 2 | Simple join |
| `LINEAR` | R(A,B) ⋈ S(B,C) ⋈ T(C,D) | 3 | Chain |
| `CYCLIC` | R(A,B) ⋈ S(B,C) ⋈ T(C,A) | 3 | Triangle — WCOJ excels here |
| `STAR` | R(A,B) ⋈ S(A,C) ⋈ T(A,D) | 3 | Star on attribute A |
| `FOUR_WAY_LINEAR` | R ⋈ S ⋈ T ⋈ U | 4 | 4-relation chain |
| `CROSS_PRODUCT` | R(A) × S(B) | 2 | No shared attributes |

## 📖 Algorithm Details

### Loomis-Whitney skew-aware WCOJ

#### Key Concepts

1. **AGM Bound** (fractional edge cover):
   $$P = \prod_{R} |R|^{1/d(R)}$$
   where $d(R)$ is the maximum degree of any attribute in $R$'s schema. This is a tighter bound than the naive $1/(n{-}1)$ exponent for most real query shapes.

2. **C and D sets**:
   - **C** — tuples that have been fully joined and are part of the final answer
   - **D** — fully-joined subtree tuples that are deferred to be joined at a higher level or at the root

3. **Heavy vs. light hitters** at node `x`:
   - `F = π_λ(D_L) ∩ π_λ(D_R)` — join-key values that appear in both sides
   - threshold `= ⌈|F| / |D_R|⌉`
   - `G = selectTop(F, threshold)` — heavy-hitter keys → joined eagerly
   - `F \ G` — light-hitter keys → fully joined within subtree, deferred in D

#### Pseudocode

```
loomisWhitney(node):
  if node is leaf:
    return (∅, tuples_of_relation(node))

  (C_L, D_L) ← loomisWhitney(node.left)
  (C_R, D_R) ← loomisWhitney(node.right)

  F = π_λ(D_L) ∩ π_λ(D_R)         // matching join-key values
  P = max(1, |F|)
  threshold = max(1, P / |D_R|)
  G = selectTop(F, threshold)        // heavy hitters

  if node is root:
    C = (D_L ⋈ D_R) ∪ C_L ∪ C_R
    D = ∅
  else:
    C = (D_L ⋈_G D_R) ∪ C_L ∪ C_R  // join heavy-hitter tuples
    D = D_L ⋈_{F\G} D_R             // full join for light hitters, deferred

  return (C, D)
```

#### References
- Atserias, Grohe, Marx (2008) — "Size Bounds and Query Plans for Relational Joins" (AGM bound)
- Ngo, Porat, Ré, Rudra (2012/2014) — "Skew strikes back: New developments in the theory of join algorithms"
- Ngo et al. (2018) — "Worst-Case Optimal Join Algorithms" (survey)

## 🧪 Benchmarking

### GUI
Launch `benchmark.BenchmarkGUILauncher`. The window has:
- **Left sidebar** — select query pattern, DB size, or tick *Run all patterns*; green **Run Benchmark** button, **Clear Results** button
- **Right panel** — results table (Query Pattern, DB Size, Time (ms), Result Size, AGM Bound, Memory (MB)); output log below

### CLI
```bash
# All patterns (5 warmup + 5 timed runs each)
java -cp bin benchmark.BenchmarkRunner

# Specific pattern + DB size
java -cp bin benchmark.BenchmarkRunner TWO_WAY 1000
java -cp bin benchmark.BenchmarkRunner CYCLIC 50
```

**Benchmark metrics:**
- Execution time — averaged over 5 runs after 3 warmup runs
- Result size — number of output tuples
- AGM bound — fractional-edge-cover bound
- Memory usage — heap delta before/after

### Adding a new algorithm
1. Implement `AlgorithmBenchmark` (`execute()`, `getName()`, `getSizeBound()`)
2. Wrap it in an adapter and pass it to `BenchmarkRunner.runBenchmark(adapter, pattern, size)`

## 🔧 Implementation Status

### Completed
- ✅ Loomis-Whitney skew-aware WCOJ algorithm
- ✅ Fractional-edge-cover AGM bound
- ✅ Heavy/light hitter partition with per-node threshold
- ✅ Fully-joined D-set propagation (correct multi-level deferral)
- ✅ Attribute-aware `Tuple` (schema + `projectOn` / `canJoin` / `join`)
- ✅ Six query patterns with synthetic database generator
- ✅ CLI benchmark runner (warmup + timed iterations, formatted table)
- ✅ Swing GUI with sidebar layout (Metal L&F, custom colour theme)
- ✅ VS Code tasks and launch configs

## 📚 Background & Motivation

Traditional binary join plans can suffer exponential intermediate-result blowup:
```
(R ⋈ S) ⋈ T   →  |R ⋈ S| can be O(N²) before joining with T
```
Worst-case optimal join algorithms guarantee that the **total work is bounded by the AGM output-size bound**, which for cyclic queries (e.g., triangle) is $O(N^{3/2})$ — far below the naive $O(N^3)$. The Loomis-Whitney framework achieves this by deferring light-hitter groups and immediately processing heavy-hitter groups, so no intermediate set exceeds the bound.

## 📝 Documentation

- **Algorithm Details**: [LoomisWhitneyInstance-Documentation.md](LoomisWhitneyInstance-Documentation.md) - Detailed WCOJ documentation
- **Inline Documentation**: Comprehensive JavaDoc throughout codebase
- **Examples**: Working examples in `Main.java`
- **This README**: High-level overview and comparison

## 🎓 Academic Context

This implementation is part of a Bachelor thesis at the **GUC (German University in Cairo)** on **Conjunction Query Evaluation**. The goal is to provide a clean, documented Java reference implementation of the Loomis-Whitney skew-aware WCOJ algorithm and an empirical benchmarking framework that demonstrates its performance characteristics across different query shapes and database sizes.

## 🛠️ Development Roadmap

- [ ] Hash-indexed join to replace nested-loop join in `loomisWhitney()`
- [ ] Leapfrog trie-join iterator
- [ ] Additional join algorithms for comparison (e.g., Yannakakis, NPRR)
- [ ] Query optimizer / join-tree selector
- [ ] Export benchmark results to CSV / charts in GUI

## 📫 Contributing

This is an academic research project. Suggestions and improvements are welcome for:
- Additional algorithm implementations
- Performance optimizations
- Bug fixes
- Documentation improvements
- Test cases

## 📄 License

[Add your license information here]

---

**Project**: Conjunction Query Evaluation Thesis  
**Institution**: GUC (German University in Cairo)  
**Focus**: Comparative analysis of join algorithms for conjunctive queries
