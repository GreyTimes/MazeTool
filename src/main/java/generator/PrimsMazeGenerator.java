package generator;

import model.Cell;
import model.MazeGrid;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Randomized Prim's algorithm over graph neighbors. */
public class PrimsMazeGenerator implements MazeGenerator {
    private MazeGrid grid;
    private List<Cell> frontier;
    private Random random;
    private Cell currentHead;

    @Override
    public void initialize(MazeGrid grid) {
        if (grid == null || grid.getAllCells().isEmpty()) {
            throw new IllegalArgumentException("A non-empty maze grid is required.");
        }

        this.grid = grid;
        this.frontier = new ArrayList<>();
        this.random = new Random();

        for (Cell cell : grid.getAllCells()) {
            cell.visited = false;
        }

        Cell start = grid.getAllCells().get(
                random.nextInt(grid.getCellCount())
        );

        start.visited = true;

        addFrontierCells(start);

        currentHead = start;
    }

    @Override
    public boolean step() {
        if (frontier == null || frontier.isEmpty()) {
            currentHead = null;
            return false;
        }

        Cell current = frontier.remove(
                random.nextInt(frontier.size())
        );

        currentHead = current;

        List<Cell> visitedNeighbors = new ArrayList<>();

        for (Cell neighbor : current.getAdjacentCells()) {
            if (neighbor.visited) {
                visitedNeighbors.add(neighbor);
            }
        }

        if (!visitedNeighbors.isEmpty()) {

            Cell connection = visitedNeighbors.get(
                    random.nextInt(visitedNeighbors.size())
            );

            grid.removeWalls(current, connection);

            current.visited = true;

            addFrontierCells(current);
        }

        if (frontier.isEmpty()) {
            currentHead = null;
        }

        return !frontier.isEmpty();
    }

    private void addFrontierCells(Cell cell) {
        for (Cell neighbor : cell.getAdjacentCells()) {
            if (!neighbor.visited && !frontier.contains(neighbor)) {
                frontier.add(neighbor);
            }
        }
    }

    @Override
    public Cell getCurrentHead() {
        return currentHead;
    }
}