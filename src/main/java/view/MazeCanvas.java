
package view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import model.Cell;
import model.MazeGrid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Canvas facade that delegates drawing and topology-specific hit testing. */
public class MazeCanvas extends Canvas {

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
        super(width, height);
        draw();
    }

    public void setRenderer(MazeRenderer renderer) {
        if (renderer == null) {
            throw new IllegalArgumentException(
                    "Renderer cannot be null."
            );
        }

        this.renderer = renderer;

        renderer.setDarkMode(darkMode);
        renderer.clearPlayerScreenPosition();
        renderer.setHighlightedCells(highlightedCells);
        renderer.setHoveredCell(hoveredCell);
    }

    public void setGrid(MazeGrid grid) {
        this.grid = grid;

        highlightedCells = Collections.emptyList();
        hoveredCell = null;

        renderer.setHighlightedCells(highlightedCells);
        renderer.setHoveredCell(null);
        renderer.clearPlayerScreenPosition();

        draw();
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
        this.fogRadius = radius;
        draw();
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        renderer.setDarkMode(darkMode);
        draw();
    }

    /** Highlights the legal open neighbors of the current player cell. */
    public void setHighlightedCells(Collection<Cell> cells) {
        highlightedCells = cells == null
                ? Collections.emptyList()
                : new ArrayList<>(cells);

        renderer.setHighlightedCells(highlightedCells);

        draw();
    }

    public void setHoveredCell(Cell cell) {
        hoveredCell = cell;
        renderer.setHoveredCell(cell);
        draw();
    }

    /** Used while the player marker is between two cell centers. */
    public void setPlayerScreenPosition(double x, double y) {
        renderer.setPlayerScreenPosition(x, y);
        draw();
    }

    public void clearPlayerScreenPosition() {
        renderer.clearPlayerScreenPosition();
        draw();
    }

    /** Returns the cell underneath the mouse, if supported by the renderer. */
    public Cell findCellAt(double x, double y) {
        return renderer.findCellAt(
                x,
                y,
                getWidth(),
                getHeight(),
                grid
        );
    }

    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();

        renderer.render(
                gc,
                getWidth(),
                getHeight(),
                grid,
                playerCell,
                solvedPath,
                currentHead,
                fogOfWarActive,
                fogRadius
        );
    }
}
