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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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


/**
 * JavaFX application for rectangular, circular, hexagonal,
 * and triangular maze generation and solving.
 *
 * Supports configurable entrance and exit positions:
 *
 * - Left to Right
 * - Top to Bottom
 * - Center to Outer Edge (circular only)
 * - Custom Click Selection
 */
public class MazeGeneratorApp extends Application {

    // ============================================================
    // CONSTANTS
    // ============================================================

    private static final double CANVAS_SIZE = 600.0;

    private static final double GEOMETRY_PADDING = 22.0;

    private static final double PLAYER_MOVE_MILLIS = 160.0;


    // ============================================================
    // APPLICATION STATE
    // ============================================================

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


    // ============================================================
    // APPLICATION START
    // ============================================================

    @Override
    public void start(Stage primaryStage) {

        primaryStage.setTitle(
                "Customizable JavaFX Maze Generator & Solver"
        );

        mazeCanvas = new MazeCanvas(
                CANVAS_SIZE,
                CANVAS_SIZE
        );


        // ========================================================
        // TOPOLOGY SELECTION
        // ========================================================

        Label shapeHeader = new Label(
                "Maze Layout Shape:"
        );

        Label shapeInfo = createInfoIcon(
                "Choose rectangular, circular/polar, hexagonal, or triangular geometry."
        );

        shapeCombo = new ComboBox<>();

        shapeCombo.getItems().addAll(
                TopologyType.values()
        );

        shapeCombo.setValue(
                TopologyType.RECTANGULAR
        );

        shapeCombo.setMaxWidth(Double.MAX_VALUE);


        // ========================================================
        // ENTRANCE / EXIT MODE SELECTION
        // ========================================================

        Label entranceExitHeader = new Label(
                "Entrance / Exit Mode:"
        );

        Label entranceExitInfo = createInfoIcon(
                "Left to Right: start on the left and finish on the right. "
                        + "Top to Bottom: start at the top and finish at the bottom. "
                        + "Center to Outer Edge: available for circular mazes. "
                        + "Custom: click two cells after generation to select START and END."
        );

        entranceExitModeCombo = new ComboBox<>();

        entranceExitModeCombo.setMaxWidth(
                Double.MAX_VALUE
        );

        openingStatusLabel = new Label(
                "The selected mode will be applied when you generate a maze."
        );

        openingStatusLabel.setWrapText(true);

        openingStatusLabel.setMaxWidth(
                Double.MAX_VALUE
        );

        configureEntranceExitModeOptions(
                shapeCombo.getValue()
        );


        // ========================================================
        // GENERATION ALGORITHM SELECTION
        // ========================================================

        Label algoHeader = new Label(
                "Gen Algorithm:"
        );

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

        algorithmCombo.setValue(
                "Recursive Backtracker (DFS)"
        );

        algorithmCombo.setMaxWidth(Double.MAX_VALUE);


        // ========================================================
        // SOLVER SELECTION
        // ========================================================

        Label solverHeader = new Label(
                "Solver Algorithm:"
        );

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

        solverCombo.setValue(
                "Breadth-First Search (BFS)"
        );

        solverCombo.setMaxWidth(Double.MAX_VALUE);

        solverCombo.valueProperty().addListener(
                (obs, oldValue, newValue) -> {

                    if (showSolutionToggle != null
                            && showSolutionToggle.isSelected()
                            && grid != null) {

                        solveAndDisplayPath();
                    }
                }
        );


        // ========================================================
        // PLAYABLE MODE
        // ========================================================

        playModeToggle = new ToggleButton(
                "Playable Mode: OFF"
        );

        playModeToggle.setMaxWidth(Double.MAX_VALUE);

        Label playInfo = createInfoIcon(
                "Rectangular: WASD. Circular: click highlighted passages "
                        + "or use W/S/A/D for outward/inward/counterclockwise/clockwise. "
                        + "Hexagonal and triangular: click a highlighted open neighbor; "
                        + "WASD uses approximate screen directions."
        );

        HBox playBox = new HBox(
                5,
                playModeToggle,
                playInfo
        );

        HBox.setHgrow(
                playModeToggle,
                Priority.ALWAYS
        );

        playBox.setAlignment(
                Pos.CENTER_LEFT
        );


        // ========================================================
        // FOG OF WAR
        // ========================================================

        fogOfWarToggle = new ToggleButton(
                "Fog of War: OFF"
        );

        fogOfWarToggle.setMaxWidth(Double.MAX_VALUE);

        Label fogInfo = createInfoIcon(
                "Hides undiscovered cells outside the player's "
                        + "graph-distance visibility radius."
        );

        HBox fogBox = new HBox(
                5,
                fogOfWarToggle,
                fogInfo
        );

        HBox.setHgrow(
                fogOfWarToggle,
                Priority.ALWAYS
        );

        fogBox.setAlignment(
                Pos.CENTER_LEFT
        );


        // ========================================================
        // THEME
        // ========================================================

        themeToggle = new ToggleButton(
                "Theme: Dark"
        );

        themeToggle.setMaxWidth(Double.MAX_VALUE);

        themeToggle.setSelected(true);

        Label themeInfo = createInfoIcon(
                "Switches between dark and light visual themes."
        );

        HBox themeBox = new HBox(
                5,
                themeToggle,
                themeInfo
        );

        HBox.setHgrow(
                themeToggle,
                Priority.ALWAYS
        );

        themeBox.setAlignment(
                Pos.CENTER_LEFT
        );


        // ========================================================
        // WIDTH / TOPOLOGY PARAMETER SLIDER
        // ========================================================

        Label widthLabel = new Label(
                "Grid Width (Cols): 20"
        );

        Label widthInfo = createInfoIcon(
                "Rectangular maze column count."
        );

        HBox widthHeaderBox = new HBox(
                5,
                widthLabel,
                widthInfo
        );

        Slider widthSlider = new Slider(
                10,
                50,
                20
        );

        configureSlider(
                widthSlider,
                10
        );

        widthSlider.valueProperty().addListener(
                (obs, oldValue, newValue) -> {

                    widthLabel.setText(
                            widthLabelFor(shapeCombo.getValue())
                                    + newValue.intValue()
                    );
                }
        );


        // ========================================================
        // HEIGHT / TOPOLOGY PARAMETER SLIDER
        // ========================================================

        Label heightLabel = new Label(
                "Grid Height (Rows): 20"
        );

        Label heightInfo = createInfoIcon(
                "Rectangular maze row count."
        );

        HBox heightHeaderBox = new HBox(
                5,
                heightLabel,
                heightInfo
        );

        Slider heightSlider = new Slider(
                10,
                50,
                20
        );

        configureSlider(
                heightSlider,
                10
        );

        heightSlider.valueProperty().addListener(
                (obs, oldValue, newValue) -> {

                    heightLabel.setText(
                            heightLabelFor(shapeCombo.getValue())
                                    + newValue.intValue()
                    );
                }
        );


        // ========================================================
        // TOPOLOGY-DEPENDENT SLIDERS
        // ========================================================

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

                    configureEntranceExitModeOptions(
                            newTopology
                    );
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

        configureEntranceExitModeOptions(
                shapeCombo.getValue()
        );


        // ========================================================
        // GENERATION ANIMATION SPEED
        // ========================================================

        Label speedLabel = new Label(
                "Animation Delay (ms): 12"
        );

        Label speedInfo = createInfoIcon(
                "Controls the step-by-step maze generation animation speed."
        );

        HBox speedHeaderBox = new HBox(
                5,
                speedLabel,
                speedInfo
        );

        Slider speedSlider = new Slider(
                1,
                50,
                12
        );

        configureSlider(
                speedSlider,
                10
        );

        speedSlider.valueProperty().addListener(
                (obs, oldValue, newValue) ->

                        speedLabel.setText(
                                "Animation Delay (ms): "
                                        + newValue.intValue()
                        )
        );


        // ========================================================
        // COMPLEXITY
        // ========================================================

        Label complexityLabel = new Label(
                "Complexity / Dead-End Density: 50%"
        );

        Label complexityInfo = createInfoIcon(
                "Controls how aggressively extra passages are carved to reduce dead ends."
        );

        HBox complexityHeaderBox = new HBox(
                5,
                complexityLabel,
                complexityInfo
        );

        Slider complexitySlider = new Slider(
                0,
                100,
                50
        );

        configureSlider(
                complexitySlider,
                25
        );

        complexitySlider.valueProperty().addListener(
                (obs, oldValue, newValue) ->

                        complexityLabel.setText(
                                "Complexity / Dead-End Density: "
                                        + newValue.intValue()
                                        + "%"
                        )
        );


        // ========================================================
        // FOG RADIUS
        // ========================================================

        Label fogRadiusLabel = new Label(
                "Fog Radius: 2 cells"
        );

        Label fogRadiusInfo = createInfoIcon(
                "Controls the player's visible graph-distance radius."
        );

        HBox fogRadiusHeaderBox = new HBox(
                5,
                fogRadiusLabel,
                fogRadiusInfo
        );

        Slider fogRadiusSlider = new Slider(
                1,
                5,
                2
        );

        configureSlider(
                fogRadiusSlider,
                1
        );

        fogRadiusSlider.valueProperty().addListener(
                (obs, oldValue, newValue) -> {

                    fogRadius = newValue.intValue();

                    fogRadiusLabel.setText(
                            "Fog Radius: " + fogRadius + " cells"
                    );

                    if (isFogOfWar && grid != null) {

                        player.updateDiscovery(
                                grid,
                                true,
                                fogRadius
                        );

                        mazeCanvas.setFogOfWarActive(
                                true,
                                fogRadius
                        );
                    }
                }
        );


        // ========================================================
        // GENERATE BUTTON
        // ========================================================

        Button generateBtn = new Button(
                "Generate Labyrinth"
        );

        generateBtn.setMaxWidth(
                Double.MAX_VALUE
        );

        Label genInfo = createInfoIcon(
                "Generates a new maze using the selected topology and settings."
        );

        HBox genBox = new HBox(
                5,
                generateBtn,
                genInfo
        );

        HBox.setHgrow(
                generateBtn,
                Priority.ALWAYS
        );

        genBox.setAlignment(
                Pos.CENTER_LEFT
        );


        // ========================================================
        // SHOW SOLUTION
        // ========================================================

        showSolutionToggle = new ToggleButton(
                "Show Solution"
        );

        showSolutionToggle.setMaxWidth(
                Double.MAX_VALUE
        );

        Label solutionInfo = createInfoIcon(
                "Displays the route from entrance to exit using the selected solver."
        );

        HBox solutionBox = new HBox(
                5,
                showSolutionToggle,
                solutionInfo
        );

        HBox.setHgrow(
                showSolutionToggle,
                Priority.ALWAYS
        );

        solutionBox.setAlignment(
                Pos.CENTER_LEFT
        );

        showSolutionToggle.setOnAction(event -> {

            if (grid == null) {

                showSolutionToggle.setSelected(false);

                return;
            }

            if (showSolutionToggle.isSelected()) {

                showSolutionToggle.setText(
                        "Hide Solution"
                );

                if (customSelectionActive
                        || grid.getEntranceCell() == null
                        || grid.getExitCell() == null) {

                    mazeCanvas.setSolvedPath(null);
                    updateStatistics(-1);

                } else {

                    solveAndDisplayPath();
                }

            } else {

                showSolutionToggle.setText(
                        "Show Solution"
                );

                mazeCanvas.setSolvedPath(null);

                updateStatistics(-1);
            }
        });


        // ========================================================
        // EXPORT IMAGE
        // ========================================================

        Button exportBtn = new Button(
                "Export Image"
        );

        exportBtn.setMaxWidth(
                Double.MAX_VALUE
        );

        Label exportInfo = createInfoIcon(
                "Exports the current maze canvas as a PNG image."
        );

        HBox exportBox = new HBox(
                5,
                exportBtn,
                exportInfo
        );

        HBox.setHgrow(
                exportBtn,
                Priority.ALWAYS
        );

        exportBox.setAlignment(
                Pos.CENTER_LEFT
        );

        exportBtn.setOnAction(
                event -> exportMazeImage(primaryStage)
        );


        // ========================================================
        // STATISTICS
        // ========================================================

        statsLabel = new Label(
                "Statistics:\n"
                        + "Total Cells: -\n"
                        + "Dead Ends: -\n"
                        + "Path Length: -"
        );

        statsLabel.setStyle(
                "-fx-font-family: monospace; -fx-padding: 10;"
        );

        Label statsInfo = createInfoIcon(
                "Live metrics for cells, dead ends, and solution path length."
        );

        HBox statsHeaderBox = new HBox(
                5,
                new Label("Maze Metrics"),
                statsInfo
        );


        // ========================================================
        // CONTROLS LAYOUT
        // ========================================================

        VBox controls = new VBox(
                6,

                new HBox(5, shapeHeader, shapeInfo),
                shapeCombo,

                new HBox(5, entranceExitHeader, entranceExitInfo),
                entranceExitModeCombo,
                openingStatusLabel,

                new HBox(5, algoHeader, algoInfo),
                algorithmCombo,

                new HBox(5, solverHeader, solverInfo),
                solverCombo,

                playBox,
                fogBox,
                themeBox,

                widthHeaderBox,
                widthSlider,

                heightHeaderBox,
                heightSlider,

                speedHeaderBox,
                speedSlider,

                complexityHeaderBox,
                complexitySlider,

                fogRadiusHeaderBox,
                fogRadiusSlider,

                genBox,
                solutionBox,
                exportBox,

                new Separator(),

                statsHeaderBox,
                statsLabel
        );

        controls.setPadding(
                new Insets(10)
        );

        controls.setPrefWidth(290);


        // ========================================================
        // MAIN LAYOUT
        // ========================================================

        BorderPane root = new BorderPane();

        root.setCenter(mazeCanvas);

        root.setRight(controls);

        Scene scene = new Scene(
                root,
                920,
                840
        );

        scene.getStylesheets().add(
                getClass()
                        .getResource("/style.css")
                        .toExternalForm()
        );


        // ========================================================
        // THEME ACTION
        // ========================================================

        themeToggle.setOnAction(event -> {

            String stylesheet = getClass()
                    .getResource("/style.css")
                    .toExternalForm();

            if (themeToggle.isSelected()) {

                themeToggle.setText(
                        "Theme: Dark"
                );

                if (!scene.getStylesheets().contains(stylesheet)) {

                    scene.getStylesheets().add(stylesheet);
                }

                mazeCanvas.setDarkMode(true);

            } else {

                themeToggle.setText(
                        "Theme: Light"
                );

                scene.getStylesheets().remove(stylesheet);

                mazeCanvas.setDarkMode(false);
            }
        });


        // ========================================================
        // GENERATE ACTION
        // ========================================================

        generateBtn.setOnAction(event ->

                startGenerationAnimation(
                        (int) widthSlider.getValue(),
                        (int) heightSlider.getValue(),
                        speedSlider.getValue(),
                        complexitySlider.getValue() / 100.0,
                        algorithmCombo.getValue()
                )
        );


        // ========================================================
        // PLAYABLE MODE ACTION
        // ========================================================

        playModeToggle.setOnAction(event -> {

            isPlayMode = playModeToggle.isSelected();

            if (isPlayMode) {

                if (grid == null
                        || grid.getEntranceCell() == null
                        || grid.getExitCell() == null
                        || customSelectionActive) {

                    playModeToggle.setSelected(false);

                    isPlayMode = false;

                    return;
                }

                playModeToggle.setText(
                        "Playable Mode: ON"
                );

                spawnPlayerForCurrentMaze();

            } else {

                playModeToggle.setText(
                        "Playable Mode: OFF"
                );

                stopMovementAnimation();

                player.reset();

                mazeCanvas.setPlayerCell(null);

                mazeCanvas.setFogOfWarActive(
                        false,
                        fogRadius
                );

                mazeCanvas.clearPlayerScreenPosition();

                mazeCanvas.setHighlightedCells(
                        List.of()
                );

                mazeCanvas.setHoveredCell(null);
            }
        });


        // ========================================================
        // FOG ACTION
        // ========================================================

        fogOfWarToggle.setOnAction(event -> {

            isFogOfWar = fogOfWarToggle.isSelected();

            if (isFogOfWar) {

                fogOfWarToggle.setText(
                        "Fog of War: ON"
                );

                if (grid != null) {

                    for (Cell cell : grid.getAllCells()) {

                        cell.discovered = false;
                    }

                    if (isPlayMode
                            && player.getCurrentCell() != null) {

                        player.updateDiscovery(
                                grid,
                                true,
                                fogRadius
                        );
                    }
                }

                mazeCanvas.setFogOfWarActive(
                        true,
                        fogRadius
                );

            } else {

                fogOfWarToggle.setText(
                        "Fog of War: OFF"
                );

                if (grid != null) {

                    for (Cell cell : grid.getAllCells()) {

                        cell.discovered = true;
                    }
                }

                mazeCanvas.setFogOfWarActive(
                        false,
                        fogRadius
                );
            }
        });


        // ========================================================
        // KEYBOARD MOVEMENT
        // ========================================================

        scene.setOnKeyPressed(event -> {

            if (!isPlayMode
                    || customSelectionActive
                    || isPlayerMoving
                    || grid == null
                    || player.getCurrentCell() == null) {

                return;
            }

            Cell current = player.getCurrentCell();

            Cell target;

            switch (grid.getTopology()) {

                case RECTANGULAR ->

                        target = findRectangularKeyboardNeighbor(
                                current,
                                event.getCode()
                        );

                case CIRCULAR ->

                        target = findPolarKeyboardNeighbor(
                                current,
                                event.getCode()
                        );

                case HEXAGONAL, TRIANGULAR ->

                        target = findPolygonKeyboardNeighbor(
                                current,
                                event.getCode()
                        );

                default -> target = null;
            }

            if (target != null) {

                movePlayerTo(target);

                event.consume();
            }
        });


        // ========================================================
        // GEOMETRIC MOUSE HOVER
        // ========================================================

        mazeCanvas.setOnMouseMoved(event -> {

            if (customSelectionActive
                    || !isPlayMode
                    || isPlayerMoving
                    || grid == null
                    || !grid.isGeometric()
                    || player.getCurrentCell() == null) {

                mazeCanvas.setHoveredCell(null);

                return;
            }

            Cell hovered = mazeCanvas.findCellAt(
                    event.getX(),
                    event.getY()
            );

            boolean legal = hovered != null
                    && player.getCurrentCell()
                    .isPassageOpenTo(hovered);

            mazeCanvas.setHoveredCell(
                    legal ? hovered : null
            );
        });


        mazeCanvas.setOnMouseExited(
                event -> mazeCanvas.setHoveredCell(null)
        );


        // ========================================================
        // MOUSE CLICK HANDLING
        // ========================================================

        mazeCanvas.setOnMouseClicked(event -> {

            // Custom selection has priority over normal gameplay.
            // It works for all four topologies.
            if (customSelectionActive) {

                if (!isPlayerMoving) {

                    handleCustomSelectionClick(
                            event.getX(),
                            event.getY()
                    );
                }

                event.consume();

                return;
            }

            // Normal geometric click-to-move.
            if (!isPlayMode
                    || isPlayerMoving
                    || grid == null
                    || !grid.isGeometric()
                    || player.getCurrentCell() == null) {

                return;
            }

            Cell clicked = mazeCanvas.findCellAt(
                    event.getX(),
                    event.getY()
            );

            if (clicked != null
                    && player.getCurrentCell()
                    .isPassageOpenTo(clicked)) {

                movePlayerTo(clicked);

                event.consume();
            }
        });


        // ========================================================
        // SHOW WINDOW
        // ========================================================

        primaryStage.setScene(scene);

        primaryStage.show();
    }


