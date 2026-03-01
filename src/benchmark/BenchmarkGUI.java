package benchmark;

import algorithm.WorstCaseOptimalJoin;
import database.Relation;
import tree.TreeNode;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.*;

/**
 * JavaFX benchmark dashboard for Conjunction Query Evaluation.
 *
 * Compile (from project root):
 *   javac -cp "bin;lib/javafx.base.jar;lib/javafx.controls.jar;lib/javafx.graphics.jar" ^
 *         -d bin src/database/*.java src/tree/*.java src/algorithm/*.java src/benchmark/*.java src/Main.java
 *
 * Run (from project root):
 *   java --module-path lib --add-modules javafx.controls,javafx.graphics ^
 *        -cp bin benchmark.BenchmarkGUI
 *
 * Or press F5 and select "BenchmarkGUI (JavaFX)" in VS Code.
 */
public class BenchmarkGUI extends Application {

    // ── Observable rows fed into the TableView ───────────────────────────────
    private final ObservableList<ResultRow> rows = FXCollections.observableArrayList();

    // ── UI controls ──────────────────────────────────────────────────────────
    private final Map<QueryPattern, CheckBox> patternBoxes = new LinkedHashMap<>();
    private TextField   sizesField;
    private Button      runBtn;
    private Button      clearBtn;
    private Label       statusLabel;
    private ProgressBar progressBar;
    private BarChart<String, Number> barChart;

