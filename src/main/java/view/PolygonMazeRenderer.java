package view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import model.Cell;
import model.MazeGrid;
import model.PolygonCell;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Shared renderer for triangular and hexagonal mazes.
 *
 * IMPORTANT:
 * Boundary walls are detected directly from polygon geometry.
 * The renderer does not rely on PolygonCell.isBoundaryEdge() to
 * decide whether an exterior wall should be displayed.
 */
public abstract class PolygonMazeRenderer implements MazeRenderer {
    private static final double EDGE_KEY_SCALE = 10_000.0;
    private static final double INTERNAL_WALL_WIDTH = 1.65;
    private static final double EXTERIOR_WALL_WIDTH = 3.5;
    private boolean darkMode = true;
    private Set<Cell> highlightedCells = Collections.emptySet();
    private Cell hoveredCell;
    private Double playerScreenX;
    private Double playerScreenY;

    // GEOMETRIC EDGE IDENTIFICATION

    /**
     * A quantized vertex coordinate.
     *
     * Quantization handles tiny floating-point differences between
     * vertices calculated independently by neighboring polygons.
     */
    private record VertexKey(long x, long y) implements Comparable<VertexKey> {
        static VertexKey of(double x, double y) {
            return new VertexKey(Math.round(x * EDGE_KEY_SCALE), Math.round(y * EDGE_KEY_SCALE));
        }

        @Override
        public int compareTo(VertexKey other) {
            int compareX = Long.compare(x, other.x);
            return compareX != 0 ? compareX : Long.compare(y, other.y);
        }
    }

    /**
     * The same physical edge receives the same key regardless
     * of the direction in which its vertices are ordered.
     */
    private record SegmentKey(VertexKey first, VertexKey second) {
        static SegmentKey of(double x1, double y1, double x2, double y2) {
            VertexKey a = VertexKey.of(x1, y1);
            VertexKey b = VertexKey.of(x2, y2);
            return a.compareTo(b) <= 0 ? new SegmentKey(a, b) : new SegmentKey(b, a);
        }
    }

    /**
     * One polygon's occurrence of a physical edge.
     */
    private record EdgeOccurrence(PolygonCell cell, int edgeIndex) { }

    // THEME

    @Override
    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    // INTERACTION

    @Override
    public void setHighlightedCells(Collection<Cell> cells) {
        highlightedCells = cells == null ? Collections.emptySet() : new HashSet<>(cells);
    }

    @Override
    public void setHoveredCell(Cell cell) {
        hoveredCell = cell;
    }

    @Override
    public void setPlayerScreenPosition(double x, double y) {
        playerScreenX = x;
        playerScreenY = y;
    }

    @Override
    public void clearPlayerScreenPosition() {
        playerScreenX = null;
        playerScreenY = null;
    }

    // MOUSE HIT TESTING

    @Override
    public Cell findCellAt(double x, double y, double width, double height, MazeGrid grid) {
        if (grid == null || !grid.isPolygonal()) {
            return null;
        }
        List<Cell> allCells = grid.getAllCells();
        for (int i = allCells.size() - 1; i >= 0; i--) {
            Cell cell = allCells.get(i);
            if (cell instanceof PolygonCell polygon && polygon.containsPoint(x, y)) {
                return cell;
            }
        }
        return null;
    }

    // BUILD PHYSICAL EDGE INDEX

