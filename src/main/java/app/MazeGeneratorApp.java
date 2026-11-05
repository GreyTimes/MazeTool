package app;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import model.Cell;
import model.EntranceExitMode;
import model.MazeGrid;
import model.PolarCell;
import model.TopologyType;

import view.MazeCanvas;
import view.RectangularRenderer;
import view.CircularRenderer;
import view.HexagonalRenderer;
import view.TriangularRenderer;

import generator.*;
import solver.*;
import game.Player;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


/**
 * JavaFX application for rectangular, circular, hexagonal,
 * and triangular maze generation and solving.
 * Supports configurable entrance and exit positions:
 * - Left to Right
 * - Top to Bottom
 * - Center to Outer Edge (circular only)
 * - Custom Click Selection
 */
public class MazeGeneratorApp extends Application {
    // CONSTANTS

    private static final double CANVAS_SIZE = 600.0;
    private static final double GEOMETRY_PADDING = 22.0;
    private static final double PLAYER_MOVE_MILLIS = 160.0;

    // APPLICATION STATE

    private MazeGrid grid;
    private MazeCanvas mazeCanvas;
    private Timeline animTimeline;
    private Timeline movementTimeline;
    private Label statsLabel;
    private ComboBox<String> algorithmCombo;
    private ComboBox<String> solverCombo;
    private ComboBox<TopologyType> shapeCombo;
    private ComboBox<EntranceExitMode> entranceExitModeCombo;
    private ToggleButton playModeToggle;
    private ToggleButton fogOfWarToggle;
    private ToggleButton showSolutionToggle;
    private ToggleButton themeToggle;
    private Label openingStatusLabel;
    private final Player player = new Player();
    private boolean isPlayMode = false;
    private boolean isFogOfWar = false;
    private boolean isPlayerMoving = false;
    private boolean customSelectionActive = false;
    private Cell customStartCell;
    private int fogRadius = 2;
    private javafx.scene.control.TextField seedTextField;
    private long currentSeed = System.currentTimeMillis();

    // APPLICATION START

