package model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Stores a maze as a graph while retaining the Cell[][] lookup
 * used by the rectangular renderer.
 * Circular, triangular and hexagonal topologies use the generic
 * cell collection and graph adjacency.
 */
public class MazeGrid {
    private static final double TWO_PI = Math.PI * 2.0;
    private static final double SQRT_3 = Math.sqrt(3.0);
    private static final double TRIANGLE_HEIGHT = SQRT_3 / 2.0;
    private static final double EDGE_QUANTIZATION = 1_000_000.0;
    public final int cols;
    public final int rows;
    public Cell[][] cells;
    private final List<Cell> allCells = new ArrayList<>();
    private TopologyType topology;
    private int polarRingCount;
    private Cell entranceCell;
    private Cell exitCell;

    private record BoundaryEdgeCandidate(PolygonCell cell, int edgeIndex, double projection) { }

    // EDGE MATCHING DATA

    private record PointKey(long x, long y) implements Comparable<PointKey> {
        static PointKey of(double x, double y) {
            return new PointKey(Math.round(x * EDGE_QUANTIZATION), Math.round(y * EDGE_QUANTIZATION));
        }

        @Override
        public int compareTo(PointKey other) {
            int byX = Long.compare(x, other.x);
            return byX != 0 ? byX : Long.compare(y, other.y);
        }
    }

    private record EdgeKey(PointKey first, PointKey second) {
        static EdgeKey of(PointKey a, PointKey b) {
            return a.compareTo(b) <= 0 ? new EdgeKey(a, b) : new EdgeKey(b, a);
        }
    }

    private record EdgeReference(PolygonCell cell, int edgeIndex) { }

    // CONSTRUCTORS

    public MazeGrid(int cols, int rows) {
        this(cols, rows, TopologyType.RECTANGULAR);
    }

