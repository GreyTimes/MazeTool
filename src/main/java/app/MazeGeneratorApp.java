
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
import model.MazeGrid;
import model.PolarCell;
import model.TopologyType;

import view.MazeCanvas;
import view.MazeRenderer;
import view.RectangularRenderer;
import view.HexagonalRenderer;
import view.TriangularRenderer;
import view.CircularRenderer;

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

    private ToggleButton playModeToggle;

    private ToggleButton fogOfWarToggle;

    private ToggleButton showSolutionToggle;

    private ToggleButton themeToggle;

    private final Player player = new Player();

    private boolean isPlayMode = false;

    private boolean isFogOfWar = false;

    private boolean isPlayerMoving = false;

    private int fogRadius = 2;


    // ============================================================
    // APPLICATION START
    // ============================================================

    @Override
    public void start(Stage primaryStage) {

        primaryStage.setTitle(
                "Customizable JavaFX Maze Generator & Solver"
        );

        mazeCanvas = new MazeCanvas(600, 600);


        // ========================================================
        // TOPOLOGY SELECTION
        // ========================================================

        Label shapeHeader = new Label("Maze Layout Shape:");

        Label shapeInfo = createInfoIcon(
                "Selects the geometric topology layout for the maze."
        );

        shapeCombo = new ComboBox<>();

        shapeCombo.getItems().addAll(TopologyType.values());

        shapeCombo.setValue(TopologyType.RECTANGULAR);

        shapeCombo.setMaxWidth(Double.MAX_VALUE);


        // ========================================================
        // GENERATION ALGORITHM SELECTION
        // ========================================================

        Label algoHeader = new Label("Gen Algorithm:");

        Label algoInfo = createInfoIcon(
                "Generation Algorithms:\n" +
                        "• DFS: Creates long, winding corridors.\n" +
                        "• Prim's / Kruskal's: Produces highly branched mazes.\n" +
                        "• Aldous-Broder / Wilson's: Generates uniform spanning trees."
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

        Label solverHeader = new Label("Solver Algorithm:");

        Label solverInfo = createInfoIcon(
                "Pathfinding Solvers:\n" +
                        "• BFS: Finds the shortest path in number of moves.\n" +
                        "• DFS: Explores deep paths, not necessarily shortest.\n" +
                        "• Dijkstra: Finds the minimum geometric-distance path."
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
                (obs, oldVal, newVal) -> {
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
                "Rectangular: use WASD.\n" +
                        "Circular: click highlighted open neighbors to move.\n" +
                        "Circular keyboard: W outward, S inward, " +
                        "A counterclockwise, D clockwise."
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

        playBox.setAlignment(Pos.CENTER_LEFT);


        // ========================================================
        // FOG OF WAR
        // ========================================================

        fogOfWarToggle = new ToggleButton(
                "Fog of War: OFF"
        );

        fogOfWarToggle.setMaxWidth(Double.MAX_VALUE);

        Label fogInfo = createInfoIcon(
                "Hides distant undiscovered cells outside the player's visibility radius."
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

        fogBox.setAlignment(Pos.CENTER_LEFT);


        // ========================================================
        // THEME
        // ========================================================

        themeToggle = new ToggleButton(
                "Theme: Dark"
        );

        themeToggle.setMaxWidth(Double.MAX_VALUE);

        themeToggle.setSelected(true);

        Label themeInfo = createInfoIcon(
                "Switches between Dark and Light visual themes."
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

        themeBox.setAlignment(Pos.CENTER_LEFT);


        // ========================================================
        // WIDTH / SECTOR SLIDER
        // ========================================================

        Label widthLabel = new Label(
                "Grid Width (Cols): 20"
        );

        Label widthInfo = createInfoIcon(
                "Defines the number of columns in a rectangular maze."
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

        widthSlider.setShowTickLabels(true);
        widthSlider.setShowTickMarks(true);
        widthSlider.setMajorTickUnit(10);
        widthSlider.setBlockIncrement(1);
        widthSlider.setSnapToTicks(true);

        widthSlider.valueProperty().addListener(
                (obs, oldVal, newVal) -> {

                    String label =
                            shapeCombo.getValue() == TopologyType.CIRCULAR
                                    ? "Outer Ring Sectors: "
                                    : "Grid Width (Cols): ";

                    widthLabel.setText(
                            label + newVal.intValue()
                    );
                }
        );


        // ========================================================
        // HEIGHT / RING SLIDER
        // ========================================================

        Label heightLabel = new Label(
                "Grid Height (Rows): 20"
        );

        Label heightInfo = createInfoIcon(
                "Defines the number of rows in a rectangular maze."
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

        heightSlider.setShowTickLabels(true);
        heightSlider.setShowTickMarks(true);
        heightSlider.setMajorTickUnit(10);
        heightSlider.setBlockIncrement(1);
        heightSlider.setSnapToTicks(true);

        heightSlider.valueProperty().addListener(
                (obs, oldVal, newVal) -> {

                    String label =
                            shapeCombo.getValue() == TopologyType.CIRCULAR
                                    ? "Number of Rings: "
                                    : "Grid Height (Rows): ";

                    heightLabel.setText(
                            label + newVal.intValue()
                    );
                }
        );


        // ========================================================
        // TOPOLOGY-DEPENDENT SLIDER CONFIGURATION
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
                            heightInfo
                    );

                    // Changing the dropdown changes the next generated maze.
                    // The currently displayed maze remains untouched.
                }
        );

        configureDimensionSliders(
                shapeCombo.getValue(),
                widthSlider,
                heightSlider,
                widthLabel,
                heightLabel,
                widthInfo,
                heightInfo
        );


        // ========================================================
        // GENERATION SPEED
        // ========================================================

        Label speedLabel = new Label(
                "Animation Delay (ms): 12"
        );

        Label speedInfo = createInfoIcon(
                "Controls the generation animation speed."
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

        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setMajorTickUnit(10);
        speedSlider.setBlockIncrement(1);
        speedSlider.setSnapToTicks(true);

        speedSlider.valueProperty().addListener(
                (obs, oldVal, newVal) ->
                        speedLabel.setText(
                                "Animation Delay (ms): "
                                        + newVal.intValue()
                        )
        );


        // ========================================================
        // COMPLEXITY
        // ========================================================

        Label complexityLabel = new Label(
                "Complexity / Dead-End Density: 50%"
        );

        Label complexityInfo = createInfoIcon(
                "Controls loop carving. Higher complexity retains more dead ends."
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

        complexitySlider.setShowTickLabels(true);
        complexitySlider.setShowTickMarks(true);
        complexitySlider.setMajorTickUnit(25);
        complexitySlider.setBlockIncrement(5);
        complexitySlider.setSnapToTicks(true);

        complexitySlider.valueProperty().addListener(
                (obs, oldVal, newVal) ->
                        complexityLabel.setText(
                                "Complexity / Dead-End Density: "
                                        + newVal.intValue()
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
                "Controls the number of neighboring cells visible to the player."
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

        fogRadiusSlider.setShowTickLabels(true);
        fogRadiusSlider.setShowTickMarks(true);
        fogRadiusSlider.setMajorTickUnit(1);
        fogRadiusSlider.setBlockIncrement(1);
        fogRadiusSlider.setSnapToTicks(true);

        fogRadiusSlider.valueProperty().addListener(
                (obs, oldVal, newVal) -> {

                    fogRadius = newVal.intValue();

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

        generateBtn.setMaxWidth(Double.MAX_VALUE);

        Label genInfo = createInfoIcon(
                "Generates a new maze using the selected algorithm and settings."
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

        genBox.setAlignment(Pos.CENTER_LEFT);


        // ========================================================
        // SHOW SOLUTION
        // ========================================================

        showSolutionToggle = new ToggleButton(
                "Show Solution"
        );

        showSolutionToggle.setMaxWidth(Double.MAX_VALUE);

        Label solutionInfo = createInfoIcon(
                "Displays the path between entrance and exit."
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

        solutionBox.setAlignment(Pos.CENTER_LEFT);

        showSolutionToggle.setOnAction(e -> {

            if (grid == null) {

                showSolutionToggle.setSelected(false);

                return;
            }

            if (showSolutionToggle.isSelected()) {

                showSolutionToggle.setText(
                        "Hide Solution"
                );

                solveAndDisplayPath();

            } else {

                showSolutionToggle.setText(
                        "Show Solution"
                );

                mazeCanvas.setSolvedPath(null);

                updateStatistics(-1);
            }
        });


        // ========================================================
        // EXPORT BUTTON
        // ========================================================

        Button exportBtn = new Button(
                "Export Image"
        );

        exportBtn.setMaxWidth(Double.MAX_VALUE);

        Label exportInfo = createInfoIcon(
                "Exports the currently displayed maze as a PNG image."
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

        exportBox.setAlignment(Pos.CENTER_LEFT);

        exportBtn.setOnAction(e -> {

            FileChooser fileChooser = new FileChooser();

            fileChooser.setTitle("Save Maze Image");

            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            "PNG Image (*.png)",
                            "*.png"
                    )
            );

            fileChooser.setInitialFileName("maze.png");

            File file = fileChooser.showSaveDialog(
                    primaryStage
            );

            if (file != null) {

                WritableImage writableImage =
                        new WritableImage(
                                (int) mazeCanvas.getWidth(),
                                (int) mazeCanvas.getHeight()
                        );

                mazeCanvas.snapshot(
                        null,
                        writableImage
                );

                try {

                    ImageIO.write(
                            SwingFXUtils.fromFXImage(
                                    writableImage,
                                    null
                            ),
                            "png",
                            file
                    );

                } catch (IOException ex) {

                    Alert alert = new Alert(
                            Alert.AlertType.ERROR,
                            "Failed to save image: "
                                    + ex.getMessage(),
                            ButtonType.OK
                    );

                    alert.showAndWait();
                }
            }
        });


        // ========================================================
        // STATISTICS
        // ========================================================

        statsLabel = new Label(
                "Statistics:\n" +
                        "Total Cells: -\n" +
                        "Dead Ends: -\n" +
                        "Path Length: -"
        );

        statsLabel.setStyle(
                "-fx-font-family: monospace; -fx-padding: 10;"
        );

        Label statsInfo = createInfoIcon(
                "Live metrics showing total cells, dead ends and solution path length."
        );

        HBox statsHeaderBox = new HBox(
                5,
                new Label("Maze Metrics"),
                statsInfo
        );


        // ========================================================
        // CONTROL PANEL
        // ========================================================

        VBox controls = new VBox(
                6,

                new HBox(5, shapeHeader, shapeInfo),
                shapeCombo,

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

        controls.setPadding(new Insets(10));

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
        // THEME CONTROL
        // ========================================================

        themeToggle.setOnAction(e -> {

            String stylesheet = getClass()
                    .getResource("/style.css")
                    .toExternalForm();

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


        // ========================================================
        // GENERATE ACTION
        // ========================================================

        generateBtn.setOnAction(e -> {

            int width = (int) widthSlider.getValue();

            int height = (int) heightSlider.getValue();

            double animDelay = speedSlider.getValue();

            double complexityFactor =
                    complexitySlider.getValue() / 100.0;

            String selectedAlgo = algorithmCombo.getValue();

            startGenerationAnimation(
                    width,
                    height,
                    animDelay,
                    complexityFactor,
                    selectedAlgo
            );
        });


        // ========================================================
        // PLAYABLE MODE ACTION
        // ========================================================

        playModeToggle.setOnAction(e -> {

            isPlayMode = playModeToggle.isSelected();

            if (isPlayMode) {

                if (grid == null) {

                    playModeToggle.setSelected(false);

                    isPlayMode = false;

                    return;
                }

                playModeToggle.setText(
                        "Playable Mode: ON"
                );

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

                refreshCircularInteractionHints();

            } else {

                playModeToggle.setText(
                        "Playable Mode: OFF"
                );

                if (movementTimeline != null) {
                    movementTimeline.stop();
                }

                isPlayerMoving = false;

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
        // FOG OF WAR ACTION
        // ========================================================

        fogOfWarToggle.setOnAction(e -> {

            isFogOfWar = fogOfWarToggle.isSelected();

            if (isFogOfWar) {

                fogOfWarToggle.setText(
                        "Fog of War: ON"
                );

                if (grid != null) {

                    for (Cell cell : grid.getAllCells()) {
                        cell.discovered = false;
                    }

                    if (isPlayMode) {

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
        // RECTANGULAR AND POLAR KEYBOARD MOVEMENT
        // ========================================================

        scene.setOnKeyPressed(event -> {

            if (!isPlayMode
                    || isPlayerMoving
                    || grid == null
                    || player.getCurrentCell() == null) {

                return;
            }

            Cell current = player.getCurrentCell();

            Cell target = grid.isCircular()

                    ? findPolarKeyboardNeighbor(
                    current,
                    event.getCode()
            )

                    : findRectangularKeyboardNeighbor(
                    current,
                    event.getCode()
            );

            if (target != null) {

                movePlayerTo(target);

                event.consume();
            }
        });


        // ========================================================
        // CIRCULAR MOUSE HOVER
        // ========================================================

        mazeCanvas.setOnMouseMoved(event -> {

            if (!isPlayMode
                    || isPlayerMoving
                    || grid == null
                    || !grid.isCircular()
                    || player.getCurrentCell() == null) {

                mazeCanvas.setHoveredCell(null);

                return;
            }

            Cell hovered = mazeCanvas.findCellAt(
                    event.getX(),
                    event.getY()
            );

            boolean isReachable =
                    hovered != null
                            && player.getCurrentCell()
                            .isPassageOpenTo(hovered);

            mazeCanvas.setHoveredCell(
                    isReachable ? hovered : null
            );
        });

        mazeCanvas.setOnMouseExited(
                event -> mazeCanvas.setHoveredCell(null)
        );


        // ========================================================
        // CIRCULAR CLICK-TO-MOVE
        // ========================================================

        mazeCanvas.setOnMouseClicked(event -> {

            if (!isPlayMode
                    || isPlayerMoving
                    || grid == null
                    || !grid.isCircular()
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
    // INFO TOOLTIP
    // ============================================================

    private Label createInfoIcon(String tooltipText) {

        Label infoLabel = new Label("ℹ");

        infoLabel.setStyle(
                "-fx-text-fill: #00bcd4; " +
                        "-fx-font-weight: bold; " +
                        "-fx-cursor: hand; " +
                        "-fx-padding: 0 0 0 4;"
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
    // TOPOLOGY-DEPENDENT SLIDERS
    // ============================================================

    private void configureDimensionSliders(
            TopologyType topology,
            Slider widthSlider,
            Slider heightSlider,
            Label widthLabel,
            Label heightLabel,
            Label widthInfo,
            Label heightInfo
    ) {

        boolean circular =
                topology == TopologyType.CIRCULAR;


        // Width / sectors.

        widthSlider.setMin(
                circular ? 8 : 10
        );

        widthSlider.setMax(
                circular ? 24 : 50
        );

        widthSlider.setMajorTickUnit(
                circular ? 4 : 10
        );

        widthSlider.setBlockIncrement(1);

        widthSlider.setSnapToTicks(true);

        widthSlider.setValue(
                circular ? 16 : 20
        );


        // Height / rings.

        heightSlider.setMin(
                circular ? 4 : 10
        );

        heightSlider.setMax(
                circular ? 12 : 50
        );

        heightSlider.setMajorTickUnit(
                circular ? 2 : 10
        );

        heightSlider.setBlockIncrement(1);

        heightSlider.setSnapToTicks(true);

        heightSlider.setValue(
                circular ? 8 : 20
        );


        // Update labels.

        widthLabel.setText(
                (circular
                        ? "Outer Ring Sectors: "
                        : "Grid Width (Cols): ")
                        + (int) widthSlider.getValue()
        );

        heightLabel.setText(
                (circular
                        ? "Number of Rings: "
                        : "Grid Height (Rows): ")
                        + (int) heightSlider.getValue()
        );


        // Update tooltips.

        updateInfoTooltip(
                widthInfo,

                circular
                        ? "Controls the number of sectors in the outermost circular ring (8–24)."
                        : "Defines the number of columns of the rectangular maze (10–50)."
        );

        updateInfoTooltip(
                heightInfo,

                circular
                        ? "Controls the number of concentric rings outside the center (4–12)."
                        : "Defines the number of rows of the rectangular maze (10–50)."
        );
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


    // ============================================================
    // UPDATE CIRCULAR MOVEMENT HIGHLIGHTS
    // ============================================================

    private void refreshCircularInteractionHints() {

        if (grid != null
                && grid.isCircular()
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
    // RECTANGULAR WASD MOVEMENT
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
    // POLAR WASD MOVEMENT
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


        // --------------------------------------------------------
        // CENTRAL CELL
        // --------------------------------------------------------

        if (polar.isCore()) {

            // The center has no angular orientation.
            // W selects the closest open first-ring sector to 12 o'clock.

            if (key != KeyCode.W) {
                return null;
            }

            Cell best = null;

            double bestAngle = Double.POSITIVE_INFINITY;

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


        // --------------------------------------------------------
        // RADIAL TARGET
        // --------------------------------------------------------

        int targetRing = switch (key) {

            case W -> polar.getRingIndex() + 1;

            case S -> polar.getRingIndex() - 1;

            default -> polar.getRingIndex();
        };


        // --------------------------------------------------------
        // ANGULAR TARGET
        // --------------------------------------------------------

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


        // --------------------------------------------------------
        // FIND THE CORRESPONDING OPEN NEIGHBOR
        // --------------------------------------------------------

        Cell best = null;

        double bestAngle = Double.POSITIVE_INFINITY;

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
        // RECTANGULAR MOVEMENT: IMMEDIATE
        // ========================================================

        if (!grid.isCircular()) {

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
        // CIRCULAR MOVEMENT: SMOOTH ANIMATION
        // ========================================================

        if (movementTimeline != null) {
            movementTimeline.stop();
        }

        isPlayerMoving = true;


        // Animation properties.

        DoubleProperty animatedX =
                new SimpleDoubleProperty(
                        from.getScreenX()
                );

        DoubleProperty animatedY =
                new SimpleDoubleProperty(
                        from.getScreenY()
                );


        // Initially place the animated marker at the old cell.

        mazeCanvas.setPlayerScreenPosition(
                from.getScreenX(),
                from.getScreenY()
        );


        // Update actual player position.

        if (!player.moveToCell(target)) {

            isPlayerMoving = false;

            mazeCanvas.clearPlayerScreenPosition();

            return;
        }


        // Update discovery immediately.

        player.updateDiscovery(
                grid,
                isFogOfWar,
                fogRadius
        );


        // Tell the renderer that the player has moved to the target.
        // The temporary screen coordinates control the visual animation.

        mazeCanvas.setPlayerCell(target);

        mazeCanvas.setFogOfWarActive(
                isFogOfWar,
                fogRadius
        );


        // Animation listeners.

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


        // Animate the player marker over 160 milliseconds.

        movementTimeline = new Timeline(

                new KeyFrame(
                        Duration.millis(160),

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


        // Animation completion.

        movementTimeline.setOnFinished(event -> {

            mazeCanvas.clearPlayerScreenPosition();

            isPlayerMoving = false;

            refreshCircularInteractionHints();

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

    private void showVictory() {

        Alert alert = new Alert(
                Alert.AlertType.INFORMATION
        );

        alert.setTitle("Victory!");

        alert.setHeaderText(null);

        alert.setContentText(
                "🎉 Congratulations! You successfully navigated " +
                        "through the maze in " +
                        player.getSteps() +
                        " steps!"
        );

        alert.showAndWait();

        isPlayMode = false;

        playModeToggle.setSelected(false);

        playModeToggle.setText(
                "Playable Mode: OFF"
        );

        if (movementTimeline != null) {
            movementTimeline.stop();
        }

        isPlayerMoving = false;

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
    }


    // ============================================================
    // GENERATION ANIMATION
    // ============================================================

    private void startGenerationAnimation(
            int width,
            int height,
            double animDelay,
            double complexityFactor,
            String algorithm
    ) {

        // Stop previous generation.

        if (animTimeline != null) {
            animTimeline.stop();
        }

        // Stop previous player movement.

        if (movementTimeline != null) {
            movementTimeline.stop();
        }

        isPlayerMoving = false;

        mazeCanvas.clearPlayerScreenPosition();


        // ========================================================
        // SELECT TOPOLOGY
        // ========================================================

        TopologyType topology =
                shapeCombo != null
                        ? shapeCombo.getValue()
                        : TopologyType.RECTANGULAR;


        // ========================================================
        // SELECT RENDERER
        // ========================================================

        switch (topology) {

            case RECTANGULAR ->

                    mazeCanvas.setRenderer(
                            new RectangularRenderer()
                    );

            case HEXAGONAL ->

                    mazeCanvas.setRenderer(
                            new HexagonalRenderer()
                    );

            case CIRCULAR ->

                    mazeCanvas.setRenderer(
                            new CircularRenderer()
                    );

            case TRIANGULAR ->

                    mazeCanvas.setRenderer(
                            new TriangularRenderer()
                    );
        }


        // ========================================================
        // CREATE CORRECT GRID
        // ========================================================

        if (topology == TopologyType.CIRCULAR) {

            // Width = outer ring sectors.
            // Height = number of concentric rings.

            grid = new MazeGrid(
                    width,
                    height,
                    TopologyType.CIRCULAR
            );

        } else {

            // Existing rectangular grid construction.

            grid = new MazeGrid(
                    width,
                    height
            );
        }


        // ========================================================
        // CANVAS DIMENSIONS
        // ========================================================

        double maxDimension = 600.0;

        double cellDim = maxDimension
                / Math.max(width, height);


        switch (topology) {

            case RECTANGULAR -> {

                mazeCanvas.setWidth(
                        width * cellDim
                );

                mazeCanvas.setHeight(
                        height * cellDim
                );

                for (Cell cell : grid.getAllCells()) {

                    cell.screenX =
                            (cell.x + 0.5) * cellDim;

                    cell.screenY =
                            (cell.y + 0.5) * cellDim;
                }
            }


            case CIRCULAR -> {

                mazeCanvas.setWidth(maxDimension);

                mazeCanvas.setHeight(maxDimension);

                // Assign correct pixel coordinates to every polar cell.

                grid.layoutCircularGrid(
                        maxDimension,
                        maxDimension,
                        18.0
                );
            }


            case HEXAGONAL -> {

                mazeCanvas.setWidth(
                        width * cellDim
                );

                mazeCanvas.setHeight(
                        height * cellDim
                );
            }


            case TRIANGULAR -> {

                mazeCanvas.setWidth(
                        width * cellDim
                );

                mazeCanvas.setHeight(
                        height * cellDim
                );
            }
        }


        // ========================================================
        // INITIALIZE CELL STATES
        // ========================================================

        for (Cell cell : grid.getAllCells()) {
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


                        // Apply complexity BEFORE selecting the openings.
                        // Entrance and exit distance therefore accounts
                        // for the final maze passages.

                        grid.applyComplexityFactor(
                                complexityFactor
                        );


                        // =================================================
                        // CREATE ENTRANCE AND EXIT
                        // =================================================

                        switch (topology) {

                            case RECTANGULAR ->

                                    grid.createRectangularOpenings();

                            case CIRCULAR ->

                                    grid.createCircularOpenings();

                            case HEXAGONAL,
                                 TRIANGULAR -> {
                                // These topology-specific openings are
                                // outside the circular implementation.
                            }
                        }


                        mazeCanvas.setCurrentHead(null);


                        // =================================================
                        // SOLUTION
                        // =================================================

                        if (showSolutionToggle.isSelected()) {

                            solveAndDisplayPath();

                        } else {

                            updateStatistics(-1);
                        }


                        // =================================================
                        // SPAWN PLAYER IF PLAY MODE IS ACTIVE
                        // =================================================

                        if (isPlayMode) {

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

                            refreshCircularInteractionHints();
                        }
                    }
                }
        );


        animTimeline.getKeyFrames().add(keyFrame);

        animTimeline.setCycleCount(
                Timeline.INDEFINITE
        );

        animTimeline.play();
    }


    // ============================================================
    // SOLVE AND DISPLAY PATH
    // ============================================================

    private void solveAndDisplayPath() {

        if (grid == null) {
            return;
        }

        String selectedSolver = solverCombo.getValue();

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
    // UPDATE STATISTICS
    // ============================================================

    private void updateStatistics(int pathLength) {

        if (grid == null) {
            return;
        }

        int totalCells =
                grid.getAllCells().size();

        int deadEnds =
                grid.countDeadEnds();

        String pathString = pathLength > 0

                ? pathLength + " steps"

                : "Hidden / Not solved";


        statsLabel.setText(
                String.format(
                        "Statistics:\n" +
                                "Total Cells: %d\n" +
                                "Dead Ends: %d\n" +
                                "Path Length: %s",

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
