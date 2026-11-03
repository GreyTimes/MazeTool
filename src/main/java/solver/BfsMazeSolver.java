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

/** Breadth-first shortest path measured in number of cell-to-cell moves. */
public final class BfsMazeSolver {
    private BfsMazeSolver() { }

    public static List<Cell> solve(MazeGrid grid) {
        if (grid == null) return null;

        Cell start = grid.getEntranceCell();
        Cell target = grid.getExitCell();

        if (start == null || target == null) return null;
        if (start == target) return Collections.singletonList(start);

        Queue<Cell> queue = new ArrayDeque<>();
        Set<Cell> visited = new HashSet<>();
        Map<Cell, Cell> parent = new HashMap<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Cell current = queue.remove();
            if (current == target) {
                return reconstructPath(parent, target);
            }

            for (Map.Entry<Cell, Boolean> edge : current.getNeighbors().entrySet()) {
                Cell neighbor = edge.getKey();
                if (Boolean.TRUE.equals(edge.getValue()) && visited.add(neighbor)) {
                    parent.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }
        return null;
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