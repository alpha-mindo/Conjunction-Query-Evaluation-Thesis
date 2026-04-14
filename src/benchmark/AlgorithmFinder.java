package benchmark;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class AlgorithmFinder {
    
    public static List<String> getAvailableAlgorithms() {
        List<String> algorithms = new ArrayList<>();
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL resource = classLoader.getResource("Algorithms");
            if (resource != null) {
                File directory = new File(resource.getFile());
                if (directory.exists() && directory.isDirectory()) {
                    File[] files = directory.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.getName().endsWith(".class")) {
                                String className = file.getName().substring(0, file.getName().length() - 6);
                                algorithms.add(className);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Fallback
        }
        
        // Always ensure at least the base one is there if reflection fails
        if (algorithms.isEmpty()) {
            algorithms.add("LoomisWhitneyInstance");
        }
        return algorithms;
    }
}
