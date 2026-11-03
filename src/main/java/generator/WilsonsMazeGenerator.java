package generator;

import model.Cell;
import model.MazeGrid;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Wilson's loop-erased random-walk uniform spanning-tree generator. */
public class WilsonsMazeGenerator implements MazeGenerator {
    private MazeGrid grid;
    private final List<Cell> unvisited = new ArrayList<>();
    private final List<Cell> walkPath = new ArrayList<>();
    private final Random random = new Random();
    private boolean building;

    @Override
    public void initialize(MazeGrid grid) {
        if (grid == null || grid.getAllCells().isEmpty()) {
            throw new IllegalArgumentException("A non-empty maze grid is required.");
        }

        this.grid = grid;
        unvisited.clear();
        walkPath.clear();

        for (Cell cell : grid.getAllCells()) {
            cell.visited = false;
            unvisited.add(cell);
        }

        Cell first = unvisited.remove(random.nextInt(unvisited.size()));
        first.visited = true;
        building = false;
    }

    @Override
    public boolean step() {

        if (unvisited.isEmpty()) {
            building = false;
            walkPath.clear();
            return false;
        }

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
                grid.removeWalls(a, b);
                a.visited = true;
                unvisited.remove(a);
            }

            walkPath.clear();
            building = false;
            return !unvisited.isEmpty();
        }

        List<Cell> neighbors = current.getAdjacentCells();

        if (neighbors.isEmpty()) {
            throw new IllegalStateException("The maze graph contains an isolated cell.");
        }

        Cell next = neighbors.get(random.nextInt(neighbors.size()));

        int loopStart = walkPath.indexOf(next);

        if (loopStart >= 0) {
            while (walkPath.size() > loopStart + 1) {
                walkPath.remove(walkPath.size() - 1);
            }
        } else {
            walkPath.add(next);
        }
        return true;
    }

    @Override
    public Cell getCurrentHead() {
        return walkPath.isEmpty() ? null : walkPath.get(walkPath.size() - 1);
    }
}