    // ============================================================
    // ENTRANCE / EXIT MODE OPTIONS
    // ============================================================

    private void configureEntranceExitModeOptions(
            TopologyType topology
    ) {

        EntranceExitMode previous =
                entranceExitModeCombo.getValue();

        entranceExitModeCombo.getItems().clear();

        entranceExitModeCombo.getItems().addAll(
                EntranceExitMode.LEFT_TO_RIGHT,
                EntranceExitMode.TOP_TO_BOTTOM
        );

        if (topology == TopologyType.CIRCULAR) {

            entranceExitModeCombo.getItems().add(
                    EntranceExitMode.CENTER_TO_OUTER
            );
        }

        entranceExitModeCombo.getItems().add(
                EntranceExitMode.CUSTOM
        );

        if (previous != null
                && entranceExitModeCombo.getItems().contains(previous)) {

            entranceExitModeCombo.setValue(previous);

        } else {

            entranceExitModeCombo.setValue(
                    EntranceExitMode.LEFT_TO_RIGHT
            );
        }
    }


    // ============================================================
    // CUSTOM ENTRANCE / EXIT SELECTION
    // ============================================================

    private void handleCustomSelectionClick(
            double mouseX,
            double mouseY
    ) {

        if (!customSelectionActive || grid == null) {
            return;
        }

        Cell clickedCell = mazeCanvas.findCellAt(
                mouseX,
                mouseY
        );

        // Ignore clicks outside the actual maze.
        if (clickedCell == null) {
            return;
        }

        // ========================================================
        // FIRST CLICK: SELECT START
        // ========================================================

        if (customStartCell == null) {

            customStartCell = clickedCell;

            grid.setCustomEntrancePreview(
                    customStartCell
            );

            mazeCanvas.setSolvedPath(null);

            mazeCanvas.draw();

            updateStatistics(-1);

            openingStatusLabel.setText(
                    "START selected. Click a different cell to set END."
            );

            return;
        }

        // ========================================================
        // SECOND CLICK: SELECT END
        // ========================================================

        if (clickedCell == customStartCell) {

            openingStatusLabel.setText(
                    "Choose a different cell for END."
            );

            return;
        }

        Cell selectedStart = customStartCell;

        Cell selectedEnd = clickedCell;

        grid.createCustomOpenings(
                selectedStart,
                selectedEnd
        );

        customStartCell = null;

        customSelectionActive = false;

        openingStatusLabel.setText(
                "Custom START and END configured successfully."
        );

        mazeCanvas.draw();

        if (showSolutionToggle.isSelected()) {

            solveAndDisplayPath();

        } else {

            updateStatistics(-1);
        }

        // If Playable Mode was already active, spawn at the
        // newly selected START cell.
        if (isPlayMode) {

            spawnPlayerForCurrentMaze();
        }
    }