    public void start(Stage primaryStage) {
        primaryStage.setTitle("Customizable JavaFX Maze Generator & Solver");

        mazeCanvas = new MazeCanvas(
                CANVAS_SIZE,
                CANVAS_SIZE
        );

        // TOPOLOGY SELECTION

        Label shapeHeader = new Label("Maze Layout Shape:");
        Label shapeInfo = createInfoIcon("Choose rectangular, circular/polar, hexagonal, or triangular geometry.");
        shapeCombo = new ComboBox<>();
        shapeCombo.getItems().addAll(TopologyType.values());
        shapeCombo.setValue(TopologyType.RECTANGULAR);
        shapeCombo.setMaxWidth(Double.MAX_VALUE);

        // ENTRANCE / EXIT MODE SELECTION

        Label entranceExitHeader = new Label("Entrance / Exit Mode:");
        Label entranceExitInfo = createInfoIcon(
                "Left to Right: start on the left and finish on the right. "
                        + "Top to Bottom: start at the top and finish at the bottom. "
                        + "Center to Outer Edge: available for circular mazes. "
                        + "Custom: click two cells after generation to select START and END."
        );

        entranceExitModeCombo = new ComboBox<>();
        entranceExitModeCombo.setMaxWidth(Double.MAX_VALUE);
        openingStatusLabel = new Label("The selected mode will be applied when you generate a maze.");
        openingStatusLabel.setWrapText(true);
        openingStatusLabel.setMaxWidth(Double.MAX_VALUE);
        configureEntranceExitModeOptions(shapeCombo.getValue());

        // GENERATION ALGORITHM SELECTION

        Label algoHeader = new Label("Gen Algorithm:");
        Label algoInfo = createInfoIcon(
                "DFS creates long winding corridors. Prim's and Kruskal's "
                        + "produce branched mazes. Aldous-Broder and Wilson's "
                        + "generate uniform spanning trees."
        );

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

        // SOLVER SELECTION

        Label solverHeader = new Label("Solver Algorithm:");
        Label solverInfo = createInfoIcon(
                "BFS finds the fewest cell transitions. DFS explores "
                        + "a deep route. Dijkstra minimizes geometric distance."
        );

        solverCombo = new ComboBox<>();
        solverCombo.getItems().addAll(
                "Breadth-First Search (BFS)",
                "Depth-First Search (DFS)",
                "Dijkstra's Algorithm"
        );

        solverCombo.setValue("Breadth-First Search (BFS)");
        solverCombo.setMaxWidth(Double.MAX_VALUE);
        solverCombo.valueProperty().addListener(
                (obs, oldValue, newValue) -> {
                    if (showSolutionToggle != null && showSolutionToggle.isSelected() && grid != null) {
                        solveAndDisplayPath();
                    }
                }
        );

        // PLAYABLE MODE

        playModeToggle = new ToggleButton("Playable Mode: OFF");
        playModeToggle.setMaxWidth(Double.MAX_VALUE);
        Label playInfo = createInfoIcon(
                "Rectangular: WASD. Circular: click highlighted passages "
                        + "or use W/S/A/D for outward/inward/counterclockwise/clockwise. "
                        + "Hexagonal and triangular: click a highlighted open neighbor; "
                        + "WASD uses approximate screen directions."
        );
        HBox playBox = new HBox(5, playModeToggle, playInfo);
        HBox.setHgrow(playModeToggle, Priority.ALWAYS);
        playBox.setAlignment(Pos.CENTER_LEFT);

        // FOG OF WAR

        fogOfWarToggle = new ToggleButton("Fog of War: OFF");
        fogOfWarToggle.setMaxWidth(Double.MAX_VALUE);
        Label fogInfo = createInfoIcon(
                "Hides undiscovered cells outside the player's "
                        + "graph-distance visibility radius."
        );
        HBox fogBox = new HBox(5, fogOfWarToggle, fogInfo);
        HBox.setHgrow(fogOfWarToggle, Priority.ALWAYS);
        fogBox.setAlignment(Pos.CENTER_LEFT);

        // THEME

        themeToggle = new ToggleButton("Theme: Dark");
        themeToggle.setMaxWidth(Double.MAX_VALUE);
        themeToggle.setSelected(true);
        Label themeInfo = createInfoIcon("Switches between dark and light visual themes.");
        HBox themeBox = new HBox(5, themeToggle, themeInfo);
        HBox.setHgrow(themeToggle, Priority.ALWAYS);
        themeBox.setAlignment(Pos.CENTER_LEFT);

        // WIDTH / TOPOLOGY PARAMETER SLIDER

        Label widthLabel = new Label("Grid Width (Cols): 20");
        Label widthInfo = createInfoIcon("Rectangular maze column count.");
        HBox widthHeaderBox = new HBox(5, widthLabel, widthInfo);
        Slider widthSlider = new Slider(10, 50, 20);
        configureSlider(widthSlider, 10);

        widthSlider.valueProperty().addListener(
                (obs, oldValue, newValue) -> {
                    widthLabel.setText(widthLabelFor(shapeCombo.getValue()) + newValue.intValue());
                }
        );

        // HEIGHT / TOPOLOGY PARAMETER SLIDER

        Label heightLabel = new Label("Grid Height (Rows): 20");
        Label heightInfo = createInfoIcon("Rectangular maze row count.");
        HBox heightHeaderBox = new HBox(5, heightLabel, heightInfo);
        Slider heightSlider = new Slider(10, 50, 20);
        configureSlider(heightSlider, 10);
        heightSlider.valueProperty().addListener(
                (obs, oldValue, newValue) -> {
                    heightLabel.setText(heightLabelFor(shapeCombo.getValue()) + newValue.intValue());
                }
        );

        // TOPOLOGY-DEPENDENT SLIDERS

        shapeCombo.valueProperty().addListener(
                (obs, oldTopology, newTopology) -> {
                    configureDimensionSliders(
                            newTopology,
                            widthSlider,
                            heightSlider,
                            widthLabel,
                            heightLabel,
                            widthInfo,
                            heightInfo,
                            heightHeaderBox
                    );
                    configureEntranceExitModeOptions(newTopology);
                }
        );

        configureDimensionSliders(
                shapeCombo.getValue(),
                widthSlider,
                heightSlider,
                widthLabel,
                heightLabel,
                widthInfo,
                heightInfo,
                heightHeaderBox
        );
        configureEntranceExitModeOptions(shapeCombo.getValue());

        // GENERATION ANIMATION SPEED

        Label speedLabel = new Label("Animation Delay (ms): 10");
        Label speedInfo = createInfoIcon("Controls the step-by-step maze generation animation speed.");
        HBox speedHeaderBox = new HBox(5, speedLabel, speedInfo);

        Slider speedSlider = new Slider(0, 50, 10);
        speedSlider.setMajorTickUnit(10);
        speedSlider.setMinorTickCount(4);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(true);

        speedSlider.valueProperty().addListener(
                (obs, oldValue, newValue) -> {
                    int val = newValue.intValue();
                    if (val == 0) {
                        // Prevent 0ms freeze by snapping back to 1 or 2 silently
                        speedSlider.setValue(1);
                        val = 1;
                    }
                    speedLabel.setText("Animation Delay (ms): " + val);
                }
        );

        // COMPLEXITY

        Label complexityLabel = new Label("Complexity / Dead-End Density: 50%");
        Label complexityInfo = createInfoIcon("Controls how aggressively extra passages are carved to reduce dead ends.");
        HBox complexityHeaderBox = new HBox(5, complexityLabel, complexityInfo);
        Slider complexitySlider = new Slider(0, 100, 50);
        configureSlider(complexitySlider, 25);
        complexitySlider.valueProperty().addListener(
                (obs, oldValue, newValue) ->
                        complexityLabel.setText("Complexity / Dead-End Density: " + newValue.intValue() + "%")
        );

        // FOG RADIUS

        Label fogRadiusLabel = new Label("Fog Radius: 2 cells");
        Label fogRadiusInfo = createInfoIcon("Controls the player's visible graph-distance radius.");
        HBox fogRadiusHeaderBox = new HBox(5, fogRadiusLabel, fogRadiusInfo);
        Slider fogRadiusSlider = new Slider(1, 5, 2);
        configureSlider(fogRadiusSlider, 1);
        fogRadiusSlider.valueProperty().addListener(
                (obs, oldValue, newValue) -> {
                    fogRadius = newValue.intValue();
                    fogRadiusLabel.setText("Fog Radius: " + fogRadius + " cells");
                    if (isFogOfWar && grid != null) {
                        player.updateDiscovery(grid, true, fogRadius);
                        mazeCanvas.setFogOfWarActive(true, fogRadius);
                    }
                }
        );

        // GENERATE BUTTON

        Button generateBtn = new Button("Generate Labyrinth");
        generateBtn.setMaxWidth(Double.MAX_VALUE);
        Label genInfo = createInfoIcon("Generates a new maze using the selected topology and settings.");
        HBox genBox = new HBox(5, generateBtn, genInfo);
        HBox.setHgrow(generateBtn, Priority.ALWAYS);
        genBox.setAlignment(Pos.CENTER_LEFT);

        // SHOW SOLUTION

        showSolutionToggle = new ToggleButton("Show Solution");
        showSolutionToggle.setMaxWidth(Double.MAX_VALUE);
        Label solutionInfo = createInfoIcon("Displays the route from entrance to exit using the selected solver.");
        HBox solutionBox = new HBox(5, showSolutionToggle, solutionInfo);
        HBox.setHgrow(showSolutionToggle, Priority.ALWAYS);
        solutionBox.setAlignment(Pos.CENTER_LEFT);
        showSolutionToggle.setOnAction(event -> {
            if (grid == null) {
                showSolutionToggle.setSelected(false);
                return;
            }
            if (showSolutionToggle.isSelected()) {
                showSolutionToggle.setText("Hide Solution");
                if (customSelectionActive || grid.getEntranceCell() == null || grid.getExitCell() == null) {
                    mazeCanvas.setSolvedPath(null);
                    updateStatistics(-1);
                } else {
                    solveAndDisplayPath();
                }
            } else {
                showSolutionToggle.setText("Show Solution");
                mazeCanvas.setSolvedPath(null);
                updateStatistics(-1);
            }
        });

        // EXPORT IMAGE

        Button exportBtn = new Button("Export Image");
        exportBtn.setMaxWidth(Double.MAX_VALUE);
        Label exportInfo = createInfoIcon("Exports the current maze canvas as a PNG image.");
        HBox exportBox = new HBox(5, exportBtn, exportInfo);
        HBox.setHgrow(exportBtn, Priority.ALWAYS);
        exportBox.setAlignment(Pos.CENTER_LEFT);
        exportBtn.setOnAction(event -> exportMazeImage(primaryStage));

        // SAVE & LOAD LABYRINTH ACTIONS

        Button saveMazeBtn = new Button("Save Labyrinth");
        saveMazeBtn.setMaxWidth(Double.MAX_VALUE);
        Label saveInfo = createInfoIcon("Serializes and saves the current maze to a .maze file.");
        HBox saveBox = new HBox(5, saveMazeBtn, saveInfo);
        HBox.setHgrow(saveMazeBtn, Priority.ALWAYS);
        saveBox.setAlignment(Pos.CENTER_LEFT);
        saveMazeBtn.setOnAction(event -> saveLabyrinthFile(primaryStage));

        Button loadMazeBtn = new Button("Load Labyrinth");
        loadMazeBtn.setMaxWidth(Double.MAX_VALUE);
        Label loadInfo = createInfoIcon("Loads and deserializes a maze from a .maze file.");
        HBox loadBox = new HBox(5, loadMazeBtn, loadInfo);
        HBox.setHgrow(loadMazeBtn, Priority.ALWAYS);
        loadBox.setAlignment(Pos.CENTER_LEFT);
        loadMazeBtn.setOnAction(event -> loadLabyrinthFile(primaryStage));

        // STATISTICS

        statsLabel = new Label(
                "Statistics:\n"
                        + "Total Cells: -\n"
                        + "Dead Ends: -\n"
                        + "Path Length: -"
        );

        statsLabel.setStyle("-fx-font-family: monospace; -fx-padding: 10;");
        Label statsInfo = createInfoIcon("Live metrics for cells, dead ends, and solution path length.");
        HBox statsHeaderBox = new HBox(5, new Label("Maze Metrics"), statsInfo);

        // SEED & SHARE CODE CONTROLS (Issue #30)
        Label seedHeader = new Label("Generation Seed / Share Code:");
        Label seedInfo = createInfoIcon("Enter a custom seed or share code to reproduce exact mazes.");
        seedTextField = new javafx.scene.control.TextField(String.valueOf(currentSeed));
        seedTextField.setMaxWidth(Double.MAX_VALUE);

        Button randomizeSeedBtn = new Button("Randomize Seed");
        randomizeSeedBtn.setMaxWidth(Double.MAX_VALUE);
        randomizeSeedBtn.setOnAction(e -> {
            currentSeed = new java.util.Random().nextLong();
            seedTextField.setText(String.valueOf(currentSeed));
        });

        Button dailyChallengeBtn = new Button("Maze of the Day (Daily Challenge)");
        dailyChallengeBtn.setMaxWidth(Double.MAX_VALUE);
        dailyChallengeBtn.setOnAction(e -> {
            String todayStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_DATE);
            currentSeed = todayStr.hashCode();
            seedTextField.setText(todayStr);
            openingStatusLabel.setText("Loaded Daily Challenge for: " + todayStr);
        });

