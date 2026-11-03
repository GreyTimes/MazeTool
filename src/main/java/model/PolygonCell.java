package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Base class for cells whose boundaries are polygons.
 *
 * Edge i runs from vertex i to vertex (i + 1) % edgeCount.
 * Geometry and graph adjacency remain independent.
 */
public abstract class PolygonCell extends Cell {
    private final double[] baseX;
    private final double[] baseY;
    private final double[] vertexX;
    private final double[] vertexY;
    private final Map<Integer, Cell> edgeNeighbors = new LinkedHashMap<>();
    private int openingEdgeIndex = -1;

    protected PolygonCell(int x, int y, double[] verticesX, double[] verticesY) {
        super(x, y);
        if (verticesX == null || verticesY == null || verticesX.length != verticesY.length || verticesX.length < 3) {
            throw new IllegalArgumentException("A polygon cell needs at least three matching vertices.");
        }
        baseX = verticesX.clone();
        baseY = verticesY.clone();
        vertexX = verticesX.clone();
        vertexY = verticesY.clone();
        updateCenter();
    }

    // GEOMETRY

    public final int getEdgeCount() {
        return vertexX.length;
    }

    public final double getVertexX(int index) {
        return vertexX[index];
    }

    public final double getVertexY(int index) {
        return vertexY[index];
    }

    public final double getBaseVertexX(int index) {
        return baseX[index];
    }

    public final double getBaseVertexY(int index) {
        return baseY[index];
    }

    public final double[] getVerticesX() {
        return vertexX.clone();
    }

    public final double[] getVerticesY() {
        return vertexY.clone();
    }

    public final void applyLayout(double scale, double offsetX, double offsetY) {
        for (int i = 0; i < vertexX.length; i++) {
            vertexX[i] = baseX[i] * scale + offsetX;
            vertexY[i] = baseY[i] * scale + offsetY;
        }
        updateCenter();
    }

    /**
     * Calculates the polygon's area centroid.
     * This works for both regular polygons and clipped boundary cells.
     */
    private void updateCenter() {
        double twiceArea = 0.0;
        double centroidXNumerator = 0.0;
        double centroidYNumerator = 0.0;
        for (int i = 0; i < vertexX.length; i++) {
            int next = (i + 1) % vertexX.length;
            double cross =vertexX[i] * vertexY[next] - vertexX[next] * vertexY[i];
            twiceArea += cross;
            centroidXNumerator += (vertexX[i] + vertexX[next]) * cross;
            centroidYNumerator += (vertexY[i] + vertexY[next]) * cross;
        }

        if (Math.abs(twiceArea) > 1.0e-12) {
            screenX = centroidXNumerator / (3.0 * twiceArea);
            screenY = centroidYNumerator / (3.0 * twiceArea);
        } else {
            double sumX = 0.0;
            double sumY = 0.0;
            for (int i = 0; i < vertexX.length; i++) {
                sumX += vertexX[i];
                sumY += vertexY[i];
            }
            screenX = sumX / vertexX.length;
            screenY = sumY / vertexY.length;
        }
    }

    // SHARED EDGES

    public final void registerSharedEdge(int thisEdge, PolygonCell other, int otherEdge) {
        if (other == null || thisEdge < 0 || thisEdge >= getEdgeCount() || otherEdge < 0 || otherEdge >= other.getEdgeCount()) {
            throw new IllegalArgumentException("Invalid shared polygon edge.");
        }

        addNeighbor(other);
        other.addNeighbor(this);

        edgeNeighbors.put(thisEdge, other);
        other.edgeNeighbors.put(otherEdge, this);
    }

    public final Cell getNeighborAcrossEdge(int edgeIndex) {
        return edgeNeighbors.get(edgeIndex);
    }

    public final boolean isBoundaryEdge(int edgeIndex) {
        return !edgeNeighbors.containsKey(edgeIndex);
    }

    public final List<Integer> getBoundaryEdgeIndices() {
        List<Integer> result = new ArrayList<>();
        for (int edge = 0; edge < getEdgeCount(); edge++) {
            if (isBoundaryEdge(edge)) {
                result.add(edge);
            }
        }
        return Collections.unmodifiableList(result);
    }

    // ENTRANCE / EXIT

    public final int getOpeningEdgeIndex() {
        return openingEdgeIndex;
    }

    public final void setOpeningEdgeIndex(int openingEdgeIndex) {
        if (openingEdgeIndex < -1 || openingEdgeIndex >= getEdgeCount()) {
            throw new IllegalArgumentException("Invalid polygon opening edge index.");
        }
        if (openingEdgeIndex >= 0 && !isBoundaryEdge(openingEdgeIndex)) {
            throw new IllegalArgumentException("An exterior opening must use a boundary edge.");
        }
        this.openingEdgeIndex = openingEdgeIndex;
    }

    // MOUSE HIT TESTING

    public final boolean containsPoint(double x, double y) {
        boolean inside = false;
        for (int i = 0, j = vertexX.length - 1; i < vertexX.length; j = i++) {
            double xi = vertexX[i];
            double yi = vertexY[i];
            double xj = vertexX[j];
            double yj = vertexY[j];

            boolean crosses = ((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi);

            if (crosses) {
                inside = !inside;
            }
        }

        return inside;
    }
}