package view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import model.Cell;
import model.MazeGrid;

import java.util.List;

public class MazeCanvas extends Canvas {
    private MazeGrid grid;
    private List<Cell> solvedPath;
    private Cell currentHead;
    private Cell playerCell;
    private boolean fogOfWarActive = false;
    private int fogRadius = 2;

    // Abstracted renderer interface with default rectangular implementation
    private MazeRenderer renderer = new RectangularRenderer();

    public MazeCanvas(double width, double height) {
        super(width, height);
        draw();
    }

    public void setRenderer(MazeRenderer renderer) {
        this.renderer = renderer;
        draw();
    }

    public void setGrid(MazeGrid grid) {
        this.grid = grid;
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
        renderer.setDarkMode(darkMode);
        draw();
    }

    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        renderer.render(gc, getWidth(), getHeight(), grid, playerCell, solvedPath, currentHead, fogOfWarActive, fogRadius);
    }
}