    /**
     * Groups every physical polygon edge.
     * One occurrence:
     *     Exterior boundary.
     * Two occurrences:
     *     Internal edge shared by two cells.
     * More than two occurrences:
     *     Invalid/overlapping geometry, treated conservatively as a wall.
     */
    private Map<SegmentKey, List<EdgeOccurrence>>
    buildPhysicalEdgeIndex(MazeGrid grid) {
        Map<SegmentKey, List<EdgeOccurrence>> edgeIndex = new LinkedHashMap<>();
        for (Cell rawCell : grid.getAllCells()) {
            if (!(rawCell instanceof PolygonCell polygon)) {
                continue;
            }
            for (int edge = 0; edge < polygon.getEdgeCount(); edge++) {
                int next = (edge + 1) % polygon.getEdgeCount();
                double x1 = polygon.getVertexX(edge);
                double y1 = polygon.getVertexY(edge);
                double x2 = polygon.getVertexX(next);
                double y2 = polygon.getVertexY(next);
                double lengthSquared = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);

                if (lengthSquared < 1.0e-10) {
                    continue;
                }

                SegmentKey key = SegmentKey.of(x1, y1, x2, y2);
                edgeIndex.computeIfAbsent(key, ignored -> new java.util.ArrayList<>()).add(new EdgeOccurrence(polygon, edge));
            }
        }
        return edgeIndex;
    }

    // DRAW

    @Override
    public void render(
            GraphicsContext gc,
            double width,
            double height,
            MazeGrid grid,
            Cell playerCell,
            List<Cell> solvedPath,
            Cell currentHead,
            boolean fogOfWarActive,
            int fogRadius
    ) {

        Color background = darkMode ? Color.web("#1e1e1e") : Color.web("#f5f5f5");
        Color wallColor = darkMode ? Color.web("#777777") : Color.web("#333333");
        Color exteriorWallColor = darkMode ? Color.web("#999999") : Color.web("#222222");
        Color pathColor = Color.web("#00bcd4");
        Color playerColor = Color.web("#e91e63");
        Color headColor = Color.web("#ffeb3b");
        Color entranceColor = Color.web("#4caf50");
        Color exitColor = Color.web("#f44336");
        Color fogColor = darkMode ? Color.web("#111111") : Color.web("#d0d0d0");
        Color exploredDimColor = darkMode ? Color.web("#2a2a2a") : Color.web("#e0e0e0");
        Color reachableColor = darkMode ? Color.web("#155c66") : Color.web("#b6e8ec");
        Color hoverColor = darkMode ? Color.web("#946018") : Color.web("#ffe0a3");

        // CLEAR BACKGROUND

        gc.setGlobalAlpha(1.0);
        gc.setFill(background);
        gc.fillRect(0, 0, width, height);

        if (grid == null || !grid.isPolygonal()) {
            return;
        }

        // BUILD EDGE INDEX FROM ACTUAL GEOMETRY

        Map<SegmentKey, List<EdgeOccurrence>> physicalEdges = buildPhysicalEdgeIndex(grid);

        // FOG DISTANCES

        Map<Cell, Integer> distances = fogOfWarActive ? graphDistances(playerCell, Math.max(0, fogRadius)) : Collections.emptyMap();

        // SOLUTION

        Set<Cell> solution = solvedPath == null ? Collections.emptySet() : new HashSet<>(solvedPath);

        // CELL BACKGROUNDS

        for (Cell cell : grid.getAllCells()) {
            if (!(cell instanceof PolygonCell polygon)) {
                continue;
            }
            boolean discovered = !fogOfWarActive || cell.discovered;
            Color fill;
            if (!discovered) {
                fill = fogColor;
            } else if (cell.isEntrance) {
                fill = entranceColor;
            } else if (cell.isExit) {
                fill = exitColor;
            } else if (cell == hoveredCell) {
                fill = hoverColor;
            } else if (highlightedCells.contains(cell)) {
                fill = reachableColor;
            } else if (solution.contains(cell)) {
                fill = pathColor;
            } else {
                fill = background;
            }
            fillPolygon(gc, polygon, fill
            );

            if (fogOfWarActive && discovered && distances.getOrDefault(cell, Integer.MAX_VALUE) > fogRadius && !cell.isEntrance && !cell.isExit) {
                gc.setGlobalAlpha(0.78);
                fillPolygon(gc, polygon, exploredDimColor);
                gc.setGlobalAlpha(1.0);
            }
        }

        // DRAW INTERNAL WALLS

        gc.setStroke(wallColor);
        gc.setLineWidth(INTERNAL_WALL_WIDTH);
        for (Map.Entry<SegmentKey, List<EdgeOccurrence>> entry : physicalEdges.entrySet()) {
            List<EdgeOccurrence> occurrences = entry.getValue();

            if (occurrences.size() == 1) {
                continue;
            }

            if (occurrences.size() != 2) {
                continue;
            }

            EdgeOccurrence first = occurrences.get(0);
            EdgeOccurrence second = occurrences.get(1);
            PolygonCell firstCell = first.cell();
            PolygonCell secondCell = second.cell();

            if (fogOfWarActive && !firstCell.discovered && !secondCell.discovered) {
                continue;
            }

            boolean passageOpen = firstCell.isPassageOpenTo(secondCell);

            if (passageOpen) {
                continue;
            }

            strokePolygonEdge(gc, firstCell, first.edgeIndex());
        }

        // DRAW EXTERIOR WALLS
        // IMPORTANT:
        // These walls are drawn independently of Fog of War.
        // The outside border must always be visible.

        gc.setStroke(exteriorWallColor);
        gc.setLineWidth(EXTERIOR_WALL_WIDTH);
        int exteriorEdgeCount = 0;
        int openingEdgeCount = 0;

        for (Map.Entry<SegmentKey, List<EdgeOccurrence>> entry : physicalEdges.entrySet()) {
            List<EdgeOccurrence> occurrences = entry.getValue();

            if (occurrences.size() != 1) {
                continue;
            }

            EdgeOccurrence occurrence = occurrences.get(0);
            PolygonCell polygon = occurrence.cell();
            int edgeIndex = occurrence.edgeIndex();
            exteriorEdgeCount++;

            if (polygon.getOpeningEdgeIndex() == edgeIndex && (polygon.isEntrance || polygon.isExit)) {
                openingEdgeCount++;
                continue;
            }

            strokePolygonEdge(gc, polygon, edgeIndex);
        }

        // ENTRANCE / EXIT MARKERS

        for (Cell cell : grid.getAllCells()) {
            if (!(cell instanceof PolygonCell polygon)) {
                continue;
            }
            if (fogOfWarActive && !cell.discovered) {
                continue;
            }
            double shortestEdge = Double.POSITIVE_INFINITY;
            for (int edge = 0; edge < polygon.getEdgeCount(); edge++) {
                int next = (edge + 1) % polygon.getEdgeCount();
                double edgeLength = Math.hypot(polygon.getVertexX(next) - polygon.getVertexX(edge), polygon.getVertexY(next) - polygon.getVertexY(edge));
                shortestEdge = Math.min(shortestEdge, edgeLength);
            }

            double markerDiameter = Math.max(5.0, Math.min(12.0, shortestEdge * 0.48));
            if (cell.isEntrance) {
                EndpointMarkerRenderer.draw(gc, cell.getScreenX(), cell.getScreenY(), markerDiameter, entranceColor, "S");
            } else if (cell.isExit) {
                EndpointMarkerRenderer.draw(gc, cell.getScreenX(), cell.getScreenY(), markerDiameter, exitColor, "E");
            }
        }

        // GENERATION HEAD

        if (currentHead instanceof PolygonCell headCell && (!fogOfWarActive || currentHead.discovered)) {
            drawMarker(gc, headCell.getScreenX(), headCell.getScreenY(), headColor, 0.75);
        }

        // PLAYER

        if (playerCell instanceof PolygonCell polygon) {
            double x = playerScreenX == null ? polygon.getScreenX() : playerScreenX;
            double y = playerScreenY == null ? polygon.getScreenY() : playerScreenY;
            drawMarker(gc, x, y, playerColor, 1.0);
        }
        gc.setGlobalAlpha(1.0);
    }

    // DRAW ONE POLYGON EDGE

    private static void strokePolygonEdge(GraphicsContext gc, PolygonCell polygon, int edge) {
        int next = (edge + 1) % polygon.getEdgeCount();
        gc.strokeLine(
                polygon.getVertexX(edge),
                polygon.getVertexY(edge),
                polygon.getVertexX(next),
                polygon.getVertexY(next)
        );
    }

    // FILL POLYGON

    private static void fillPolygon(GraphicsContext gc, PolygonCell polygon, Color color) {
        gc.setFill(color);
        gc.fillPolygon(
                polygon.getVerticesX(),
                polygon.getVerticesY(),
                polygon.getEdgeCount()
        );
    }

    // PLAYER / GENERATION MARKER

    private static void drawMarker(GraphicsContext gc, double x, double y, Color color, double opacity) {
        double radius = 6.0;
        gc.setGlobalAlpha(opacity);
        gc.setFill(color);
        gc.fillOval(
                x - radius,
                y - radius,
                radius * 2.0,
                radius * 2.0
        );
        gc.setGlobalAlpha(1.0);
    }

    // FOG OF WAR GRAPH DISTANCES

    private static Map<Cell, Integer> graphDistances(Cell start, int maximumDistance) {
        Map<Cell, Integer> distances = new HashMap<>();
        if (start == null) {
            return distances;
        }
        Queue<Cell> queue = new ArrayDeque<>();
        queue.add(start);
        distances.put(start, 0);
        while (!queue.isEmpty()) {
            Cell current = queue.remove();
            int distance = distances.get(current);
            if (distance >= maximumDistance) {
                continue;
            }

            for (Cell neighbor : current.getAdjacentCells()) {
                if (!distances.containsKey(neighbor)) {
                    distances.put(neighbor, distance + 1);
                    queue.add(neighbor);
                }
            }
        }
        return distances;
    }
}
