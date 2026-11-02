package game;

import model.Cell;
import model.MazeGrid;

public class Player {
    private Cell currentCell;
    private int steps = 0;

    public void spawn(MazeGrid grid) {
        steps = 1;
        currentCell = null;
        // Shape-agnostic: search all cells for the designated entrance
        for (Cell c : grid.getAllCells()) {
            if (c.isEntrance) {
                currentCell = c;
                break;
            }
        }
    }

    // New graph-based movement method that replaces dx, dy, and wallIndex
    public boolean moveToCell(Cell nextCell) {
        if (nextCell != null) {
            currentCell = nextCell;
            steps++;
            return true;
        }
        return false;
    }

    // Helper to reveal cells around the player based on fog radius (shape-agnostic)
    public void updateDiscovery(MazeGrid grid, boolean fogActive, int fogRadius) {
        if (grid == null || currentCell == null) return;

        for (Cell c : grid.getAllCells()) {
            if (!fogActive) {
                c.discovered = true;
            } else {
                // Using coordinate difference or graph distance.
                // For a general grid, Euclidean/Manhattan works if x/y are projected,
                // but looping through all cells and checking graph distance is even safer.
                double dist = Math.abs(c.x - currentCell.x) + Math.abs(c.y - currentCell.y);
                if (dist <= fogRadius) {
                    c.discovered = true;
                }
            }
        }
    }

    public Cell getCurrentCell() { return currentCell; }
    public int getSteps() { return steps; }
    public void reset() {
        currentCell = null;
        steps = 0;
    }
}