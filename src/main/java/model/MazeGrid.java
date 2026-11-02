package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Map;

/**
 * Represents the topological maze grid structure.
 */
public class MazeGrid {
    public final int cols;
    public final int rows;
    public Cell[][] cells;
    private final List<Cell> allCells;

    public MazeGrid(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        this.allCells = new ArrayList<>();

        // Initialize layout (can later be delegated based on shape/topology)
        initializeRectangularGrid();
    }

    public void initializeRectangularGrid() {
        this.cells = new Cell[cols][rows];
        this.allCells.clear();

        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                cells[x][y] = new Cell(x, y);
                allCells.add(cells[x][y]);
            }
        }

        // Establish standard orthogonal grid adjacencies
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                Cell current = cells[x][y];

                if (x > 0) current.addNeighbor(cells[x - 1][y]);
                if (x < cols - 1) current.addNeighbor(cells[x + 1][y]);
                if (y > 0) current.addNeighbor(cells[x][y - 1]);
                if (y < rows - 1) current.addNeighbor(cells[x][y + 1]);
            }
        }
    }

    /**
     * Safely retrieves a cell at the given coordinates.
     */
    public Cell getCell(int x, int y) {
        if (x < 0 || x >= cols || y < 0 || y >= rows) {
            return null;
        }
        return cells[x][y];
    }

    public List<Cell> getAllCells() {
        return allCells;
    }

    /**
     * Removes the wall between two adjacent cells.
     */
    public void removeWalls(Cell a, Cell b) {
        if (a == null || b == null) return;

        // Handle standard orthogonal grid wall removal
        if (a.x == b.x) {
            if (a.y == b.y + 1) { // b is above a
                a.walls[0] = false;
                b.walls[2] = false;
            } else if (a.y == b.y - 1) { // b is below a
                a.walls[2] = false;
                b.walls[0] = false;
            }
        } else if (a.y == b.y) {
            if (a.x == b.x + 1) { // b is to the left of a
                a.walls[3] = false;
                b.walls[1] = false;
            } else if (a.x == b.x - 1) { // b is to the right of a
                a.walls[1] = false;
                b.walls[3] = false;
            }
        }

        // Also update graph neighbor map references
        a.getNeighbors().put(b, true);
        b.getNeighbors().put(a, true);
    }

    /**
     * Creates default entrance and exit openings for a rectangular grid.
     */
    public void createRectangularOpenings() {
        Random rand = new Random();

        // Entrance on the left edge (column 0)
        int entranceY = rand.nextInt(rows);
        Cell entranceCell = getCell(0, entranceY);
        if (entranceCell != null) {
            entranceCell.walls[3] = false; // Left wall
            entranceCell.isEntrance = true;
            entranceCell.discovered = true;
        }

        // Exit on the right edge (column cols - 1)
        int exitY = rand.nextInt(rows);
        Cell exitCell = getCell(cols - 1, exitY);
        if (exitCell != null) {
            exitCell.walls[1] = false; // Right wall
            exitCell.isExit = true;
        }
    }

    /**
     * Counts how many cells act as dead ends in the grid.
     */
    public int countDeadEnds() {
        int deadEnds = 0;
        for (Cell cell : allCells) {
            int openConnections = 0;
            for (boolean isOpen : cell.getNeighbors().values()) {
                if (isOpen) openConnections++;
            }
            if (openConnections == 1) deadEnds++;
        }
        return deadEnds;
    }

    /**
     * Applies the complexity factor by carving loops into dead-end cells.
     */
    public void applyComplexityFactor(double complexityFactor) {
        Random rand = new Random();
        double removalChance = 1.0 - complexityFactor;

        for (Cell cell : allCells) {
            int openConnections = 0;
            for (boolean isOpen : cell.getNeighbors().values()) {
                if (isOpen) openConnections++;
            }

            // If it's a dead end (1 open connection) and hits the random chance
            if (openConnections == 1 && rand.nextDouble() < removalChance) {
                // Find closed neighbors to carve a loop into
                List<Cell> closedNeighbors = new ArrayList<>();
                for (Map.Entry<Cell, Boolean> entry : cell.getNeighbors().entrySet()) {
                    if (!entry.getValue()) { // if wall is still closed
                        closedNeighbors.add(entry.getKey());
                    }
                }

                if (!closedNeighbors.isEmpty()) {
                    removeWalls(cell, closedNeighbors.get(rand.nextInt(closedNeighbors.size())));
                }
            }
        }
    }
}