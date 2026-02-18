# Conjunction Query Evaluation - Algorithm Implementations

A comprehensive Java implementation of various join algorithms for conjunctive query evaluation, focusing on worst-case optimal approaches and comparative analysis.

## 📁 Project Structure

```
.
├── database/
│   ├── Tuple.java           # Represents a tuple (row) in a relation
│   └── Relation.java        # Represents a database relation (table)
│
├── tree/
│   ├── TreeNode.java        # Represents nodes in the join tree
│   └── Result.java          # Container for C and D result sets
│
├── algorithm/
│   ├── WorstCaseOptimalJoin.java  # WCOJ algorithm implementation
│   └── [Future algorithms...]     # Additional join algorithms
│
└── Main.java                # Example usage and test cases
```

## 🔬 Implemented Algorithms

### 1. Worst-Case Optimal Join (WCOJ)
**Status**: ✅ Implemented  
**File**: `algorithm/WorstCaseOptimalJoin.java`  
**Complexity**: O(N + Output)  
**Best For**: Cyclic queries, multi-way joins

The WCOJ algorithm uses delayed materialization and conditional joins to ensure no intermediate result exceeds the AGM bound.

### 2. [Algorithm Name - Coming Soon]
**Status**: 🚧 Planned  
**Description**: TBD

### 3. [Algorithm Name - Coming Soon]
**Status**: 🚧 Planned  
**Description**: TBD

## 🎯 Package Overview

### `database` Package
Contains the core database structures:
- **`Tuple`**: Represents a single row with ordered attribute values
- **`Relation`**: Represents a database table containing a set of tuples

### `tree` Package
Contains join tree structures:
- **`TreeNode`**: Represents nodes in the join tree (leaves = relations, internal = joins)
- **`Result`**: Wraps the C (complete) and D (delayed) result sets

### `algorithm` Package
Contains the main algorithm:
- **`WorstCaseOptimalJoin`**: Implements the WCOJ algorithm with O(N + Output) complexity

## 🚀 Quick Start

### Running Examples

```bash
javac Main.java
java Main
```

The `Main.java` file demonstrates various algorithms with example queries:
- Two-way joins (R ⋈ S)
- Three-way cyclic joins (R ⋈ S ⋈ T) - the "triangle query"
- Additional examples as more algorithms are implemented

### Basic Usage - WCOJ Algorithm

```java
import algorithm.WorstCaseOptimalJoin;
import database.Relation;
import database.Tuple;
import tree.TreeNode;
import java.util.*;

// 1. Create relations
Map<String, Relation> relations = new HashMap<>();

Relation R = new Relation("R");
R.addTuple(new Tuple("a1", "b1"));
R.addTuple(new Tuple("a2", "b2"));
relations.put("R", R);

Relation S = new Relation("S");
S.addTuple(new Tuple("b1", "c1"));
S.addTuple(new Tuple("b2", "c2"));
relations.put("S", S);

// 2. Build join tree
TreeNode root = new TreeNode("root");
TreeNode leftLeaf = new TreeNode("R");
TreeNode rightLeaf = new TreeNode("S");
root.setLeft(leftLeaf);
root.setRight(rightLeaf);

// 3. Execute WCOJ
WorstCaseOptimalJoin wcoj = new WorstCaseOptimalJoin(relations, root);
Set<Tuple> results = wcoj.execute();

// 4. Process results
for (Tuple t : results) {
    System.out.println(t);
}
```

## 📊 Algorithm Comparison

| Algorithm | Time Complexity | Space | Best Use Case | Status |
|-----------|----------------|-------|---------------|--------|
| WCOJ      | O(N + Output)  | O(N)  | Cyclic queries, multi-way joins | ✅ |
| [TBD]     | TBD            | TBD   | TBD           | 🚧 |
| [TBD]     | TBD            | TBD   | TBD           | 🚧 |

## 📖 Algorithm Details

### Worst-Case Optimal Join (WCOJ)

#### Overview
- **Time Complexity**: O(N + Output) where N is the input size and Output is the result size
- **Worst-case optimal** for conjunctive queries
- Particularly efficient for cyclic queries

#### Key Concepts

1. **AGM Bound**: `P = ∏_{c∈C} N_c^{1/(n-1)}`
   - Theoretical worst-case size of join results
   - Algorithm ensures no intermediate result exceeds this bound

2. **C and D Sets**:
   - **C**: Complete results (fully materialized)
   - **D**: Delayed results (deferred processing)
   - Strategy avoids exponential blowup in intermediate results

3. **Conditional Join** (⋈_G):
   - Only materializes tuples that satisfy size constraints
   - Limits intermediate result sizes based on AGM bound

#### Algorithm Flow

```
LW(x):
  if x is leaf:
    return (∅, relation_tuples)
  
  (C_L, D_L) ← LW(left_child)
  (C_R, D_R) ← LW(right_child)
  
  F ← project(D_L) ∩ project(D_R)
  G ← top-⌈P/|D_R|⌉ tuples from F
  
  if x is root:
    C ← (D_L ⋈ D_R) ∪ C_L ∪ C_R
    D ← ∅
  else:
    C ← (D_L ⋈_G D_R) ∪ C_L ∪ C_R
    D ← F \ G
  
  return (C, D)
```

