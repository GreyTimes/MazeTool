package model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A maze cell represented as a graph node.
 * The four wall flags are retained for the rectangular renderer and for
 * backwards compatibility. Graph adjacency is the source of truth for
 * generators, solvers, and gameplay across every topology.
 */
public class Cell {
    public int x;
    public int y;
    public double screenX;
    public double screenY;

    // Rectangular wall indices: 0 = Top, 1 = Right, 2 = Bottom, 3 = Left.
    public boolean[] walls = {true, true, true, true};

    public boolean visited;
    public boolean discovered;
    public boolean isEntrance;
    public boolean isExit;

    // Insertion order keeps animation and path traversal visually stable.
    private final Map<Cell, Boolean> neighbors = new LinkedHashMap<>();

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

    /** Adds a closed graph adjacency. MazeGrid normally calls this symmetrically. */
    public void addNeighbor(Cell neighbor) {
        if (neighbor != null && neighbor != this) {
            neighbors.putIfAbsent(neighbor, false);
        }
    }

    /** Returns the neighboring cells and whether each connecting edge is open. */
    public Map<Cell, Boolean> getNeighbors() {
        return neighbors;
    }

    /** Returns all topological neighbors, regardless of whether their edge is open. */
    public List<Cell> getAdjacentCells() {
        return new ArrayList<>(neighbors.keySet());
    }

    /** Returns only neighbors reachable through an open passage. */
    public List<Cell> getOpenNeighbors() {
        List<Cell> result = new ArrayList<>();
        for (Map.Entry<Cell, Boolean> entry : neighbors.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public boolean isPassageOpenTo(Cell neighbor) {
        return Boolean.TRUE.equals(neighbors.get(neighbor));
    }

    public int getOpenDegree() {
        int degree = 0;
        for (boolean open : neighbors.values()) {
            if (open) degree++;
        }
        return degree;
    }
}