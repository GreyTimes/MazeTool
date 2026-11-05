package generator;

import model.Cell;
import model.MazeGrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Randomized Kruskal's algorithm with bounds and disjoint-set safety. */
public class KruskalsMazeGenerator implements MazeGenerator {
    private static final Logger LOGGER = Logger.getLogger(KruskalsMazeGenerator.class.getName());

    private MazeGrid grid;
    private List<Edge> edges;
    private int edgeIndex;
    private Map<Cell, Integer> cellIndexes;
    private int[] parent;
    private byte[] rank;
    private int acceptedEdges;
    private int requiredEdges;
    private Cell currentHead;

    private static final class Edge {
        private final Cell a;
        private final Cell b;
        private Edge(Cell a, Cell b) {
            this.a = a;
            this.b = b;
        }
    }

    @Override
    public void initialize(MazeGrid grid, Random random) {
        if (grid == null || grid.getAllCells() == null || grid.getAllCells().isEmpty()) {
            throw new IllegalArgumentException("A non-empty maze grid is required.");
        }

        this.grid = grid;
        List<Cell> allCells = grid.getAllCells();
        int size = allCells.size();
        this.edges = new ArrayList<>();
        this.cellIndexes = new HashMap<>();
        this.parent = new int[size];
        this.rank = new byte[size];
        this.edgeIndex = 0;
        this.acceptedEdges = 0;
        this.requiredEdges = size - 1;

        for (int i = 0; i < size; i++) {
            Cell cell = allCells.get(i);
            if (cell != null) {
                cell.visited = true;
                cellIndexes.put(cell, i);
                parent[i] = i;
            }
        }

        for (int i = 0; i < size; i++) {
            Cell cell = allCells.get(i);
            if (cell == null) continue;
            List<Cell> adj = cell.getAdjacentCells();
            if (adj != null) {
                for (Cell neighbor : adj) {
                    if (neighbor == null) continue;
                    Integer neighborIndex = cellIndexes.get(neighbor);
                    if (neighborIndex != null && i < neighborIndex) {
                        edges.add(new Edge(cell, neighbor));
                    }
                }
            }
        }

        Collections.shuffle(edges, new Random());
        currentHead = null;
    }

    @Override
    public boolean step() {
        if (acceptedEdges >= requiredEdges || edgeIndex >= edges.size()) {
            currentHead = null;
            return false;
        }

        try {
            Edge edge = edges.get(edgeIndex++);
            currentHead = edge.a;

            Integer idxA = cellIndexes.get(edge.a);
            Integer idxB = cellIndexes.get(edge.b);
            if (idxA != null && idxB != null) {
                int rootA = find(idxA);
                int rootB = find(idxB);

                if (rootA != rootB) {
                    grid.removeWalls(edge.a, edge.b);
                    union(rootA, rootB);
                    acceptedEdges++;
                }
            }

            if (acceptedEdges >= requiredEdges || edgeIndex >= edges.size()) {
                currentHead = null;
                return false;
            }

            return true;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error during Kruskal's step: " + ex.getMessage(), ex);
            currentHead = null;
            return false;
        }
    }

    private int find(int value) {
        if (value < 0 || value >= parent.length) return value;
        if (parent[value] != value) {
            parent[value] = find(parent[value]);
        }
        return parent[value];
    }

    private void union(int rootA, int rootB) {
        if (rootA < 0 || rootA >= parent.length || rootB < 0 || rootB >= parent.length) return;
        if (rank[rootA] < rank[rootB]) {
            parent[rootA] = rootB;
        } else if (rank[rootA] > rank[rootB]) {
            parent[rootB] = rootA;
        } else {
            parent[rootB] = rootA;
            rank[rootA]++;
        }
    }

    @Override
    public Cell getCurrentHead() {
        return currentHead;
    }
}