    // ============================================================
    // PLAYER SPAWN
    // ============================================================

    private void spawnPlayerForCurrentMaze() {

        if (grid == null
                || grid.getEntranceCell() == null
                || grid.getExitCell() == null) {

            return;
        }

        player.spawn(grid);

        player.updateDiscovery(
                grid,
                isFogOfWar,
                fogRadius
        );

        mazeCanvas.setPlayerCell(
                player.getCurrentCell()
        );

        mazeCanvas.setFogOfWarActive(
                isFogOfWar,
                fogRadius
        );

        refreshInteractionHints();
    }


    // ============================================================
    // SLIDER HELPERS
    // ============================================================

    private void configureSlider(
            Slider slider,
            double majorTickUnit
    ) {

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

                // R = 2 through R = 7.
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

                // N = 4 through N = 16.
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

            default -> throw new IllegalStateException(
                    "Unknown topology: " + topology
            );
        }


        // Configure width / shape size.

        widthSlider.setMin(widthMin);
        widthSlider.setMax(widthMax);
        widthSlider.setMajorTickUnit(widthMajor);
        widthSlider.setBlockIncrement(1);
        widthSlider.setSnapToTicks(true);
        widthSlider.setValue(widthDefault);


        // Configure height.

        heightSlider.setMin(heightMin);
        heightSlider.setMax(heightMax);
        heightSlider.setMajorTickUnit(heightMajor);
        heightSlider.setBlockIncrement(1);
        heightSlider.setSnapToTicks(true);
        heightSlider.setValue(heightDefault);


