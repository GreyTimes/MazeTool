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

/** Depth-first path search over open graph edges. */
public final class DfsMazeSolver {
    private DfsMazeSolver() { }

    public static List<Cell> solve(MazeGrid grid) {
        if (grid == null) return null;

        Cell start = grid.getEntranceCell();
        Cell target = grid.getExitCell();

        if (start == null || target == null) return null;
        if (start == target) return Collections.singletonList(start);

        Stack<Cell> stack = new Stack<>();

        Map<Cell, Cell> parent = new HashMap<>();
        Set<Cell> visited = new HashSet<>();

        stack.push(start);
        visited.add(start);

        while (!stack.isEmpty()) {

            Cell current = stack.pop();

            if (current == target) {

                List<Cell> path = new ArrayList<>();

                for (Cell cell = target;
                     cell != null;
                     cell = parent.get(cell)) {

                    path.add(cell);
                }

                Collections.reverse(path);

                return path;
            }

            for (Map.Entry<Cell, Boolean> edge
                    : current.getNeighbors().entrySet()) {

                Cell neighbor = edge.getKey();

                if (Boolean.TRUE.equals(edge.getValue())
                        && visited.add(neighbor)) {

                    parent.put(neighbor, current);
                    stack.push(neighbor);
                }
            }
        }

        return null;
    }
}