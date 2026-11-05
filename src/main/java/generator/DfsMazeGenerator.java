package generator;

import model.Cell;
import model.MazeGrid;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Recursive Backtracker (iterative DFS) with robust loop limits and null safety. */
public class DfsMazeGenerator implements MazeGenerator {
    private static final Logger LOGGER = Logger.getLogger(DfsMazeGenerator.class.getName());
    private static final int MAX_STEPS = 5_000_000;

    private MazeGrid grid;
    private Stack<Cell> stack;
    private Random random;
    private Cell currentHead;
    private int totalSteps;

    @Override
    public void initialize(MazeGrid grid, Random random) {
        if (grid == null || grid.getAllCells() == null || grid.getAllCells().isEmpty()) {
            throw new IllegalArgumentException("A non-empty maze grid is required.");
        }

        this.grid = grid;
        this.stack = new Stack<>();
        this.random = random != null ? random : new Random();
        this.totalSteps = 0;

        for (Cell cell : grid.getAllCells()) {
            if (cell != null) {
                cell.visited = false;
            }
        }

        Cell startCell = grid.getAllCells().get(0);
        if (startCell != null) {
            startCell.visited = true;
            stack.push(startCell);
            currentHead = startCell;
        }
    }

    @Override
    public boolean step() {
        if (stack.isEmpty()) {
            currentHead = null;
            return false;
        }

        if (++totalSteps > MAX_STEPS) {
            LOGGER.warning("DFS generator exceeded maximum step limit.");
            currentHead = null;
            return false;
        }

        try {
            Cell current = stack.peek();
            currentHead = current;
            List<Cell> unvisitedNeighbors = new ArrayList<>();

            List<Cell> adj = current.getAdjacentCells();
            if (adj != null) {
                for (Cell neighbor : adj) {
                    if (neighbor != null && !neighbor.visited) {
                        unvisitedNeighbors.add(neighbor);
                    }
                }
            }

            if (!unvisitedNeighbors.isEmpty()) {
                Cell next = unvisitedNeighbors.get(random.nextInt(unvisitedNeighbors.size()));
                grid.removeWalls(current, next);
                next.visited = true;
                stack.push(next);
            } else {
                stack.pop();
            }
            return !stack.isEmpty();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error during DFS generator step: " + ex.getMessage(), ex);
            currentHead = null;
            return false;
        }
    }

    @Override
    public Cell getCurrentHead() {
        return currentHead;
    }
}