#### When to Use WCOJ
- **Cyclic queries** (e.g., triangle queries, clique queries)
- **Multi-way joins** with complex patterns
- **Queries where intermediate results might explode**

#### References
- **AGM Bound**: Atserias, Grohe, Marx (2008) "Size Bounds and Query Plans for Relational Joins"
- **Leapfrog Join**: Veldhuizen (2014) "Leapfrog Triejoin: A Simple, Worst-Case Optimal Join Algorithm"
- **WCOJ Survey**: Ngo et al. (2018) "Worst-Case Optimal Join Algorithms"

---

### [Future Algorithm 1]
*Coming soon...*

---

### [Future Algorithm 2]
*Coming soon...*

## 🧪 Testing & Benchmarking

### Current Test Cases
- Simple two-way joins
- Three-way cyclic joins (triangle queries)

### Future Benchmarking
- [ ] Performance comparison across algorithms
- [ ] Scalability tests with varying input sizes
- [ ] Cyclic vs. acyclic query performance
- [ ] Memory usage profiling
- [ ] Query plan optimization evaluation

## 🔧 Implementation Status

### Completed Features
- ✅ Complete WCOJ algorithm structure
- ✅ AGM bound computation
- ✅ Delayed materialization strategy
- ✅ Conditional join operations
- ✅ Clean modular architecture
- ✅ Comprehensive documentation
- ✅ Basic example queries

### In Progress
- 🚧 Additional join algorithms
- 🚧 Performance benchmarking suite
- 🚧 Query optimization framework

### Planned Enhancements
- [ ] Attribute-based join matching
- [ ] Schema management system
- [ ] Hash-based indexes for faster lookups
- [ ] Leapfrog iterators implementation
- [ ] Memory management and disk spilling
- [ ] Query optimization and tree selection
- [ ] Comparative performance analysis
- [ ] Visual query plan representation

## 📚 Background & Motivation

### Why Study Different Join Algorithms?

Join algorithms are fundamental to database query processing. Different algorithms excel in different scenarios:

- **Traditional Binary Joins**: Good for simple chain queries
- **Worst-Case Optimal Joins**: Excel at cyclic queries and avoid intermediate result explosions
- **Hash Joins**: Fast for equi-joins with good hash distribution
- **Sort-Merge Joins**: Efficient when inputs are pre-sorted

### The Problem with Traditional Approaches

Traditional binary join plans can suffer exponential blowup:
```
(R ⋈ S) ⋈ T
```
The intermediate result `R ⋈ S` might be huge!

### Our Solution

This project implements and compares multiple join strategies to:
1. Understand their theoretical foundations
2. Measure practical performance differences
3. Identify optimal use cases for each algorithm
4. Provide a reference implementation for academic study

## 🤝 Benefits of This Architecture

### Modularity
- Each package has a clear responsibility
- Easy to add new algorithms without modifying existing code
- Simple to extend with new features
- Algorithms share common data structures

### Maintainability
- Clear separation of concerns
- Well-documented with JavaDoc
- Straightforward to test individual components
- Easy to update algorithms independently

### Extensibility
- Easy to add new database structures (e.g., Schema, Index)
- Simple to implement alternative join strategies
- Straightforward to add optimization techniques
- Ready for comparative benchmarking

### Comparative Analysis Ready
- Common interfaces allow algorithm comparison
- Shared data structures enable fair benchmarking
- Easy to swap algorithms for the same query
- Structured for performance profiling

## 📝 Documentation

- **Algorithm Details**: [WorstCaseOptimalJoin-Documentation.md](WorstCaseOptimalJoin-Documentation.md) - Detailed WCOJ documentation
- **Inline Documentation**: Comprehensive JavaDoc throughout codebase
- **Examples**: Working examples in `Main.java`
- **This README**: High-level overview and comparison

## 🎓 Academic Context

This implementation is part of a thesis on **Conjunction Query Evaluation**, demonstrating:
- Practical implementation of multiple join algorithms
- Comparative analysis of algorithmic approaches
- Performance characteristics across query types
- Theoretical foundations applied to real-world scenarios

### Research Goals
1. Implement various conjunctive query evaluation algorithms
2. Compare performance characteristics empirically
3. Identify optimal use cases for each approach
4. Provide educational reference implementations

## 🛠️ Development Roadmap

### Phase 1: Foundation (Current)
- [x] WCOJ implementation
- [x] Core data structures
- [x] Modular architecture
- [x] Basic examples

### Phase 2: Additional Algorithms
- [ ] Implement Algorithm 2
- [ ] Implement Algorithm 3
- [ ] Implement Algorithm 4
- [ ] Standardize algorithm interfaces

### Phase 3: Benchmarking
- [ ] Create comprehensive test suite
- [ ] Performance profiling framework
- [ ] Comparative analysis tools
- [ ] Visualization of results

### Phase 4: Optimization
- [ ] Advanced indexing structures
- [ ] Query plan optimization
- [ ] Memory management
- [ ] Parallel execution support

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
