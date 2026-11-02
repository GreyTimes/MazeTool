package generator;

import model.Cell;
import model.MazeGrid;

import java.util.List;
import java.util.Random;

/** Aldous-Broder uniform spanning-tree generator using a graph random walk. */
public class AldousBroderMazeGenerator implements MazeGenerator {
    private MazeGrid grid;

    private Cell current;
    private int unvisitedCount;

    private final Random random = new Random();

    @Override
    public void initialize(MazeGrid grid) {
        if (grid == null || grid.getAllCells().isEmpty()) {
            throw new IllegalArgumentException("A non-empty maze grid is required.");
        }

        this.grid = grid;

        for (Cell cell : grid.getAllCells()) {
            cell.visited = false;
        }

        this.unvisitedCount = grid.getCellCount();

        this.current = grid.getAllCells().get(
                random.nextInt(grid.getCellCount())
        );

        current.visited = true;

        unvisitedCount--;
    }

    @Override
    public boolean step() {
        if (unvisitedCount <= 0) {
            return false;
        }

        List<Cell> neighbors = current.getAdjacentCells();

        if (neighbors.isEmpty()) {
            throw new IllegalStateException(
                    "The maze graph contains an isolated cell."
            );
        }

        Cell next = neighbors.get(
                random.nextInt(neighbors.size())
        );

        if (!next.visited) {

            grid.removeWalls(current, next);

            next.visited = true;

            unvisitedCount--;
        }

        current = next;

        return unvisitedCount > 0;
    }

    @Override
    public Cell getCurrentHead() {
        return current;
    }
}