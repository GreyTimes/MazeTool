package generator;

import model.Cell;
import model.MazeGrid;

import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Aldous-Broder uniform spanning-tree generator with safety step limits. */
public class AldousBroderMazeGenerator implements MazeGenerator {
    private static final Logger LOGGER = Logger.getLogger(AldousBroderMazeGenerator.class.getName());
    private static final int MAX_STEPS = 5_000_000;

    private MazeGrid grid;
    private Cell current;
    private int unvisitedCount;
    private int totalSteps;
    private final Random random = new Random();

    @Override
    public void initialize(MazeGrid grid) {
        if (grid == null || grid.getAllCells() == null || grid.getAllCells().isEmpty()) {
            throw new IllegalArgumentException("A non-empty maze grid is required.");
        }

        this.grid = grid;
        this.totalSteps = 0;
        for (Cell cell : grid.getAllCells()) {
            if (cell != null) {
                cell.visited = false;
            }
        }

        List<Cell> cells = grid.getAllCells();
        this.unvisitedCount = cells.size();
        this.current = cells.get(random.nextInt(cells.size()));
        current.visited = true;
        unvisitedCount--;
    }

    @Override
    public boolean step() {
        if (unvisitedCount <= 0 || current == null) {
            return false;
        }

        if (++totalSteps > MAX_STEPS) {
            LOGGER.warning("Aldous-Broder generator exceeded maximum step limit.");
            return false;
        }

        try {
            List<Cell> neighbors = current.getAdjacentCells();
            if (neighbors == null || neighbors.isEmpty()) {
                throw new IllegalStateException("The maze graph contains an isolated cell.");
            }

            Cell next = neighbors.get(random.nextInt(neighbors.size()));
            if (next != null) {
                if (!next.visited) {
                    grid.removeWalls(current, next);
                    next.visited = true;
                    unvisitedCount--;
                }
                current = next;
            }
            return unvisitedCount > 0;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error during Aldous-Broder step: " + ex.getMessage(), ex);
            return false;
        }
    }

    @Override
    public Cell getCurrentHead() {
        return current;
    }
}