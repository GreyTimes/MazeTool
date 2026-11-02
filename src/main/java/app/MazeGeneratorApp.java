package app;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import model.Cell;
import model.MazeGrid;
import model.TopologyType;
import view.MazeCanvas;
import generator.*;
import solver.BfsMazeSolver;
import solver.DfsMazeSolver;
import solver.DijkstraMazeSolver;
import game.Player;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class MazeGeneratorApp extends Application {

    private MazeGrid grid;
    private MazeCanvas mazeCanvas;
    private Timeline animTimeline;
    private Label statsLabel;
    private ComboBox<String> algorithmCombo;
    private ComboBox<String> solverCombo;
    private ComboBox<TopologyType> shapeCombo;
    private ToggleButton playModeToggle;
    private ToggleButton fogOfWarToggle;
    private ToggleButton showSolutionToggle;
    private ToggleButton themeToggle;

    private final Player player = new Player();
    private boolean isPlayMode = false;
    private boolean isFogOfWar = false;
    private int fogRadius = 2;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Customizable JavaFX Maze Generator & Solver");

        mazeCanvas = new MazeCanvas(600, 600);

        Label shapeHeader = new Label("Maze Layout Shape:");
        Label shapeInfo = createInfoIcon("Selects the geometric topology layout for the maze.");
        shapeCombo = new ComboBox<>();
        shapeCombo.getItems().addAll(TopologyType.values());
        shapeCombo.setValue(TopologyType.RECTANGULAR);
        shapeCombo.setMaxWidth(Double.MAX_VALUE);

        Label algoHeader = new Label("Gen Algorithm:");
        Label algoInfo = createInfoIcon("Generation Algorithms:\n" +
                "• DFS: Creates long, winding corridors with fewer dead ends.\n" +
                "• Prim's / Kruskal's: Produces highly branched, short corridors.\n" +
                "• Aldous-Broder / Wilson's: Generates uniform spanning trees using random walks.");
        algorithmCombo = new ComboBox<>();
        algorithmCombo.getItems().addAll(
                "Recursive Backtracker (DFS)",
                "Randomized Prim's",
                "Randomized Kruskal's",
                "Aldous-Broder",
                "Wilson's"
        );
        algorithmCombo.setValue("Recursive Backtracker (DFS)");
        algorithmCombo.setMaxWidth(Double.MAX_VALUE);

        Label solverHeader = new Label("Solver Algorithm:");
        Label solverInfo = createInfoIcon("Pathfinding Solvers:\n" +
                "• BFS: Finds the absolute shortest path guaranteed.\n" +
                "• DFS: Explores deep paths quickly, not necessarily the shortest.\n" +
                "• Dijkstra's: Computes weighted shortest paths across cell distances.");
        solverCombo = new ComboBox<>();
        solverCombo.getItems().addAll(
                "Breadth-First Search (BFS)",
                "Depth-First Search (DFS)",
                "Dijkstra's Algorithm"
        );
        solverCombo.setValue("Breadth-First Search (BFS)");
        solverCombo.setMaxWidth(Double.MAX_VALUE);

        solverCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (showSolutionToggle.isSelected() && grid != null) {
                solveAndDisplayPath();
            }
        });

        playModeToggle = new ToggleButton("Playable Mode: OFF");
        playModeToggle.setMaxWidth(Double.MAX_VALUE);
        Label playInfo = createInfoIcon("Enable keyboard navigation (WASD keys) to solve the maze manually.");
        HBox playBox = new HBox(5, playModeToggle, playInfo);
        HBox.setHgrow(playModeToggle, Priority.ALWAYS);
        playBox.setAlignment(Pos.CENTER_LEFT);

        fogOfWarToggle = new ToggleButton("Fog of War: OFF");
        fogOfWarToggle.setMaxWidth(Double.MAX_VALUE);
        Label fogInfo = createInfoIcon("Hides distant unvisited cells outside the player's visibility radius.");
        HBox fogBox = new HBox(5, fogOfWarToggle, fogInfo);
        HBox.setHgrow(fogOfWarToggle, Priority.ALWAYS);
        fogBox.setAlignment(Pos.CENTER_LEFT);

        themeToggle = new ToggleButton("Theme: Dark");
        themeToggle.setMaxWidth(Double.MAX_VALUE);
        themeToggle.setSelected(true);
        Label themeInfo = createInfoIcon("Switches between Dark and Light visual themes.");
        HBox themeBox = new HBox(5, themeToggle, themeInfo);
        HBox.setHgrow(themeToggle, Priority.ALWAYS);
        themeBox.setAlignment(Pos.CENTER_LEFT);

        // Independent Width & Height Controls for Rectangular Layouts
        Label widthLabel = new Label("Grid Width (Cols): 20");
        Label widthInfo = createInfoIcon("Defines the number of columns (width) of the maze grid.");
        HBox widthHeaderBox = new HBox(5, widthLabel, widthInfo);

        Slider widthSlider = new Slider(10, 50, 20);
        widthSlider.setShowTickLabels(true);
        widthSlider.setShowTickMarks(true);
        widthSlider.setMajorTickUnit(10);
        widthSlider.setBlockIncrement(1);
        widthSlider.setSnapToTicks(true);
        widthSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                widthLabel.setText("Grid Width (Cols): " + newVal.intValue())
        );

        Label heightLabel = new Label("Grid Height (Rows): 20");
        Label heightInfo = createInfoIcon("Defines the number of rows (height) of the maze grid.");
        HBox heightHeaderBox = new HBox(5, heightLabel, heightInfo);

        Slider heightSlider = new Slider(10, 50, 20);
        heightSlider.setShowTickLabels(true);
        heightSlider.setShowTickMarks(true);
        heightSlider.setMajorTickUnit(10);
        heightSlider.setBlockIncrement(1);
        heightSlider.setSnapToTicks(true);
        heightSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                heightLabel.setText("Grid Height (Rows): " + newVal.intValue())
        );

        Label speedLabel = new Label("Animation Delay (ms): 12");
        Label speedInfo = createInfoIcon("Controls the step-by-step slow-motion generation animation speed in milliseconds.");
        HBox speedHeaderBox = new HBox(5, speedLabel, speedInfo);

        Slider speedSlider = new Slider(1, 50, 12);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setMajorTickUnit(10);
        speedSlider.setBlockIncrement(1);
        speedSlider.setSnapToTicks(true);
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                speedLabel.setText("Animation Delay (ms): " + newVal.intValue())
        );

        Label complexityLabel = new Label("Complexity / Dead-End Density: 50%");
        Label complexityInfo = createInfoIcon("Tunes dead-end frequency. Higher complexity retains more dead ends by carving fewer loops.");
        HBox complexityHeaderBox = new HBox(5, complexityLabel, complexityInfo);

        Slider complexitySlider = new Slider(0, 100, 50);
        complexitySlider.setShowTickLabels(true);
        complexitySlider.setShowTickMarks(true);
        complexitySlider.setMajorTickUnit(25);
        complexitySlider.setBlockIncrement(5);
        complexitySlider.setSnapToTicks(true);
        complexitySlider.valueProperty().addListener((obs, oldVal, newVal) ->
                complexityLabel.setText("Complexity / Dead-End Density: " + newVal.intValue() + "%")
        );

        Label fogRadiusLabel = new Label("Fog Radius: 2 cells");
        Label fogRadiusInfo = createInfoIcon("Defines the player visibility radius in cells when Fog of War mode is active.");
        HBox fogRadiusHeaderBox = new HBox(5, fogRadiusLabel, fogRadiusInfo);

        Slider fogRadiusSlider = new Slider(1, 5, 2);
        fogRadiusSlider.setShowTickLabels(true);
        fogRadiusSlider.setShowTickMarks(true);
        fogRadiusSlider.setMajorTickUnit(1);
        fogRadiusSlider.setBlockIncrement(1);
        fogRadiusSlider.setSnapToTicks(true);
        fogRadiusSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            fogRadius = newVal.intValue();
            fogRadiusLabel.setText("Fog Radius: " + fogRadius + " cells");
            if (isFogOfWar && grid != null) {
                player.updateDiscovery(grid, isFogOfWar, fogRadius);
                mazeCanvas.setFogOfWarActive(true, fogRadius);
            }
        });

        Button generateBtn = new Button("Generate Labyrinth");
        generateBtn.setMaxWidth(Double.MAX_VALUE);
        Label genInfo = createInfoIcon("Generates a new maze using the selected algorithm and settings.");
        HBox genBox = new HBox(5, generateBtn, genInfo);
        HBox.setHgrow(generateBtn, Priority.ALWAYS);
        genBox.setAlignment(Pos.CENTER_LEFT);

        showSolutionToggle = new ToggleButton("Show Solution");
        showSolutionToggle.setMaxWidth(Double.MAX_VALUE);
        Label solutionInfo = createInfoIcon("Toggles the display of the solved path using the selected pathfinding solver.");
        HBox solutionBox = new HBox(5, showSolutionToggle, solutionInfo);
        HBox.setHgrow(showSolutionToggle, Priority.ALWAYS);
        solutionBox.setAlignment(Pos.CENTER_LEFT);

        showSolutionToggle.setOnAction(e -> {
            if (grid == null) {
                showSolutionToggle.setSelected(false);
                return;
            }

            if (showSolutionToggle.isSelected()) {
                showSolutionToggle.setText("Hide Solution");
                solveAndDisplayPath();
            } else {
                showSolutionToggle.setText("Show Solution");
                if (mazeCanvas != null) {
                    mazeCanvas.setSolvedPath(null);
                    updateStatistics(-1);
                }
            }
        });

        Button exportBtn = new Button("Export Image");
        exportBtn.setMaxWidth(Double.MAX_VALUE);
        Label exportInfo = createInfoIcon("Exports the current canvas view of the maze as a PNG image file.");
        HBox exportBox = new HBox(5, exportBtn, exportInfo);
        HBox.setHgrow(exportBtn, Priority.ALWAYS);
        exportBox.setAlignment(Pos.CENTER_LEFT);

        exportBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Maze Image");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image (*.png)", "*.png"));
            fileChooser.setInitialFileName("maze.png");

            File file = fileChooser.showSaveDialog(primaryStage);
            if (file != null) {
                WritableImage writableImage = new WritableImage((int) mazeCanvas.getWidth(), (int) mazeCanvas.getHeight());
                mazeCanvas.snapshot(null, writableImage);
                try {
                    ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png", file);
                } catch (IOException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to save image: " + ex.getMessage(), ButtonType.OK);
                    alert.showAndWait();
                }
            }
        });

        statsLabel = new Label("Statistics:\nTotal Cells: -\nDead Ends: -\nPath Length: -");
        statsLabel.setStyle("-fx-font-family: monospace; -fx-padding: 10;");
        Label statsInfo = createInfoIcon("Live metrics showing total cells, dead-end counts, and solver path length.");
        HBox statsHeaderBox = new HBox(5, new Label("Maze Metrics"), statsInfo);

        VBox controls = new VBox(6,
                new HBox(5, shapeHeader, shapeInfo), shapeCombo,
                new HBox(5, algoHeader, algoInfo), algorithmCombo,
                new HBox(5, solverHeader, solverInfo), solverCombo,
                playBox,
                fogBox,
                themeBox,
                widthHeaderBox, widthSlider,
                heightHeaderBox, heightSlider,
                speedHeaderBox, speedSlider,
                complexityHeaderBox, complexitySlider,
                fogRadiusHeaderBox, fogRadiusSlider,
                genBox,
                solutionBox,
                exportBox,
                new Separator(),
                statsHeaderBox,
                statsLabel
        );
        controls.setPadding(new Insets(10));
        controls.setPrefWidth(290);

        BorderPane root = new BorderPane();
        root.setCenter(mazeCanvas);
        root.setRight(controls);

        Scene scene = new Scene(root, 920, 840);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        themeToggle.setOnAction(e -> {
            if (themeToggle.isSelected()) {
                themeToggle.setText("Theme: Dark");
                if (!scene.getStylesheets().contains(getClass().getResource("/style.css").toExternalForm())) {
                    scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
                }
                mazeCanvas.setDarkMode(true);
            } else {
                themeToggle.setText("Theme: Light");
                scene.getStylesheets().remove(getClass().getResource("/style.css").toExternalForm());
                mazeCanvas.setDarkMode(false);
            }
        });

        generateBtn.setOnAction(e -> {
            int width = (int) widthSlider.getValue();
            int height = (int) heightSlider.getValue();
            double animDelay = speedSlider.getValue();
            double complexityFactor = complexitySlider.getValue() / 100.0;
            String selectedAlgo = algorithmCombo.getValue();

            startGenerationAnimation(width, height, animDelay, complexityFactor, selectedAlgo);
        });

        playModeToggle.setOnAction(e -> {
            isPlayMode = playModeToggle.isSelected();
            if (isPlayMode) {
                if (grid == null) {
                    playModeToggle.setSelected(false);
                    isPlayMode = false;
                    return;
                }
                playModeToggle.setText("Playable Mode: ON");
                player.spawn(grid);
                player.updateDiscovery(grid, isFogOfWar, fogRadius);
                mazeCanvas.setPlayerCell(player.getCurrentCell());
                mazeCanvas.setFogOfWarActive(isFogOfWar, fogRadius);
            } else {
                playModeToggle.setText("Playable Mode: OFF");
                player.reset();
                mazeCanvas.setPlayerCell(null);
                mazeCanvas.setFogOfWarActive(false, fogRadius);
            }
        });

        fogOfWarToggle.setOnAction(e -> {
            isFogOfWar = fogOfWarToggle.isSelected();
            if (isFogOfWar) {
                fogOfWarToggle.setText("Fog of War: ON");
                if (grid != null) {
                    for (Cell c : grid.getAllCells()) {
                        c.discovered = false;
                    }
                    if (isPlayMode) {
                        player.updateDiscovery(grid, true, fogRadius);
                    }
                }
                mazeCanvas.setFogOfWarActive(true, fogRadius);
            } else {
                fogOfWarToggle.setText("Fog of War: OFF");
                if (grid != null) {
                    for (Cell c : grid.getAllCells()) {
                        c.discovered = !isFogOfWar;
                    }
                }
                mazeCanvas.setFogOfWarActive(false, fogRadius);
            }
        });

        scene.setOnKeyPressed(event -> {
            if (!isPlayMode || grid == null || player.getCurrentCell() == null) return;

            Cell current = player.getCurrentCell();
            Cell targetCell = null;

            // Determine desired screen direction based on WASD
            double targetDx = 0, targetDy = 0;
            switch (event.getCode()) {
                case W -> targetDy = -1; // Up
                case S -> targetDy = 1;  // Down
                case A -> targetDx = -1; // Left
                case D -> targetDx = 1;  // Right
                default -> { return; }
            }

            // Find the closest open neighbor in that general screen direction
            double bestScore = Double.NEGATIVE_INFINITY;
            for (java.util.Map.Entry<Cell, Boolean> entry : current.getNeighbors().entrySet()) {
                Cell neighbor = entry.getKey();
                boolean isOpen = entry.getValue();

                if (isOpen) {
                    // Calculate vector from current to neighbor
                    double ndx = neighbor.getScreenX() - current.getScreenX();
                    double ndy = neighbor.getScreenY() - current.getScreenY();

                    // Dot product alignment score with desired direction
                    double alignment = (ndx * targetDx) + (ndy * targetDy);
                    if (alignment > 0 && alignment > bestScore) {
                        bestScore = alignment;
                        targetCell = neighbor;
                    }
                }
            }

            // If a valid open neighbor exists in that direction, move to it
            if (targetCell != null) {
                player.moveToCell(targetCell); // Move directly to the cell graph node
                player.updateDiscovery(grid, isFogOfWar, fogRadius);
                mazeCanvas.setPlayerCell(player.getCurrentCell());
                mazeCanvas.setFogOfWarActive(isFogOfWar, fogRadius);

                if (player.getCurrentCell().isExit) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Victory!");
                    alert.setHeaderText(null);
                    alert.setContentText("🎉 Congratulations! You successfully navigated through the maze in " + player.getSteps() + " steps!");
                    alert.showAndWait();

                    isPlayMode = false;
                    playModeToggle.setSelected(false);
                    playModeToggle.setText("Playable Mode: OFF");
                    player.reset();
                    mazeCanvas.setPlayerCell(null);
                    mazeCanvas.setFogOfWarActive(false, fogRadius);
                }
            }
        });

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Label createInfoIcon(String tooltipText) {
        Label infoLabel = new Label("ℹ");
        infoLabel.setStyle("-fx-text-fill: #00bcd4; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0 0 0 4;");
        Tooltip.install(infoLabel, new Tooltip(tooltipText));
        return infoLabel;
    }

    private void startGenerationAnimation(int width, int height, double animDelay, double complexityFactor, String algorithm) {
        if (animTimeline != null) animTimeline.stop();

        // 1. Get the selected topology from the UI dropdown
        TopologyType topology = shapeCombo != null ? shapeCombo.getValue() : TopologyType.RECTANGULAR;

        // 2. Dynamically swap the renderer based on the selected shape
        switch (topology) {
            case RECTANGULAR -> mazeCanvas.setRenderer(new view.RectangularRenderer());
            case HEXAGONAL -> mazeCanvas.setRenderer(new view.HexagonalRenderer());
            case CIRCULAR -> mazeCanvas.setRenderer(new view.CircularRenderer());
            case TRIANGULAR -> mazeCanvas.setRenderer(new view.TriangularRenderer());
        }

        // 3. Initialize the grid (currently rectangular, but ready for custom builders later)
        grid = new MazeGrid(width, height);

        // Dynamically adjust canvas dimensions (can be customized per shape later)
        double maxDimension = 600.0;
        double cellDim = maxDimension / Math.max(width, height);

        switch (topology) {
            case RECTANGULAR -> {
                // Standard orthogonal scaling
                mazeCanvas.setWidth(width * cellDim);
                mazeCanvas.setHeight(height * cellDim);

                for (Cell c : grid.getAllCells()) {
                    c.screenX = c.x * cellDim;
                    c.screenY = c.y * cellDim;
                }
            }
            case CIRCULAR -> {
                // Strict 1:1 square ratio for polar/concentric rings
                mazeCanvas.setWidth(maxDimension);
                mazeCanvas.setHeight(maxDimension);
            }
            case HEXAGONAL -> {
                // Custom sizing for hex grids (adjusting for row staggering/aspect ratios)
                mazeCanvas.setWidth(width * cellDim);
                mazeCanvas.setHeight(height * cellDim); // You can fine-tune multipliers here later
            }
            case TRIANGULAR -> {
                // Custom sizing for triangular grids (factoring in equilateral height ratios)
                mazeCanvas.setWidth(width * cellDim);
                mazeCanvas.setHeight(height * cellDim); // You can fine-tune multipliers here later
            }
        }

        for (Cell c : grid.getAllCells()) {
            c.discovered = !isFogOfWar;
        }

        mazeCanvas.setGrid(grid);
        mazeCanvas.setSolvedPath(null);
        updateStatistics(-1);
        player.reset();
        mazeCanvas.setPlayerCell(null);
        mazeCanvas.setFogOfWarActive(isFogOfWar, fogRadius);

        if (showSolutionToggle != null) {
            showSolutionToggle.setSelected(false);
            showSolutionToggle.setText("Show Solution");
        }

        MazeGenerator generator = switch (algorithm) {
            case "Randomized Prim's" -> new PrimsMazeGenerator();
            case "Randomized Kruskal's" -> new KruskalsMazeGenerator();
            case "Aldous-Broder" -> new AldousBroderMazeGenerator();
            case "Wilson's" -> new WilsonsMazeGenerator();
            default -> new DfsMazeGenerator();
        };

        generator.initialize(grid);

        animTimeline = new Timeline();
        KeyFrame keyFrame = new KeyFrame(Duration.millis(animDelay), event -> {
            boolean running = generator.step();
            mazeCanvas.setCurrentHead(generator.getCurrentHead());

            if (!running) {
                animTimeline.stop();
                grid.applyComplexityFactor(complexityFactor);

                // 4. Shape-agnostic openings creation
                switch (topology) {
                    case RECTANGULAR -> grid.createRectangularOpenings();
                    // Later when you implement them:
                    // case HEXAGONAL -> grid.createHexagonalOpenings();
                    // case CIRCULAR -> grid.createCircularOpenings();
                    // case TRIANGULAR -> grid.createTriangularOpenings();
                }

                mazeCanvas.setCurrentHead(null);

                if (showSolutionToggle.isSelected()) {
                    solveAndDisplayPath();
                } else {
                    updateStatistics(-1);
                }

                if (isPlayMode) {
                    player.spawn(grid);
                    player.updateDiscovery(grid, isFogOfWar, fogRadius);
                    mazeCanvas.setPlayerCell(player.getCurrentCell());
                    mazeCanvas.setFogOfWarActive(isFogOfWar, fogRadius);
                }
            }
        });

        animTimeline.getKeyFrames().add(keyFrame);
        animTimeline.setCycleCount(Timeline.INDEFINITE);
        animTimeline.play();
    }

    private void solveAndDisplayPath() {
        if (grid == null) return;

        String selectedSolver = solverCombo.getValue();
        List<Cell> path = switch (selectedSolver) {
            case "Depth-First Search (DFS)" -> DfsMazeSolver.solve(grid);
            case "Dijkstra's Algorithm" -> DijkstraMazeSolver.solve(grid);
            default -> BfsMazeSolver.solve(grid);
        };

        if (path != null && !path.isEmpty()) {
            mazeCanvas.setSolvedPath(path);
            updateStatistics(path.size());
        } else {
            mazeCanvas.setSolvedPath(null);
            updateStatistics(-1);
        }
    }

    private void updateStatistics(int pathLength) {
        if (grid == null) return;
        int totalCells = grid.getAllCells().size();
        int deadEnds = grid.countDeadEnds();

        String pathStr = (pathLength > 0) ? pathLength + " steps" : "Hidden / Not solved";
        statsLabel.setText(String.format("Statistics:\nTotal Cells: %d\nDead Ends: %d\nPath Length: %s",
                totalCells, deadEnds, pathStr));
    }

    public static void main(String[] args) {
        launch(args);
    }
}