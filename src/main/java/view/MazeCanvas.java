package view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import model.Cell;
import model.MazeGrid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Canvas facade shared by rectangular, polar, and polygonal renderers with robust error guarding.
 */
public class MazeCanvas extends Canvas {
    private static final Logger LOGGER = Logger.getLogger(MazeCanvas.class.getName());

    private MazeGrid grid;
    private List<Cell> solvedPath;
    private Cell currentHead;
    private Cell playerCell;
    private boolean fogOfWarActive;
    private int fogRadius = 2;
    private boolean darkMode = true;
    private List<Cell> highlightedCells = Collections.emptyList();
    private Cell hoveredCell;
    private MazeRenderer renderer = new RectangularRenderer();

    public MazeCanvas(double width, double height) {
        super(Math.max(1.0, width), Math.max(1.0, height));
        draw();
    }

    public void setRenderer(MazeRenderer renderer) {
        if (renderer == null) {
            LOGGER.warning("Attempted to set a null renderer; keeping existing renderer.");
            return;
        }
        this.renderer = renderer;
        renderer.setDarkMode(darkMode);
        renderer.setHighlightedCells(highlightedCells);
        renderer.setHoveredCell(hoveredCell);
        renderer.clearPlayerScreenPosition();
    }

    public void setGrid(MazeGrid grid) {
        this.grid = grid;
        highlightedCells = Collections.emptyList();
        hoveredCell = null;
        if (renderer != null) {
            renderer.setHighlightedCells(highlightedCells);
            renderer.setHoveredCell(null);
            renderer.clearPlayerScreenPosition();
        }
        draw();
    }

    public MazeGrid getGrid() {
        return grid;
    }

    public void setSolvedPath(List<Cell> solvedPath) {
        this.solvedPath = solvedPath;
        draw();
    }

    public void setCurrentHead(Cell currentHead) {
        this.currentHead = currentHead;
        draw();
    }

    public void setPlayerCell(Cell playerCell) {
        this.playerCell = playerCell;
        draw();
    }

    public void setFogOfWarActive(boolean active, int radius) {
        this.fogOfWarActive = active;
        this.fogRadius = Math.max(1, radius);
        draw();
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        if (renderer != null) {
            renderer.setDarkMode(darkMode);
        }
        draw();
    }

    // INTERACTION

    public void setHighlightedCells(Collection<Cell> cells) {
        highlightedCells = cells == null ? Collections.emptyList() : new ArrayList<>(cells);
        if (renderer != null) {
            renderer.setHighlightedCells(highlightedCells);
        }
        draw();
    }

    public void setHoveredCell(Cell cell) {
        hoveredCell = cell;
        if (renderer != null) {
            renderer.setHoveredCell(cell);
        }
        draw();
    }

    public void setPlayerScreenPosition(double x, double y) {
        if (renderer != null) {
            renderer.setPlayerScreenPosition(x, y);
        }
        draw();
    }

    public void clearPlayerScreenPosition() {
        if (renderer != null) {
            renderer.clearPlayerScreenPosition();
        }
        draw();
    }

    public Cell findCellAt(double x, double y) {
        if (renderer == null || grid == null) {
            return null;
        }
        try {
            return renderer.findCellAt(x, y, Math.max(1.0, getWidth()), Math.max(1.0, getHeight()), grid);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error finding cell at coordinates (" + x + ", " + y + "): " + ex.getMessage());
            return null;
        }
    }

    // DRAW

    public void draw() {
        try {
            GraphicsContext gc = getGraphicsContext2D();
            if (gc == null || renderer == null) {
                return;
            }

            double w = Math.max(1.0, getWidth());
            double h = Math.max(1.0, getHeight());

            renderer.render(
                    gc,
                    w,
                    h,
                    grid,
                    playerCell,
                    solvedPath,
                    currentHead,
                    fogOfWarActive,
                    fogRadius
            );
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unexpected rendering exception in MazeCanvas: " + ex.getMessage(), ex);
        }
    }

    public void resizeCanvas(double width, double height) {
        this.setWidth(width);
        this.setHeight(height);
        draw();
    }
}