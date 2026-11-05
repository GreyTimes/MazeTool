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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dijkstra shortest path weighted by Euclidean distance between neighboring
 * cell centers, with robust null-safety and exception boundary guards.
 */
public final class DijkstraMazeSolver {
    private static final Logger LOGGER = Logger.getLogger(DijkstraMazeSolver.class.getName());

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
        if (grid == null) {
            LOGGER.warning("Dijkstra Solver received a null grid.");
            return null;
        }

        try {
            Cell start = grid.getEntranceCell();
            Cell target = grid.getExitCell();

            if (start == null || target == null) {
                LOGGER.warning("Dijkstra Solver could not find valid entrance or exit cells.");
                return null;
            }
            if (start == target) {
                return Collections.singletonList(start);
            }

            Map<Cell, Double> distances = new HashMap<>();
            Map<Cell, Cell> parent = new HashMap<>();

            PriorityQueue<QueueNode> queue = new PriorityQueue<>(Comparator.comparingDouble(node -> node.distance));

            List<Cell> allCells = grid.getAllCells();
            if (allCells == null) {
                return null;
            }

            for (Cell cell : allCells) {
                if (cell != null) {
                    distances.put(cell, Double.POSITIVE_INFINITY);
                }
            }

            distances.put(start, 0.0);
            queue.add(new QueueNode(start, 0.0));

            while (!queue.isEmpty()) {
                QueueNode node = queue.poll();
                Cell current = node.cell;
                if (current == null) {
                    continue;
                }

                Double currentDistance = distances.get(current);
                if (currentDistance == null || node.distance > currentDistance) {
                    continue;
                }
                if (current == target) {
                    break;
                }

                Map<Cell, Boolean> neighbors = current.getNeighbors();
                if (neighbors == null) {
                    continue;
                }

                for (Map.Entry<Cell, Boolean> edge : neighbors.entrySet()) {
                    if (!Boolean.TRUE.equals(edge.getValue())) {
                        continue;
                    }

                    Cell neighbor = edge.getKey();
                    if (neighbor == null) {
                        continue;
                    }

                    double stepCost = centerDistance(current, neighbor);
                    double candidate = currentDistance + stepCost;

                    double neighborBest = distances.getOrDefault(neighbor, Double.POSITIVE_INFINITY);
                    if (candidate < neighborBest) {
                        distances.put(neighbor, candidate);
                        parent.put(neighbor, current);
                        queue.add(new QueueNode(neighbor, candidate));
                    }
                }
            }

            if (!parent.containsKey(target)) {
                LOGGER.info("No valid Dijkstra path found between entrance and exit.");
                return null;
            }

            List<Cell> path = new ArrayList<>();
            for (Cell current = target; current != null; current = parent.get(current)) {
                path.add(current);
            }

            Collections.reverse(path);
            return path;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unexpected error during Dijkstra maze solving: " + ex.getMessage(), ex);
            return null;
        }
    }

    private static double centerDistance(Cell a, Cell b) {
        try {
            double distance = Math.hypot(a.getScreenX() - b.getScreenX(), a.getScreenY() - b.getScreenY());
            return distance > 1.0e-9 ? distance : 1.0;
        } catch (Exception e) {
            return 1.0; // Fallback unit cost if screen coordinates are uninitialized
        }
    }
}