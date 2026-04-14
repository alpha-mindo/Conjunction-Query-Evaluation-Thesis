package benchmark;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import database.Relation;
import java.io.File;
import java.util.List;

public class CQEStudio extends Application {

    private ComboBox<String> algorithmComboBox;
    private TextField queryField;
    private TextArea logArea;
    private VBox tablesVBox;
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("CQE Studio - JavaFX");

        // Main Layout
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setStyle("-fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-base: #2D2D2D; -fx-control-inner-background: #1E1E1E; -fx-text-background-color: #E0E0E0;");

        // Top Sidebar (Controls)
        VBox controlPanel = new VBox(10);
        controlPanel.setAlignment(Pos.TOP_CENTER);
        controlPanel.setPadding(new Insets(15));
        controlPanel.setStyle("-fx-background-color: #3C3F41; -fx-background-radius: 5;");
        controlPanel.setPrefWidth(300);

        Label titleLabel = new Label("Configuration");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Algorithm Selection
        algorithmComboBox = new ComboBox<>();
        algorithmComboBox.setMaxWidth(Double.MAX_VALUE);
        refreshAlgorithms();

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> refreshAlgorithms());
        
        HBox algoBox = new HBox(10, algorithmComboBox, refreshBtn);
        algoBox.setAlignment(Pos.CENTER_LEFT);

        // Query Input
        queryField = new TextField();
        queryField.setPromptText("Enter Datalog query, e.g., R(A,B), S(B,C)");

        // Table Inputs
        tablesVBox = new VBox(5);
        tablesVBox.setPadding(new Insets(5, 0, 5, 0));
        Button addTableBtn = new Button("Add Table / CSV");
        addTableBtn.setOnAction(e -> addTableEntry(primaryStage));

        Button runBtn = new Button("Run Benchmark");
        runBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        runBtn.setMaxWidth(Double.MAX_VALUE);
        runBtn.setOnAction(e -> runBenchmark());

        controlPanel.getChildren().addAll(
                titleLabel,
                new Label("Select Algorithm:"),
                algoBox,
                new Label("Query:"),
                queryField,
                new Separator(),
                new Label("Tables:"),
                tablesVBox,
                addTableBtn,
                new Separator(),
                runBtn
        );

        // Center Area (Logs/Visualization)
        VBox centerArea = new VBox(10);
        centerArea.setPadding(new Insets(0, 0, 0, 10));

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setStyle("-fx-control-inner-background: #1E1E1E; -fx-text-fill: #A9B7C6; -fx-font-family: 'Consolas', monospace;");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        centerArea.getChildren().addAll(new Label("Execution Logs:"), logArea);

        root.setLeft(controlPanel);
        root.setCenter(centerArea);

        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add("data:text/css," +
                ".label { -fx-text-fill: #E0E0E0; }" +
                ".button { -fx-background-radius: 3; -fx-cursor: hand; }" +
                ".text-field, .text-area, .combo-box { -fx-background-radius: 3; }"
        );

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void refreshAlgorithms() {
        try {
            algorithmComboBox.getItems().clear();
            List<String> algos = AlgorithmFinder.getAvailableAlgorithms();
            for (String className : algos) {
                algorithmComboBox.getItems().add(className);
            }
            if (!algorithmComboBox.getItems().isEmpty()) {
                algorithmComboBox.getSelectionModel().selectFirst();
            }
        } catch (Exception ex) {
            log("Error loading algorithms: " + ex.getMessage());
        }
    }

    private void addTableEntry(Stage stage) {
        HBox row = new HBox(5);
        row.setAlignment(Pos.CENTER_LEFT);
        
        TextField nameField = new TextField();
        nameField.setPromptText("Name (e.g. R)");
        nameField.setPrefWidth(60);

        TextField pathField = new TextField();
        pathField.setPromptText("CSV Path");
        HBox.setHgrow(pathField, Priority.ALWAYS);

        Button browseBtn = new Button("...");
        browseBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select CSV File");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                pathField.setText(file.getAbsolutePath());
                if (nameField.getText().isEmpty()) {
                    nameField.setText(file.getName().replace(".csv", ""));
                }
            }
        });

        Button removeBtn = new Button("X");
        removeBtn.setStyle("-fx-background-color: #E53935; -fx-text-fill: white;");
        removeBtn.setOnAction(e -> tablesVBox.getChildren().remove(row));

        row.getChildren().addAll(nameField, pathField, browseBtn, removeBtn);
        tablesVBox.getChildren().add(row);
    }

    private void runBenchmark() {
        String algoName = algorithmComboBox.getValue();
        String query = queryField.getText().trim();

        if (algoName == null || algoName.isEmpty() || query.isEmpty()) {
            log("Error: Select an algorithm and enter a query.");
            return;
        }

        // Collect table paths
        java.util.Map<String, String> tablePaths = new java.util.HashMap<>();
        for (javafx.scene.Node node : tablesVBox.getChildren()) {
            if (node instanceof HBox) {
                HBox row = (HBox) node;
                TextField nameField = (TextField) row.getChildren().get(0);
                TextField pathField = (TextField) row.getChildren().get(1);
                String tName = nameField.getText().trim();
                String tPath = pathField.getText().trim();
                if (!tName.isEmpty() && !tPath.isEmpty()) {
                    tablePaths.put(tName, tPath);
                }
            }
        }

        log("--------------");
        log("Starting run for query: " + query);
        log("Algorithm: " + algoName);
        
        new Thread(() -> {
            try {
                // 1. Parse Query, e.g. R(A,B), S(B,C)
                java.util.Map<String, java.util.List<String>> schemas = new java.util.HashMap<>();
                String[] parts = query.split("\\),");
                for (String part : parts) {
                    String p = part.trim();
                    if (p.endsWith(")")) p = p.substring(0, p.length() - 1);
                    int parenIdx = p.indexOf('(');
                    if (parenIdx == -1) throw new Exception("Invalid query format at: " + p);
                    String tName = p.substring(0, parenIdx).trim();
                    String attrsStr = p.substring(parenIdx + 1);
                    String[] attrs = attrsStr.split(",");
                    java.util.List<String> attrList = new java.util.ArrayList<>();
                    for (String a : attrs) attrList.add(a.trim());
                    schemas.put(tName, attrList);
                }

                // 2. Load Relations from CSV
                java.util.Map<String, Relation> relations = new java.util.HashMap<>();
                for (java.util.Map.Entry<String, java.util.List<String>> entry : schemas.entrySet()) {
                    String tName = entry.getKey();
                    java.util.List<String> cols = entry.getValue();
                    String path = tablePaths.get(tName);
                    if (path == null) throw new Exception("Missing CSV file path for relation: " + tName);
                    
                    Relation r = new Relation(tName, cols);
                    int rowCount = 0;
                    try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(path))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            if (line.trim().isEmpty()) continue;
                            String[] vals = line.split(",");
                            if (vals.length != cols.size()) continue;
                            Object[] objVals = new Object[vals.length];
                            for (int i=0; i<vals.length; i++) {
                                try {
                                    objVals[i] = Integer.parseInt(vals[i].trim());
                                } catch(Exception e) {
                                    objVals[i] = vals[i].trim();
                                }
                            }
                            database.Tuple t = new database.Tuple(objVals);
                            r.addTuple(t);
                            rowCount++;
                        }
                    }
                    relations.put(tName, r);
                    final int finalRowCount = rowCount;
                    final String finalTName = tName;
                    Platform.runLater(() -> log("Loaded relation " + finalTName + " with " + finalRowCount + " tuples."));
                }

                // 3. Build Query Tree
                tree.TreeNode queryTree = tree.QueryTreeBuilder.build(relations);
                Platform.runLater(() -> log("Constructed Join Tree: " + queryTree.getLabel()));

                // 4. Instantiate Algorithm using Reflection
                Class<?> clazz = Class.forName("Algorithms." + algoName);
                Object instance = clazz.getConstructor(java.util.Map.class, tree.TreeNode.class).newInstance(relations, queryTree);

                // 5. Execute
                Platform.runLater(() -> log("Executing query..."));
                long startTime = System.currentTimeMillis();
                java.lang.reflect.Method executeMethod = clazz.getMethod("execute");
                @SuppressWarnings("unchecked")
                java.util.Set<database.Tuple> results = (java.util.Set<database.Tuple>) executeMethod.invoke(instance);
                long endTime = System.currentTimeMillis();
                
                Platform.runLater(() -> {
                    log("Query completed in " + (endTime - startTime) + " ms");
                    if (results != null) {
                        log("Result Set Size: " + results.size());
                        // Optional: log first few results
                        int limit = Math.min(10, results.size());
                        int count = 0;
                        for (database.Tuple res : results) {
                            if (count >= limit) break;
                            log("  " + res.getValues());
                            count++;
                        }
                        if (results.size() > limit) log("  ... and " + (results.size() - limit) + " more");
                    }
                });

            } catch (Exception ex) {
                Platform.runLater(() -> log("Execution Error: " + ex.getMessage()));
                ex.printStackTrace();
            }
        }).start();
    }

    private void log(String msg) {
        logArea.appendText(msg + "\n");
    }

    public static void main(String[] args) {
        launch(args);
    }
}