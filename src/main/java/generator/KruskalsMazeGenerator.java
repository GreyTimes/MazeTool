package generator;

import model.Cell;
import model.MazeGrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Randomized Kruskal's algorithm with a disjoint-set (union-find) structure. */
public class KruskalsMazeGenerator implements MazeGenerator {
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
    public void initialize(MazeGrid grid) {
        if (grid == null || grid.getAllCells().isEmpty()) {
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
            cell.visited = true;
            cellIndexes.put(cell, i);
            parent[i] = i;
        }

        for (int i = 0; i < size; i++) {
            Cell cell = allCells.get(i);
            for (Cell neighbor : cell.getAdjacentCells()) {
                Integer neighborIndex = cellIndexes.get(neighbor);
                if (neighborIndex != null && i < neighborIndex) {
                    edges.add(new Edge(cell, neighbor));
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

        Edge edge = edges.get(edgeIndex++);
        currentHead = edge.a;

        int rootA = find(cellIndexes.get(edge.a));
        int rootB = find(cellIndexes.get(edge.b));

        if (rootA != rootB) {
            grid.removeWalls(edge.a, edge.b);
            union(rootA, rootB);
            acceptedEdges++;
        }

        if (acceptedEdges >= requiredEdges || edgeIndex >= edges.size()) {
            currentHead = null;
            return false;
        }

        return true;
    }

    private int find(int value) {
        if (parent[value] != value) {
            parent[value] = find(parent[value]);
        }
        return parent[value];
    }

    private void union(int rootA, int rootB) {
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