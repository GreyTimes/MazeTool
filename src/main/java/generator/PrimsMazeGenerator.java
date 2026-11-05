package generator;

import model.Cell;
import model.MazeGrid;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Randomized Prim's algorithm with frontier boundary safety. */
public class PrimsMazeGenerator implements MazeGenerator {
    private static final Logger LOGGER = Logger.getLogger(PrimsMazeGenerator.class.getName());
    private static final int MAX_STEPS = 5_000_000;

    private MazeGrid grid;
    private List<Cell> frontier;
    private Random random;
    private Cell currentHead;
    private int totalSteps;

    @Override
    public void initialize(MazeGrid grid, Random random) {
        if (grid == null || grid.getAllCells() == null || grid.getAllCells().isEmpty()) {
            throw new IllegalArgumentException("A non-empty maze grid is required.");
        }

        this.grid = grid;
        this.frontier = new ArrayList<>();
        this.random = random != null ? random : new Random();
        this.totalSteps = 0;

        List<Cell> cells = grid.getAllCells();
        for (Cell cell : cells) {
            if (cell != null) {
                cell.visited = false;
            }
        }

        Cell start = cells.get(random.nextInt(cells.size()));
        if (start != null) {
            start.visited = true;
            addFrontierCells(start);
            currentHead = start;
        }
    }

    @Override
    public boolean step() {
        if (frontier == null || frontier.isEmpty()) {
            currentHead = null;
            return false;
        }

        if (++totalSteps > MAX_STEPS) {
            LOGGER.warning("Prim's generator exceeded maximum step limit.");
            currentHead = null;
            return false;
        }

        try {
            Cell current = frontier.remove(random.nextInt(frontier.size()));
            currentHead = current;
            List<Cell> visitedNeighbors = new ArrayList<>();

            List<Cell> adj = current.getAdjacentCells();
            if (adj != null) {
                for (Cell neighbor : adj) {
                    if (neighbor != null && neighbor.visited) {
                        visitedNeighbors.add(neighbor);
                    }
                }
            }

            if (!visitedNeighbors.isEmpty()) {
                Cell connection = visitedNeighbors.get(random.nextInt(visitedNeighbors.size()));
                grid.removeWalls(current, connection);
                current.visited = true;
                addFrontierCells(current);
            }

            if (frontier.isEmpty()) {
                currentHead = null;
            }

            return !frontier.isEmpty();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error during Prim's step: " + ex.getMessage(), ex);
            currentHead = null;
            return false;
        }
    }

    private void addFrontierCells(Cell cell) {
        if (cell == null) return;
        List<Cell> adj = cell.getAdjacentCells();
        if (adj != null) {
            for (Cell neighbor : adj) {
                if (neighbor != null && !neighbor.visited && !frontier.contains(neighbor)) {
                    frontier.add(neighbor);
                }
            }
        }
    }

    @Override
    public Cell getCurrentHead() {
        return currentHead;
    }
}