        Button copyShareCodeBtn = new Button("Copy Share Code");
        copyShareCodeBtn.setMaxWidth(Double.MAX_VALUE);
        copyShareCodeBtn.setOnAction(e -> {
            try {
                String shareCode = shapeCombo.getValue() + "|" + seedTextField.getText() + "|" + algorithmCombo.getValue();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(shareCode);
                javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
                new Alert(Alert.AlertType.INFORMATION, "Share code copied to clipboard!", ButtonType.OK).show();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Failed to copy to clipboard: " + ex.getMessage(), ButtonType.OK).show();
            }
        });

        // --- TABPANE INTERFACE STRUCTURE ---
        javafx.scene.control.TabPane controlTabs = new javafx.scene.control.TabPane();
        controlTabs.setTabClosingPolicy(javafx.scene.control.TabPane.TabClosingPolicy.UNAVAILABLE);

        // TAB 1: COMBINED GENERATION & SOLVING ("Maze Editor") WRAPPED IN A SCROLLPANE
        VBox genSolveContent = new VBox(8,
                new HBox(5, shapeHeader, shapeInfo), shapeCombo,
                new HBox(5, entranceExitHeader, entranceExitInfo), entranceExitModeCombo, openingStatusLabel,
                new HBox(5, algoHeader, algoInfo), algorithmCombo,
                widthHeaderBox, widthSlider,
                heightHeaderBox, heightSlider,
                complexityHeaderBox, complexitySlider,
                new Separator(),
                new HBox(5, solverHeader, solverInfo), solverCombo,
                speedHeaderBox, speedSlider,
                solutionBox,
                new Separator(),
                new HBox(5, seedHeader, seedInfo), seedTextField,
                randomizeSeedBtn, dailyChallengeBtn, copyShareCodeBtn
        );
        genSolveContent.setPadding(new Insets(10));

        ScrollPane genSolveScroll = new ScrollPane(genSolveContent);
        genSolveScroll.setFitToWidth(true);
        genSolveScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        javafx.scene.control.Tab genSolveTab = new javafx.scene.control.Tab("Maze Editor", genSolveScroll);

        // TAB 2: GAMEPLAY ("Play")
        VBox gameContent = new VBox(8,
                playBox,
                fogBox,
                fogRadiusHeaderBox, fogRadiusSlider
        );
        gameContent.setPadding(new Insets(10));
        javafx.scene.control.Tab gameTab = new javafx.scene.control.Tab("Play", gameContent);

        // TAB 3: FILE & THEME ("File")
        VBox fileContent = new VBox(8,
                themeBox,
                exportBox,
                saveBox,
                loadBox,
                new Separator(),
                statsHeaderBox,
                statsLabel
        );
        fileContent.setPadding(new Insets(10));
        javafx.scene.control.Tab fileTab = new javafx.scene.control.Tab("File", fileContent);

        controlTabs.getTabs().addAll(genSolveTab, gameTab, fileTab);

        // PINNED GLOBAL TOOLBAR (Always accessible outside tabs)
        VBox pinnedGlobalBar = new VBox(8,
                genBox,
                playBox
        );
        pinnedGlobalBar.setPadding(new Insets(10));

        // CONTROLS LAYOUT CONTAINER
        VBox controls = new VBox(0, pinnedGlobalBar, new Separator(), controlTabs);
        controls.setPrefWidth(320);

        // WRAP CANVAS IN A STACKPANE FOR CLEAN CENTERING & MARGINS
        StackPane canvasContainer = new StackPane(mazeCanvas);
        canvasContainer.setAlignment(Pos.CENTER);
        canvasContainer.setStyle("-fx-padding: 15;");

        // MAIN LAYOUT
        BorderPane root = new BorderPane();
        root.setCenter(canvasContainer);
        root.setRight(controls);
        Scene scene = new Scene(root, 940, 840);

        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        // THEME ACTION

        themeToggle.setOnAction(event -> {
            String stylesheet = getClass().getResource("/style.css").toExternalForm();
            if (themeToggle.isSelected()) {
                themeToggle.setText("Theme: Dark");
                if (!scene.getStylesheets().contains(stylesheet)) {
                    scene.getStylesheets().add(stylesheet);
                }
                mazeCanvas.setDarkMode(true);
            } else {
                themeToggle.setText("Theme: Light");
                scene.getStylesheets().remove(stylesheet);
                mazeCanvas.setDarkMode(false);
            }
        });

        // GENERATE ACTION

        generateBtn.setOnAction(event ->
                startGenerationAnimation(
                        (int) widthSlider.getValue(),
                        (int) heightSlider.getValue(),
                        speedSlider.getValue(),
                        complexitySlider.getValue() / 100.0,
                        algorithmCombo.getValue()
                )
        );

        // PLAYABLE MODE ACTION

        playModeToggle.setOnAction(event -> {
            isPlayMode = playModeToggle.isSelected();
            if (isPlayMode) {
                if (grid == null || grid.getEntranceCell() == null || grid.getExitCell() == null || customSelectionActive) {
                    playModeToggle.setSelected(false);
                    isPlayMode = false;
                    return;
                }
                playModeToggle.setText("Playable Mode: ON");
                spawnPlayerForCurrentMaze();
            } else {
                playModeToggle.setText("Playable Mode: OFF");
                stopMovementAnimation();
                player.reset();
                mazeCanvas.setPlayerCell(null);
                mazeCanvas.setFogOfWarActive(false, fogRadius);
                mazeCanvas.clearPlayerScreenPosition();
                mazeCanvas.setHighlightedCells(List.of());
                mazeCanvas.setHoveredCell(null);
            }
        });

        // FOG ACTION

        fogOfWarToggle.setOnAction(event -> {
            isFogOfWar = fogOfWarToggle.isSelected();
            if (isFogOfWar) {
                fogOfWarToggle.setText("Fog of War: ON");
                if (grid != null) {
                    for (Cell cell : grid.getAllCells()) {
                        cell.discovered = false;
                    }
                    if (isPlayMode && player.getCurrentCell() != null) {
                        player.updateDiscovery(grid, true, fogRadius);
                    }
                }
                mazeCanvas.setFogOfWarActive(true, fogRadius);
            } else {
                fogOfWarToggle.setText("Fog of War: OFF");
                if (grid != null) {
                    for (Cell cell : grid.getAllCells()) {
                        cell.discovered = true;
                    }
                }
                mazeCanvas.setFogOfWarActive(false, fogRadius);
            }
        });

        // KEYBOARD MOVEMENT

        scene.setOnKeyPressed(event -> {
            if (!isPlayMode || customSelectionActive || isPlayerMoving || grid == null || player.getCurrentCell() == null) {
                return;
            }
            Cell current = player.getCurrentCell();
            Cell target;
            switch (grid.getTopology()) {
                case RECTANGULAR ->
                        target = findRectangularKeyboardNeighbor(current, event.getCode());
                case CIRCULAR ->
                        target = findPolarKeyboardNeighbor(current, event.getCode());
                case HEXAGONAL, TRIANGULAR ->
                        target = findPolygonKeyboardNeighbor(current, event.getCode());
                default -> target = null;
            }
            if (target != null) {
                movePlayerTo(target);
                event.consume();
            }
        });

        // GEOMETRIC MOUSE HOVER

