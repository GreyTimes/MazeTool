
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
 *
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


    // ============================================================
    // EDGE MATCHING DATA
    // ============================================================

    private record PointKey(long x, long y)
            implements Comparable<PointKey> {

        static PointKey of(double x, double y) {

            return new PointKey(
                    Math.round(x * EDGE_QUANTIZATION),
                    Math.round(y * EDGE_QUANTIZATION)
            );
        }

        @Override
        public int compareTo(PointKey other) {

            int byX = Long.compare(x, other.x);

            return byX != 0
                    ? byX
                    : Long.compare(y, other.y);
        }
    }


    private record EdgeKey(
            PointKey first,
            PointKey second
    ) {

        static EdgeKey of(PointKey a, PointKey b) {

            return a.compareTo(b) <= 0
                    ? new EdgeKey(a, b)
                    : new EdgeKey(b, a);
        }
    }


    private record EdgeReference(
            PolygonCell cell,
            int edgeIndex
    ) { }


    // ============================================================
    // CONSTRUCTORS
    // ============================================================

    public MazeGrid(int cols, int rows) {

        this(
                cols,
                rows,
                TopologyType.RECTANGULAR
        );
    }


    public MazeGrid(
            int width,
            int height,
            TopologyType topology
    ) {

        if (width < 1 || height < 1) {

            throw new IllegalArgumentException(
                    "Maze width and height must both be positive."
            );
        }

        this.cols = width;
        this.rows = height;

        this.topology = topology == null
                ? TopologyType.RECTANGULAR
                : topology;

        switch (this.topology) {

            case RECTANGULAR ->
                    buildRectangularGrid();

            case CIRCULAR ->
                    initializeCircularGrid();

            case HEXAGONAL ->
                    initializeHexagonalGrid();

            case TRIANGULAR ->
                    initializeTriangularGrid();
        }
    }


    // ============================================================
    // TOPOLOGY INFORMATION
    // ============================================================

    public TopologyType getTopology() {
        return topology;
    }


    public boolean isCircular() {
        return topology == TopologyType.CIRCULAR;
    }


    public boolean isPolygonal() {

        return topology == TopologyType.HEXAGONAL
                || topology == TopologyType.TRIANGULAR;
    }


    /** True for every non-rectangular topology. */
    public boolean isGeometric() {

        return topology != TopologyType.RECTANGULAR;
    }


    // ============================================================
    // STORAGE INITIALIZATION
    // ============================================================

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


    // ============================================================
    // RECTANGULAR GRID
    // ============================================================

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


    // ============================================================
    // CIRCULAR / POLAR GRID
    // ============================================================

    private void initializeCircularGrid() {

        resetNonRectangularStorage();

        polarRingCount = rows;

        double radialStep =
                1.0 / (polarRingCount + 1.0);

        double angleStart = -Math.PI / 2.0;


        // Central cell.

        PolarCell core = new PolarCell(
                0,
                0,
                1,
                0.0,
                radialStep,
                angleStart,
                angleStart + TWO_PI
        );

        allCells.add(core);


        List<List<PolarCell>> rings =
                new ArrayList<>();

        rings.add(
                Collections.singletonList(core)
        );


        // Create concentric rings.

        for (int ring = 1; ring <= polarRingCount; ring++) {

            int sectorCount = Math.max(
                    6,
                    (int) Math.round(
                            cols * (ring / (double) polarRingCount)
                    )
            );

            List<PolarCell> ringCells =
                    new ArrayList<>(sectorCount);

            double innerRadius =
                    ring * radialStep;

            double outerRadius =
                    (ring + 1.0) * radialStep;


            for (int sector = 0;
                 sector < sectorCount;
                 sector++) {

                double start =
                        angleStart
                                + TWO_PI * sector / sectorCount;

                double end =
                        angleStart
                                + TWO_PI * (sector + 1.0) / sectorCount;

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


        // Connect clockwise and counterclockwise neighbors.

        for (int ring = 1; ring < rings.size(); ring++) {

            List<PolarCell> ringCells = rings.get(ring);

            for (int sector = 0;
                 sector < ringCells.size();
                 sector++) {

                connectCells(
                        ringCells.get(sector),
                        ringCells.get(
                                (sector + 1) % ringCells.size()
                        )
                );
            }
        }


        // Connect the core to every first-ring sector.

        for (PolarCell firstRingCell : rings.get(1)) {

            connectCells(core, firstRingCell);
        }


        // Connect adjacent rings using angular overlap.

        for (int ring = 1;
             ring < polarRingCount;
             ring++) {

            List<PolarCell> innerRing = rings.get(ring);

            List<PolarCell> outerRing = rings.get(ring + 1);

            for (PolarCell inner : innerRing) {

                for (PolarCell outer : outerRing) {

                    double overlapStart = Math.max(
                            inner.getStartAngle(),
                            outer.getStartAngle()
                    );

                    double overlapEnd = Math.min(
                            inner.getEndAngle(),
                            outer.getEndAngle()
                    );

                    if (overlapEnd - overlapStart > 1.0e-10) {

                        connectCells(inner, outer);
                    }
                }
            }
        }

        layoutCircularGrid(2.0, 2.0, 0.0);
    }


    public void layoutCircularGrid(
            double width,
            double height,
            double padding
    ) {

        if (!isCircular()) return;

        double centerX = width / 2.0;
        double centerY = height / 2.0;

        double scale = Math.max(
                1.0,
                Math.min(width, height) / 2.0 - padding
        );

        for (Cell cell : allCells) {

            PolarCell polar = (PolarCell) cell;

            double radius =
                    polar.getCenterRadius() * scale;

            double angle =
                    polar.getCenterAngle();

            cell.screenX =
                    centerX + radius * Math.cos(angle);

            cell.screenY =
                    centerY + radius * Math.sin(angle);
        }
    }


    // ============================================================
    // HEXAGONAL GRID
    // ============================================================

    /**
     * Builds a regular hexagonal maze board.
     *
     * cols is the board-radius parameter R.
     *
     * The outside hexagon has side length:
     *
     *     S = 2R + 1
     *
     * Interior tiles are regular hexagons.
     * Boundary tiles are clipped to the regular outer hexagon.
     */
    private void initializeHexagonalGrid() {

        resetNonRectangularStorage();

        int boardSideLength = 2 * cols + 1;

        double outerApothem =
                boardSideLength * SQRT_3 / 2.0;

        int coordinateLimit = boardSideLength + 2;


        // Six outward normals of the regular hexagonal boundary.
        double[] normalAngles = {
                -Math.PI / 3.0,
                0.0,
                Math.PI / 3.0,
                2.0 * Math.PI / 3.0,
                Math.PI,
                4.0 * Math.PI / 3.0
        };


        // Generate enough pointy-top hexagons to cover the clipping area.
        for (int q = -coordinateLimit;
             q <= coordinateLimit;
             q++) {

            for (int r = -coordinateLimit;
                 r <= coordinateLimit;
                 r++) {

                // Axial coordinate to pixel coordinate.
                double centerX =
                        SQRT_3 * (q + r / 2.0);

                double centerY =
                        1.5 * r;


                // Create the original regular hexagon.
                List<double[]> polygon =
                        new ArrayList<>(6);

                for (int vertex = 0;
                     vertex < 6;
                     vertex++) {

                    double angle =
                            -Math.PI / 2.0
                                    + vertex * Math.PI / 3.0;

                    polygon.add(new double[]{
                            centerX + Math.cos(angle),
                            centerY + Math.sin(angle)
                    });
                }


                // Clip against all six sides of the outer hexagon.
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


                // Ignore cells completely outside the board.
                if (polygon.size() < 3
                        || polygonArea(polygon) < 1.0e-8) {

                    continue;
                }


                double[] verticesX =
                        new double[polygon.size()];

                double[] verticesY =
                        new double[polygon.size()];


                for (int i = 0;
                     i < polygon.size();
                     i++) {

                    verticesX[i] =
                            polygon.get(i)[0];

                    verticesY[i] =
                            polygon.get(i)[1];
                }


                allCells.add(
                        new HexagonalCell(
                                q,
                                r,
                                verticesX,
                                verticesY
                        )
                );
            }
        }


        // Create graph adjacency from matching polygon sides.
        linkPolygonEdges();

        layoutPolygonGrid(
                2.0,
                2.0,
                0.0
        );
    }


    /**
     * Sutherland-Hodgman polygon clipping.
     *
     * Keeps the part of a polygon satisfying:
     *
     *     nx * x + ny * y <= limit
     */
    private static List<double[]> clipPolygonAgainstPlane(
            List<double[]> input,
            double nx,
            double ny,
            double limit
    ) {

        List<double[]> output = new ArrayList<>();

        if (input.isEmpty()) {
            return output;
        }

        final double epsilon = 1.0e-9;


        for (int i = 0; i < input.size(); i++) {

            double[] current = input.get(i);

            double[] next =
                    input.get((i + 1) % input.size());


            double currentDistance =
                    current[0] * nx
                            + current[1] * ny
                            - limit;

            double nextDistance =
                    next[0] * nx
                            + next[1] * ny
                            - limit;


            boolean currentInside =
                    currentDistance <= epsilon;

            boolean nextInside =
                    nextDistance <= epsilon;


            if (currentInside && nextInside) {

                output.add(next.clone());

            } else if (currentInside) {

                double t = currentDistance
                        / (currentDistance - nextDistance);

                output.add(new double[]{
                        current[0] + t * (next[0] - current[0]),
                        current[1] + t * (next[1] - current[1])
                });

            } else if (nextInside) {

                double t = currentDistance
                        / (currentDistance - nextDistance);

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
    private static double polygonArea(
            List<double[]> polygon
    ) {

        if (polygon.size() < 3) {
            return 0.0;
        }

        double twiceArea = 0.0;

        for (int i = 0; i < polygon.size(); i++) {

            double[] a = polygon.get(i);

            double[] b =
                    polygon.get((i + 1) % polygon.size());

            twiceArea +=
                    a[0] * b[1] - b[0] * a[1];
        }

        return Math.abs(twiceArea) / 2.0;
    }

    // ============================================================
    // TRIANGULAR GRID
    // ============================================================

    /**
     * Creates a true equilateral triangular maze.
     *
     * cols is the number of small triangle edges along each
     * side of the complete board.
     *
     * Total cell count = N * N.
     */
    private void initializeTriangularGrid() {

        resetNonRectangularStorage();

        int size = cols;


        // Create a triangular lattice.
        //
        // The large board points upward.
        // Its bottom edge is horizontal.
        //
        // Lattice points:
        // P(i,j) = (i + j/2, (N-j) * sqrt(3)/2)

        for (int j = 0; j < size; j++) {

            for (int i = 0;
                 i < size - j;
                 i++) {

                double[] a =
                        triangularPoint(i, j, size);

                double[] b =
                        triangularPoint(i + 1, j, size);

                double[] c =
                        triangularPoint(i, j + 1, size);


                // First triangle: upward orientation.

                TriangularCell first =
                        new TriangularCell(
                                i,
                                j,
                                true,

                                new double[]{
                                        a[0],
                                        b[0],
                                        c[0]
                                },

                                new double[]{
                                        a[1],
                                        b[1],
                                        c[1]
                                }
                        );

                allCells.add(first);


                // Second triangle: inverted orientation.
                //
                // It is omitted along the outer diagonal boundary
                // because that area is outside the large triangle.

                if (i + j < size - 1) {

                    double[] d =
                            triangularPoint(
                                    i + 1,
                                    j + 1,
                                    size
                            );


                    TriangularCell second =
                            new TriangularCell(
                                    i,
                                    j,
                                    false,

                                    new double[]{
                                            b[0],
                                            d[0],
                                            c[0]
                                    },

                                    new double[]{
                                            b[1],
                                            d[1],
                                            c[1]
                                    }
                            );

                    allCells.add(second);
                }
            }
        }


        // Match the shared sides and build graph adjacency.
        linkPolygonEdges();

        layoutPolygonGrid(
                2.0,
                2.0,
                0.0
        );
    }


    /** Converts a triangular lattice coordinate to model coordinates. */
    private static double[] triangularPoint(
            int i,
            int j,
            int size
    ) {

        double x = i + 0.5 * j;

        double y = (size - j) * TRIANGLE_HEIGHT;

        return new double[]{
                x,
                y
        };
    }


    // ============================================================
    // AUTOMATIC POLYGON ADJACENCY
    // ============================================================

    /**
     * Matches polygon edges using their endpoint coordinates.
     *
     * When two polygons share exactly the same edge, both cells receive
     * a graph adjacency and store their corresponding local edge indices.
     */
    private void linkPolygonEdges() {

        Map<EdgeKey, EdgeReference> unmatched =
                new HashMap<>();


        for (Cell rawCell : allCells) {

            if (!(rawCell instanceof PolygonCell cell)) {
                continue;
            }

            for (int edge = 0;
                 edge < cell.getEdgeCount();
                 edge++) {

                int next =
                        (edge + 1) % cell.getEdgeCount();

                PointKey a = PointKey.of(
                        cell.getBaseVertexX(edge),
                        cell.getBaseVertexY(edge)
                );

                PointKey b = PointKey.of(
                        cell.getBaseVertexX(next),
                        cell.getBaseVertexY(next)
                );

                EdgeKey key = EdgeKey.of(a, b);


                EdgeReference previous =
                        unmatched.remove(key);


                if (previous == null) {

                    unmatched.put(
                            key,
                            new EdgeReference(cell, edge)
                    );

                } else {

                    previous.cell().registerSharedEdge(
                            previous.edgeIndex(),
                            cell,
                            edge
                    );
                }
            }
        }
    }


    // ============================================================
    // POLYGON LAYOUT
    // ============================================================

    /**
     * Fits all polygon cells into a canvas while preserving their geometry.
     */
    public void layoutPolygonGrid(
            double width,
            double height,
            double padding
    ) {

        if (!isPolygonal() || allCells.isEmpty()) {
            return;
        }

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;

        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;


        // Calculate original geometry bounds.

        for (Cell rawCell : allCells) {

            PolygonCell cell = (PolygonCell) rawCell;

            for (int i = 0;
                 i < cell.getEdgeCount();
                 i++) {

                minX = Math.min(
                        minX,
                        cell.getBaseVertexX(i)
                );

                minY = Math.min(
                        minY,
                        cell.getBaseVertexY(i)
                );

                maxX = Math.max(
                        maxX,
                        cell.getBaseVertexX(i)
                );

                maxY = Math.max(
                        maxY,
                        cell.getBaseVertexY(i)
                );
            }
        }


        double availableWidth =
                Math.max(1.0, width - 2.0 * padding);

        double availableHeight =
                Math.max(1.0, height - 2.0 * padding);

        double sourceWidth =
                Math.max(1.0e-9, maxX - minX);

        double sourceHeight =
                Math.max(1.0e-9, maxY - minY);


        // Uniform scaling prevents distorted polygons.

        double scale = Math.min(
                availableWidth / sourceWidth,
                availableHeight / sourceHeight
        );


        // Center geometry in the canvas.

        double offsetX =
                (width - sourceWidth * scale) / 2.0
                        - minX * scale;

        double offsetY =
                (height - sourceHeight * scale) / 2.0
                        - minY * scale;


        for (Cell rawCell : allCells) {

            PolygonCell cell = (PolygonCell) rawCell;

            cell.applyLayout(
                    scale,
                    offsetX,
                    offsetY
            );
        }
    }


    public void layoutHexagonalGrid(
            double width,
            double height,
            double padding
    ) {

        if (topology == TopologyType.HEXAGONAL) {

            layoutPolygonGrid(
                    width,
                    height,
                    padding
            );
        }
    }


    public void layoutTriangularGrid(
            double width,
            double height,
            double padding
    ) {

        if (topology == TopologyType.TRIANGULAR) {

            layoutPolygonGrid(
                    width,
                    height,
                    padding
            );
        }
    }


    // ============================================================
    // COMMON GRAPH API
    // ============================================================

    public void connectCells(Cell a, Cell b) {

        if (a == null || b == null || a == b) {
            return;
        }

        a.addNeighbor(b);
        b.addNeighbor(a);
    }


    public Cell getCell(int x, int y) {

        if (isCircular()
                || isPolygonal()
                || cells == null
                || x < 0
                || x >= cols
                || y < 0
                || y >= rows) {

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


    // ============================================================
    // WALL REMOVAL
    // ============================================================

    /**
     * Opens an existing graph connection.
     *
     * Rectangular cells also update their original four wall flags.
     * Polygonal and polar cells use their graph representation.
     */
    public void removeWalls(Cell a, Cell b) {

        if (a == null || b == null) {
            return;
        }

        if (!a.getNeighbors().containsKey(b)
                || !b.getNeighbors().containsKey(a)) {

            return;
        }

        a.getNeighbors().put(b, true);
        b.getNeighbors().put(a, true);


        if (a instanceof PolygonCell
                || b instanceof PolygonCell
                || a instanceof PolarCell
                || b instanceof PolarCell) {

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


    // ============================================================
    // RECTANGULAR OPENINGS
    // ============================================================

    public void createRectangularOpenings() {

        if (isGeometric() || cols < 1 || rows < 1) {
            return;
        }

        clearEntranceAndExitFlags();

        Random random = new Random();

        int entranceY = random.nextInt(rows);
        int exitY = random.nextInt(rows);

        entranceCell = getCell(0, entranceY);
        exitCell = getCell(cols - 1, exitY);

        entranceCell.walls[3] = false;
        entranceCell.isEntrance = true;
        entranceCell.discovered = true;

        exitCell.walls[1] = false;
        exitCell.isExit = true;
    }


    // ============================================================
    // CIRCULAR OPENINGS
    // ============================================================

    public void createCircularOpenings() {

        if (!isCircular() || polarRingCount < 1) {
            return;
        }

        clearEntranceAndExitFlags();

        List<Cell> outerRing = new ArrayList<>();

        for (Cell cell : allCells) {

            if (cell instanceof PolarCell polar
                    && polar.getRingIndex() == polarRingCount) {

                outerRing.add(cell);
            }
        }

        Cell[] pair = chooseFarthestPair(outerRing);

        assignCircularOpenings(pair);
    }


    // ============================================================
    // HEXAGONAL OPENINGS
    // ============================================================

    public void createHexagonalOpenings() {

        if (topology != TopologyType.HEXAGONAL) {
            return;
        }

        createPolygonOpenings();
    }


    // ============================================================
    // TRIANGULAR OPENINGS
    // ============================================================

    public void createTriangularOpenings() {

        if (topology != TopologyType.TRIANGULAR) {
            return;
        }

        createPolygonOpenings();
    }


    /**
     * Selects the boundary cells with the greatest passage distance,
     * then opens one actual exterior edge on each selected cell.
     */
    private void createPolygonOpenings() {

        clearEntranceAndExitFlags();

        List<Cell> boundaryCells = new ArrayList<>();

        for (Cell cell : allCells) {

            if (cell instanceof PolygonCell polygon
                    && !polygon.getBoundaryEdgeIndices().isEmpty()) {

                boundaryCells.add(cell);
            }
        }

        if (boundaryCells.size() < 2) {

            throw new IllegalStateException(
                    "Polygon maze needs at least two boundary cells."
            );
        }


        Cell[] pair = chooseFarthestPair(boundaryCells);

        Random random = new Random();

        PolygonCell first = (PolygonCell) pair[0];
        PolygonCell second = (PolygonCell) pair[1];

        List<Integer> firstEdges =
                first.getBoundaryEdgeIndices();

        List<Integer> secondEdges =
                second.getBoundaryEdgeIndices();


        // Select an actual exterior polygon side for each opening.

        first.setOpeningEdgeIndex(
                firstEdges.get(
                        random.nextInt(firstEdges.size())
                )
        );

        second.setOpeningEdgeIndex(
                secondEdges.get(
                        random.nextInt(secondEdges.size())
                )
        );


        // Randomize entrance and exit roles.

        entranceCell = random.nextBoolean()
                ? first
                : second;

        exitCell = entranceCell == first
                ? second
                : first;


        entranceCell.isEntrance = true;
        entranceCell.discovered = true;

        exitCell.isExit = true;
    }


    // ============================================================
    // FARTHEST PAIR SELECTION
    // ============================================================

    /**
     * Calculates the exact maximum shortest-path distance between
     * any two candidate cells.
     *
     * This is restricted to the supplied candidates:
     * outer-ring cells for circular mazes, boundary cells for polygons.
     */
    private Cell[] chooseFarthestPair(
            List<Cell> candidates
    ) {

        if (candidates.size() < 2) {

            throw new IllegalStateException(
                    "At least two opening candidates are required."
            );
        }

        int bestDistance = -1;

        List<Cell[]> bestPairs = new ArrayList<>();


        for (int i = 0; i < candidates.size(); i++) {

            Cell source = candidates.get(i);

            Map<Cell, Integer> distances =
                    shortestPassageDistances(source);


            for (int j = i + 1;
                 j < candidates.size();
                 j++) {

                Cell target = candidates.get(j);

                Integer distance = distances.get(target);

                if (distance == null) {
                    continue;
                }


                if (distance > bestDistance) {

                    bestDistance = distance;

                    bestPairs.clear();

                    bestPairs.add(
                            new Cell[]{source, target}
                    );

                } else if (distance == bestDistance) {

                    bestPairs.add(
                            new Cell[]{source, target}
                    );
                }
            }
        }


        if (bestPairs.isEmpty()) {

            throw new IllegalStateException(
                    "Maze graph is disconnected; cannot select openings."
            );
        }


        return bestPairs.get(
                new Random().nextInt(bestPairs.size())
        );
    }


    private void assignCircularOpenings(Cell[] pair) {

        Random random = new Random();

        entranceCell = random.nextBoolean()
                ? pair[0]
                : pair[1];

        exitCell = entranceCell == pair[0]
                ? pair[1]
                : pair[0];

        entranceCell.isEntrance = true;
        entranceCell.discovered = true;

        exitCell.isExit = true;
    }


    /**
     * BFS through OPEN passages only.
     */
    private Map<Cell, Integer> shortestPassageDistances(
            Cell start
    ) {

        Map<Cell, Integer> distances =
                new LinkedHashMap<>();

        ArrayDeque<Cell> queue =
                new ArrayDeque<>();

        distances.put(start, 0);

        queue.add(start);


        while (!queue.isEmpty()) {

            Cell current = queue.remove();

            int nextDistance =
                    distances.get(current) + 1;


            for (Cell neighbor : current.getOpenNeighbors()) {

                if (!distances.containsKey(neighbor)) {

                    distances.put(
                            neighbor,
                            nextDistance
                    );

                    queue.add(neighbor);
                }
            }
        }

        return distances;
    }


    // ============================================================
    // RESET OPENINGS
    // ============================================================

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


    // ============================================================
    // DEAD END STATISTICS
    // ============================================================

    public int countDeadEnds() {

        int deadEnds = 0;

        for (Cell cell : allCells) {

            if (!cell.isEntrance
                    && !cell.isExit
                    && cell.getOpenDegree() == 1) {

                deadEnds++;
            }
        }

        return deadEnds;
    }


    // ============================================================
    // COMPLEXITY / LOOP CARVING
    // ============================================================

    public void applyComplexityFactor(
            double complexityFactor
    ) {

        double clamped = Math.max(
                0.0,
                Math.min(1.0, complexityFactor)
        );

        double removalChance = 1.0 - clamped;

        Random random = new Random();


        for (Cell cell : allCells) {

            if (cell.getOpenDegree() != 1
                    || random.nextDouble() >= removalChance) {

                continue;
            }


            List<Cell> closedNeighbors = new ArrayList<>();

            for (Map.Entry<Cell, Boolean> entry
                    : cell.getNeighbors().entrySet()) {

                if (!entry.getValue()) {

                    closedNeighbors.add(entry.getKey());
                }
            }


            if (!closedNeighbors.isEmpty()) {

                removeWalls(
                        cell,
                        closedNeighbors.get(
                                random.nextInt(
                                        closedNeighbors.size()
                                )
                        )
                );
            }
        }
    }
}
