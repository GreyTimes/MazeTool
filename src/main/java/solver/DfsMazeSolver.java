package solver;

import model.Cell;
import model.MazeGrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Depth-first path search over open graph edges with robust error guards. */
public final class DfsMazeSolver {
    private static final Logger LOGGER = Logger.getLogger(DfsMazeSolver.class.getName());

    private DfsMazeSolver() { }

    public static List<Cell> solve(MazeGrid grid) {
        if (grid == null) {
            LOGGER.warning("DFS Solver received a null grid.");
            return null;
        }

        try {
            Cell start = grid.getEntranceCell();
            Cell target = grid.getExitCell();

            if (start == null || target == null) {
                LOGGER.warning("DFS Solver could not find valid entrance or exit cells.");
                return null;
            }
            if (start == target) {
                return Collections.singletonList(start);
            }

            Stack<Cell> stack = new Stack<>();
            Map<Cell, Cell> parent = new HashMap<>();
            Set<Cell> visited = new HashSet<>();

            stack.push(start);
            visited.add(start);

            while (!stack.isEmpty()) {
                Cell current = stack.pop();
                if (current == null) {
                    continue;
                }

                if (current == target) {
                    List<Cell> path = new ArrayList<>();
                    for (Cell cell = target; cell != null; cell = parent.get(cell)) {
                        path.add(cell);
                    }
                    Collections.reverse(path);
                    return path;
                }

                Map<Cell, Boolean> neighbors = current.getNeighbors();
                if (neighbors == null) {
                    continue;
                }

                for (Map.Entry<Cell, Boolean> edge : neighbors.entrySet()) {
                    Cell neighbor = edge.getKey();
                    if (neighbor != null && Boolean.TRUE.equals(edge.getValue()) && visited.add(neighbor)) {
                        parent.put(neighbor, current);
                        stack.push(neighbor);
                    }
                }
            }

            LOGGER.info("No valid DFS path found between entrance and exit.");
            return null;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unexpected error during DFS maze solving: " + ex.getMessage(), ex);
            return null;
        }
    }
}