package view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import model.Cell;
import model.MazeGrid;
import model.MazeThemeConfig;
import model.TopologyType;

import java.util.List;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class RectangularRenderer implements MazeRenderer {

    private boolean darkMode = true;
    private MazeThemeConfig themeConfig = new MazeThemeConfig(MazeThemeConfig.ThemePreset.DEFAULT_DARK);

    @Override
    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        themeConfig.setDarkMode(darkMode);
    }

    @Override
    public void setThemeConfig(MazeThemeConfig config) {
        if (config != null) {
            this.themeConfig = config;
        }
    }

    @Override
    public Cell findCellAt(double x, double y, double width, double height, MazeGrid grid) {
        if (grid == null || grid.getTopology() != TopologyType.RECTANGULAR || x < 0 || y < 0 || x >= width || y >= height) {
            return null;
        }
        double cellWidth = width / grid.cols;
        double cellHeight = height / grid.rows;
        int column = (int) (x / cellWidth);
        int row = (int) (y / cellHeight);
        return grid.getCell(column, row);
    }


    @Override
    public void render(
            GraphicsContext gc,
            double width,
            double height,
            MazeGrid grid,
            Cell playerCell,
            List<Cell> solvedPath,
            Cell currentHead,
            boolean fogOfWarActive,
            int fogRadius
    ) {

        // COLORS FROM THE THEME CONFIGURATION

        Color backgroundColor = themeConfig.getBackgroundColor();
        Color floorColor = themeConfig.getFloorColor();
        Color wallColor = themeConfig.getWallColor();

        Color pathColor = themeConfig.getPathColor();
        Color playerColor = themeConfig.getPlayerColor();
        Color headColor = themeConfig.getHeadColor();

        Color entranceColor = themeConfig.getEntranceColor();
        Color exitColor = themeConfig.getExitColor();

        // FOG COLORS

        Color fogColor = darkMode
                ? Color.web("#111111")
                : Color.web("#d0d0d0");

        Color exploredDimColor = darkMode
                ? Color.web("#2a2a2a")
                : Color.web("#e0e0e0");

        // CANVAS BACKGROUND

        gc.setFill(backgroundColor);
        gc.fillRect(0, 0, width, height);

        if (grid == null || grid.getTopology() != TopologyType.RECTANGULAR) {
            return;
        }

        double cellWidth = width / grid.cols;
        double cellHeight = height / grid.rows;

        Set<Cell> solution = solvedPath == null
                ? Collections.emptySet()
                : new HashSet<>(solvedPath);

        // CELL BACKGROUNDS AND SOLUTION

        for (int x = 0; x < grid.cols; x++) {
            for (int y = 0; y < grid.rows; y++) {

                Cell cell = grid.cells[x][y];

                boolean visible = !fogOfWarActive || cell.discovered;

                boolean inRadius =
                        fogOfWarActive
                                && playerCell != null
                                && (
                                Math.abs(cell.x - playerCell.x)
                                        + Math.abs(cell.y - playerCell.y)
                                        <= fogRadius
                        );

                double px = x * cellWidth;
                double py = y * cellHeight;

                if (!visible) {
                    gc.setFill(fogColor);

                    gc.fillRect(
                            px,
                            py,
                            cellWidth,
                            cellHeight
                    );

                    continue;
                }

                // NORMAL FLOOR

                gc.setFill(floorColor);

                gc.fillRect(
                        px,
                        py,
                        cellWidth,
                        cellHeight
                );

                // SOLUTION HIGHLIGHT

                if (solution.contains(cell)
                        && !cell.isEntrance
                        && !cell.isExit) {

                    gc.setFill(pathColor);

                    gc.fillRect(
                            px + cellWidth * 0.2,
                            py + cellHeight * 0.2,
                            cellWidth * 0.6,
                            cellHeight * 0.6
                    );
                }

                // EXPLORED BUT OUTSIDE PLAYER RADIUS

                if (fogOfWarActive && !inRadius) {
                    gc.setFill(exploredDimColor);

                    gc.fillRect(
                            px,
                            py,
                            cellWidth,
                            cellHeight
                    );
                }

                // ENTRANCE AND EXIT

                if (cell.isEntrance) {
                    gc.setFill(entranceColor);
                    gc.fillRect(px, py, cellWidth, cellHeight);

                } else if (cell.isExit) {
                    gc.setFill(exitColor);
                    gc.fillRect(px, py, cellWidth, cellHeight);
                }
            }
        }

        // GENERATION HEAD

        if (currentHead != null) {
            gc.setFill(headColor);

            gc.fillRect(
                    currentHead.x * cellWidth + cellWidth * 0.1,
                    currentHead.y * cellHeight + cellHeight * 0.1,
                    cellWidth * 0.8,
                    cellHeight * 0.8
            );
        }

        // PLAYER

        if (playerCell != null) {
            gc.setFill(playerColor);

            gc.fillOval(
                    playerCell.x * cellWidth + cellWidth * 0.15,
                    playerCell.y * cellHeight + cellHeight * 0.15,
                    cellWidth * 0.7,
                    cellHeight * 0.7
            );
        }

        // WALLS

        gc.setStroke(wallColor);

        double baseWallWidth = Math.max(
                1.5,
                Math.min(cellWidth, cellHeight) / 10.0
        );

        MazeRenderSupport.configureWallStroke(
                gc,
                themeConfig,
                baseWallWidth * themeConfig.getWallWidthMultiplier()
        );

        for (int x = 0; x < grid.cols; x++) {
            for (int y = 0; y < grid.rows; y++) {

                Cell cell = grid.cells[x][y];

                if (fogOfWarActive && !cell.discovered) {
                    continue;
                }

                double px = x * cellWidth;
                double py = y * cellHeight;

                if (cell.walls[0]) {
                    gc.strokeLine(
                            px, py,
                            px + cellWidth, py
                    );
                }

                if (cell.walls[1]) {
                    gc.strokeLine(
                            px + cellWidth, py,
                            px + cellWidth, py + cellHeight
                    );
                }

                if (cell.walls[2]) {
                    gc.strokeLine(
                            px, py + cellHeight,
                            px + cellWidth, py + cellHeight
                    );
                }

                if (cell.walls[3]) {
                    gc.strokeLine(
                            px, py,
                            px, py + cellHeight
                    );
                }
            }
        }

        // ENTRANCE / EXIT MARKERS

        double markerDiameter = Math.max(
                6.0,
                Math.min(13.0, Math.min(cellWidth, cellHeight) * 0.58)
        );

        for (Cell cell : grid.getAllCells()) {

            if (fogOfWarActive && !cell.discovered) {
                continue;
            }

            double centerX = (cell.x + 0.5) * cellWidth;
            double centerY = (cell.y + 0.5) * cellHeight;

            if (cell.isEntrance) {

                EndpointMarkerRenderer.draw(
                        gc,
                        centerX,
                        centerY,
                        markerDiameter,
                        entranceColor,
                        "S"
                );

            } else if (cell.isExit) {

                EndpointMarkerRenderer.draw(
                        gc,
                        centerX,
                        centerY,
                        markerDiameter,
                        exitColor,
                        "E"
                );
            }
        }
    }

}