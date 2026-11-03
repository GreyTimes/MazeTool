package generator;

import model.Cell;
import model.MazeGrid;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

/** Recursive Backtracker (iterative DFS) over graph neighbors. */
public class DfsMazeGenerator implements MazeGenerator {
    private MazeGrid grid;
    private Stack<Cell> stack;
    private Random random;
    private Cell currentHead;

    @Override
    public void initialize(MazeGrid grid) {
        if (grid == null || grid.getAllCells().isEmpty()) {
            throw new IllegalArgumentException("A non-empty maze grid is required.");
        }

        this.grid = grid;
        this.stack = new Stack<>();
        this.random = new Random();

        for (Cell cell : grid.getAllCells()) {
            cell.visited = false;
        }

        Cell startCell = grid.getAllCells().get(0);
        startCell.visited = true;
        stack.push(startCell);
        currentHead = startCell;
    }

    @Override
    public boolean step() {
        if (stack.isEmpty()) {
            currentHead = null;
            return false;
        }

        Cell current = stack.peek();
        currentHead = current;
        List<Cell> unvisitedNeighbors = new ArrayList<>();

        for (Cell neighbor : current.getAdjacentCells()) {
            if (!neighbor.visited) {
                unvisitedNeighbors.add(neighbor);
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
    }

    @Override
    public Cell getCurrentHead() {
        return currentHead;
    }
}