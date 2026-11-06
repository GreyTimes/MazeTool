package view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.BenchmarkResult;
import model.TopologyType;
import service.BenchmarkRunner;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class BenchmarkModal {

    public static void display(Stage owner) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initOwner(owner);
        modal.setTitle("Maze Solver Benchmarking & Performance Dashboard (#28)");

        ComboBox<TopologyType> topoCombo = new ComboBox<>(FXCollections.observableArrayList(TopologyType.values()));
        topoCombo.setValue(TopologyType.RECTANGULAR);

        Spinner<Integer> iterSpinner = new Spinner<>(1, 20, 5);
        iterSpinner.setEditable(true);
        iterSpinner.setPrefWidth(80);

        Button runBtn = new Button("Run Batch Benchmarks");
        runBtn.setStyle("-fx-base: #00bcd4; -fx-font-weight: bold;");

        Button exportCsvBtn = new Button("Export Results to CSV");
        exportCsvBtn.setDisable(true);

        HBox toolbar = new HBox(10, new Label("Topology:"), topoCombo, new Label("Iterations (1-20):"), iterSpinner, runBtn, exportCsvBtn);
        toolbar.setPadding(new Insets(10));

        TableView<BenchmarkResult> table = new TableView<>();
        ObservableList<BenchmarkResult> dataList = FXCollections.observableArrayList();

        TableColumn<BenchmarkResult, String> colGen = new TableColumn<>("Generator");
        colGen.setCellValueFactory(new PropertyValueFactory<>("generatorName"));

        TableColumn<BenchmarkResult, String> colSolver = new TableColumn<>("Solver");
        colSolver.setCellValueFactory(new PropertyValueFactory<>("solverName"));

        TableColumn<BenchmarkResult, Integer> colSize = new TableColumn<>("Grid Size / Radius");
        colSize.setCellValueFactory(new PropertyValueFactory<>("width"));

        TableColumn<BenchmarkResult, Long> colGenTime = new TableColumn<>("Gen Time (ms)");
        colGenTime.setCellValueFactory(new PropertyValueFactory<>("generationTimeMs"));

        TableColumn<BenchmarkResult, Long> colSolveTime = new TableColumn<>("Solve Time (ms)");
        colSolveTime.setCellValueFactory(new PropertyValueFactory<>("solvingTimeMs"));

        TableColumn<BenchmarkResult, Integer> colNodes = new TableColumn<>("Nodes Visited");
        colNodes.setCellValueFactory(new PropertyValueFactory<>("nodesVisited"));

        TableColumn<BenchmarkResult, Double> colRatio = new TableColumn<>("Path Efficiency");
        colRatio.setCellValueFactory(new PropertyValueFactory<>("pathEfficiencyRatio"));

        table.getColumns().add(colGen);
        table.getColumns().add(colSolver);
        table.getColumns().add(colSize);
        table.getColumns().add(colGenTime);
        table.getColumns().add(colSolveTime);
        table.getColumns().add(colNodes);
        table.getColumns().add(colRatio);

        table.setItems(dataList);
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);

        runBtn.setOnAction(e -> {
            runBtn.setDisable(true);
            topoCombo.setDisable(true);
            iterSpinner.setDisable(true);

            List<String> gens = List.of("Recursive Backtracker (DFS)", "Randomized Prim's", "Randomized Kruskal's");
            List<String> solvers = List.of("Breadth-First Search (BFS)", "Depth-First Search (DFS)", "Dijkstra's Algorithm");

            Thread benchmarkThread = new Thread(() -> {
                try {
                    List<BenchmarkResult> results = BenchmarkRunner.runBatch(
                            gens, solvers, topoCombo.getValue(), iterSpinner.getValue(), System.currentTimeMillis()
                    );
                    javafx.application.Platform.runLater(() -> {
                        dataList.setAll(results);
                        runBtn.setDisable(false);
                        topoCombo.setDisable(false);
                        iterSpinner.setDisable(false);
                        if (results.isEmpty()) {
                            new Alert(Alert.AlertType.WARNING, "No benchmark results were generated.", ButtonType.OK).show();
                        } else {
                            exportCsvBtn.setDisable(false);
                        }
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Benchmark Error");
                        alert.setHeaderText("An error occurred during benchmark execution.");
                        alert.setContentText("Details: " + ex.getMessage());
                        alert.showAndWait();

                        runBtn.setDisable(false);
                        topoCombo.setDisable(false);
                        iterSpinner.setDisable(false);
                    });
                }
            });
            benchmarkThread.setUncaughtExceptionHandler((thread, throwable) ->
                    javafx.application.Platform.runLater(() -> {
                        new Alert(Alert.AlertType.ERROR, "Critical Thread Error: " + throwable.getMessage(), ButtonType.OK).show();
                        runBtn.setDisable(false);
                        topoCombo.setDisable(false);
                        iterSpinner.setDisable(false);
                    })
            );
            benchmarkThread.start();
        });

        exportCsvBtn.setOnAction(e -> {
            if (dataList.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "No data available to export.", ButtonType.OK).show();
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Benchmark CSV");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
            fileChooser.setInitialFileName("maze_benchmarks.csv");
            File file = fileChooser.showSaveDialog(modal);

            if (file != null) {
                try {
                    StringBuilder sb = new StringBuilder();
                    sb.append(BenchmarkResult.getCsvHeader()).append("\n");
                    for (BenchmarkResult r : dataList) {
                        sb.append(r.toCsvRow()).append("\n");
                    }
                    Files.writeString(file.toPath(), sb.toString(), StandardCharsets.UTF_8);
                    new Alert(Alert.AlertType.INFORMATION, "Benchmark CSV exported successfully!", ButtonType.OK).show();
                } catch (SecurityException se) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Permission Denied");
                    alert.setHeaderText("Failed to write CSV file.");
                    alert.setContentText("Insufficient permissions to save file at path: " + file.getPath());
                    alert.showAndWait();
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Export Error");
                    alert.setHeaderText("Failed to export benchmark CSV.");
                    alert.setContentText("Error: " + ex.getMessage());
                    alert.showAndWait();
                }
            }
        });

        VBox root = new VBox(toolbar, table);
        root.setPadding(new Insets(10));
        Scene scene = new Scene(root, 950, 520);
        modal.setScene(scene);
        modal.show();
    }
}