        mazeCanvas.setOnMouseMoved(event -> {
            if (customSelectionActive || !isPlayMode || isPlayerMoving || grid == null || !grid.isGeometric() || player.getCurrentCell() == null) {
                mazeCanvas.setHoveredCell(null);
                return;
            }
            Cell hovered = mazeCanvas.findCellAt(event.getX(), event.getY());
            boolean legal = hovered != null && player.getCurrentCell().isPassageOpenTo(hovered);
            mazeCanvas.setHoveredCell(legal ? hovered : null);
        });
        mazeCanvas.setOnMouseExited(event -> mazeCanvas.setHoveredCell(null));

        // MOUSE CLICK HANDLING

        mazeCanvas.setOnMouseClicked(event -> {
            if (customSelectionActive) {
                if (!isPlayerMoving) {
                    handleCustomSelectionClick(event.getX(), event.getY());
                }
                event.consume();
                return;
            }
            if (!isPlayMode || isPlayerMoving || grid == null || !grid.isGeometric() || player.getCurrentCell() == null) {
                return;
            }
            Cell clicked = mazeCanvas.findCellAt(
                    event.getX(),
                    event.getY()
            );
            if (clicked != null && player.getCurrentCell().isPassageOpenTo(clicked)) {
                movePlayerTo(clicked);
                event.consume();
            }
        });

        // SHOW WINDOW

        primaryStage.setScene(scene);
        primaryStage.show();

        // --- ISSUE #34: RESPONSIVE CANVAS AUTO-SCALING & DEBOUNCED RESIZE LISTENER ---[cite: 3, 4]
        javafx.animation.PauseTransition resizeDebouncer = new javafx.animation.PauseTransition(javafx.util.Duration.millis(100));
        resizeDebouncer.setOnFinished(event -> {
            double availableWidth = Math.max(300, scene.getWidth() - 340);
            double availableHeight = Math.max(300, scene.getHeight() - 40);

            if (mazeCanvas != null) {
                double renderWidth = availableWidth;
                double renderHeight = availableHeight;

                // If it's a rectangular grid, preserve its structural aspect ratio so cells don't stretch!
                if (grid != null && grid.getTopology() == TopologyType.RECTANGULAR && widthSlider != null && heightSlider != null) {
                    double cols = widthSlider.getValue();
                    double rows = heightSlider.getValue();
                    double gridAspect = cols / rows;

                    if (availableWidth / availableHeight > gridAspect) {
                        renderWidth = availableHeight * gridAspect;
                    } else {
                        renderHeight = availableWidth / gridAspect;
                    }
                } else {
                    // Circular/Polygonal shapes remain square-bound
                    double size = Math.min(availableWidth, availableHeight);
                    renderWidth = size;
                    renderHeight = size;
                }

                mazeCanvas.resizeCanvas(renderWidth, renderHeight);
                if (grid != null) {
                    if (grid.isCircular()) {
                        grid.layoutCircularGrid(renderWidth, renderHeight, 0.0);
                    } else if (grid.isPolygonal()) {
                        grid.layoutPolygonGrid(renderWidth, renderHeight, 0.0);
                    }
                    mazeCanvas.draw();
                }
            }
        });

