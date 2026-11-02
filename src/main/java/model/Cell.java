package model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an individual cell/node in the maze grid, supporting
 * both traditional orthogonal grid states and graph-based adjacencies.
 */
public class Cell {
    public int x, y;
    public double screenX, screenY;
    // Indices: 0 = Top, 1 = Right, 2 = Bottom, 3 = Left
    public boolean[] walls = {true, true, true, true};
    public boolean visited = false;
    public boolean discovered = false;
    public boolean isEntrance = false;
    public boolean isExit = false;

    // Graph-based neighbors for multi-shape geometries
    private final Map<Cell, Boolean> neighbors = new HashMap<>();

    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public double getScreenX() {
        return screenX;
    }

    public double getScreenY() {
        return screenY;
    }

    public void addNeighbor(Cell neighbor) {
        neighbors.putIfAbsent(neighbor, false);
    }

    public Map<Cell, Boolean> getNeighbors() {
        return neighbors;
    }
}