
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import database.Relation;
import database.Tuple;
import visualization.TracingLoomisWhitney;
import tree.QueryTreeBuilder;
import tree.TreeNode;

public class RunTestsCombined {
    public static void main(String[] args) throws Exception {
        String[] dirs = {
            "src/test/Loomis/binary",
            "src/test/Loomis/multi"
        };
        
        StringBuilder tex = new StringBuilder();
        tex.append("\\chapter{Experimental Results}\\label{chap:results}\n\n");
        
        // SUMMARY TABLE
        tex.append("\\section{Summary of Loomis-Whitney Tests}\n");
        tex.append("\\begin{table}[h]\n");
        tex.append("\\centering\n");
        tex.append("\\begin{tabular}{|l|l|l|l|l|l|l|}\n");
        tex.append("\\hline\n");
        tex.append("Test Group & Case & Query Tree & AGM Bound & Steps & Result Size & Time (ms) \\\\\n");
        tex.append("\\hline\n");

        for (String d : dirs) {
            File dir = new File(d);
            if (!dir.exists()) continue;
            
            Map<String, List<File>> testCases = new HashMap<>();
            for (File f : dir.listFiles()) {
                if (f.getName().endsWith(".csv")) {
                    String name = f.getName().substring(0, f.getName().lastIndexOf("_"));
                    testCases.putIfAbsent(name, new ArrayList<>());
                    testCases.get(name).add(f);
                }
            }
            
            for (String caseName : new TreeSet<>(testCases.keySet())) {
                Map<String, Relation> relations = new HashMap<>(); 
                for (File f : testCases.get(caseName)) {
                    String relName = f.getName().substring(f.getName().lastIndexOf("_") + 1, f.getName().length() - 4);
                    List<String> lines = Files.readAllLines(f.toPath());
                    if (lines.isEmpty()) continue;
                    String[] headers = lines.get(0).split(",");
                    Relation rel = new Relation(relName, Arrays.asList(headers));
                    for (int i = 1; i < lines.size(); i++) {
                        String[] vals = lines.get(i).split(",");
                        rel.addRow((Object[])vals);
                    }
                    relations.put(relName, rel);
                }
                
                try {
                    TreeNode tree = QueryTreeBuilder.buildForLoomisWhitney(relations);
                    TracingLoomisWhitney lw = new TracingLoomisWhitney(relations);
                    
                    long start = System.currentTimeMillis();
                    Set<Tuple> res = lw.execute();
                    long end = System.currentTimeMillis();
                    
                    String bound = "-"; 
                    try {
                        Algorithms.LoomisWhitneyInstance realLw = new Algorithms.LoomisWhitneyInstance(relations);
                        bound = String.format("%.2f", realLw.getSizeBound());
                    } catch (Exception e) {}
                    
                    int steps = lw.getSteps().size();
                    String qTree = "$" + tree.getLabel().replace("⋈", " \\bowtie ") + "$";
                    
                    String row = String.format("%s & %s & %s & %s & %d & %d & %d \\\\\\hline\n", 
                                 dir.getName().replace("_", "\\_"), caseName.replace("_", "\\_"), qTree, bound, steps, res.size(), (end-start));
                    tex.append(row);
                } catch (Exception e) {
                }
            }
        }
        tex.append("\\end{tabular}\n");
        tex.append("\\caption{Summary of Loomis-Whitney algorithm evaluation}\n");
        tex.append("\\end{table}\n\n");
        
        // DETAILED RESULTS
        tex.append("This section presents the full datasets, output results, and statistics for all test cases.\n\n");

        for (String d : dirs) {
            File dir = new File(d);
            if (!dir.exists()) continue;
            
            tex.append("\\section{Test Group: ").append(dir.getName().replace("_", "\\_")).append("}\n");

            Map<String, List<File>> testCases = new HashMap<>();
            for (File f : dir.listFiles()) {
                if (f.getName().endsWith(".csv")) {
                    String name = f.getName().substring(0, f.getName().lastIndexOf("_"));
                    testCases.putIfAbsent(name, new ArrayList<>());
                    testCases.get(name).add(f);
                }
            }
            
            for (String caseName : new TreeSet<>(testCases.keySet())) {
                tex.append("\\subsection{Case: ").append(caseName.replace("_", "\\_")).append("}\n");
                Map<String, Relation> relations = new HashMap<>(); 
                
                for (File f : testCases.get(caseName)) {
                    String relName = f.getName().substring(f.getName().lastIndexOf("_") + 1, f.getName().length() - 4);
                    List<String> lines = Files.readAllLines(f.toPath());
                    if (lines.isEmpty()) continue;
                    String[] headers = lines.get(0).split(",");
                    Relation rel = new Relation(relName, Arrays.asList(headers));
                    for (int i = 1; i < lines.size(); i++) {
                        String[] vals = lines.get(i).split(",");
                        rel.addRow((Object[])vals);
                    }
                    relations.put(relName, rel);
                }
                
                tex.append("\\subsubsection*{Input Relations}\n");
                for (String relName : new TreeSet<>(relations.keySet())) {
                    Relation rel = relations.get(relName);
                    tex.append("\\textbf{Relation ").append(relName).append("}\n\n");
                    tex.append("\\begin{center}\n\\begin{tabular}{|");
                    for (int i = 0; i < rel.getColumns().size(); i++) tex.append("l|");
                    tex.append("}\n\\hline\n");
                    
                    for (int i = 0; i < rel.getColumns().size(); i++) {
                        tex.append(rel.getColumns().get(i));
                        if (i < rel.getColumns().size() - 1) tex.append(" & ");
                    }
                    tex.append(" \\\\\\hline\n");
                    
                    for (Tuple row : rel.getTuples()) {
                        for (int i = 0; i < rel.getColumns().size(); i++) {
                            Object val = row.getValue(i);
                            tex.append(val == null ? "null" : val.toString());
                            if (i < rel.getColumns().size() - 1) tex.append(" & ");
                        }
                        tex.append(" \\\\\\hline\n");
                    }
                    tex.append("\\end{tabular}\n\\end{center}\n\n");
                }
                
                try {
                    TreeNode tree = QueryTreeBuilder.buildForLoomisWhitney(relations);
                    TracingLoomisWhitney lw = new TracingLoomisWhitney(relations);
                    
                    long start = System.currentTimeMillis();
                    Set<Tuple> res = lw.execute();
                    long end = System.currentTimeMillis();
                    
                    String bound = "-"; 
                    try {
                        Algorithms.LoomisWhitneyInstance realLw = new Algorithms.LoomisWhitneyInstance(relations);
                        bound = String.format("%.2f", realLw.getSizeBound());
                    } catch (Exception e) {}
                    int steps = lw.getSteps().size();
                    
                    tex.append("\\subsubsection*{Execution Stats}\n");
                    tex.append("\\begin{itemize}\n");
                    tex.append("\\item \\textbf{Query Tree:} $").append(tree.getLabel().replace("⋈", " \\bowtie ")).append("$\n");
                    tex.append("\\item \\textbf{AGM Bound:} ").append(bound).append("\n");
                    tex.append("\\item \\textbf{Recursive Steps:} ").append(steps).append("\n");
                    tex.append("\\item \\textbf{Execution Time:} ").append(end - start).append(" ms\n");
                    tex.append("\\end{itemize}\n\n");
                    
                    tex.append("\\subsubsection*{Output Result (").append(res.size()).append(" rows)}\n");
                    if (res.isEmpty()) {
                        tex.append("Result is empty.\\\\ \n\n");
                    } else {
                        Tuple first = res.iterator().next();
                        List<String> outputCols = new ArrayList<>(first.getAttributeMap().keySet());
                        Collections.sort(outputCols);
                        
                        tex.append("\\begin{center}\n\\begin{tabular}{|");
                        for (int i = 0; i < outputCols.size(); i++) tex.append("l|");
                        tex.append("}\n\\hline\n");
                        
                        for (int i = 0; i < outputCols.size(); i++) {
                            tex.append(outputCols.get(i));
                            if (i < outputCols.size() - 1) tex.append(" & ");
                        }
                        tex.append(" \\\\\\hline\n");
                        
                        for (Tuple row : res) {
                            for (int i = 0; i < outputCols.size(); i++) {
                                Object val = row.getValueByAttribute(outputCols.get(i));
                                tex.append(val == null ? "null" : val.toString());
                                if (i < outputCols.size() - 1) tex.append(" & ");
                            }
                            tex.append(" \\\\\\hline\n");
                        }
                        tex.append("\\end{tabular}\n\\end{center}\n\n");
                    }
                    
                } catch (Exception e) {
                    tex.append("Error executing test case: ").append(e.getMessage()).append("\n\n");
                }
                tex.append("\\clearpage\n\n");
            }
        }
        
        Files.write(Paths.get("General.tex"), tex.toString().getBytes());
        System.out.println("Written to General.tex with summary table!");
    }
}
