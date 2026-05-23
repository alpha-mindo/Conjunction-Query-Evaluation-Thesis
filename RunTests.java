import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import database.Relation;
import database.Tuple;
import visualization.TracingLoomisWhitney;
import tree.QueryTreeBuilder;
import tree.TreeNode;

public class RunTests {
    public static void main(String[] args) throws Exception {
        String[] dirs = {
            "src/test/General",
            "src/test/Loomis/binary",
            "src/test/Loomis/multi"
        };
        
        StringBuilder tex = new StringBuilder();
        tex.append("\\begin{table}[h]\n");
        tex.append("\\centering\n");
        tex.append("\\begin{tabular}{|l|l|l|l|l|l|}\n");
        tex.append("\\hline\n");
        tex.append("Test Group & Case & Bound & Steps & Result Size & Time (ms) \\\\\n");
        tex.append("\\hline\n");

        for (String d : dirs) {
            File dir = new File(d);
            if (!dir.exists()) continue;
            
            // Group files by case name
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
                    
                    String row = String.format("%s & %s & %s & %d & %d & %d \\\\\\hline\n", 
                                 dir.getName(), caseName, bound, steps, res.size(), (end-start));
                    System.out.print(row);
                    tex.append(row);
                } catch (Exception e) {
                    System.out.println("Error on " + caseName + ": " + e.getMessage());
                }
            }
        }
        tex.append("\\end{tabular}\n");
        tex.append("\\caption{Results of running TracingLoomisWhitney algorithm on test cases}\n");
        tex.append("\\end{table}\n");
        
        System.out.println("=== LATEX OUTPUT ===");
        System.out.println(tex.toString());
    }
}