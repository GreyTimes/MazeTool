package solver;

import model.Cell;
import model.MazeGrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Dijkstra shortest path weighted by Euclidean distance between neighboring
 * cell centers. This avoids a Cartesian-coordinate assumption for polar grids.
 */
public final class DijkstraMazeSolver {
    private DijkstraMazeSolver() { }

    private static final class QueueNode {
        private final Cell cell;
        private final double distance;

        private QueueNode(Cell cell, double distance) {
            this.cell = cell;
            this.distance = distance;
        }
    }

    public static List<Cell> solve(MazeGrid grid) {
        if (grid == null) return null;

        Cell start = grid.getEntranceCell();
        Cell target = grid.getExitCell();

        if (start == null || target == null) return null;
        if (start == target) return Collections.singletonList(start);

        Map<Cell, Double> distances = new HashMap<>();
        Map<Cell, Cell> parent = new HashMap<>();

        PriorityQueue<QueueNode> queue = new PriorityQueue<>(Comparator.comparingDouble(node -> node.distance));
        for (Cell cell : grid.getAllCells()) {
            distances.put(cell, Double.POSITIVE_INFINITY);
        }

        distances.put(start, 0.0);
        queue.add(new QueueNode(start, 0.0));

        while (!queue.isEmpty()) {
            QueueNode node = queue.poll();
            Cell current = node.cell;
            if (node.distance > distances.get(current)) {
                continue;
            }
            if (current == target) {
                break;
            }
            for (Map.Entry<Cell, Boolean> edge : current.getNeighbors().entrySet()) {
                if (!Boolean.TRUE.equals(edge.getValue())) {
                    continue;
                }

                Cell neighbor = edge.getKey();
                double stepCost = centerDistance(current, neighbor);
                double candidate = distances.get(current) + stepCost;
                if (candidate < distances.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    distances.put(neighbor, candidate);
                    parent.put(neighbor, current);
                    queue.add(new QueueNode(neighbor, candidate));
                }
            }
        }

        if (!parent.containsKey(target)) {
            return null;
        }

        List<Cell> path = new ArrayList<>();

        for (Cell current = target; current != null; current = parent.get(current)) {
            path.add(current);
        }

        Collections.reverse(path);
        return path;
    }

    private static double centerDistance(Cell a, Cell b) {
        double distance = Math.hypot(a.getScreenX() - b.getScreenX(), a.getScreenY() - b.getScreenY());

        return distance > 1.0e-9 ? distance : 1.0;
    }
}