package solver;

import model.Cell;
import model.MazeGrid;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Breadth-first shortest path measured in number of cell-to-cell moves. */
public final class BfsMazeSolver {
    private static final Logger LOGGER = Logger.getLogger(BfsMazeSolver.class.getName());

    private BfsMazeSolver() { }

    public static List<Cell> solve(MazeGrid grid) {
        if (grid == null) {
            LOGGER.warning("Solver received a null grid.");
            return null;
        }

        try {
            Cell start = grid.getEntranceCell();
            Cell target = grid.getExitCell();

            if (start == null || target == null) {
                LOGGER.warning("Solver could not find valid entrance or exit cells.");
                return null;
            }
            if (start == target) {
                return Collections.singletonList(start);
            }

            Queue<Cell> queue = new ArrayDeque<>();
            Set<Cell> visited = new HashSet<>();
            Map<Cell, Cell> parent = new HashMap<>();

            queue.add(start);
            visited.add(start);

            while (!queue.isEmpty()) {
                Cell current = queue.remove();
                if (current == null) {
                    continue;
                }

                if (current == target) {
                    return reconstructPath(parent, target);
                }

                Map<Cell, Boolean> neighbors = current.getNeighbors();
                if (neighbors == null) {
                    continue;
                }

                for (Map.Entry<Cell, Boolean> edge : neighbors.entrySet()) {
                    Cell neighbor = edge.getKey();
                    if (neighbor != null && Boolean.TRUE.equals(edge.getValue()) && visited.add(neighbor)) {
                        parent.put(neighbor, current);
                        queue.add(neighbor);
                    }
                }
            }

            LOGGER.info("No valid path found between entrance and exit.");
            return null;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unexpected error during BFS maze solving: " + ex.getMessage(), ex);
            return null;
        }
    }

    static List<Cell> reconstructPath(Map<Cell, Cell> parent, Cell target) {
        List<Cell> path = new ArrayList<>();
        Cell current = target;
        while (current != null) {
            path.add(current);
            current = parent.get(current);
        }
        Collections.reverse(path);
        return path;
    }
}