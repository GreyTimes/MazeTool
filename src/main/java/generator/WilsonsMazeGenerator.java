package generator;

import model.Cell;
import model.MazeGrid;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Wilson's generator with step-limit guardrails and loop-erasure safety. */
public class WilsonsMazeGenerator implements MazeGenerator {
    private static final Logger LOGGER = Logger.getLogger(WilsonsMazeGenerator.class.getName());
    private static final int MAX_STEPS = 10_000_000;

    private MazeGrid grid;
    private final List<Cell> unvisited = new ArrayList<>();
    private final List<Cell> walkPath = new ArrayList<>();
    private Random random;
    private boolean building;
    private int totalSteps;

    @Override
    public void initialize(MazeGrid grid, Random random) {
        if (grid == null || grid.getAllCells() == null || grid.getAllCells().isEmpty()) {
            throw new IllegalArgumentException("A non-empty maze grid is required.");
        }

        this.grid = grid;
        unvisited.clear();
        walkPath.clear();
        this.totalSteps = 0;
        this.random = random != null ? random : new Random();

        for (Cell cell : grid.getAllCells()) {
            if (cell != null) {
                cell.visited = false;
                unvisited.add(cell);
            }
        }

        if (!unvisited.isEmpty()) {
            Cell first = unvisited.remove(random.nextInt(unvisited.size()));
            first.visited = true;
        }
        building = false;
    }

    @Override
    public boolean step() {
        if (unvisited.isEmpty()) {
            building = false;
            walkPath.clear();
            return false;
        }

        if (++totalSteps > MAX_STEPS) {
            LOGGER.warning("Wilson's generator exceeded maximum step limit.");
            building = false;
            walkPath.clear();
            return false;
        }

        try {
            if (!building) {
                walkPath.clear();
                walkPath.add(unvisited.get(random.nextInt(unvisited.size())));
                building = true;
            }

            Cell current = walkPath.get(walkPath.size() - 1);
            if (current.visited) {
                for (int i = 0; i < walkPath.size() - 1; i++) {
                    Cell a = walkPath.get(i);
                    Cell b = walkPath.get(i + 1);
                    if (a != null && b != null) {
                        grid.removeWalls(a, b);
                        a.visited = true;
                        unvisited.remove(a);
                    }
                }

                walkPath.clear();
                building = false;
                return !unvisited.isEmpty();
            }

            List<Cell> neighbors = current.getAdjacentCells();
            if (neighbors == null || neighbors.isEmpty()) {
                throw new IllegalStateException("The maze graph contains an isolated cell.");
            }

            Cell next = neighbors.get(random.nextInt(neighbors.size()));
            if (next != null) {
                int loopStart = walkPath.indexOf(next);
                if (loopStart >= 0) {
                    while (walkPath.size() > loopStart + 1) {
                        walkPath.remove(walkPath.size() - 1);
                    }
                } else {
                    walkPath.add(next);
                }
            }
            return true;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error during Wilson's step: " + ex.getMessage(), ex);
            building = false;
            walkPath.clear();
            return false;
        }
    }

    @Override
    public Cell getCurrentHead() {
        return walkPath.isEmpty() ? null : walkPath.get(walkPath.size() - 1);
    }
}