    // ── Catppuccin-Mocha dark theme (inline CSS) ─────────────────────────────
    private static final String DARK_CSS = String.join("",
        ".root{-fx-base:#1e1e2e;-fx-background:#1e1e2e;-fx-font-family:'Segoe UI',sans-serif;}",

        // Generic label
        ".label{-fx-text-fill:#cdd6f4;}",

        // Dim section headers
        ".section-lbl{-fx-text-fill:#6c7086;-fx-font-size:10;-fx-font-weight:bold;}",

        // Text field
        ".text-field{-fx-background-color:#313244;-fx-text-fill:#cdd6f4;",
            "-fx-prompt-text-fill:#6c7086;-fx-border-color:#45475a;",
            "-fx-border-radius:4;-fx-background-radius:4;-fx-padding:5 8;}",

        // Checkbox
        ".check-box{-fx-text-fill:#cdd6f4;}",
        ".check-box .box{-fx-background-color:#313244;-fx-border-color:#45475a;",
            "-fx-border-radius:3;-fx-background-radius:3;}",
        ".check-box:selected .box{-fx-background-color:#89b4fa;}",
        ".check-box:selected .mark{-fx-background-color:#1e1e2e;}",

        // Run button
        ".btn-run{-fx-background-color:#313244;-fx-text-fill:#89b4fa;",
            "-fx-border-color:#89b4fa;-fx-border-radius:6;-fx-background-radius:6;",
            "-fx-font-weight:bold;-fx-cursor:hand;}",
        ".btn-run:hover{-fx-background-color:#45475a;}",
        ".btn-run:pressed{-fx-background-color:#6c7086;}",
        ".btn-run:disabled{-fx-opacity:0.45;}",

        // Clear button
        ".btn-clear{-fx-background-color:#313244;-fx-text-fill:#f38ba8;",
            "-fx-border-color:#f38ba8;-fx-border-radius:6;-fx-background-radius:6;",
            "-fx-font-weight:bold;-fx-cursor:hand;}",
        ".btn-clear:hover{-fx-background-color:#45475a;}",
        ".btn-clear:pressed{-fx-background-color:#6c7086;}",

        // Separator
        ".separator .line{-fx-border-color:#45475a;}",

        // Tab pane
        ".tab-pane>.tab-header-area>.tab-header-background{-fx-background-color:#181825;}",
        ".tab{-fx-background-color:#313244;-fx-background-radius:0;}",
        ".tab:selected{-fx-background-color:#45475a;}",
        ".tab>.tab-container>.tab-label{-fx-text-fill:#cdd6f4;-fx-font-size:13;}",
        ".tab-content-area{-fx-background-color:#1e1e2e;}",

        // Table
        ".table-view{-fx-background-color:#1e1e2e;-fx-border-color:#45475a;}",
        ".table-view .column-header-background{-fx-background-color:#282838;}",
        ".table-view .column-header .label{-fx-text-fill:#89b4fa;-fx-font-weight:bold;}",
        ".table-row-cell{-fx-background-color:#1e1e2e;-fx-table-cell-border-color:transparent;}",
        ".table-row-cell:odd{-fx-background-color:#252535;}",
        ".table-row-cell:selected{-fx-background-color:#45475a;}",
        ".table-cell{-fx-text-fill:#cdd6f4;-fx-alignment:CENTER;}",
        ".table-view .placeholder .label{-fx-text-fill:#6c7086;}",

        // Chart
        ".chart{-fx-background-color:#1e1e2e;-fx-padding:10;}",
        ".chart-plot-background{-fx-background-color:#181825;}",
        ".chart-title{-fx-text-fill:#89b4fa;-fx-font-size:14;-fx-font-weight:bold;}",
        ".axis{-fx-tick-label-fill:#cdd6f4;}",
        ".axis-label{-fx-text-fill:#cdd6f4;}",
        ".chart-horizontal-grid-lines{-fx-stroke:#313244;}",
        ".chart-vertical-grid-lines{-fx-stroke:#313244;}",
        ".default-color0.chart-bar{-fx-bar-fill:#89b4fa;}",
        ".default-color1.chart-bar{-fx-bar-fill:#a6e3a1;}",
        ".default-color2.chart-bar{-fx-bar-fill:#fab387;}",
        ".default-color3.chart-bar{-fx-bar-fill:#f5c2e7;}",
        ".default-color4.chart-bar{-fx-bar-fill:#cba6f7;}",
        ".default-color5.chart-bar{-fx-bar-fill:#f9e2af;}",
        ".default-color6.chart-bar{-fx-bar-fill:#f38ba8;}",
        ".chart-legend{-fx-background-color:#313244;-fx-padding:6;}",
        ".chart-legend-item{-fx-text-fill:#cdd6f4;}",

        // Progress bar
        ".progress-bar>.track{-fx-background-color:#313244;-fx-background-radius:3;}",
        ".progress-bar>.bar{-fx-background-color:#89b4fa;-fx-background-radius:3;-fx-background-insets:0;}",

        // Scroll bars
        ".scroll-bar>.track{-fx-background-color:#313244;}",
        ".scroll-bar>.thumb{-fx-background-color:#45475a;-fx-background-radius:4;}",
        ".scroll-bar>.increment-button,.scroll-bar>.decrement-button{-fx-background-color:#313244;}",

        // Alert
        ".dialog-pane{-fx-background-color:#1e1e2e;}",
        ".dialog-pane .header-panel{-fx-background-color:#181825;}",
        ".dialog-pane>.content.label{-fx-text-fill:#cdd6f4;}"
    );

    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public void start(Stage stage) {
        stage.setTitle("Conjunction Query Evaluation  \u2013  Benchmark Dashboard");
        stage.setMinWidth(980);
        stage.setMinHeight(620);

        BorderPane root = new BorderPane();
        root.setTop(buildTitleBar());
        root.setLeft(buildSidebar());
        root.setCenter(buildCenter());
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1200, 740);
        scene.getStylesheets().add("data:text/css," + DARK_CSS);
        stage.setScene(scene);
        stage.show();
    }

    // ── Title bar ─────────────────────────────────────────────────────────────

    private Node buildTitleBar() {
        Label title = new Label("CONJUNCTION QUERY EVALUATION  \u2013  BENCHMARK DASHBOARD");
        title.setStyle("-fx-text-fill:#89b4fa;-fx-font-size:16;-fx-font-weight:bold;");

        HBox bar = new HBox(title);
        bar.setAlignment(Pos.CENTER);
        bar.setStyle("-fx-background-color:#181825;-fx-border-color:#45475a;" +
                     "-fx-border-width:0 0 1 0;-fx-padding:12 16;");
        return bar;
    }

    // ── Left sidebar (controls) ───────────────────────────────────────────────

    private Node buildSidebar() {
        VBox panel = new VBox(8);
        panel.setPrefWidth(225);
        panel.setStyle("-fx-background-color:#181825;-fx-border-color:#45475a;" +
                       "-fx-border-width:0 1 0 0;-fx-padding:16 12;");

        // Pattern checkboxes
        panel.getChildren().add(sectionLabel("QUERY PATTERNS"));
        for (QueryPattern qp : QueryPattern.values()) {
            if (qp == QueryPattern.CLIQUE || qp == QueryPattern.CROSS_PRODUCT) continue;
            CheckBox cb = new CheckBox(qp.getDescription());
            cb.setSelected(true);
            patternBoxes.put(qp, cb);
            panel.getChildren().add(cb);
        }

        panel.getChildren().add(new Separator());

        // Sizes input
        panel.getChildren().add(sectionLabel("DATABASE SIZES  (comma-sep)"));
        sizesField = new TextField("100, 500, 1000");
        panel.getChildren().add(sizesField);

        // Push buttons to bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        panel.getChildren().add(spacer);

        runBtn   = new Button("Run Benchmark");
        clearBtn = new Button("Clear Results");
        runBtn  .getStyleClass().add("btn-run");
        clearBtn.getStyleClass().add("btn-clear");
        runBtn  .setMaxWidth(Double.MAX_VALUE);
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        runBtn  .setPrefHeight(36);
        clearBtn.setPrefHeight(36);

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(10);

        panel.getChildren().addAll(runBtn, clearBtn, new Separator(), progressBar);

        runBtn  .setOnAction(e -> startBenchmark());
        clearBtn.setOnAction(e -> clearResults());

        return panel;
    }

    // ── Centre (tabs) ─────────────────────────────────────────────────────────

    private Node buildCenter() {
        Tab tableTab = new Tab("Results Table", buildTable());
        Tab chartTab = new Tab("Time Chart",    buildChart());
        tableTab.setClosable(false);
        chartTab.setClosable(false);
        return new TabPane(tableTab, chartTab);
    }

    // ── Table tab ─────────────────────────────────────────────────────────────

    private Node buildTable() {
        TableView<ResultRow> tv = new TableView<>(rows);
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tv.setPlaceholder(new Label("No results yet.  Configure options and press Run Benchmark."));

        tv.getColumns().addAll(List.of(
            tcol("Algorithm",     "algorithm",  110),
            tcol("Query Pattern", "pattern",    150),
            tcol("DB Size",       "dbSize",      80),
            tcol("Avg Time (ms)", "timeMs",     115),
            tcol("Results",       "resultSize",  80),
            tcol("AGM Bound",     "agmBound",    95),
            tcol("Memory (MB)",   "memoryMb",   100)
        ));
        return tv;
    }

    private TableColumn<ResultRow, Object> tcol(String title, String prop, double w) {
        TableColumn<ResultRow, Object> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        c.setStyle("-fx-alignment:CENTER;");
        return c;
    }

    // ── Chart tab ─────────────────────────────────────────────────────────────

    private Node buildChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setLabel("Database Size");
        yAxis.setLabel("Avg Time (ms)");

        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Execution Time by Query Pattern & Database Size");
        barChart.setAnimated(false);
        barChart.setCategoryGap(18);
        barChart.setBarGap(3);
        VBox.setVgrow(barChart, Priority.ALWAYS);
        return barChart;
    }

    // ── Status bar ────────────────────────────────────────────────────────────

    private Node buildStatusBar() {
        statusLabel = new Label("Ready.");
        statusLabel.setStyle("-fx-text-fill:#6c7086;-fx-font-size:12;");

        Label credit = new Label("Worst-Case Optimal Join  \u2022  Loomis-Whitney");
        credit.setStyle("-fx-text-fill:#6c7086;-fx-font-size:11;-fx-font-style:italic;");

        BorderPane bar = new BorderPane();
        bar.setLeft(statusLabel);
        bar.setRight(credit);
        bar.setStyle("-fx-background-color:#181825;-fx-border-color:#45475a;" +
                     "-fx-border-width:1 0 0 0;-fx-padding:5 12;");
        return bar;
    }

    // ── Benchmark logic (background Task) ────────────────────────────────────

    private void startBenchmark() {
        // Collect selected patterns
        List<QueryPattern> selected = new ArrayList<>();
        for (Map.Entry<QueryPattern, CheckBox> e : patternBoxes.entrySet()) {
            if (e.getValue().isSelected()) selected.add(e.getKey());
        }
        if (selected.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Select at least one query pattern.");
            return;
        }

        // Parse sizes
        int[] sizes;
        try {
            String[] parts = sizesField.getText().split(",");
            sizes = new int[parts.length];
            for (int i = 0; i < parts.length; i++) sizes[i] = Integer.parseInt(parts[i].trim());
        } catch (NumberFormatException ex) {
            alert(Alert.AlertType.ERROR, "Invalid sizes. Use comma-separated integers, e.g.  100, 500, 1000");
            return;
        }

        runBtn .setDisable(true);
        clearBtn.setDisable(true);
        progressBar.setProgress(-1); // indeterminate while initialising

        int total  = selected.size() * sizes.length;
        int[] done = {0};
        int[] finalSizes = sizes;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                for (QueryPattern pattern : selected) {
                    for (int size : finalSizes) {
                        updateMessage("Running " + pattern.getDescription() + "  (n = " + size + ")...");
                        try {
                            Object[] generated = DatabaseGenerator.generate(pattern, size);
                            @SuppressWarnings("unchecked")
                            Map<String, Relation> relations = (Map<String, Relation>) generated[0];
                            TreeNode tree = (TreeNode) generated[1];

                            WorstCaseOptimalJoin wcoj = new WorstCaseOptimalJoin(relations, tree);
                            WCOJBenchmarkAdapter adapter = new WCOJBenchmarkAdapter(wcoj);
                            BenchmarkResult result = BenchmarkRunner.runBenchmark(adapter, pattern, size);

                            Platform.runLater(() -> {
                                rows.add(new ResultRow(result));
                                refreshChart();
                            });
                        } catch (Exception ex) {
                            System.err.println("[BenchmarkGUI] Error – " + pattern + " n=" + size + ": " + ex.getMessage());
                        }
                        updateProgress(++done[0], total);
                    }
                }
                return null;
            }
        };

        // Bind UI to task
        statusLabel.textProperty().bind(task.messageProperty());
        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(e -> {
            statusLabel.textProperty().unbind();
            statusLabel.setText("Done  \u2013  " + rows.size() + " result(s) recorded.");
            progressBar.progressProperty().unbind();
            progressBar.setProgress(1.0);
            runBtn .setDisable(false);
            clearBtn.setDisable(false);
        });

        task.setOnFailed(e -> {
            statusLabel.textProperty().unbind();
            String msg = task.getException() != null ? task.getException().getMessage() : "unknown error";
            statusLabel.setText("Error: " + msg);
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
            runBtn .setDisable(false);
            clearBtn.setDisable(false);
        });

        Thread t = new Thread(task, "benchmark-worker");
        t.setDaemon(true);
        t.start();
    }

    private void clearResults() {
        rows.clear();
        barChart.getData().clear();
        progressBar.setProgress(0);
        statusLabel.setText("Results cleared.");
    }

    // ── Chart refresh (called on FX thread after each result) ─────────────────

    private void refreshChart() {
        // Group: patternName -> (sizeLabel -> timeMs)
        Map<String, Map<String, Double>> byPattern = new LinkedHashMap<>();
        for (ResultRow r : rows) {
            byPattern
                .computeIfAbsent(r.getPattern(), k -> new LinkedHashMap<>())
                .put("n=" + r.getDbSize(), r.rawTimeMs());
        }

        barChart.getData().clear();
        for (Map.Entry<String, Map<String, Double>> entry : byPattern.entrySet()) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(entry.getKey());
            for (Map.Entry<String, Double> pt : entry.getValue().entrySet()) {
                series.getData().add(new XYChart.Data<>(pt.getKey(), pt.getValue()));
            }
            barChart.getData().add(series);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section-lbl");
        return l;
    }

    private void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg);
        a.getDialogPane().getStylesheets()
            .add("data:text/css," + DARK_CSS);
        a.showAndWait();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  JavaFX Property row model for TableView
    // ═════════════════════════════════════════════════════════════════════════

    public static final class ResultRow {
        private final SimpleStringProperty  algorithm  = new SimpleStringProperty();
        private final SimpleStringProperty  pattern    = new SimpleStringProperty();
        private final SimpleIntegerProperty dbSize     = new SimpleIntegerProperty();
        private final SimpleStringProperty  timeMs     = new SimpleStringProperty();
        private final SimpleIntegerProperty resultSize = new SimpleIntegerProperty();
        private final SimpleStringProperty  agmBound   = new SimpleStringProperty();
        private final SimpleStringProperty  memoryMb   = new SimpleStringProperty();
        private final double rawTime;

        ResultRow(BenchmarkResult r) {
            algorithm .set(r.getAlgorithmName());
            pattern   .set(r.getQueryPattern());
            dbSize    .set(r.getDatabaseSize());
            rawTime    = r.getExecutionTimeNanos() / 1_000_000.0;
            timeMs    .set(String.format("%.3f", rawTime));
            resultSize.set(r.getResultSize());
            agmBound  .set(String.format("%.2f", r.getAgmBound()));
            memoryMb  .set(String.format("%.3f", r.getMemoryUsedMB()));
        }

        double rawTimeMs() { return rawTime; }

        // Property accessors (used by PropertyValueFactory)
        public StringProperty  algorithmProperty()  { return algorithm;  }
        public StringProperty  patternProperty()    { return pattern;    }
        public IntegerProperty dbSizeProperty()     { return dbSize;     }
        public StringProperty  timeMsProperty()     { return timeMs;     }
        public IntegerProperty resultSizeProperty() { return resultSize; }
        public StringProperty  agmBoundProperty()   { return agmBound;   }
        public StringProperty  memoryMbProperty()   { return memoryMb;   }

        // Plain getters required by PropertyValueFactory reflection
        public String getAlgorithm()  { return algorithm.get();  }
        public String getPattern()    { return pattern.get();    }
        public int    getDbSize()     { return dbSize.get();     }
        public String getTimeMs()     { return timeMs.get();     }
        public int    getResultSize() { return resultSize.get(); }
        public String getAgmBound()   { return agmBound.get();   }
        public String getMemoryMb()   { return memoryMb.get();   }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Entry point
    // ═════════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        launch(args);
    }
}