    public MazeGrid(int width, int height, TopologyType topology) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Maze width and height must both be positive.");
        }

        if (topology == TopologyType.CIRCULAR && (width > 50 || height > 30)) {
            throw new IllegalArgumentException("Circular grid parameters exceed safe safety limits.");
        }

        this.cols = width;
        this.rows = height;
        this.topology = topology == null ? TopologyType.RECTANGULAR : topology;

        try {
            switch (this.topology) {
                case RECTANGULAR -> buildRectangularGrid();
                case CIRCULAR -> initializeCircularGrid();
                case HEXAGONAL -> initializeHexagonalGrid();
                case TRIANGULAR -> initializeTriangularGrid();
            }
        } catch (Exception e) {
            throw new RuntimeException("Topology generation failed for " + topology + ": " + e.getMessage(), e);
        }
    }

    // TOPOLOGY INFORMATION

    public TopologyType getTopology() {return topology;}
    public boolean isCircular() {return topology == TopologyType.CIRCULAR;}
    public boolean isPolygonal() {return topology == TopologyType.HEXAGONAL || topology == TopologyType.TRIANGULAR;}
    /** True for every non-rectangular topology. */
    public boolean isGeometric() {return topology != TopologyType.RECTANGULAR;}

    // STORAGE INITIALIZATION

    public void initializeRectangularGrid() {
        topology = TopologyType.RECTANGULAR;
        polarRingCount = 0;
        entranceCell = null;
        exitCell = null;
        buildRectangularGrid();
    }

    private void resetNonRectangularStorage() {
        allCells.clear();
        cells = new Cell[0][0];
        entranceCell = null;
        exitCell = null;
        polarRingCount = 0;
    }

    // RECTANGULAR GRID

    private void buildRectangularGrid() {
        resetNonRectangularStorage();
        cells = new Cell[cols][rows];
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                Cell cell = new Cell(x, y);
                cell.screenX = x + 0.5;
                cell.screenY = y + 0.5;
                cells[x][y] = cell;
                allCells.add(cell);
            }
        }

        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                Cell current = cells[x][y];
                if (x > 0) {
                    connectCells(current, cells[x - 1][y]);
                }
                if (x < cols - 1) {
                    connectCells(current, cells[x + 1][y]);
                }
                if (y > 0) {
                    connectCells(current, cells[x][y - 1]);
                }
                if (y < rows - 1) {
                    connectCells(current, cells[x][y + 1]);
                }
            }
        }
    }

    // CIRCULAR / POLAR GRID

    private void initializeCircularGrid() {
        resetNonRectangularStorage();
        polarRingCount = rows;
        double radialStep = 1.0 / (polarRingCount + 1.0);
        double angleStart = -Math.PI / 2.0;
        PolarCell core = new PolarCell(0, 0, 1, 0.0, radialStep, angleStart, angleStart + TWO_PI);
        allCells.add(core);
        List<List<PolarCell>> rings = new ArrayList<>();
        rings.add(Collections.singletonList(core));
        for (int ring = 1; ring <= polarRingCount; ring++) {
            int sectorCount = Math.max(6, (int) Math.round(cols * (ring / (double) polarRingCount)));
            List<PolarCell> ringCells = new ArrayList<>(sectorCount);
            double innerRadius = ring * radialStep;
            double outerRadius = (ring + 1.0) * radialStep;
            for (int sector = 0; sector < sectorCount; sector++) {
                double start = angleStart + TWO_PI * sector / sectorCount;
                double end = angleStart + TWO_PI * (sector + 1.0) / sectorCount;
                PolarCell cell = new PolarCell(
                        ring,
                        sector,
                        sectorCount,
                        innerRadius,
                        outerRadius,
                        start,
                        end
                );
                ringCells.add(cell);
                allCells.add(cell);
            }
            rings.add(ringCells);
        }

        for (int ring = 1; ring < rings.size(); ring++) {
            List<PolarCell> ringCells = rings.get(ring);
            for (int sector = 0; sector < ringCells.size(); sector++) {
                connectCells(ringCells.get(sector), ringCells.get((sector + 1) % ringCells.size()));
            }
        }
        for (PolarCell firstRingCell : rings.get(1)) {
            connectCells(core, firstRingCell);
        }

        for (int ring = 1; ring < polarRingCount; ring++) {
            List<PolarCell> innerRing = rings.get(ring);
            List<PolarCell> outerRing = rings.get(ring + 1);
            for (PolarCell inner : innerRing) {
                for (PolarCell outer : outerRing) {
                    double overlapStart = Math.max(inner.getStartAngle(), outer.getStartAngle());
                    double overlapEnd = Math.min(inner.getEndAngle(), outer.getEndAngle());
                    if (overlapEnd - overlapStart > 1.0e-10) {
                        connectCells(inner, outer);
                    }
                }
            }
        }
        layoutCircularGrid(2.0, 2.0, 0.0);
    }

    public void layoutCircularGrid(double width, double height, double padding) {
        if (!isCircular()) return;
        double centerX = width / 2.0;
        double centerY = height / 2.0;
        double scale = Math.max(1.0, Math.min(width, height) / 2.0 - padding);
        for (Cell cell : allCells) {
            PolarCell polar = (PolarCell) cell;
            double radius = polar.getCenterRadius() * scale;
            double angle = polar.getCenterAngle();
            cell.screenX = centerX + radius * Math.cos(angle);
            cell.screenY = centerY + radius * Math.sin(angle);
        }
    }

    // HEXAGONAL GRID

    /**
     * Builds a regular hexagonal maze board.
     * cols is the board-radius parameter R.
     * The outside hexagon has side length:
     *     S = 2R + 1
     * Interior tiles are regular hexagons.
     * Boundary tiles are clipped to the regular outer hexagon.
     */
    private void initializeHexagonalGrid() {
        resetNonRectangularStorage();
        int boardSideLength = 2 * cols + 1;
        double outerApothem = boardSideLength * SQRT_3 / 2.0;
        int coordinateLimit = boardSideLength + 2;
        double[] normalAngles = {
                -Math.PI / 3.0,
                0.0,
                Math.PI / 3.0,
                2.0 * Math.PI / 3.0,
                Math.PI,
                4.0 * Math.PI / 3.0
        };

        for (int q = -coordinateLimit; q <= coordinateLimit; q++) {
            for (int r = -coordinateLimit; r <= coordinateLimit; r++) {
                double centerX = SQRT_3 * (q + r / 2.0);
                double centerY = 1.5 * r;
                List<double[]> polygon = new ArrayList<>(6);
                for (int vertex = 0; vertex < 6; vertex++) {
                    double angle = -Math.PI / 2.0 + vertex * Math.PI / 3.0;
                    polygon.add(new double[]{centerX + Math.cos(angle), centerY + Math.sin(angle)});
                }

                for (double angle : normalAngles) {
                    polygon = clipPolygonAgainstPlane(
                            polygon,
                            Math.cos(angle),
                            Math.sin(angle),
                            outerApothem
                    );
                    if (polygon.size() < 3) {
                        break;
                    }
                }

                if (polygon.size() < 3 || polygonArea(polygon) < 1.0e-8) {
                    continue;
                }


                double[] verticesX = new double[polygon.size()];
                double[] verticesY = new double[polygon.size()];

                for (int i = 0; i < polygon.size(); i++) {
                    verticesX[i] = polygon.get(i)[0];
                    verticesY[i] = polygon.get(i)[1];
                }

                allCells.add(new HexagonalCell(q, r, verticesX, verticesY));
            }
        }


        linkPolygonEdges();
        layoutPolygonGrid(2.0, 2.0, 0.0);
    }

    /**
     * Sutherland-Hodgman polygon clipping.
     * Keeps the part of a polygon satisfying:
     *     nx * x + ny * y <= limit
     */
    private static List<double[]> clipPolygonAgainstPlane(List<double[]> input, double nx, double ny, double limit) {
        List<double[]> output = new ArrayList<>();
        if (input.isEmpty()) {
            return output;
        }
        final double epsilon = 1.0e-9;
        for (int i = 0; i < input.size(); i++) {
            double[] current = input.get(i);
            double[] next = input.get((i + 1) % input.size());
            double currentDistance = current[0] * nx + current[1] * ny - limit;
            double nextDistance = next[0] * nx + next[1] * ny - limit;
            boolean currentInside = currentDistance <= epsilon;
            boolean nextInside = nextDistance <= epsilon;

            if (currentInside && nextInside) {
                output.add(next.clone());
            } else if (currentInside) {
                double t = currentDistance / (currentDistance - nextDistance);
                output.add(new double[]{
                        current[0] + t * (next[0] - current[0]),
                        current[1] + t * (next[1] - current[1])
                });
            } else if (nextInside) {
                double t = currentDistance / (currentDistance - nextDistance);
                output.add(new double[]{
                        current[0] + t * (next[0] - current[0]),
                        current[1] + t * (next[1] - current[1])
                });
                output.add(next.clone());
            }
        }
        return output;
    }

    /** Calculates the absolute area of a polygon. */
    private static double polygonArea(List<double[]> polygon) {
        if (polygon.size() < 3) {
            return 0.0;
        }
        double twiceArea = 0.0;
        for (int i = 0; i < polygon.size(); i++) {
            double[] a = polygon.get(i);
            double[] b = polygon.get((i + 1) % polygon.size());
            twiceArea += a[0] * b[1] - b[0] * a[1];
        }
        return Math.abs(twiceArea) / 2.0;
    }

    // TRIANGULAR GRID

    /**
     * Creates a true equilateral triangular maze.
     * cols is the number of small triangle edges along each
     * side of the complete board.
     * Total cell count = N * N.
     */
    private void initializeTriangularGrid() {
        resetNonRectangularStorage();
        int size = cols;
        for (int j = 0; j < size; j++) {
            for (int i = 0; i < size - j; i++) {
                double[] a = triangularPoint(i, j, size);
                double[] b = triangularPoint(i + 1, j, size);
                double[] c = triangularPoint(i, j + 1, size);
                TriangularCell first = new TriangularCell(i, j, true, new double[]{a[0], b[0], c[0]}, new double[]{a[1], b[1], c[1]});
                allCells.add(first);

                if (i + j < size - 1) {
                    double[] d = triangularPoint(i + 1, j + 1, size);
                    TriangularCell second = new TriangularCell(i, j, false, new double[]{b[0], d[0], c[0]}, new double[]{b[1], d[1], c[1]});
                    allCells.add(second);
                }
            }
        }
        linkPolygonEdges();
        layoutPolygonGrid(2.0, 2.0, 0.0);
    }

    /** Converts a triangular lattice coordinate to model coordinates. */
    private static double[] triangularPoint(int i, int j, int size) {
        double x = i + 0.5 * j;
        double y = (size - j) * TRIANGLE_HEIGHT;
        return new double[]{x, y};
    }

    // AUTOMATIC POLYGON ADJACENCY

    /**
     * Matches polygon edges using their endpoint coordinates.
     *
     * When two polygons share exactly the same edge, both cells receive
     * a graph adjacency and store their corresponding local edge indices.
     */
    private void linkPolygonEdges() {
        Map<EdgeKey, EdgeReference> unmatched = new HashMap<>();
        for (Cell rawCell : allCells) {
            if (!(rawCell instanceof PolygonCell cell)) {
                continue;
            }
            for (int edge = 0; edge < cell.getEdgeCount(); edge++) {
                int next = (edge + 1) % cell.getEdgeCount();
                PointKey a = PointKey.of(cell.getBaseVertexX(edge), cell.getBaseVertexY(edge));
                PointKey b = PointKey.of(cell.getBaseVertexX(next), cell.getBaseVertexY(next));
                EdgeKey key = EdgeKey.of(a, b);
                EdgeReference previous = unmatched.remove(key);
                if (previous == null) {
                    unmatched.put(key, new EdgeReference(cell, edge));
                } else {
                    previous.cell().registerSharedEdge(previous.edgeIndex(), cell, edge);
                }
            }
        }
    }

    // POLYGON LAYOUT

    /**
     * Fits all polygon cells into a canvas while preserving their geometry.
     */
    public void layoutPolygonGrid(double width, double height, double padding) {
        if (!isPolygonal() || allCells.isEmpty()) {
            return;
        }

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (Cell rawCell : allCells) {
            PolygonCell cell = (PolygonCell) rawCell;
            for (int i = 0; i < cell.getEdgeCount(); i++) {
                minX = Math.min(minX, cell.getBaseVertexX(i));
                minY = Math.min(minY, cell.getBaseVertexY(i));
                maxX = Math.max(maxX, cell.getBaseVertexX(i));
                maxY = Math.max(maxY, cell.getBaseVertexY(i));
            }
        }

        double availableWidth = Math.max(1.0, width - 2.0 * padding);
        double availableHeight = Math.max(1.0, height - 2.0 * padding);
        double sourceWidth = Math.max(1.0e-9, maxX - minX);
        double sourceHeight = Math.max(1.0e-9, maxY - minY);
        double scale = Math.min(availableWidth / sourceWidth, availableHeight / sourceHeight);
        double offsetX = (width - sourceWidth * scale) / 2.0 - minX * scale;
        double offsetY = (height - sourceHeight * scale) / 2.0 - minY * scale;

        for (Cell rawCell : allCells) {
            PolygonCell cell = (PolygonCell) rawCell;
            cell.applyLayout(scale, offsetX, offsetY
            );
        }
    }

    public void layoutHexagonalGrid(double width, double height, double padding) {
        if (topology == TopologyType.HEXAGONAL) {
            layoutPolygonGrid(width, height, padding);
        }
    }

    public void layoutTriangularGrid(double width, double height, double padding) {
        if (topology == TopologyType.TRIANGULAR) {
            layoutPolygonGrid(width, height, padding);
        }
    }

    // COMMON GRAPH API

    public void connectCells(Cell a, Cell b) {
        if (a == null || b == null || a == b) {
            return;
        }
        a.addNeighbor(b);
        b.addNeighbor(a);
    }

    public Cell getCell(int x, int y) {
        if (isCircular() || isPolygonal() || cells == null || x < 0 || x >= cols || y < 0 || y >= rows) {
            return null;
        }
        return cells[x][y];
    }

    public List<Cell> getAllCells() {
        return allCells;
    }

    public int getCellCount() {
        return allCells.size();
    }

    public int getPolarRingCount() {
        return polarRingCount;
    }

    public Cell getEntranceCell() {
        if (entranceCell != null) {
            return entranceCell;
        }
        for (Cell cell : allCells) {
            if (cell.isEntrance) {
                return cell;
            }
        }
        return null;
    }

    public Cell getExitCell() {
        if (exitCell != null) {
            return exitCell;
        }
        for (Cell cell : allCells) {
            if (cell.isExit) {
                return cell;
            }
        }
        return null;
    }

    // WALL REMOVAL

    /**
     * Opens an existing graph connection.
     * Rectangular cells also update their original four wall flags.
     * Polygonal and polar cells use their graph representation.
     */
    public void removeWalls(Cell a, Cell b) {
        if (a == null || b == null) {
            return;
        }
        if (!a.getNeighbors().containsKey(b) || !b.getNeighbors().containsKey(a)) {
            return;
        }
        a.getNeighbors().put(b, true);
        b.getNeighbors().put(a, true);
        if (a instanceof PolygonCell || b instanceof PolygonCell || a instanceof PolarCell || b instanceof PolarCell) {
            return;
        }

        int dx = b.x - a.x;
        int dy = b.y - a.y;

        if (dx == 0 && dy == -1) {
            a.walls[0] = false;
            b.walls[2] = false;
        } else if (dx == 0 && dy == 1) {
            a.walls[2] = false;
            b.walls[0] = false;
        } else if (dx == 1 && dy == 0) {
            a.walls[1] = false;
            b.walls[3] = false;
        } else if (dx == -1 && dy == 0) {
            a.walls[3] = false;
            b.walls[1] = false;
        }
    }

    // RECTANGULAR OPENINGS

    /**
     * Creates entrance and exit points according to the selected mode.
     * CUSTOM deliberately creates no automatic openings. The application
     * will wait for the user to click the entrance and exit cells.
     */
    public void createOpenings(EntranceExitMode mode) {
        if (mode == null) {
            mode = EntranceExitMode.LEFT_TO_RIGHT;
        }
        clearEntranceAndExitFlags();
        if (mode == EntranceExitMode.CUSTOM) {
            return;
        }
        switch (topology) {
            case RECTANGULAR -> {
                if (mode == EntranceExitMode.LEFT_TO_RIGHT) {
                    createDirectionalRectangularOpenings(true);
                } else if (mode == EntranceExitMode.TOP_TO_BOTTOM) {
                    createDirectionalRectangularOpenings(false);
                } else {
                    throw new IllegalArgumentException("Center to Outer Edge is only available for circular mazes.");
                }
            }
            case CIRCULAR -> {
                switch (mode) {
                    case LEFT_TO_RIGHT -> createDirectionalCircularOpenings(-1.0, 0.0, 1.0, 0.0);
                    case TOP_TO_BOTTOM -> createDirectionalCircularOpenings(0.0, -1.0, 0.0, 1.0);
                    case CENTER_TO_OUTER -> createCenterToOuterOpenings();
                    default -> throw new IllegalStateException("Unexpected circular opening mode: " + mode);
                }
            }

            case HEXAGONAL, TRIANGULAR -> {
                if (mode == EntranceExitMode.LEFT_TO_RIGHT) {
                    createDirectionalPolygonOpenings(1.0, 0.0);
                } else if (mode == EntranceExitMode.TOP_TO_BOTTOM) {
                    createDirectionalPolygonOpenings(0.0, 1.0);
                } else {
                    throw new IllegalArgumentException("Center to Outer Edge is only available for circular mazes.");
                }
            }
        }
    }

    // RECTANGULAR OPENINGS

    private void createDirectionalRectangularOpenings(boolean horizontal) {
        List<Cell> starts = new ArrayList<>();
        List<Cell> ends = new ArrayList<>();
        for (Cell cell : allCells) {
            if (horizontal) {
                if (cell.x == 0) {
                    starts.add(cell);
                }
                if (cell.x == cols - 1) {
                    ends.add(cell);
                }
            } else {
                if (cell.y == 0) {
                    starts.add(cell);
                }
                if (cell.y == rows - 1) {
                    ends.add(cell);
                }
            }
        }
        Cell[] pair = chooseFarthestPair(starts, ends);
        assignEndpoints(pair[0], pair[1]);
        if (horizontal) {
            // 3 = Left, 1 = Right.
            entranceCell.walls[3] = false;
            exitCell.walls[1] = false;
        } else {
            // 0 = Top, 2 = Bottom.
            entranceCell.walls[0] = false;
            exitCell.walls[2] = false;
        }
    }

    /**
     * Determines which exterior wall to open for a manually selected
     * rectangular cell.
     * Interior cells return -1 because they do not touch the exterior.
     */
    private int chooseCustomRectangularWall(Cell cell, Cell other) {
        List<Integer> boundaryWalls = new ArrayList<>();
        if (cell.y == 0) {
            boundaryWalls.add(0);
        }
        if (cell.x == cols - 1) {
            boundaryWalls.add(1);
        }
        if (cell.y == rows - 1) {
            boundaryWalls.add(2);
        }
        if (cell.x == 0) {
            boundaryWalls.add(3);
        }
        if (boundaryWalls.isEmpty()) {
            return -1;
        }
        double awayX = cell.getScreenX() - other.getScreenX();
        double awayY = cell.getScreenY() - other.getScreenY();
        int bestWall = boundaryWalls.get(0);
        double bestAlignment = Double.NEGATIVE_INFINITY;

        for (int wall : boundaryWalls) {
            double nx = 0.0;
            double ny = 0.0;
            switch (wall) {
                case 0 -> ny = -1.0;
                case 1 -> nx = 1.0;
                case 2 -> ny = 1.0;
                case 3 -> nx = -1.0;
                default -> throw new IllegalStateException("Unexpected rectangular wall index.");
            }
            double alignment = awayX * nx + awayY * ny;
            if (alignment > bestAlignment) {
                bestAlignment = alignment;
                bestWall = wall;
            }
        }
        return bestWall;
    }

    // CIRCULAR OPENINGS

    private List<Cell> getOuterRingCells() {
        List<Cell> outerRing = new ArrayList<>();
        for (Cell cell : allCells) {
            if (cell instanceof PolarCell polar && polar.getRingIndex() == polarRingCount) {
                outerRing.add(cell);
            }
        }
        return outerRing;
    }

    /**
     * Finds outer-ring sectors facing the requested directions.
     *
     * A directional score of at least 0.5 means that a sector lies
     * within 60 degrees of the requested direction.
     */
    private List<Cell> getPolarDirectionalCandidates(List<Cell> outerRing, double directionX, double directionY) {
        List<Cell> candidates = new ArrayList<>();
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Cell cell : outerRing) {
            PolarCell polar = (PolarCell) cell;
            double score = Math.cos(polar.getCenterAngle()) * directionX + Math.sin(polar.getCenterAngle()) * directionY;
            bestScore = Math.max(bestScore, score);
            if (score >= 0.5) {
                candidates.add(cell);
            }
        }

        if (candidates.isEmpty()) {
            for (Cell cell : outerRing) {
                PolarCell polar = (PolarCell) cell;
                double score = Math.cos(polar.getCenterAngle()) * directionX + Math.sin(polar.getCenterAngle()) * directionY;
                if (Math.abs(score - bestScore) < 1.0e-9) {
                    candidates.add(cell);
                }
            }
        }
        return candidates;
    }

    private void createDirectionalCircularOpenings(double startDirectionX, double startDirectionY, double endDirectionX, double endDirectionY) {
        List<Cell> outerRing = getOuterRingCells();
        List<Cell> starts = getPolarDirectionalCandidates(outerRing, startDirectionX, startDirectionY);
        List<Cell> ends = getPolarDirectionalCandidates(outerRing, endDirectionX, endDirectionY);
        Cell[] pair = chooseFarthestPair(starts, ends);
        assignEndpoints(pair[0], pair[1]);
    }

    /**
     * Special circular mode:
     * Entrance = central cell.
     * Exit = outer-ring cell with the greatest shortest-passage distance
     * from the center.
     */
    private void createCenterToOuterOpenings() {
        Cell core = null;
        for (Cell cell : allCells) {
            if (cell instanceof PolarCell polar && polar.isCore()) {
                core = cell;
                break;
            }
        }
        if (core == null) {
            throw new IllegalStateException("Circular maze has no central cell.");
        }
        List<Cell> outerRing = getOuterRingCells();
        Map<Cell, Integer> distances = shortestPassageDistances(core);
        int bestDistance = -1;
        List<Cell> bestExits = new ArrayList<>();
        for (Cell candidate : outerRing) {
            Integer distance = distances.get(candidate);
            if (distance == null) {
                continue;
            }
            if (distance > bestDistance) {
                bestDistance = distance;
                bestExits.clear();
                bestExits.add(candidate);
            } else if (distance == bestDistance) {
                bestExits.add(candidate);
            }
        }
        if (bestExits.isEmpty()) {
            throw new IllegalStateException("No reachable outer-ring exit was found.");
        }
        Cell selectedExit = bestExits.get(new Random().nextInt(bestExits.size()));
        assignEndpoints(core, selectedExit);
    }

    // HEXAGONAL / TRIANGULAR OPENINGS

    /**
     * Selects entrance and exit boundary regions using the requested
     * geometric axis.
     * dx = 1, dy = 0 means left-to-right.
     * dx = 0, dy = 1 means top-to-bottom.
     * Actual exterior edge midpoints are used rather than assuming
     * rectangular x/y cell coordinates.
     */
    private void createDirectionalPolygonOpenings(double dx, double dy) {
        List<BoundaryEdgeCandidate> boundaryEdges = new ArrayList<>();
        for (Cell rawCell : allCells) {
            if (!(rawCell instanceof PolygonCell polygon)) {
                continue;
            }
            for (int edge : polygon.getBoundaryEdgeIndices()) {
                int next = (edge + 1) % polygon.getEdgeCount();
                double midpointX = (polygon.getVertexX(edge) + polygon.getVertexX(next)) / 2.0;
                double midpointY = (polygon.getVertexY(edge) + polygon.getVertexY(next)) / 2.0;
                double projection = midpointX * dx + midpointY * dy;
                boundaryEdges.add(new BoundaryEdgeCandidate(polygon, edge, projection));
            }
        }

        if (boundaryEdges.size() < 2) {
            throw new IllegalStateException("Polygon maze needs at least two exterior edges.");
        }

        double minimum = boundaryEdges.stream()
                .mapToDouble(BoundaryEdgeCandidate::projection)
                .min()
                .orElseThrow();

        double maximum = boundaryEdges.stream()
                .mapToDouble(BoundaryEdgeCandidate::projection)
                .max()
                .orElseThrow();

        double tolerance = Math.max(1.0e-8, (maximum - minimum) * 0.25);
        Map<Cell, BoundaryEdgeCandidate> startEdges = new LinkedHashMap<>();
        Map<Cell, BoundaryEdgeCandidate> endEdges = new LinkedHashMap<>();

        for (BoundaryEdgeCandidate candidate : boundaryEdges) {
            if (candidate.projection() <= minimum + tolerance) {
                startEdges.merge(candidate.cell(), candidate, (a, b) -> a.projection() <= b.projection() ? a : b);
            }

            if (candidate.projection() >= maximum - tolerance) {
                endEdges.merge(candidate.cell(), candidate, (a, b) -> a.projection() >= b.projection() ? a : b);
            }
        }

        Cell[] pair = chooseFarthestPair(new ArrayList<>(startEdges.keySet()), new ArrayList<>(endEdges.keySet()));
        PolygonCell start = (PolygonCell) pair[0];
        PolygonCell end = (PolygonCell) pair[1];
        start.setOpeningEdgeIndex(startEdges.get(start).edgeIndex());
        end.setOpeningEdgeIndex(endEdges.get(end).edgeIndex());
        assignEndpoints(start, end);
    }

    /**
     * Chooses an exterior edge for a custom polygon selection.
     * The edge that faces most directly away from the board center
     * is used. Interior polygon cells have no boundary edge and
     * therefore receive no exterior opening.
     */
    private int chooseCustomPolygonEdge(PolygonCell cell) {
        List<Integer> boundaryEdges = cell.getBoundaryEdgeIndices();
        if (boundaryEdges.isEmpty()) {
            return -1;
        }
        double boardCenterX = 0.0;
        double boardCenterY = 0.0;
        for (Cell candidate : allCells) {
            boardCenterX += candidate.getScreenX();
            boardCenterY += candidate.getScreenY();
        }
        boardCenterX /= allCells.size();
        boardCenterY /= allCells.size();

        double outwardX = cell.getScreenX() - boardCenterX;
        double outwardY = cell.getScreenY() - boardCenterY;
        double magnitude = Math.hypot(outwardX, outwardY);

        if (magnitude < 1.0e-9) {
            outwardX = 1.0;
            outwardY = 0.0;
        } else {
            outwardX /= magnitude;
            outwardY /= magnitude;
        }

        int bestEdge = boundaryEdges.get(0);
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int edge : boundaryEdges) {
            int next = (edge + 1) % cell.getEdgeCount();
            double midpointX = (cell.getVertexX(edge) + cell.getVertexX(next)) / 2.0;
            double midpointY = (cell.getVertexY(edge) + cell.getVertexY(next)) / 2.0;
            double edgeX = midpointX - cell.getScreenX();
            double edgeY = midpointY - cell.getScreenY();
            double score = edgeX * outwardX + edgeY * outwardY;
            if (score > bestScore) {
                bestScore = score;
                bestEdge = edge;
            }
        }
        return bestEdge;
    }

    // CUSTOM CLICK SELECTION

    /**
     * Displays the first custom selection before the exit is chosen.
     * This lets the renderer display the START marker immediately.
     */
    public void setCustomEntrancePreview(Cell start) {
        if (start == null || !allCells.contains(start)) {
            throw new IllegalArgumentException("The selected entrance must belong to this maze.");
        }
        clearEntranceAndExitFlags();
        entranceCell = start;
        start.isEntrance = true;
        start.discovered = true;
    }

    /**
     * Applies two cells selected by the user.
     * Interior cells are valid endpoints. An exterior wall is opened
     * only when the selected cell actually lies on the board boundary.
     */
    public void createCustomOpenings(Cell start, Cell end) {
        if (start == null || end == null || !allCells.contains(start) || !allCells.contains(end)) {
            throw new IllegalArgumentException("Both custom endpoints must belong to this maze.");
        }

        if (start == end) {
            throw new IllegalArgumentException("Entrance and exit must be different cells.");
        }

        clearEntranceAndExitFlags();
        assignEndpoints(start, end);

        switch (topology) {
            case RECTANGULAR -> {
                int startWall = chooseCustomRectangularWall(start, end);
                int endWall = chooseCustomRectangularWall(end, start);
                if (startWall >= 0) {
                    start.walls[startWall] = false;
                }
                if (endWall >= 0) {
                    end.walls[endWall] = false;
                }
            }
            case CIRCULAR -> {
                // The circular renderer opens the outer arc automatically
                // for selected entrance/exit cells on the outermost ring.
                // Interior and core endpoints remain internal positions.
            }
            case HEXAGONAL, TRIANGULAR -> {
                PolygonCell polygonStart = (PolygonCell) start;
                PolygonCell polygonEnd = (PolygonCell) end;
                polygonStart.setOpeningEdgeIndex(chooseCustomPolygonEdge(polygonStart));
                polygonEnd.setOpeningEdgeIndex(chooseCustomPolygonEdge(polygonEnd));
            }
        }
    }

    // SHARED ENDPOINT ASSIGNMENT

    private void assignEndpoints(Cell start, Cell end) {
        entranceCell = start;
        exitCell = end;
        entranceCell.isEntrance = true;
        entranceCell.discovered = true;
        exitCell.isExit = true;
    }

    // FARTHEST PAIR SELECTION

    /**
     * Finds the pair with the greatest shortest-path distance,
     * where the first cell must come from starts and the second
     * cell must come from ends.
     */
    private Cell[] chooseFarthestPair(List<Cell> starts, List<Cell> ends) {
        if (starts.isEmpty() || ends.isEmpty()) {
            throw new IllegalStateException("At least one candidate is required on each side.");
        }
        int bestDistance = -1;
        List<Cell[]> bestPairs = new ArrayList<>();
        for (Cell source : starts) {
            Map<Cell, Integer> distances = shortestPassageDistances(source);
            for (Cell target : ends) {
                if (source == target) {
                    continue;
                }
                Integer distance = distances.get(target);
                if (distance == null) {
                    continue;
                }
                if (distance > bestDistance) {
                    bestDistance = distance;
                    bestPairs.clear();
                    bestPairs.add(new Cell[]{source, target});
                } else if (distance == bestDistance) {
                    bestPairs.add(new Cell[]{source, target});
                }
            }
        }
        if (bestPairs.isEmpty()) {
            throw new IllegalStateException("Maze graph is disconnected or has no distinct endpoint pair.");
        }
        return bestPairs.get(new Random().nextInt(bestPairs.size()));
    }

    /**
     * Breadth-first search through OPEN passages only.
     */
    private Map<Cell, Integer> shortestPassageDistances(Cell start) {
        Map<Cell, Integer> distances = new LinkedHashMap<>();
        ArrayDeque<Cell> queue = new ArrayDeque<>();
        distances.put(start, 0);
        queue.add(start);

        while (!queue.isEmpty()) {
            Cell current = queue.remove();
            int nextDistance = distances.get(current) + 1;
            for (Cell neighbor : current.getOpenNeighbors()) {
                if (!distances.containsKey(neighbor)) {
                    distances.put(neighbor, nextDistance);
                    queue.add(neighbor);
                }
            }
        }
        return distances;
    }

    // COMPATIBILITY METHODS

    /**
     * Retained for compatibility with older application code.
     */
    public void createRectangularOpenings() {
        if (topology == TopologyType.RECTANGULAR) {
            createOpenings(EntranceExitMode.LEFT_TO_RIGHT);
        }
    }


    public void createCircularOpenings() {
        if (topology == TopologyType.CIRCULAR) {
            createOpenings(EntranceExitMode.LEFT_TO_RIGHT);
        }
    }


    public void createHexagonalOpenings() {
        if (topology == TopologyType.HEXAGONAL) {
            createOpenings(EntranceExitMode.LEFT_TO_RIGHT);
        }
    }


    public void createTriangularOpenings() {
        if (topology == TopologyType.TRIANGULAR) {
            createOpenings(EntranceExitMode.LEFT_TO_RIGHT);
        }
    }

    // RESET OPENINGS

    private void clearEntranceAndExitFlags() {
        entranceCell = null;
        exitCell = null;
        for (Cell cell : allCells) {
            cell.isEntrance = false;
            cell.isExit = false;
            if (cell instanceof PolygonCell polygon) {
                polygon.setOpeningEdgeIndex(-1);
            }
        }
    }

    // DEAD END STATISTICS

    public int countDeadEnds() {
        int deadEnds = 0;
        for (Cell cell : allCells) {
            if (!cell.isEntrance && !cell.isExit && cell.getOpenDegree() == 1) {
                deadEnds++;
            }
        }
        return deadEnds;
    }

    // COMPLEXITY / LOOP CARVING

    public void applyComplexityFactor(double complexityFactor) {
        double clamped = Math.max(0.0, Math.min(1.0, complexityFactor));
        double removalChance = 1.0 - clamped;
        Random random = new Random();
        for (Cell cell : allCells) {
            if (cell.getOpenDegree() != 1 || random.nextDouble() >= removalChance) {
                continue;
            }
            List<Cell> closedNeighbors = new ArrayList<>();
            for (Map.Entry<Cell, Boolean> entry : cell.getNeighbors().entrySet()) {
                if (!entry.getValue()) {
                    closedNeighbors.add(entry.getKey());
                }
            }

            if (!closedNeighbors.isEmpty()) {
                removeWalls(cell, closedNeighbors.get(random.nextInt(closedNeighbors.size())));
            }
        }
    }
}