        // Hide the second dimension for the two polygonal boards.

        heightHeaderBox.setVisible(!singleDimension);
        heightHeaderBox.setManaged(!singleDimension);

        heightSlider.setVisible(!singleDimension);
        heightSlider.setManaged(!singleDimension);


        // Refresh labels.

        widthLabel.setText(
                widthLabelFor(topology)
                        + (int) widthDefault
        );

        heightLabel.setText(
                heightLabelFor(topology)
                        + (int) heightDefault
        );


        // Refresh information tooltips.

        updateInfoTooltip(
                widthInfo,
                widthTooltipFor(topology)
        );

        updateInfoTooltip(
                heightInfo,
                heightTooltipFor(topology)
        );
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

            case RECTANGULAR ->

                    "Number of rectangular columns (10–50).";

            case CIRCULAR ->

                    "Number of sectors along the outermost polar ring (8–24).";

            case HEXAGONAL ->

                    "Controls the radius of the regular hexagonal board (2–7). "
                            + "The outer boundary is a regular hexagon.";

            case TRIANGULAR ->

                    "Controls the number of small equilateral triangle edges "
                            + "along each side of the large triangular board (4–16). "
                            + "Total cells = N squared.";
        };
    }


    private String heightTooltipFor(TopologyType topology) {

        return switch (topology) {

            case RECTANGULAR ->

                    "Number of rectangular rows (10–50).";

            case CIRCULAR ->

                    "Number of concentric rings outside the center (4–12).";

            case HEXAGONAL ->

                    "Hexagonal board dimensions are controlled by its radius.";

            case TRIANGULAR ->

                    "Triangular board dimensions are controlled by its side length.";
        };
    }


    private void updateInfoTooltip(
            Label infoLabel,
            String text
    ) {

        Tooltip tooltip = (Tooltip) infoLabel
                .getProperties()
                .get("mazeInfoTooltip");

        if (tooltip == null) {

            tooltip = new Tooltip(text);

            infoLabel.getProperties().put(
                    "mazeInfoTooltip",
                    tooltip
            );

            Tooltip.install(
                    infoLabel,
                    tooltip
            );

        } else {

            tooltip.setText(text);
        }
    }


    private Label createInfoIcon(
            String tooltipText
    ) {

        Label infoLabel = new Label("ℹ");

        infoLabel.setStyle(
                "-fx-text-fill: #00bcd4; "
                        + "-fx-font-weight: bold; "
                        + "-fx-cursor: hand; "
                        + "-fx-padding: 0 0 0 4;"
        );

        Tooltip tooltip = new Tooltip(tooltipText);

        infoLabel.getProperties().put(
                "mazeInfoTooltip",
                tooltip
        );

        Tooltip.install(
                infoLabel,
                tooltip
        );

        return infoLabel;
    }


    // ============================================================
    // RENDERER SELECTION AND MAZE GENERATION
    // ============================================================

    private void startGenerationAnimation(
            int width,
            int height,
            double animDelay,
            double complexityFactor,
            String algorithm
    ) {

        if (animTimeline != null) {

            animTimeline.stop();
        }

        stopMovementAnimation();

        customSelectionActive = false;

        customStartCell = null;

        openingStatusLabel.setText(
                "Generating maze..."
        );


        TopologyType topology =
                shapeCombo.getValue();

        EntranceExitMode openingMode =
                entranceExitModeCombo.getValue();


        // ========================================================
        // SELECT RENDERER
        // ========================================================

        switch (topology) {

            case RECTANGULAR ->

                    mazeCanvas.setRenderer(
                            new RectangularRenderer()
                    );

            case CIRCULAR ->

                    mazeCanvas.setRenderer(
                            new CircularRenderer()
                    );

            case HEXAGONAL ->

                    mazeCanvas.setRenderer(
                            new HexagonalRenderer()
                    );

            case TRIANGULAR ->

                    mazeCanvas.setRenderer(
                            new TriangularRenderer()
                    );
        }


        // ========================================================
        // CREATE CORRECT GRID
        // ========================================================

        grid = switch (topology) {

            case HEXAGONAL, TRIANGULAR ->

                    new MazeGrid(
                            width,
                            width,
                            topology
                    );

            case RECTANGULAR, CIRCULAR ->

                    new MazeGrid(
                            width,
                            height,
                            topology
                    );
        };


        // ========================================================
        // CANVAS DIMENSIONS AND GEOMETRY
        // ========================================================

        switch (topology) {

            case RECTANGULAR -> {

                double cellSize =
                        CANVAS_SIZE / Math.max(width, height);

                mazeCanvas.setWidth(
                        width * cellSize
                );

                mazeCanvas.setHeight(
                        height * cellSize
                );

                for (Cell cell : grid.getAllCells()) {

                    cell.screenX =
                            (cell.x + 0.5) * cellSize;

                    cell.screenY =
                            (cell.y + 0.5) * cellSize;
                }
            }


            case CIRCULAR -> {

                mazeCanvas.setWidth(CANVAS_SIZE);

                mazeCanvas.setHeight(CANVAS_SIZE);

                grid.layoutCircularGrid(
                        CANVAS_SIZE,
                        CANVAS_SIZE,
                        GEOMETRY_PADDING
                );
            }


            case HEXAGONAL -> {

                mazeCanvas.setWidth(CANVAS_SIZE);

                mazeCanvas.setHeight(CANVAS_SIZE);

                grid.layoutHexagonalGrid(
                        CANVAS_SIZE,
                        CANVAS_SIZE,
                        GEOMETRY_PADDING
                );
            }


            case TRIANGULAR -> {

                mazeCanvas.setWidth(CANVAS_SIZE);

                mazeCanvas.setHeight(CANVAS_SIZE);

                grid.layoutTriangularGrid(
                        CANVAS_SIZE,
                        CANVAS_SIZE,
                        GEOMETRY_PADDING
                );
            }
        }


        // ========================================================
        // INITIALIZE CELL STATES
        // ========================================================

        for (Cell cell : grid.getAllCells()) {

            cell.visited = false;

            cell.discovered = !isFogOfWar;
        }


        // ========================================================
        // RESET PREVIOUS VISUAL STATE
        // ========================================================

        mazeCanvas.setGrid(grid);

        mazeCanvas.setSolvedPath(null);

        mazeCanvas.setCurrentHead(null);

        mazeCanvas.setPlayerCell(null);

        mazeCanvas.setHighlightedCells(
                List.of()
        );

        mazeCanvas.setHoveredCell(null);

        mazeCanvas.clearPlayerScreenPosition();

        mazeCanvas.setFogOfWarActive(
                isFogOfWar,
                fogRadius
        );

        player.reset();

        updateStatistics(-1);


        if (showSolutionToggle != null) {

            showSolutionToggle.setSelected(false);

            showSolutionToggle.setText(
                    "Show Solution"
            );
        }


        // ========================================================
        // CHOOSE GENERATOR
        // ========================================================

        MazeGenerator generator = switch (algorithm) {

            case "Randomized Prim's" ->

                    new PrimsMazeGenerator();

            case "Randomized Kruskal's" ->

                    new KruskalsMazeGenerator();

            case "Aldous-Broder" ->

                    new AldousBroderMazeGenerator();

            case "Wilson's" ->

                    new WilsonsMazeGenerator();

            default ->

                    new DfsMazeGenerator();
        };


        generator.initialize(grid);


        // ========================================================
        // GENERATION TIMELINE
        // ========================================================

        animTimeline = new Timeline();

        KeyFrame keyFrame = new KeyFrame(
                Duration.millis(animDelay),

                event -> {

                    boolean running = generator.step();

                    mazeCanvas.setCurrentHead(
                            generator.getCurrentHead()
                    );


                    // =================================================
                    // GENERATION FINISHED
                    // =================================================

                    if (!running) {

                        animTimeline.stop();

                        entranceExitModeCombo.setDisable(false);


                        // Apply complexity before choosing openings.

                        grid.applyComplexityFactor(
                                complexityFactor
                        );


                        // =================================================
                        // CREATE ENTRANCE AND EXIT
                        // =================================================

                        if (openingMode == EntranceExitMode.CUSTOM) {

                            grid.createOpenings(
                                    EntranceExitMode.CUSTOM
                            );

                            customSelectionActive = true;

                            customStartCell = null;

                            openingStatusLabel.setText(
                                    "Custom selection: click a cell to set START. "
                                            + "Then click another cell to set END."
                            );

                        } else {

                            customSelectionActive = false;

                            customStartCell = null;

                            grid.createOpenings(
                                    openingMode
                            );

                            openingStatusLabel.setText(
                                    "Entrance and exit configured: "
                                            + openingMode
                            );
                        }

                        mazeCanvas.setCurrentHead(null);

                        mazeCanvas.draw();


                        // =================================================
                        // SOLUTION
                        // =================================================

                        if (showSolutionToggle.isSelected()
                                && !customSelectionActive) {

                            solveAndDisplayPath();

                        } else {

                            mazeCanvas.setSolvedPath(null);

                            updateStatistics(-1);
                        }


                        // =================================================
                        // SPAWN PLAYER
                        // =================================================

                        if (isPlayMode
                                && grid.getEntranceCell() != null
                                && grid.getExitCell() != null) {

                            spawnPlayerForCurrentMaze();
                        }
                    }
                }
        );


        animTimeline.getKeyFrames().add(keyFrame);

        animTimeline.setCycleCount(
                Timeline.INDEFINITE
        );

        entranceExitModeCombo.setDisable(true);

        animTimeline.play();
    }


    // ============================================================
    // INTERACTION HINTS
    // ============================================================

    private void refreshInteractionHints() {

        if (grid != null
                && grid.isGeometric()
                && isPlayMode
                && player.getCurrentCell() != null) {

            mazeCanvas.setHighlightedCells(
                    player.getCurrentCell().getOpenNeighbors()
            );

        } else {

            mazeCanvas.setHighlightedCells(
                    List.of()
            );
        }

        mazeCanvas.setHoveredCell(null);
    }


    // ============================================================
    // STOP MOVEMENT ANIMATION
    // ============================================================

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


    // ============================================================
    // RECTANGULAR KEYBOARD MOVEMENT
    // ============================================================

    private Cell findRectangularKeyboardNeighbor(
            Cell current,
            KeyCode key
    ) {

        double dx = 0.0;
        double dy = 0.0;

        switch (key) {

            case W -> dy = -1.0;

            case S -> dy = 1.0;

            case A -> dx = -1.0;

            case D -> dx = 1.0;

            default -> {
                return null;
            }
        }


        Cell best = null;

        double bestAlignment = 0.0;


        for (Cell neighbor : current.getOpenNeighbors()) {

            double ndx =
                    neighbor.getScreenX()
                            - current.getScreenX();

            double ndy =
                    neighbor.getScreenY()
                            - current.getScreenY();

            double alignment =
                    ndx * dx + ndy * dy;


            if (alignment > bestAlignment) {

                bestAlignment = alignment;

                best = neighbor;
            }
        }

        return best;
    }


    // ============================================================
    // POLAR KEYBOARD MOVEMENT
    // ============================================================

    /**
     * Circular controls:
     *
     * W = outward.
     * S = inward.
     * A = counterclockwise.
     * D = clockwise.
     */
    private Cell findPolarKeyboardNeighbor(
            Cell current,
            KeyCode key
    ) {

        if (!(current instanceof PolarCell polar)) {
            return null;
        }

        List<Cell> openNeighbors =
                current.getOpenNeighbors();

        if (openNeighbors.isEmpty()) {
            return null;
        }


        // Central cell has no angular orientation.

        if (polar.isCore()) {

            if (key != KeyCode.W) {
                return null;
            }

            Cell best = null;

            double bestAngle =
                    Double.POSITIVE_INFINITY;


            for (Cell neighbor : openNeighbors) {

                if (neighbor instanceof PolarCell candidate
                        && candidate.getRingIndex() == 1) {

                    double delta = angularDistance(
                            candidate.getCenterAngle(),
                            -Math.PI / 2.0
                    );

                    if (delta < bestAngle) {

                        bestAngle = delta;

                        best = candidate;
                    }
                }
            }

            return best;
        }


        // Determine target ring.

        int targetRing = switch (key) {

            case W -> polar.getRingIndex() + 1;

            case S -> polar.getRingIndex() - 1;

            default -> polar.getRingIndex();
        };


        // Determine target sector.

        int targetSector = switch (key) {

            case A -> Math.floorMod(
                    polar.getSectorIndex() - 1,
                    polar.getSectorsInRing()
            );

            case D -> (
                    polar.getSectorIndex() + 1
            ) % polar.getSectorsInRing();

            default -> -1;
        };


        Cell best = null;

        double bestAngle =
                Double.POSITIVE_INFINITY;


        for (Cell neighbor : openNeighbors) {

            if (!(neighbor instanceof PolarCell candidate)) {
                continue;
            }

            boolean matches;

            if (key == KeyCode.A || key == KeyCode.D) {

                matches =
                        candidate.getRingIndex()
                                == polar.getRingIndex()
                                &&
                                candidate.getSectorIndex()
                                        == targetSector;

            } else if (key == KeyCode.W || key == KeyCode.S) {

                matches =
                        candidate.getRingIndex()
                                == targetRing;

            } else {

                return null;
            }


            if (matches) {

                double delta = angularDistance(
                        candidate.getCenterAngle(),
                        polar.getCenterAngle()
                );

                if (delta < bestAngle) {

                    bestAngle = delta;

                    best = candidate;
                }
            }
        }

        return best;
    }


    // ============================================================
    // HEXAGONAL / TRIANGULAR KEYBOARD MOVEMENT
    // ============================================================

    /**
     * Screen-space directional movement.
     *
     * WASD are the main directions.
     * Q/E/Z/C also provide diagonal movement.
     *
     * Mouse movement remains the most precise way to navigate
     * polygonal cells.
     */
    private Cell findPolygonKeyboardNeighbor(
            Cell current,
            KeyCode key
    ) {

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

            double ndx =
                    neighbor.getScreenX()
                            - current.getScreenX();

            double ndy =
                    neighbor.getScreenY()
                            - current.getScreenY();

            double magnitude =
                    Math.hypot(ndx, ndy);

            if (magnitude <= 1.0e-9) {
                continue;
            }

            double alignment =
                    (ndx * dx + ndy * dy)
                            / (magnitude * Math.hypot(dx, dy));


            if (alignment > bestAlignment) {

                bestAlignment = alignment;

                best = neighbor;
            }
        }

        return best;
    }


    private double angularDistance(
            double a,
            double b
    ) {

        return Math.abs(
                Math.atan2(
                        Math.sin(a - b),
                        Math.cos(a - b)
                )
        );
    }


    // ============================================================
    // PLAYER MOVEMENT
    // ============================================================

    private void movePlayerTo(Cell target) {

        if (!isPlayMode
                || isPlayerMoving
                || grid == null
                || target == null
                || player.getCurrentCell() == null
                || !player.getCurrentCell()
                .isPassageOpenTo(target)) {

            return;
        }

        Cell from = player.getCurrentCell();


        // ========================================================
        // RECTANGULAR MOVEMENT
        // ========================================================

        if (!grid.isGeometric()) {

            if (player.moveToCell(target)) {

                player.updateDiscovery(
                        grid,
                        isFogOfWar,
                        fogRadius
                );

                mazeCanvas.setPlayerCell(
                        player.getCurrentCell()
                );

                mazeCanvas.setFogOfWarActive(
                        isFogOfWar,
                        fogRadius
                );

                if (player.getCurrentCell().isExit) {

                    showVictory();
                }
            }

            return;
        }


        // ========================================================
        // GEOMETRIC MOVEMENT
        // ========================================================

        if (movementTimeline != null) {

            movementTimeline.stop();
        }

        isPlayerMoving = true;


        DoubleProperty animatedX =
                new SimpleDoubleProperty(
                        from.getScreenX()
                );


        DoubleProperty animatedY =
                new SimpleDoubleProperty(
                        from.getScreenY()
                );


        mazeCanvas.setPlayerScreenPosition(
                from.getScreenX(),
                from.getScreenY()
        );


        // Update actual graph position.

        if (!player.moveToCell(target)) {

            isPlayerMoving = false;

            mazeCanvas.clearPlayerScreenPosition();

            return;
        }


        player.updateDiscovery(
                grid,
                isFogOfWar,
                fogRadius
        );

        mazeCanvas.setPlayerCell(target);

        mazeCanvas.setFogOfWarActive(
                isFogOfWar,
                fogRadius
        );


        // Animate marker coordinates.

        animatedX.addListener(
                (obs, oldValue, newValue) ->

                        mazeCanvas.setPlayerScreenPosition(
                                newValue.doubleValue(),
                                animatedY.get()
                        )
        );


        animatedY.addListener(
                (obs, oldValue, newValue) ->

                        mazeCanvas.setPlayerScreenPosition(
                                animatedX.get(),
                                newValue.doubleValue()
                        )
        );


        movementTimeline = new Timeline(

                new KeyFrame(
                        Duration.millis(PLAYER_MOVE_MILLIS),

                        new KeyValue(
                                animatedX,
                                target.getScreenX(),
                                Interpolator.EASE_BOTH
                        ),

                        new KeyValue(
                                animatedY,
                                target.getScreenY(),
                                Interpolator.EASE_BOTH
                        )
                )
        );


        movementTimeline.setOnFinished(event -> {

            mazeCanvas.clearPlayerScreenPosition();

            isPlayerMoving = false;

            refreshInteractionHints();

            mazeCanvas.setFogOfWarActive(
                    isFogOfWar,
                    fogRadius
            );

            if (player.getCurrentCell().isExit) {

                showVictory();
            }
        });

        movementTimeline.play();
    }


    // ============================================================
    // VICTORY
    // ============================================================

    /**
     * Non-blocking victory dialog.
     *
     * This is safe when called from a JavaFX animation callback.
     */
    private void showVictory() {

        int completedSteps = player.getSteps();

        isPlayMode = false;

        isPlayerMoving = false;

        playModeToggle.setSelected(false);

        playModeToggle.setText(
                "Playable Mode: OFF"
        );


        if (movementTimeline != null) {

            movementTimeline.stop();

            movementTimeline = null;
        }


        mazeCanvas.setHighlightedCells(
                List.of()
        );

        mazeCanvas.setHoveredCell(null);


        Alert alert = new Alert(
                Alert.AlertType.INFORMATION
        );

        alert.setTitle("Victory!");

        alert.setHeaderText(
                "Maze Completed!"
        );

        alert.setContentText(
                "Congratulations! You successfully navigated "
                        + "through the maze.\n\n"
                        + "Total Steps: " + completedSteps
        );

        alert.setResizable(false);


        alert.setOnHidden(event -> {

            player.reset();

            mazeCanvas.setPlayerCell(null);

            mazeCanvas.clearPlayerScreenPosition();

            mazeCanvas.setHighlightedCells(
                    List.of()
            );

            mazeCanvas.setHoveredCell(null);

            mazeCanvas.setFogOfWarActive(
                    false,
                    fogRadius
            );
        });


        // Never use showAndWait() inside animation processing.
        alert.show();
    }


    // ============================================================
    // IMAGE EXPORT
    // ============================================================

    private void exportMazeImage(Stage owner) {

        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle(
                "Save Maze Image"
        );

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "PNG Image (*.png)",
                        "*.png"
                )
        );

        fileChooser.setInitialFileName(
                "maze.png"
        );


        File file = fileChooser.showSaveDialog(owner);

        if (file == null) {
            return;
        }


        WritableImage image = new WritableImage(
                (int) mazeCanvas.getWidth(),
                (int) mazeCanvas.getHeight()
        );

        mazeCanvas.snapshot(
                null,
                image
        );


        try {

            ImageIO.write(
                    SwingFXUtils.fromFXImage(
                            image,
                            null
                    ),
                    "png",
                    file
            );

        } catch (IOException ex) {

            Alert alert = new Alert(
                    Alert.AlertType.ERROR,
                    "Failed to save image: " + ex.getMessage(),
                    ButtonType.OK
            );

            alert.show();
        }
    }


    // ============================================================
    // SOLVE AND DISPLAY PATH
    // ============================================================

    private void solveAndDisplayPath() {

        if (grid == null
                || grid.getEntranceCell() == null
                || grid.getExitCell() == null) {

            return;
        }

        String selectedSolver =
                solverCombo.getValue();


        List<Cell> path = switch (selectedSolver) {

            case "Depth-First Search (DFS)" ->

                    DfsMazeSolver.solve(grid);

            case "Dijkstra's Algorithm" ->

                    DijkstraMazeSolver.solve(grid);

            default ->

                    BfsMazeSolver.solve(grid);
        };


        if (path != null && !path.isEmpty()) {

            mazeCanvas.setSolvedPath(path);

            updateStatistics(path.size());

        } else {

            mazeCanvas.setSolvedPath(null);

            updateStatistics(-1);
        }
    }


    // ============================================================
    // STATISTICS
    // ============================================================

    private void updateStatistics(int pathLength) {

        if (grid == null) {
            return;
        }

        int totalCells =
                grid.getCellCount();

        int deadEnds =
                grid.countDeadEnds();

        String pathString = pathLength > 0

                ? pathLength + " steps"

                : "Hidden / Not solved";


        statsLabel.setText(
                String.format(
                        "Statistics:\n"
                                + "Total Cells: %d\n"
                                + "Dead Ends: %d\n"
                                + "Path Length: %s",

                        totalCells,
                        deadEnds,
                        pathString
                )
        );
    }


    // ============================================================
    // MAIN
    // ============================================================

    public static void main(String[] args) {

        launch(args);
    }
}