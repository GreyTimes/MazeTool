package game;

import model.Cell;
import model.MazeGrid;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages player position, step tracking, and fog-of-war discovery with robust error guards.
 */
public class Player {
    private static final Logger LOGGER = Logger.getLogger(Player.class.getName());

    private Cell currentCell;
    private int steps;

    public Player() {
        reset();
    }

    public void reset() {
        this.currentCell = null;
        this.steps = 0;
    }

    public Cell getCurrentCell() {
        return currentCell;
    }

    public int getSteps() {
        return steps;
    }

    public void spawn(MazeGrid grid) {
        if (grid == null) {
            LOGGER.warning("Attempted to spawn player on a null grid.");
            return;
        }

        try {
            Cell entrance = grid.getEntranceCell();
            if (entrance != null) {
                this.currentCell = entrance;
                this.steps = 0;
            } else {
                List<Cell> allCells = grid.getAllCells();
                if (allCells != null && !allCells.isEmpty()) {
                    this.currentCell = allCells.get(0);
                    this.steps = 0;
                } else {
                    LOGGER.warning("Grid contains no cells to spawn player.");
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error spawning player: " + ex.getMessage(), ex);
        }
    }

    public boolean moveToCell(Cell target) {
        if (target == null || currentCell == null) {
            return false;
        }

        try {
            if (currentCell.isPassageOpenTo(target)) {
                this.currentCell = target;
                this.steps++;
                return true;
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error moving player to target cell: " + ex.getMessage());
        }
        return false;
    }

    public void updateDiscovery(MazeGrid grid, boolean fogActive, int radius) {
        if (!fogActive || grid == null || currentCell == null) {
            if (grid != null && grid.getAllCells() != null) {
                for (Cell cell : grid.getAllCells()) {
                    if (cell != null) {
                        cell.discovered = true;
                    }
                }
            }
            return;
        }

        try {
            Set<Cell> visited = new HashSet<>();
            Queue<CellNode> queue = new ArrayDeque<>();

            queue.add(new CellNode(currentCell, 0));
            visited.add(currentCell);
            currentCell.discovered = true;

            while (!queue.isEmpty()) {
                CellNode node = queue.remove();
                if (node.distance >= radius) {
                    continue;
                }

                Map<Cell, Boolean> neighbors = node.cell.getNeighbors();
                if (neighbors == null) {
                    continue;
                }

                for (Map.Entry<Cell, Boolean> edge : neighbors.entrySet()) {
                    Cell neighbor = edge.getKey();
                    if (neighbor != null && Boolean.TRUE.equals(edge.getValue())) {
                        if (visited.add(neighbor)) {
                            neighbor.discovered = true;
                            queue.add(new CellNode(neighbor, node.distance + 1));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error updating player fog discovery: " + ex.getMessage(), ex);
        }
    }

    private static final class CellNode {
        private final Cell cell;
        private final int distance;

        private CellNode(Cell cell, int distance) {
            this.cell = cell;
            this.distance = distance;
        }
    }
}