
package model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A generic graph node used by every maze topology.
 */
public class Cell {

    public int x;
    public int y;

    public double screenX;
    public double screenY;

    // Legacy rectangular walls:
    // 0 = Top, 1 = Right, 2 = Bottom, 3 = Left.
    public boolean[] walls = {
            true, true, true, true
    };

    public boolean visited = false;

    public boolean discovered = false;

    public boolean isEntrance = false;

    public boolean isExit = false;

    // Neighbor -> passage state.
    private final Map<Cell, Boolean> neighbors =
            new LinkedHashMap<>();


    public Cell(int x, int y) {

        this.x = x;
        this.y = y;
    }


    public double getScreenX() {
        return screenX;
    }


    public double getScreenY() {
        return screenY;
    }


    public void addNeighbor(Cell neighbor) {

        if (neighbor != null && neighbor != this) {

            neighbors.putIfAbsent(
                    neighbor,
                    false
            );
        }
    }


    public Map<Cell, Boolean> getNeighbors() {
        return neighbors;
    }


    /** Returns every topological neighbor, including closed passages. */
    public List<Cell> getAdjacentCells() {

        return new ArrayList<>(
                neighbors.keySet()
        );
    }


    /** Returns only cells connected by an open passage. */
    public List<Cell> getOpenNeighbors() {

        List<Cell> result = new ArrayList<>();

        for (Map.Entry<Cell, Boolean> entry
                : neighbors.entrySet()) {

            if (Boolean.TRUE.equals(entry.getValue())) {

                result.add(entry.getKey());
            }
        }

        return result;
    }


    public boolean isPassageOpenTo(Cell neighbor) {

        return Boolean.TRUE.equals(
                neighbors.get(neighbor)
        );
    }


    public int getOpenDegree() {

        int degree = 0;

        for (boolean open : neighbors.values()) {

            if (open) {
                degree++;
            }
        }

        return degree;
    }
}