        scene.widthProperty().addListener((obs, oldVal, newVal) -> resizeDebouncer.playFromStart());
        scene.heightProperty().addListener((obs, oldVal, newVal) -> resizeDebouncer.playFromStart());
    }

    // ENTRANCE / EXIT MODE OPTIONS

    private void configureEntranceExitModeOptions(TopologyType topology) {
        EntranceExitMode previous = entranceExitModeCombo.getValue();
        entranceExitModeCombo.getItems().clear();
        entranceExitModeCombo.getItems().addAll(EntranceExitMode.LEFT_TO_RIGHT, EntranceExitMode.TOP_TO_BOTTOM);
        if (topology == TopologyType.CIRCULAR) {
            entranceExitModeCombo.getItems().add(EntranceExitMode.CENTER_TO_OUTER);
        }
        entranceExitModeCombo.getItems().add(EntranceExitMode.CUSTOM);
        if (previous != null && entranceExitModeCombo.getItems().contains(previous)) {
            entranceExitModeCombo.setValue(previous);
        } else {
            entranceExitModeCombo.setValue(EntranceExitMode.LEFT_TO_RIGHT);
        }
    }


    // CUSTOM ENTRANCE / EXIT SELECTION

    private void handleCustomSelectionClick(double mouseX, double mouseY) {
        if (!customSelectionActive || grid == null) {
            return;
        }
        Cell clickedCell = mazeCanvas.findCellAt(mouseX, mouseY);
        if (clickedCell == null) {
            return;
        }

        // FIRST CLICK: SELECT START

        if (customStartCell == null) {
            customStartCell = clickedCell;
            grid.setCustomEntrancePreview(customStartCell);
            mazeCanvas.setSolvedPath(null);
            mazeCanvas.draw();
            updateStatistics(-1);
            openingStatusLabel.setText("START selected. Click a different cell to set END.");
            return;
        }

        // SECOND CLICK: SELECT END

        if (clickedCell == customStartCell) {
            openingStatusLabel.setText("Choose a different cell for END.");
            return;
        }
        Cell selectedStart = customStartCell;
        Cell selectedEnd = clickedCell;
        grid.createCustomOpenings(selectedStart, selectedEnd);
        customStartCell = null;
        customSelectionActive = false;
        openingStatusLabel.setText("Custom START and END configured successfully.");
        mazeCanvas.draw();
        if (showSolutionToggle.isSelected()) {
            solveAndDisplayPath();
        } else {
            updateStatistics(-1);
        }
        if (isPlayMode) {
            spawnPlayerForCurrentMaze();
        }
    }

    // PLAYER SPAWN

    private void spawnPlayerForCurrentMaze() {
        if (grid == null || grid.getEntranceCell() == null || grid.getExitCell() == null) {
            return;
        }
        player.spawn(grid);
        player.updateDiscovery(grid, isFogOfWar, fogRadius);
        mazeCanvas.setPlayerCell(player.getCurrentCell());
        mazeCanvas.setFogOfWarActive(isFogOfWar, fogRadius);
        refreshInteractionHints();
    }

    // SLIDER HELPERS

    private void configureSlider(Slider slider, double majorTickUnit) {
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(majorTickUnit);
        slider.setBlockIncrement(1);
        slider.setSnapToTicks(true);
    }

    private void configureDimensionSliders(
            TopologyType topology,
            Slider widthSlider,
            Slider heightSlider,
            Label widthLabel,
            Label heightLabel,
            Label widthInfo,
            Label heightInfo,
            HBox heightHeaderBox
    ) {
        double widthMin;
        double widthMax;
        double widthDefault;
        double widthMajor;
        double heightMin;
        double heightMax;
        double heightDefault;
        double heightMajor;
        boolean singleDimension = false;

        switch (topology) {
            case RECTANGULAR -> {

                widthMin = 10;
                widthMax = 50;
                widthDefault = 20;
                widthMajor = 10;

                heightMin = 10;
                heightMax = 50;
                heightDefault = 20;
                heightMajor = 10;
            }

            case CIRCULAR -> {

                widthMin = 8;
                widthMax = 24;
                widthDefault = 16;
                widthMajor = 4;

                heightMin = 4;
                heightMax = 12;
                heightDefault = 8;
                heightMajor = 2;
            }

            case HEXAGONAL -> {

                widthMin = 2;
                widthMax = 7;
                widthDefault = 4;
                widthMajor = 1;

                heightMin = 1;
                heightMax = 1;
                heightDefault = 1;
                heightMajor = 1;

                singleDimension = true;
            }

            case TRIANGULAR -> {

                widthMin = 4;
                widthMax = 16;
                widthDefault = 10;
                widthMajor = 2;

                heightMin = 1;
                heightMax = 1;
                heightDefault = 1;
                heightMajor = 1;

                singleDimension = true;
            }
            default -> throw new IllegalStateException("Unknown topology: " + topology);
        }

        widthSlider.setMin(widthMin);
        widthSlider.setMax(widthMax);
        widthSlider.setMajorTickUnit(widthMajor);
        widthSlider.setBlockIncrement(1);
        widthSlider.setSnapToTicks(true);
        widthSlider.setValue(widthDefault);

        heightSlider.setMin(heightMin);
        heightSlider.setMax(heightMax);
        heightSlider.setMajorTickUnit(heightMajor);
        heightSlider.setBlockIncrement(1);
        heightSlider.setSnapToTicks(true);
        heightSlider.setValue(heightDefault);

        heightHeaderBox.setVisible(!singleDimension);
        heightHeaderBox.setManaged(!singleDimension);

        heightSlider.setVisible(!singleDimension);
        heightSlider.setManaged(!singleDimension);

        widthLabel.setText(widthLabelFor(topology) + (int) widthDefault);
        heightLabel.setText(heightLabelFor(topology) + (int) heightDefault);

        updateInfoTooltip(widthInfo, widthTooltipFor(topology));
        updateInfoTooltip(heightInfo, heightTooltipFor(topology));
    }

    private String widthLabelFor(TopologyType topology) {
        return switch (topology) {
            case RECTANGULAR -> "Grid Width (Cols): ";
            case CIRCULAR -> "Outer Ring Sectors: ";
            case HEXAGONAL -> "Hex Board Radius: ";
            case TRIANGULAR -> "Triangle Side Length: ";
        };
    }


    private String heightLabelFor(TopologyType topology) {
        return switch (topology) {
            case RECTANGULAR -> "Grid Height (Rows): ";
            case CIRCULAR -> "Number of Rings: ";
            case HEXAGONAL -> "Hexagon Height: ";
            case TRIANGULAR -> "Triangle Height: ";
        };
    }

    private String widthTooltipFor(TopologyType topology) {
        return switch (topology) {
            case RECTANGULAR -> "Number of rectangular columns (10–50).";
            case CIRCULAR -> "Number of sectors along the outermost polar ring (8–24).";
            case HEXAGONAL -> "Controls the radius of the regular hexagonal board (2–7). "
                            + "The outer boundary is a regular hexagon.";
            case TRIANGULAR -> "Controls the number of small equilateral triangle edges "
                            + "along each side of the large triangular board (4–16). "
                            + "Total cells = N squared.";
        };
    }

    private String heightTooltipFor(TopologyType topology) {
        return switch (topology) {
            case RECTANGULAR -> "Number of rectangular rows (10–50).";
            case CIRCULAR -> "Number of concentric rings outside the center (4–12).";
            case HEXAGONAL -> "Hexagonal board dimensions are controlled by its radius.";
            case TRIANGULAR -> "Triangular board dimensions are controlled by its side length.";
        };
    }


    private void updateInfoTooltip(Label infoLabel, String text) {
        Tooltip tooltip = (Tooltip) infoLabel.getProperties().get("mazeInfoTooltip");
        if (tooltip == null) {
            tooltip = new Tooltip(text);
            infoLabel.getProperties().put("mazeInfoTooltip", tooltip);
            Tooltip.install(infoLabel, tooltip);
        } else {
            tooltip.setText(text);
        }
    }

    private Label createInfoIcon(String tooltipText) {
        Label infoLabel = new Label("ℹ");
        infoLabel.setStyle("-fx-text-fill: #00bcd4; "
                        + "-fx-font-weight: bold; "
                        + "-fx-cursor: hand; "
                        + "-fx-padding: 0 0 0 4;"
        );
        Tooltip tooltip = new Tooltip(tooltipText);
        infoLabel.getProperties().put("mazeInfoTooltip", tooltip);
        Tooltip.install(infoLabel, tooltip);
        return infoLabel;
    }

    // ENHANCED IMAGE EXPORT (PNG / JPG WITH BACKGROUND FALLBACK)

    private void exportMazeImage(Stage owner) {
        if (mazeCanvas == null || grid == null) {
            new Alert(Alert.AlertType.WARNING, "No maze canvas available to export.", ButtonType.OK).show();
            return;
        }

        try {
            javafx.scene.control.ChoiceDialog<String> formatDialog = new javafx.scene.control.ChoiceDialog<>("PNG", "PNG", "JPG");
            formatDialog.setTitle("Export Image Format");
            formatDialog.setHeaderText("Choose Image Export Format");
            formatDialog.setContentText("Format:");

            java.util.Optional<String> result = formatDialog.showAndWait();
            if (result.isEmpty()) {
                return;
            }
            String format = result.get();

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Maze Image (" + format + ")");
            if ("JPG".equalsIgnoreCase(format)) {
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JPG Image (*.jpg)", "*.jpg"));
                fileChooser.setInitialFileName("maze.jpg");
            } else {
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image (*.png)", "*.png"));
                fileChooser.setInitialFileName("maze.png");
            }

            File file = fileChooser.showSaveDialog(owner);
            if (file == null) {
                return;
            }

            int width = Math.max(1, (int) mazeCanvas.getWidth());
            int height = Math.max(1, (int) mazeCanvas.getHeight());
            WritableImage image = new WritableImage(width, height);
            mazeCanvas.snapshot(null, image);
            java.awt.image.BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);

            if (bImage == null) {
                throw new IOException("Failed to create image snapshot from canvas.");
            }

            if ("JPG".equalsIgnoreCase(format)) {
                java.awt.image.BufferedImage solidImage = new java.awt.image.BufferedImage(
                        width, height, java.awt.image.BufferedImage.TYPE_INT_RGB
                );
                java.awt.Graphics2D g2d = solidImage.createGraphics();
                g2d.setColor(themeToggle.isSelected() ? java.awt.Color.BLACK : java.awt.Color.WHITE);
                g2d.fillRect(0, 0, width, height);
                g2d.drawImage(bImage, 0, 0, null);
                g2d.dispose();
                boolean written = ImageIO.write(solidImage, "jpg", file);
                if (!written) {
                    throw new IOException("No appropriate JPG writer found.");
                }
            } else {
                boolean written = ImageIO.write(bImage, "png", file);
                if (!written) {
                    throw new IOException("No appropriate PNG writer found.");
                }
            }

            new Alert(Alert.AlertType.INFORMATION, "Image successfully exported!", ButtonType.OK).show();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Export Error");
            alert.setHeaderText("Failed to export maze image.");
            alert.setContentText("Check disk space or write permissions: " + ex.getMessage());
            alert.showAndWait();
        }
    }

    // SAVE LABYRINTH (.maze Serialization using Gson)

    private void saveLabyrinthFile(Stage owner) {
        if (grid == null) {
            new Alert(Alert.AlertType.WARNING, "No maze generated to save.", ButtonType.OK).show();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Labyrinth File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Labyrinth Files (*.maze)", "*.maze"));
        fileChooser.setInitialFileName("labyrinth.maze");
        File file = fileChooser.showSaveDialog(owner);
        if (file == null) {
            return;
        }

        try {
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("cols", grid.cols);
            data.put("rows", grid.rows);
            data.put("topology", grid.getTopology().name());
            data.put("polarRingCount", grid.getPolarRingCount());

            java.util.List<java.util.Map<String, Object>> cellList = new java.util.ArrayList<>();
            for (Cell cell : grid.getAllCells()) {
                java.util.Map<String, Object> cData = new java.util.HashMap<>();
                cData.put("x", cell.x);
                cData.put("y", cell.y);
                cData.put("screenX", cell.getScreenX());
                cData.put("screenY", cell.getScreenY());
                cData.put("discovered", cell.discovered);
                cData.put("isEntrance", cell.isEntrance);
                cData.put("isExit", cell.isExit);

                java.util.Map<String, Boolean> neighbors = new java.util.HashMap<>();
                for (java.util.Map.Entry<Cell, Boolean> entry : cell.getNeighbors().entrySet()) {
                    neighbors.put(entry.getKey().x + "," + entry.getKey().y, entry.getValue());
                }
                cData.put("neighbors", neighbors);
                cellList.add(cData);
            }
            data.put("cells", cellList);

            java.nio.file.Files.writeString(file.toPath(), gson.toJson(data), java.nio.charset.StandardCharsets.UTF_8);
            new Alert(Alert.AlertType.INFORMATION, "Labyrinth successfully saved!", ButtonType.OK).show();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to save labyrinth file: " + ex.getMessage(), ButtonType.OK);
            alert.show();
        }
    }

    private void loadLabyrinthFile(Stage owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Labyrinth File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Labyrinth Files (*.maze)", "*.maze"));
        File file = fileChooser.showOpenDialog(owner);
        if (file == null) {
            return;
        }

        try {
            String json = java.nio.file.Files.readString(file.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            com.google.gson.Gson gson = new com.google.gson.Gson();
            java.util.Map<?, ?> data = gson.fromJson(json, java.util.Map.class);

            if (data == null || !data.containsKey("cols") || !data.containsKey("rows") || !data.containsKey("topology")) {
                throw new IllegalArgumentException("Invalid or corrupt .maze file structure.");
            }

            int cols = ((Number) data.get("cols")).intValue();
            int rows = ((Number) data.get("rows")).intValue();
            TopologyType topology = TopologyType.valueOf((String) data.get("topology"));

            shapeCombo.setValue(topology);
            switch (topology) {
                case RECTANGULAR -> mazeCanvas.setRenderer(new RectangularRenderer());
                case CIRCULAR -> mazeCanvas.setRenderer(new CircularRenderer());
                case HEXAGONAL -> mazeCanvas.setRenderer(new HexagonalRenderer());
                case TRIANGULAR -> mazeCanvas.setRenderer(new TriangularRenderer());
            }

            grid = switch (topology) {
                case HEXAGONAL, TRIANGULAR -> new MazeGrid(cols, cols, topology);
                case RECTANGULAR, CIRCULAR -> new MazeGrid(cols, rows, topology);
            };

            switch (topology) {
                case RECTANGULAR -> {
                    double cellSize = CANVAS_SIZE / Math.max(cols, rows);
                    mazeCanvas.setWidth(cols * cellSize);
                    mazeCanvas.setHeight(rows * cellSize);
                }
                case CIRCULAR -> grid.layoutCircularGrid(CANVAS_SIZE, CANVAS_SIZE, GEOMETRY_PADDING);
                case HEXAGONAL -> grid.layoutHexagonalGrid(CANVAS_SIZE, CANVAS_SIZE, GEOMETRY_PADDING);
                case TRIANGULAR -> grid.layoutTriangularGrid(CANVAS_SIZE, CANVAS_SIZE, GEOMETRY_PADDING);
            }

            java.util.List<?> cellList = (java.util.List<?>) data.get("cells");
            if (cellList != null) {
                java.util.Map<String, Cell> cellMap = new java.util.HashMap<>();
                for (Cell cell : grid.getAllCells()) {
                    cellMap.put(cell.x + "," + cell.y, cell);
                }

                for (Object rawCData : cellList) {
                    java.util.Map<?, ?> cData = (java.util.Map<?, ?>) rawCData;
                    int cx = ((Number) cData.get("x")).intValue();
                    int cy = ((Number) cData.get("y")).intValue();

                    Cell cell = cellMap.get(cx + "," + cy);
                    if (cell != null) {
                        cell.discovered = (Boolean) cData.get("discovered");
                        cell.isEntrance = (Boolean) cData.get("isEntrance");
                        cell.isExit = (Boolean) cData.get("isExit");

                        java.util.Map<?, ?> savedNeighbors = (java.util.Map<?, ?>) cData.get("neighbors");
                        if (savedNeighbors != null) {
                            for (java.util.Map.Entry<?, ?> entry : savedNeighbors.entrySet()) {
                                String targetKey = (String) entry.getKey();
                                boolean isOpen = (Boolean) entry.getValue();
                                Cell neighbor = cellMap.get(targetKey);
                                if (neighbor != null && isOpen) {
                                    cell.getNeighbors().put(neighbor, true);
                                }
                            }
                        }
                    }
                }
            }

            mazeCanvas.setGrid(grid);
            mazeCanvas.setSolvedPath(null);
            mazeCanvas.setCurrentHead(null);
            mazeCanvas.setPlayerCell(null);
            mazeCanvas.clearPlayerScreenPosition();
            mazeCanvas.setFogOfWarActive(isFogOfWar, fogRadius);
            mazeCanvas.draw();
            updateStatistics(-1);

            new Alert(Alert.AlertType.INFORMATION, "Labyrinth loaded successfully!", ButtonType.OK).show();
        } catch (Exception ex) {
            // Fallback State: Fall back to generating a fresh default 20x20 grid
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Load Error");
            alert.setHeaderText("Failed to load labyrinth file.");
            alert.setContentText("The file is corrupt or incompatible: " + ex.getMessage() + "\n\nFalling back to a default 20x20 maze.");
            alert.showAndWait();

            // Trigger fallback default generation
            shapeCombo.setValue(TopologyType.RECTANGULAR);
            startGenerationAnimation(20, 20, 12, 0.5, "Recursive Backtracker (DFS)");
        }
    }

    // RENDERER SELECTION AND MAZE GENERATION

    private void startGenerationAnimation(int width, int height, double animDelay, double complexityFactor, String algorithm) {
        if (animTimeline != null) {
            animTimeline.stop();
        }
        stopMovementAnimation();
        customSelectionActive = false;
        customStartCell = null;
        openingStatusLabel.setText("Generating maze...");
        TopologyType topology = shapeCombo.getValue();
        EntranceExitMode openingMode = entranceExitModeCombo.getValue();

        // PARSE SEED
        long seedToUse;
        try {
            String text = seedTextField.getText().trim();
            if (text.isEmpty()) {
                seedToUse = System.currentTimeMillis();
            } else {
                try {
                    seedToUse = Long.parseLong(text);
                } catch (NumberFormatException e) {
                    seedToUse = text.hashCode();
                }
            }
        } catch (Exception e) {
            seedToUse = System.currentTimeMillis();
        }
        java.util.Random seededRandom = new java.util.Random(seedToUse);

        // SELECT RENDERER

        switch (topology) {
            case RECTANGULAR -> mazeCanvas.setRenderer(new RectangularRenderer());
            case CIRCULAR -> mazeCanvas.setRenderer(new CircularRenderer());
            case HEXAGONAL -> mazeCanvas.setRenderer(new HexagonalRenderer());
            case TRIANGULAR -> mazeCanvas.setRenderer(new TriangularRenderer());
        }

        // CREATE CORRECT GRID

        grid = switch (topology) {
            case HEXAGONAL, TRIANGULAR -> new MazeGrid(width, width, topology);
            case RECTANGULAR, CIRCULAR -> new MazeGrid(width, height, topology);
        };

        // CALCULATE RESPONSIVE WORKSPACE DIMENSIONS BASED ON CURRENT WINDOW SIZE
        Scene currentScene = (mazeCanvas != null) ? mazeCanvas.getScene() : null;
        double availableWidth = (currentScene != null) ? Math.max(300, currentScene.getWidth() - 340) : CANVAS_SIZE;
        double availableHeight = (currentScene != null) ? Math.max(300, currentScene.getHeight() - 40) : CANVAS_SIZE;
        double renderWidth = availableWidth;
        double renderHeight = availableHeight;

        if (topology == TopologyType.RECTANGULAR) {
            double gridAspect = (double) width / height;
            if (availableWidth / availableHeight > gridAspect) {
                renderWidth = availableHeight * gridAspect;
            } else {
                renderHeight = availableWidth / gridAspect;
            }
        } else {
            double size = Math.min(availableWidth, availableHeight);
            renderWidth = size;
            renderHeight = size;
        }

        // CANVAS DIMENSIONS AND GEOMETRY

        switch (topology) {
            case RECTANGULAR -> {
                double cellWidth = renderWidth / width;
                double cellHeight = renderHeight / height;
                mazeCanvas.resizeCanvas(renderWidth, renderHeight);
                for (Cell cell : grid.getAllCells()) {
                    cell.screenX = (cell.x + 0.5) * cellWidth;
                    cell.screenY = (cell.y + 0.5) * cellHeight;
                }
            }
            case CIRCULAR -> {
                mazeCanvas.resizeCanvas(renderWidth, renderHeight);
                grid.layoutCircularGrid(renderWidth, renderHeight, GEOMETRY_PADDING);
            }
            case HEXAGONAL -> {
                mazeCanvas.resizeCanvas(renderWidth, renderHeight);
                grid.layoutHexagonalGrid(renderWidth, renderHeight, GEOMETRY_PADDING);
            }
            case TRIANGULAR -> {
                mazeCanvas.resizeCanvas(renderWidth, renderHeight);
                grid.layoutTriangularGrid(renderWidth, renderHeight, GEOMETRY_PADDING);
            }
        }

        // INITIALIZE CELL STATES

        for (Cell cell : grid.getAllCells()) {
            cell.visited = false;
            cell.discovered = !isFogOfWar;
        }

        // RESET PREVIOUS VISUAL STATE

        mazeCanvas.setGrid(grid);
        mazeCanvas.setSolvedPath(null);
        mazeCanvas.setCurrentHead(null);
        mazeCanvas.setPlayerCell(null);
        mazeCanvas.setHighlightedCells(List.of());
        mazeCanvas.setHoveredCell(null);
        mazeCanvas.clearPlayerScreenPosition();
        mazeCanvas.setFogOfWarActive(isFogOfWar, fogRadius);
        player.reset();
        updateStatistics(-1);
        if (showSolutionToggle != null) {
            showSolutionToggle.setSelected(false);
            showSolutionToggle.setText("Show Solution");
        }

        // CHOOSE GENERATOR

        MazeGenerator generator = switch (algorithm) {
            case "Randomized Prim's" -> new PrimsMazeGenerator();
            case "Randomized Kruskal's" -> new KruskalsMazeGenerator();
            case "Aldous-Broder" -> new AldousBroderMazeGenerator();
            case "Wilson's" -> new WilsonsMazeGenerator();
            default -> new DfsMazeGenerator();
        };
        generator.initialize(grid, seededRandom);

        // GENERATION TIMELINE

        animTimeline = new Timeline();
        KeyFrame keyFrame = new KeyFrame(Duration.millis(animDelay),
                event -> {
                    boolean running = generator.step();
                    mazeCanvas.setCurrentHead(generator.getCurrentHead());

                    // GENERATION FINISHED

                    if (!running) {
                        animTimeline.stop();
                        entranceExitModeCombo.setDisable(false);
                        grid.applyComplexityFactor(complexityFactor, seededRandom);

                        // CREATE ENTRANCE AND EXIT

                        if (openingMode == EntranceExitMode.CUSTOM) {
                            grid.createOpenings(EntranceExitMode.CUSTOM, seededRandom);
                            customSelectionActive = true;
                            customStartCell = null;
                            openingStatusLabel.setText("Custom selection: click a cell to set START. "
                                    + "Then click another cell to set END."
                            );
                        } else {
                            customSelectionActive = false;
                            customStartCell = null;
                            grid.createOpenings(openingMode, seededRandom);
                            openingStatusLabel.setText("Entrance and exit configured: " + openingMode);
                        }
                        mazeCanvas.setCurrentHead(null);
                        mazeCanvas.draw();

                        // SOLUTION

                        if (showSolutionToggle.isSelected() && !customSelectionActive) {
                            solveAndDisplayPath();
                        } else {
                            mazeCanvas.setSolvedPath(null);
                            updateStatistics(-1);
                        }

                        // SPAWN PLAYER

                        if (isPlayMode && grid.getEntranceCell() != null && grid.getExitCell() != null) {
                            spawnPlayerForCurrentMaze();
                        }
                    }
                }
        );
        animTimeline.getKeyFrames().add(keyFrame);
        animTimeline.setCycleCount(Timeline.INDEFINITE);
        entranceExitModeCombo.setDisable(true);
        animTimeline.play();
    }

    // INTERACTION HINTS

    private void refreshInteractionHints() {
        if (grid != null && grid.isGeometric() && isPlayMode && player.getCurrentCell() != null) {
            mazeCanvas.setHighlightedCells(player.getCurrentCell().getOpenNeighbors());
        } else {
            mazeCanvas.setHighlightedCells(List.of());
        }
        mazeCanvas.setHoveredCell(null);
    }

    // STOP MOVEMENT ANIMATION

    private void stopMovementAnimation() {
        if (movementTimeline != null) {
            movementTimeline.stop();
            movementTimeline = null;
        }
        isPlayerMoving = false;
        if (mazeCanvas != null) {
            mazeCanvas.clearPlayerScreenPosition();
        }
    }

    // RECTANGULAR KEYBOARD MOVEMENT

    private Cell findRectangularKeyboardNeighbor(Cell current, KeyCode key) {
        double dx = 0.0;
        double dy = 0.0;
        switch (key) {
            case W -> dy = -1.0;
            case S -> dy = 1.0;
            case A -> dx = -1.0;
            case D -> dx = 1.0;
            default -> {return null;}
        }
        Cell best = null;
        double bestAlignment = 0.0;
        for (Cell neighbor : current.getOpenNeighbors()) {
            double ndx = neighbor.getScreenX() - current.getScreenX();
            double ndy = neighbor.getScreenY() - current.getScreenY();
            double alignment = ndx * dx + ndy * dy;
            if (alignment > bestAlignment) {
                bestAlignment = alignment;
                best = neighbor;
            }
        }
        return best;
    }

    // POLAR KEYBOARD MOVEMENT
    /**
     * Circular controls:
     * W = outward.
     * S = inward.
     * A = counterclockwise.
     * D = clockwise.
     */
    private Cell findPolarKeyboardNeighbor(Cell current, KeyCode key) {
        if (!(current instanceof PolarCell polar)) {
            return null;
        }
        List<Cell> openNeighbors = current.getOpenNeighbors();
        if (openNeighbors.isEmpty()) {
            return null;
        }
        if (polar.isCore()) {
            if (key != KeyCode.W) {
                return null;
            }
            Cell best = null;
            double bestAngle = Double.POSITIVE_INFINITY;
            for (Cell neighbor : openNeighbors) {
                if (neighbor instanceof PolarCell candidate && candidate.getRingIndex() == 1) {
                    double delta = angularDistance(candidate.getCenterAngle(), -Math.PI / 2.0);
                    if (delta < bestAngle) {
                        bestAngle = delta;
                        best = candidate;
                    }
                }
            }
            return best;
        }
        int targetRing = switch (key) {
            case W -> polar.getRingIndex() + 1;
            case S -> polar.getRingIndex() - 1;
            default -> polar.getRingIndex();
        };

        int targetSector = switch (key) {
            case A -> Math.floorMod(polar.getSectorIndex() - 1, polar.getSectorsInRing());
            case D -> (polar.getSectorIndex() + 1) % polar.getSectorsInRing();
            default -> -1;
        };
        Cell best = null;
        double bestAngle = Double.POSITIVE_INFINITY;
        for (Cell neighbor : openNeighbors) {
            if (!(neighbor instanceof PolarCell candidate)) {
                continue;
            }
            boolean matches;
            if (key == KeyCode.A || key == KeyCode.D) {
                matches = candidate.getRingIndex() == polar.getRingIndex() && candidate.getSectorIndex() == targetSector;
            } else if (key == KeyCode.W || key == KeyCode.S) {
                matches = candidate.getRingIndex() == targetRing;
            } else {
                return null;
            }
            if (matches) {
                double delta = angularDistance(candidate.getCenterAngle(), polar.getCenterAngle());
                if (delta < bestAngle) {
                    bestAngle = delta;
                    best = candidate;
                }
            }
        }
        return best;
    }


    // HEXAGONAL / TRIANGULAR KEYBOARD MOVEMENT
    /**
     * Screen-space directional movement.
     * WASD are the main directions.
     * Q/E/Z/C also provide diagonal movement.
     * Mouse movement remains the most precise way to navigate
     * polygonal cells.
     */
    private Cell findPolygonKeyboardNeighbor(Cell current, KeyCode key) {
        double dx;
        double dy;
        switch (key) {
            case W -> {
                dx = 0.0;
                dy = -1.0;
            }
            case S -> {
                dx = 0.0;
                dy = 1.0;
            }
            case A -> {
                dx = -1.0;
                dy = 0.0;
            }
            case D -> {
                dx = 1.0;
                dy = 0.0;
            }
            case Q -> {
                dx = -1.0;
                dy = -1.0;
            }
            case E -> {
                dx = 1.0;
                dy = -1.0;
            }
            case Z -> {
                dx = -1.0;
                dy = 1.0;
            }
            case C -> {
                dx = 1.0;
                dy = 1.0;
            }
            default -> {
                return null;
            }
        }
        Cell best = null;
        double bestAlignment = 0.20;
        for (Cell neighbor : current.getOpenNeighbors()) {
            double ndx = neighbor.getScreenX() - current.getScreenX();
            double ndy = neighbor.getScreenY() - current.getScreenY();
            double magnitude = Math.hypot(ndx, ndy);
            if (magnitude <= 1.0e-9) {
                continue;
            }
            double alignment = (ndx * dx + ndy * dy) / (magnitude * Math.hypot(dx, dy));
            if (alignment > bestAlignment) {
                bestAlignment = alignment;
                best = neighbor;
            }
        }
        return best;
    }
    private double angularDistance(double a, double b) {
        return Math.abs(Math.atan2(Math.sin(a - b), Math.cos(a - b)));
    }

    // PLAYER MOVEMENT

    private void movePlayerTo(Cell target) {
        if (!isPlayMode || isPlayerMoving || grid == null || target == null || player.getCurrentCell() == null || !player.getCurrentCell().isPassageOpenTo(target)) {
            return;
        }
        Cell from = player.getCurrentCell();

        // RECTANGULAR MOVEMENT

        if (!grid.isGeometric()) {
            if (player.moveToCell(target)) {
                player.updateDiscovery(grid, isFogOfWar, fogRadius);
                mazeCanvas.setPlayerCell(player.getCurrentCell());
                mazeCanvas.setFogOfWarActive(isFogOfWar, fogRadius);
                if (player.getCurrentCell().isExit) {
                    showVictory();
                }
            }
            return;
        }

        // GEOMETRIC MOVEMENT

        if (movementTimeline != null) {
            movementTimeline.stop();
        }
        isPlayerMoving = true;
        DoubleProperty animatedX = new SimpleDoubleProperty(from.getScreenX());
        DoubleProperty animatedY = new SimpleDoubleProperty(from.getScreenY());
        mazeCanvas.setPlayerScreenPosition(from.getScreenX(), from.getScreenY());
        if (!player.moveToCell(target)) {
            isPlayerMoving = false;
            mazeCanvas.clearPlayerScreenPosition();
            return;
        }

        player.updateDiscovery(grid, isFogOfWar, fogRadius);
        mazeCanvas.setPlayerCell(target);
        mazeCanvas.setFogOfWarActive(isFogOfWar, fogRadius);

        animatedX.addListener((obs, oldValue, newValue) ->
                mazeCanvas.setPlayerScreenPosition(newValue.doubleValue(), animatedY.get())
        );

        animatedY.addListener((obs, oldValue, newValue) ->
                mazeCanvas.setPlayerScreenPosition(animatedX.get(), newValue.doubleValue())
        );

        movementTimeline = new Timeline(new KeyFrame(Duration.millis(PLAYER_MOVE_MILLIS),
                        new KeyValue(animatedX, target.getScreenX(), Interpolator.EASE_BOTH),
                    new KeyValue(animatedY, target.getScreenY(), Interpolator.EASE_BOTH))
        );

        movementTimeline.setOnFinished(event -> {
            mazeCanvas.clearPlayerScreenPosition();
            isPlayerMoving = false;
            refreshInteractionHints();
            mazeCanvas.setFogOfWarActive(isFogOfWar, fogRadius);
            if (player.getCurrentCell().isExit) {
                showVictory();
            }
        });
        movementTimeline.play();
    }

    // VICTORY
    /**
     * Non-blocking victory dialog.
     * This is safe when called from a JavaFX animation callback.
     */
    private void showVictory() {
        int completedSteps = player.getSteps();
        isPlayMode = false;
        isPlayerMoving = false;
        playModeToggle.setSelected(false);
        playModeToggle.setText("Playable Mode: OFF");
        if (movementTimeline != null) {
            movementTimeline.stop();
            movementTimeline = null;
        }
        mazeCanvas.setHighlightedCells(List.of());
        mazeCanvas.setHoveredCell(null);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Victory!");
        alert.setHeaderText("Maze Completed!");
        alert.setContentText("Congratulations! You successfully navigated "
                        + "through the maze.\n\n"
                        + "Total Steps: " + completedSteps
        );
        alert.setResizable(false);
        alert.setOnHidden(event -> {
            player.reset();
            mazeCanvas.setPlayerCell(null);
            mazeCanvas.clearPlayerScreenPosition();
            mazeCanvas.setHighlightedCells(List.of());
            mazeCanvas.setHoveredCell(null);
            mazeCanvas.setFogOfWarActive(false, fogRadius);
        });
        alert.show();
    }


    // SOLVE AND DISPLAY PATH

    private void solveAndDisplayPath() {
        if (grid == null || grid.getEntranceCell() == null || grid.getExitCell() == null) {
            return;
        }
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

    // STATISTICS

    private void updateStatistics(int pathLength) {
        if (grid == null) {
            return;
        }
        int totalCells = grid.getCellCount();
        int deadEnds = grid.countDeadEnds();
        String pathString = pathLength > 0 ? pathLength + " steps" : "Hidden / Not solved";

        statsLabel.setText(String.format("Statistics:\n"
                                + "Total Cells: %d\n"
                                + "Dead Ends: %d\n"
                                + "Path Length: %s",
                        totalCells,
                        deadEnds,
                        pathString
                )
        );
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("Uncaught exception on thread " + thread.getName() + ": " + throwable);
            try {
                java.nio.file.Files.writeString(
                        java.nio.file.Path.of("error.log"),
                        java.time.LocalDateTime.now() + " [" + thread.getName() + "] " + throwable.toString() + "\n",
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND
                );
            } catch (IOException ignored) {}

            // Run recovery dialog on JavaFX application thread if possible
            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Unexpected Application Error");
                alert.setHeaderText("An unexpected error occurred.");
                alert.setContentText("Details have been logged to error.log.\nError: " + throwable.getMessage());
                alert.showAndWait();
            });
        });

        launch(args);
    }
}