package visualization;

import database.Relation;
import database.Tuple;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tree.QueryTreeBuilder;
import tree.TreeNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.chart.*;

public class GuiApp extends Application {

    private TextArea resultArea;
    private TextArea logArea;
    private ComboBox<String> algoSelector;
    private ListView<String> tableListView;
    private BarChart<String, Number> statChart;
    private Map<String, Relation> memoryRelations = new HashMap<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Join Algorithm Evaluator");

        // Top UI: Algorithm Dropdown & Query Input
        HBox topBox = new HBox(10);
        topBox.setPadding(new Insets(10));
        
        Label algoLabel = new Label("Algorithm:");
        algoSelector = new ComboBox<>();
        algoSelector.getItems().addAll("Loomis-Whitney WCOJ"); // Ready for more!
        algoSelector.setValue("Loomis-Whitney WCOJ");

        Label queryLabel = new Label("Query (e.g. R, S):");
        TextField queryInput = new TextField();
        queryInput.setPromptText("Relation names...");
        queryInput.setPrefWidth(200);
        Button runButton = new Button("Run Query");

        topBox.getChildren().addAll(algoLabel, algoSelector, queryLabel, queryInput, runButton);

        // Left UI: Data Manager
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setPrefWidth(200);
        leftPanel.setStyle("-fx-border-color: lightgray; -fx-border-width: 0 1 0 0;");
        
        Label dataLabel = new Label("Loaded Tables");
        tableListView = new ListView<>();
        tableListView.setPrefHeight(200);
        
        Button loadCsvButton = new Button("Load CSV...");
        loadCsvButton.setMaxWidth(Double.MAX_VALUE);
        Button enterDataButton = new Button("Enter Data Manually...");
        enterDataButton.setMaxWidth(Double.MAX_VALUE);

        leftPanel.getChildren().addAll(dataLabel, tableListView, loadCsvButton, enterDataButton);

        // Center UI: Logs and Results
        resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setFont(javafx.scene.text.Font.font("Monospaced", 12));

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setFont(javafx.scene.text.Font.font("Monospaced", 12));

        SplitPane splitPane = new SplitPane();
        
        VBox centerLeftBox = new VBox(5, new Label("Execution Log / Tree / Tables"), logArea);
        VBox.setVgrow(logArea, Priority.ALWAYS);
        centerLeftBox.setPadding(new Insets(10));

        VBox centerRightBox = new VBox(5, new Label("Result"), resultArea);
        VBox.setVgrow(resultArea, Priority.ALWAYS);
        centerRightBox.setPadding(new Insets(10));

        splitPane.getItems().addAll(centerLeftBox, centerRightBox);
        splitPane.setDividerPositions(0.5);

        // Analytics UI
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        statChart = new BarChart<>(xAxis, yAxis);
        statChart.setTitle("Query Statistics");
        statChart.setAnimated(false);
        VBox analyticsBox = new VBox(10, statChart);
        VBox.setVgrow(statChart, Priority.ALWAYS);

        TabPane tabPane = new TabPane();
        Tab executionTab = new Tab("Execution & Results", splitPane);
        executionTab.setClosable(false);
        Tab analyticsTab = new Tab("Analytics", analyticsBox);
        analyticsTab.setClosable(false);
        tabPane.getTabs().addAll(executionTab, analyticsTab);

        // Event Handlers
        runButton.setOnAction(e -> executeQuery(queryInput.getText()));
        
        loadCsvButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            List<File> files = chooser.showOpenMultipleDialog(primaryStage);
            if (files != null) {
                for (File file : files) {
                    String name = file.getName().replaceFirst("[.][^.]+$", "");
                    try {
                        Relation rel = loadRelationFromCsv(name, file);
                        memoryRelations.put(name, rel);
                    } catch (Exception ex) {
                        showError("CSV Load Error", "Failed to load " + file.getName() + ": " + ex.getMessage());
                    }
                }
                refreshTableList();
            }
        });

        enterDataButton.setOnAction(e -> openManualDataEntryDialog());

        // Layout
        BorderPane mainPane = new BorderPane();
        mainPane.setTop(topBox);
        mainPane.setLeft(leftPanel);
        mainPane.setCenter(tabPane);

        Scene scene = new Scene(mainPane, 1000, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void refreshTableList() {
        tableListView.getItems().setAll(memoryRelations.keySet());
    }

    private void openManualDataEntryDialog() {
        Dialog<Relation> dialog = new Dialog<>();
        dialog.setTitle("Enter Data Manually");
        dialog.setHeaderText("Create a new table");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        VBox box = new VBox(10);
        TextField nameField = new TextField();
        nameField.setPromptText("Table Name (e.g. R)");
        
        TextArea dataArea = new TextArea();
        dataArea.setPromptText("Row 1: Col1, Col2\nRow 2: Val1, Val2\nRow 3: Val3, Val4");

        box.getChildren().addAll(new Label("Table Name:"), nameField, new Label("CSV Data (Header + Rows):"), dataArea);
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(btn -> {
            if (btn == saveButtonType) {
                String name = nameField.getText().trim();
                String data = dataArea.getText().trim();
                if (name.isEmpty() || data.isEmpty()) return null;
                
                String[] lines = data.split("\\r?\\n");
                if (lines.length < 1) return null;
                
                List<String> cols = Arrays.asList(lines[0].split(","));
                for(int i = 0; i < cols.size(); i++) cols.set(i, cols.get(i).trim());
                
                Relation r = new Relation(name, cols);
                for (int i = 1; i < lines.length; i++) {
                    if (lines[i].trim().isEmpty()) continue;
                    String[] vals = lines[i].split(",");
                    Object[] objVals = new Object[cols.size()];
                    for (int j = 0; j < cols.size(); j++) {
                        objVals[j] = j < vals.length ? vals[j].trim() : null;
                    }
                    r.addRow(objVals);
                }
                return r;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(rel -> {
            memoryRelations.put(rel.getName(), rel);
            refreshTableList();
        });
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void executeQuery(String queryText) {
        logArea.clear();
        resultArea.clear();

        if (queryText == null || queryText.trim().isEmpty()) {
            logArea.setText("Please enter a valid query.");
            return;
        }

        // Parse relations and their optional custom variables
        Map<String, List<String>> queryRelations = new LinkedHashMap<>();
        Pattern pattern = Pattern.compile("([A-Za-z0-9_]+)(?:\\s*\\(([^)]*)\\))?");
        Matcher matcher = pattern.matcher(queryText);
        while (matcher.find()) {
            String name = matcher.group(1).trim();
            String varsMatcher = matcher.group(2);
            List<String> vars = null;
            if (varsMatcher != null && !varsMatcher.trim().isEmpty()) {
                vars = new ArrayList<>();
                for (String v : varsMatcher.split(",")) {
                    vars.add(v.trim());
                }
            }
            queryRelations.put(name, vars);
        }

        if (queryRelations.isEmpty()) {
            logArea.setText("No valid relation names found in query.");
            return;
        }

        Map<String, Relation> relations = new HashMap<>();
        String testDir = "src/test"; 

        for (Map.Entry<String, List<String>> entry : queryRelations.entrySet()) {
            String name = entry.getKey();
            List<String> customCols = entry.getValue();
            Relation originalRel = null;

            // First check memory, then fallback to src/test
            if (memoryRelations.containsKey(name)) {
                originalRel = memoryRelations.get(name);
                logArea.appendText("Loaded relation '" + name + "' from Data Manager.\n");
            } else {
                File csvFile = new File(testDir, name + ".csv");
                if (!csvFile.exists()) {
                    logArea.appendText("Error: Cannot find data for relation '" + name + "' in Memory or at " + csvFile.getAbsolutePath() + "\n");
                    return;
                }
                try {
                    originalRel = loadRelationFromCsv(name, csvFile);
                    logArea.appendText("Loaded relation '" + name + "' from file.\n");
                } catch (Exception ex) {
                    logArea.appendText("Error loading relation '" + name + "': " + ex.getMessage() + "\n");
                    return;
                }
            }

            // Apply custom column renames if provided in the query like R(a,b)
            if (customCols != null) {
                if (customCols.size() != originalRel.columnCount()) {
                    logArea.appendText("Error: Query for '" + name + "' expects " + customCols.size() + 
                                       " columns but data has " + originalRel.columnCount() + ".\n");
                    return;
                }
                Relation renamedRel = new Relation(name, customCols);
                for (Tuple t : originalRel.getTuples()) {
                    Object[] vals = new Object[customCols.size()];
                    for (int i = 0; i < customCols.size(); i++) vals[i] = t.getValue(i);
                    renamedRel.addRow(vals);
                }
                relations.put(name, renamedRel);
                logArea.appendText("Renamed columns of '" + name + "' to " + customCols + "\n");
            } else {
                relations.put(name, originalRel);
            }
            logArea.appendText(relations.get(name).toTableString() + "\n\n");
        }

        try {
            String algo = algoSelector.getValue();
            logArea.appendText("Executing using: " + algo + "\n\n");

            long startTime = System.nanoTime();

            TreeNode root = QueryTreeBuilder.build(relations);
            logArea.appendText("Query tree: " + root.getLabel() + "\n");

            Set<Tuple> results;
            double sizeBound = 0.0;

            if (algo.equals("Loomis-Whitney WCOJ")) {
                TracingLoomisWhitney lw = new TracingLoomisWhitney(relations);
                sizeBound = lw.getSizeBound();
                logArea.appendText(String.format("Size Bound: %.2f\n\n", sizeBound));
                results = lw.execute();

                logArea.appendText("--- Execution Trace ---\n");
                for (AlgorithmStep step : lw.getSteps()) {
                    logArea.appendText(step.heading + "\n");
                    logArea.appendText(step.narrative + "\n\n");
                }
            } else {
                // Future algorithms will go here
                results = new HashSet<>(); 
            }

            long endTime = System.nanoTime();
            double durationMs = (endTime - startTime) / 1_000_000.0;

            resultArea.setText("Result — " + root.getLabel() + " (" + results.size() + " rows):\n");
            resultArea.appendText(formatResultSet(results, getResultColumns(root, relations)));

            // Update Analytics Chart
            statChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(algo + " Stats");
            series.getData().add(new XYChart.Data<>("Execution Time (ms)", durationMs));
            series.getData().add(new XYChart.Data<>("Theoretical Size Bound", sizeBound));
            series.getData().add(new XYChart.Data<>("Actual Result Size", results.size()));
            statChart.getData().add(series);

        } catch (Exception ex) {
            logArea.appendText("Execution Error: " + ex.getMessage() + "\n");
            ex.printStackTrace();
        }
    }

    private Relation loadRelationFromCsv(String name, File file) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String headerLine = br.readLine();
            if (headerLine == null) throw new IOException("CSV file is empty");
            List<String> columns = Arrays.asList(headerLine.split(","));
            
            Relation rel = new Relation(name, columns);
            
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] values = line.split(",");
                // Pad with nulls if some columns are missing
                Object[] objValues = new Object[columns.size()];
                for (int i = 0; i < columns.size(); i++) {
                    objValues[i] = i < values.length ? values[i].trim() : null;
                }
                rel.addRow(objValues);
            }
            return rel;
        }
    }

    private List<String> getResultColumns(TreeNode root, Map<String, Relation> relations) {
        Set<String> cols = new LinkedHashSet<>();
        for (Relation r : relations.values()) {
            cols.addAll(r.getSchema());
        }
        return new ArrayList<>(cols);
    }

    private String formatResultSet(Set<Tuple> tuples, List<String> columns) {
        if (tuples.isEmpty()) return "Empty Result Set";

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

        StringBuilder sb = new StringBuilder();
        sb.append(separator).append("\n");

        StringBuilder header = new StringBuilder("| ");
        for (int i = 0; i < columns.size(); i++) {
            header.append(String.format("%-" + w[i] + "s", columns.get(i)));
            header.append(i < columns.size() - 1 ? " | " : " |");
        }
        sb.append(header).append("\n");
        sb.append(separator).append("\n");

        for (Tuple t : tuples) {
            StringBuilder row = new StringBuilder("| ");
            for (int i = 0; i < columns.size(); i++) {
                Object v = t.getValueByAttribute(columns.get(i));
                row.append(String.format("%-" + w[i] + "s", v == null ? "null" : v));
                row.append(i < columns.size() - 1 ? " | " : " |");
            }
            sb.append(row).append("\n");
        }
        sb.append(separator).append("\n");
        return sb.toString();
    }
}