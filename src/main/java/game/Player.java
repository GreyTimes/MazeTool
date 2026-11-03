package game;

import model.Cell;
import model.MazeGrid;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/** Player state and graph-based discovery for every supported maze topology. */
public class Player {
    private Cell currentCell;
    private int steps;

    public void spawn(MazeGrid grid) {
        steps = 0;

        currentCell = grid == null
                ? null
                : grid.getEntranceCell();

        if (currentCell != null) {
            steps = 1;
        }
    }

    /** Moves only across a real, currently open graph edge. */
    public boolean moveToCell(Cell nextCell) {

        if (currentCell == null
                || nextCell == null
                || !currentCell.isPassageOpenTo(nextCell)) {
            return false;
        }

        currentCell = nextCell;
        steps++;

        return true;
    }

    /**
     * Reveals cells by topological graph distance, not by x/y differences.
     * Visibility distance follows cell adjacency regardless of wall state,
     * matching the previous behavior of revealing nearby cells through walls.
     */
    public void updateDiscovery(
            MazeGrid grid,
            boolean fogActive,
            int fogRadius
    ) {
        if (grid == null) {
            return;
        }

        if (!fogActive) {
            for (Cell cell : grid.getAllCells()) {
                cell.discovered = true;
            }
            return;
        }

        if (currentCell == null) {
            return;
        }

        int radius = Math.max(0, fogRadius);

        Queue<Cell> queue = new ArrayDeque<>();
        Map<Cell, Integer> distance = new HashMap<>();

        queue.add(currentCell);
        distance.put(currentCell, 0);

        currentCell.discovered = true;

        while (!queue.isEmpty()) {

            Cell current = queue.remove();

            int currentDistance = distance.get(current);

            if (currentDistance >= radius) {
                continue;
            }

            for (Cell neighbor : current.getAdjacentCells()) {

                if (!distance.containsKey(neighbor)) {

                    int nextDistance = currentDistance + 1;

                    distance.put(neighbor, nextDistance);

                    neighbor.discovered = true;

                    queue.add(neighbor);
                }
            }
        }
    }

    public Cell getCurrentCell() {
        return currentCell;
    }

    public int getSteps() {
        return steps;
    }

    public void reset() {
        currentCell = null;
        steps = 0;
    }
}