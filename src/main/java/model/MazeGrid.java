
package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MazeGrid {
    private static final double TWO_PI = Math.PI * 2.0;

    public final int cols;
    public final int rows;
    public Cell[][] cells;

    private final List<Cell> allCells = new ArrayList<>();

    private TopologyType topology;
    private int polarRingCount;

    private Cell entranceCell;
    private Cell exitCell;

    public MazeGrid(int cols, int rows) {
        this(cols, rows, TopologyType.RECTANGULAR);
    }

    public MazeGrid(int width, int height, TopologyType topology) {
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

        if (this.topology == TopologyType.CIRCULAR) {
            initializeCircularGrid();
        } else {
            this.topology = TopologyType.RECTANGULAR;
            buildRectangularGrid();
        }
    }

    public TopologyType getTopology() {
        return topology;
    }

    public final boolean isCircular() {
        return topology == TopologyType.CIRCULAR;
    }

    public void initializeRectangularGrid() {
        topology = TopologyType.RECTANGULAR;
        polarRingCount = 0;
        entranceCell = null;
        exitCell = null;
        buildRectangularGrid();
    }

    private void buildRectangularGrid() {
        allCells.clear();
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

                if (x > 0) connectCells(current, cells[x - 1][y]);
                if (x < cols - 1) connectCells(current, cells[x + 1][y]);
                if (y > 0) connectCells(current, cells[x][y - 1]);
                if (y < rows - 1) connectCells(current, cells[x][y + 1]);
            }
        }
    }

    /**
     * Builds a concentric polar maze.
     * Width controls outer sector count; height controls ring count.
     */
    private void initializeCircularGrid() {
        topology = TopologyType.CIRCULAR;
        polarRingCount = rows;

        entranceCell = null;
        exitCell = null;

        allCells.clear();
        cells = new Cell[0][0];

        double radialStep = 1.0 / (polarRingCount + 1.0);
        double angleStart = -Math.PI / 2.0;

        PolarCell core = new PolarCell(
                0, 0, 1,
                0.0, radialStep,
                angleStart, angleStart + TWO_PI
        );

        allCells.add(core);

        List<List<PolarCell>> rings = new ArrayList<>();
        rings.add(Collections.singletonList(core));

        for (int ring = 1; ring <= polarRingCount; ring++) {

            int sectorCount = Math.max(
                    6,
                    (int) Math.round(
                            cols * (ring / (double) polarRingCount)
                    )
            );

            List<PolarCell> ringCells =
                    new ArrayList<>(sectorCount);

            double innerRadius = ring * radialStep;
            double outerRadius = (ring + 1.0) * radialStep;

            for (int sector = 0; sector < sectorCount; sector++) {

                double start = angleStart
                        + TWO_PI * sector / sectorCount;

                double end = angleStart
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

            for (int sector = 0; sector < ringCells.size(); sector++) {

                PolarCell current = ringCells.get(sector);

                PolarCell clockwise = ringCells.get(
                        (sector + 1) % ringCells.size()
                );

                connectCells(current, clockwise);
            }
        }

        // Connect the central cell to every first-ring sector.
        for (PolarCell firstRingCell : rings.get(1)) {
            connectCells(core, firstRingCell);
        }

        // Connect neighboring rings where their angular intervals overlap.
        for (int ring = 1; ring < polarRingCount; ring++) {

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

    public final void layoutCircularGrid(
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

            double radius = polar.getCenterRadius() * scale;
            double angle = polar.getCenterAngle();

            cell.screenX = centerX + radius * Math.cos(angle);
            cell.screenY = centerY + radius * Math.sin(angle);
        }
    }

    public final void connectCells(Cell a, Cell b) {
        if (a == null || b == null || a == b) return;

        a.addNeighbor(b);
        b.addNeighbor(a);
    }

    public Cell getCell(int x, int y) {
        if (isCircular()
                || cells == null
                || x < 0 || x >= cols
                || y < 0 || y >= rows) {
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
        if (entranceCell != null) return entranceCell;

        for (Cell cell : allCells) {
            if (cell.isEntrance) return cell;
        }

        return null;
    }

    public Cell getExitCell() {
        if (exitCell != null) return exitCell;

        for (Cell cell : allCells) {
            if (cell.isExit) return cell;
        }

        return null;
    }

    public void removeWalls(Cell a, Cell b) {
        if (a == null || b == null) return;

        if (!a.getNeighbors().containsKey(b)
                || !b.getNeighbors().containsKey(a)) {
            return;
        }

        a.getNeighbors().put(b, true);
        b.getNeighbors().put(a, true);

        if (a instanceof PolarCell || b instanceof PolarCell) {
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

    public void createRectangularOpenings() {
        if (isCircular() || cols == 0 || rows == 0) return;

        Random random = new Random();

        clearEntranceAndExitFlags();

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

    /**
     * Selects the two outer-ring cells with the greatest shortest-path
     * distance through the already-generated maze.
     */
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

        if (outerRing.size() < 2) {
            throw new IllegalStateException(
                    "A circular maze needs at least two outer-ring cells."
            );
        }

        int greatestDistance = -1;
        List<Cell[]> bestPairs = new ArrayList<>();

        // Run BFS from each outer-ring cell and compare every pair.
        for (int i = 0; i < outerRing.size(); i++) {

            Cell source = outerRing.get(i);

            Map<Cell, Integer> distances =
                    shortestPassageDistances(source);

            for (int j = i + 1; j < outerRing.size(); j++) {

                Cell candidate = outerRing.get(j);
                Integer distance = distances.get(candidate);

                if (distance == null) continue;

                if (distance > greatestDistance) {

                    greatestDistance = distance;
                    bestPairs.clear();

                    bestPairs.add(new Cell[]{source, candidate});

                } else if (distance == greatestDistance) {

                    bestPairs.add(new Cell[]{source, candidate});
                }
            }
        }

        if (bestPairs.isEmpty()) {
            throw new IllegalStateException(
                    "The circular maze is disconnected; cannot choose entrance and exit."
            );
        }

        Random random = new Random();

        Cell[] selected = bestPairs.get(
                random.nextInt(bestPairs.size())
        );

        // Randomize which endpoint is entrance.
        entranceCell = random.nextBoolean()
                ? selected[0]
                : selected[1];

        exitCell = entranceCell == selected[0]
                ? selected[1]
                : selected[0];

        entranceCell.isEntrance = true;
        entranceCell.discovered = true;

        exitCell.isExit = true;
    }

    /**
     * BFS from a cell through OPEN edges only.
     * Returns the shortest passage distance to each reachable cell.
     */
    private Map<Cell, Integer> shortestPassageDistances(Cell start) {
        Map<Cell, Integer> distances = new java.util.HashMap<>();

        java.util.ArrayDeque<Cell> queue =
                new java.util.ArrayDeque<>();

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

    private void clearEntranceAndExitFlags() {
        entranceCell = null;
        exitCell = null;

        for (Cell cell : allCells) {
            cell.isEntrance = false;
            cell.isExit = false;
        }
    }

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

    public void applyComplexityFactor(double complexityFactor) {
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
                                random.nextInt(closedNeighbors.size())
                        )
                );
            }
        }
    }
}
