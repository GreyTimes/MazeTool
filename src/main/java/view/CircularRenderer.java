
package view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import model.Cell;
import model.MazeGrid;
import model.PolarCell;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class CircularRenderer implements MazeRenderer {

    private static final double PADDING = 18.0;
    private static final double START_ANGLE = -Math.PI / 2.0;
    private static final double TWO_PI = Math.PI * 2.0;

    private boolean darkMode = true;

    private Set<Cell> highlightedCells = Collections.emptySet();

    private Cell hoveredCell;

    private Double playerScreenX;
    private Double playerScreenY;

    @Override
    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    @Override
    public void setHighlightedCells(Collection<Cell> cells) {
        highlightedCells = cells == null
                ? Collections.emptySet()
                : new HashSet<>(cells);
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

    /**
     * Converts a mouse position into a polar maze cell.
     */
    @Override
    public Cell findCellAt(
            double x,
            double y,
            double width,
            double height,
            MazeGrid grid
    ) {
        if (grid == null || !grid.isCircular()) {
            return null;
        }

        double cx = width / 2.0;
        double cy = height / 2.0;

        double scale = Math.max(
                1.0,
                Math.min(width, height) / 2.0 - PADDING
        );

        double dx = x - cx;
        double dy = y - cy;

        double radius = Math.hypot(dx, dy) / scale;

        if (radius > 1.0 + 1.0e-9) {
            return null;
        }

        double angle = Math.atan2(dy, dx);

        angle = START_ANGLE
                + positiveModulo(angle - START_ANGLE, TWO_PI);

        for (Cell cell : grid.getAllCells()) {

            if (!(cell instanceof PolarCell polar)) {
                continue;
            }

            if (radius < polar.getInnerRadius() - 1.0e-9
                    || radius > polar.getOuterRadius() + 1.0e-9) {
                continue;
            }

            if (polar.isCore()) {
                return cell;
            }

            if (angle >= polar.getStartAngle() - 1.0e-9
                    && angle <= polar.getEndAngle() + 1.0e-9) {
                return cell;
            }
        }

        return null;
    }

    private static double positiveModulo(
            double value,
            double modulus
    ) {
        double result = value % modulus;
        return result < 0.0 ? result + modulus : result;
    }

    @Override
    public void render(
            GraphicsContext gc,
            double width,
            double height,
            MazeGrid grid,
            Cell playerCell,
            java.util.List<Cell> solvedPath,
            Cell currentHead,
            boolean fogOfWarActive,
            int fogRadius
    ) {

        Color background = darkMode
                ? Color.web("#1e1e1e")
                : Color.web("#f5f5f5");

        Color wall = darkMode
                ? Color.web("#4a4a4a")
                : Color.web("#333333");

        Color path = Color.web("#00bcd4");
        Color player = Color.web("#e91e63");
        Color head = Color.web("#ffeb3b");
        Color entrance = Color.web("#4caf50");
        Color exit = Color.web("#f44336");

        Color fog = darkMode
                ? Color.web("#111111")
                : Color.web("#d0d0d0");

        Color exploredDim = darkMode
                ? Color.web("#2a2a2a")
                : Color.web("#e0e0e0");

        Color reachableFill = darkMode
                ? Color.web("#155c66")
                : Color.web("#b6e8ec");

        Color hoverFill = darkMode
                ? Color.web("#946018")
                : Color.web("#ffe0a3");

        gc.setGlobalAlpha(1.0);
        gc.setFill(background);
        gc.fillRect(0, 0, width, height);

        if (grid == null || !grid.isCircular()) {
            return;
        }

        double centerX = width / 2.0;
        double centerY = height / 2.0;

        double scale = Math.max(
                1.0,
                Math.min(width, height) / 2.0 - PADDING
        );

        int radius = Math.max(0, fogRadius);

        Map<Cell, Integer> playerDistances = fogOfWarActive
                ? graphDistances(playerCell, radius)
                : Collections.emptyMap();

        Set<Cell> solution = solvedPath == null
                ? Collections.emptySet()
                : new HashSet<>(solvedPath);

        // Draw cell backgrounds and interaction highlights.
        for (Cell cell : grid.getAllCells()) {

            if (!(cell instanceof PolarCell polar)) {
                continue;
            }

            boolean discovered = !fogOfWarActive || cell.discovered;

            Color fill;

            if (!discovered) {
                fill = fog;

            } else if (cell.isEntrance) {
                fill = entrance;

            } else if (cell.isExit) {
                fill = exit;

            } else if (cell == hoveredCell) {
                fill = hoverFill;

            } else if (highlightedCells.contains(cell)) {
                fill = reachableFill;

            } else if (solution.contains(cell)) {
                fill = path;

            } else {
                fill = background;
            }

            fillCell(
                    gc,
                    polar,
                    centerX,
                    centerY,
                    scale,
                    fill
            );

            if (fogOfWarActive
                    && discovered
                    && playerDistances.getOrDefault(
                    cell,
                    Integer.MAX_VALUE
            ) > radius
                    && !cell.isEntrance
                    && !cell.isExit) {

                gc.setGlobalAlpha(0.78);

                fillCell(
                        gc,
                        polar,
                        centerX,
                        centerY,
                        scale,
                        exploredDim
                );

                gc.setGlobalAlpha(1.0);
            }
        }

        // Draw radial and circular walls.
        gc.setStroke(wall);

        gc.setLineWidth(
                Math.max(
                        1.2,
                        Math.min(
                                3.0,
                                scale / (grid.getPolarRingCount() * 8.0)
                        )
                )
        );

        for (Cell cell : grid.getAllCells()) {

            if (!(cell instanceof PolarCell polar)) {
                continue;
            }

            if (fogOfWarActive && !cell.discovered) {
                continue;
            }

            if (polar.getRingIndex() > 0) {
                drawAngularBoundary(
                        gc,
                        polar,
                        centerX,
                        centerY,
                        scale
                );
            }

            drawOuterBoundary(
                    gc,
                    polar,
                    grid,
                    centerX,
                    centerY,
                    scale
            );
        }

        // ========================================================
// ENTRANCE / EXIT MARKERS
// ========================================================

        double endpointDiameter = Math.max(
                6.0,
                Math.min(13.0, scale * 0.045)
        );

        for (Cell cell : grid.getAllCells()) {

            if (!(cell instanceof PolarCell polar)) {
                continue;
            }

            if (fogOfWarActive && !cell.discovered) {
                continue;
            }

            if (cell.isEntrance) {

                EndpointMarkerRenderer.draw(
                        gc,
                        polar.getScreenX(),
                        polar.getScreenY(),
                        endpointDiameter,
                        entrance,
                        "S"
                );

            } else if (cell.isExit) {

                EndpointMarkerRenderer.draw(
                        gc,
                        polar.getScreenX(),
                        polar.getScreenY(),
                        endpointDiameter,
                        exit,
                        "E"
                );
            }
        }

        // Generation animation marker.
        if (currentHead instanceof PolarCell headCell
                && (!fogOfWarActive || currentHead.discovered)) {

            drawMarker(
                    gc,
                    headCell.getScreenX(),
                    headCell.getScreenY(),
                    scale,
                    head,
                    0.72
            );
        }

        // Player marker, using interpolated position while moving.
        if (playerCell instanceof PolarCell playerPolar) {

            double markerX = playerScreenX == null
                    ? playerPolar.getScreenX()
                    : playerScreenX;

            double markerY = playerScreenY == null
                    ? playerPolar.getScreenY()
                    : playerScreenY;

            drawMarker(
                    gc,
                    markerX,
                    markerY,
                    scale,
                    player,
                    1.0
            );
        }

        gc.setGlobalAlpha(1.0);
    }

    private static Map<Cell, Integer> graphDistances(
            Cell start,
            int maxDistance
    ) {
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

            if (distance >= maxDistance) {
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

    private static void fillCell(
            GraphicsContext gc,
            PolarCell cell,
            double cx,
            double cy,
            double scale,
            Color color
    ) {
        gc.setFill(color);

        if (cell.isCore()) {

            double radius = cell.getOuterRadius() * scale;

            gc.fillOval(
                    cx - radius,
                    cy - radius,
                    radius * 2.0,
                    radius * 2.0
            );

            return;
        }

        double start = cell.getStartAngle();
        double end = cell.getEndAngle();

        int samples = Math.max(
                3,
                Math.min(
                        48,
                        (int) Math.ceil(
                                (end - start)
                                        * cell.getOuterRadius()
                                        * scale / 5.0
                        )
                )
        );

        int pointCount = (samples + 1) * 2;

        double[] xs = new double[pointCount];
        double[] ys = new double[pointCount];

        int index = 0;

        // Outer arc.
        for (int i = 0; i <= samples; i++) {

            double angle = start
                    + (end - start) * i / samples;

            xs[index] = cx
                    + Math.cos(angle)
                    * cell.getOuterRadius()
                    * scale;

            ys[index] = cy
                    + Math.sin(angle)
                    * cell.getOuterRadius()
                    * scale;

            index++;
        }

        // Inner arc in reverse order.
        for (int i = samples; i >= 0; i--) {

            double angle = start
                    + (end - start) * i / samples;

            xs[index] = cx
                    + Math.cos(angle)
                    * cell.getInnerRadius()
                    * scale;

            ys[index] = cy
                    + Math.sin(angle)
                    * cell.getInnerRadius()
                    * scale;

            index++;
        }

        gc.fillPolygon(xs, ys, pointCount);
    }

    /** Draws the radial boundary shared with the previous sector. */
    private static void drawAngularBoundary(
            GraphicsContext gc,
            PolarCell cell,
            double cx,
            double cy,
            double scale
    ) {
        int previousSector = (
                cell.getSectorIndex()
                        - 1
                        + cell.getSectorsInRing()
        ) % cell.getSectorsInRing();

        boolean open = false;

        for (Map.Entry<Cell, Boolean> edge
                : cell.getNeighbors().entrySet()) {

            if (edge.getKey() instanceof PolarCell neighbor
                    && neighbor.getRingIndex() == cell.getRingIndex()
                    && neighbor.getSectorIndex() == previousSector) {

                open = Boolean.TRUE.equals(edge.getValue());
                break;
            }
        }

        if (!open) {

            double angle = cell.getStartAngle();

            double inner = cell.getInnerRadius() * scale;
            double outer = cell.getOuterRadius() * scale;

            gc.strokeLine(
                    cx + Math.cos(angle) * inner,
                    cy + Math.sin(angle) * inner,
                    cx + Math.cos(angle) * outer,
                    cy + Math.sin(angle) * outer
            );
        }
    }

    /** Draws closed outer arcs and leaves the two external openings visible. */
    private static void drawOuterBoundary(
            GraphicsContext gc,
            PolarCell cell,
            MazeGrid grid,
            double cx,
            double cy,
            double scale
    ) {
        int ring = cell.getRingIndex();
        int lastRing = grid.getPolarRingCount();

        if (ring == lastRing) {

            if (!cell.isEntrance && !cell.isExit) {

                strokeArc(
                        gc,
                        cx,
                        cy,
                        cell.getOuterRadius() * scale,
                        cell.getStartAngle(),
                        cell.getEndAngle()
                );
            }

            return;
        }

        for (Map.Entry<Cell, Boolean> edge
                : cell.getNeighbors().entrySet()) {

            if (!(edge.getKey() instanceof PolarCell outer)
                    || outer.getRingIndex() != ring + 1
                    || Boolean.TRUE.equals(edge.getValue())) {
                continue;
            }

            double overlapStart = Math.max(
                    cell.getStartAngle(),
                    outer.getStartAngle()
            );

            double overlapEnd = Math.min(
                    cell.getEndAngle(),
                    outer.getEndAngle()
            );

            if (overlapEnd - overlapStart > 1.0e-10) {

                strokeArc(
                        gc,
                        cx,
                        cy,
                        cell.getOuterRadius() * scale,
                        overlapStart,
                        overlapEnd
                );
            }
        }
    }

    private static void strokeArc(
            GraphicsContext gc,
            double cx,
            double cy,
            double radius,
            double startAngle,
            double endAngle
    ) {
        int samples = Math.max(
                2,
                Math.min(
                        64,
                        (int) Math.ceil(
                                Math.abs(endAngle - startAngle)
                                        * radius / 4.0
                        )
                )
        );

        gc.beginPath();

        for (int i = 0; i <= samples; i++) {

            double angle = startAngle
                    + (endAngle - startAngle) * i / samples;

            double x = cx + Math.cos(angle) * radius;
            double y = cy + Math.sin(angle) * radius;

            if (i == 0) {
                gc.moveTo(x, y);
            } else {
                gc.lineTo(x, y);
            }
        }

        gc.stroke();
    }

    private static void drawMarker(
            GraphicsContext gc,
            double x,
            double y,
            double scale,
            Color color,
            double opacity
    ) {
        double markerRadius = Math.max(
                3.0,
                Math.min(8.0, scale * 0.022)
        );

        gc.setGlobalAlpha(opacity);
        gc.setFill(color);

        gc.fillOval(
                x - markerRadius,
                y - markerRadius,
                markerRadius * 2.0,
                markerRadius * 2.0
        );

        gc.setGlobalAlpha(1.0);
    }
}
