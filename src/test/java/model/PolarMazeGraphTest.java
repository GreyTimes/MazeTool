package model;

import game.Player;

import generator.AldousBroderMazeGenerator;
import generator.DfsMazeGenerator;
import generator.KruskalsMazeGenerator;
import generator.MazeGenerator;
import generator.PrimsMazeGenerator;
import generator.WilsonsMazeGenerator;

import org.junit.jupiter.api.Test;

import solver.BfsMazeSolver;
import solver.DfsMazeSolver;
import solver.DijkstraMazeSolver;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PolarMazeGraphTest {

    @Test
    void polarGridHasSymmetricConnectedAdjacencyAndNoCartesianArray() {

        MazeGrid grid = new MazeGrid(
                12,
                8,
                TopologyType.CIRCULAR
        );

        assertTrue(grid.isCircular());

        assertEquals(0, grid.cells.length);

        assertTrue(grid.getCellCount() > 1);

        assertEquals(8, grid.getPolarRingCount());

        for (Cell cell : grid.getAllCells()) {

            assertFalse(cell.getAdjacentCells().isEmpty());

            for (Cell neighbor : cell.getAdjacentCells()) {

                assertTrue(
                        neighbor.getNeighbors().containsKey(cell),
                        "Adjacency must be bidirectional"
                );

                assertFalse(
                        cell.getNeighbors().get(neighbor),
                        "Every edge starts closed"
                );
            }
        }

        assertEquals(
                1.0,
                grid.getAllCells().get(0).getScreenX(),
                1.0e-9,
                "Core screen coordinates must be centered in the default normalized layout"
        );
    }

    @Test
    void allFiveGeneratorsProduceConnectedPolarSpanningTrees() {

        assertGeneratorProducesTree(
                new DfsMazeGenerator()
        );

        assertGeneratorProducesTree(
                new PrimsMazeGenerator()
        );

        assertGeneratorProducesTree(
                new KruskalsMazeGenerator()
        );

        assertGeneratorProducesTree(
                new AldousBroderMazeGenerator()
        );

        assertGeneratorProducesTree(
                new WilsonsMazeGenerator()
        );
    }

    @Test
    void allThreeSolversFindValidPolarRoutes() {

        MazeGrid grid = new MazeGrid(
                14,
                9,
                TopologyType.CIRCULAR
        );

        MazeGenerator generator = new DfsMazeGenerator();

        runGenerator(generator, grid);

        grid.createCircularOpenings();

        assertValidPath(
                grid,
                BfsMazeSolver.solve(grid)
        );

        assertValidPath(
                grid,
                DfsMazeSolver.solve(grid)
        );

        assertValidPath(
                grid,
                DijkstraMazeSolver.solve(grid)
        );
    }

    @Test
    void playerMovementAndFogUseGraphAdjacency() {

        MazeGrid grid = new MazeGrid(
                12,
                8,
                TopologyType.CIRCULAR
        );

        MazeGenerator generator = new DfsMazeGenerator();

        runGenerator(generator, grid);

        grid.createCircularOpenings();

        Player player = new Player();

        player.spawn(grid);

        assertNotNull(player.getCurrentCell());

        player.updateDiscovery(grid, true, 1);

        assertTrue(player.getCurrentCell().discovered);

        assertTrue(
                player.getCurrentCell()
                        .getAdjacentCells()
                        .stream()
                        .anyMatch(c -> c.discovered)
        );

        Cell openNeighbor = player.getCurrentCell()
                .getOpenNeighbors()
                .get(0);

        assertTrue(player.moveToCell(openNeighbor));

        assertEquals(2, player.getSteps());

        Cell nonNeighbor = grid.getAllCells()
                .stream()
                .filter(cell -> cell != player.getCurrentCell())
                .filter(cell ->
                        !player.getCurrentCell()
                                .getNeighbors()
                                .containsKey(cell)
                )
                .findFirst()
                .orElseThrow();

        assertFalse(
                player.moveToCell(nonNeighbor),
                "Player must not jump across non-adjacent cells"
        );
    }

    @Test
    void allFiveGeneratorsRemainCompatibleWithRectangularGrids() {

        assertRectangularGenerator(
                new DfsMazeGenerator()
        );

        assertRectangularGenerator(
                new PrimsMazeGenerator()
        );

        assertRectangularGenerator(
                new KruskalsMazeGenerator()
        );

        assertRectangularGenerator(
                new AldousBroderMazeGenerator()
        );

        assertRectangularGenerator(
                new WilsonsMazeGenerator()
        );
    }

    private static void assertRectangularGenerator(
            MazeGenerator generator
    ) {

        MazeGrid grid = new MazeGrid(8, 6);

        runGenerator(generator, grid);

        grid.createRectangularOpenings();

        assertNotNull(BfsMazeSolver.solve(grid));

        assertEquals(8, grid.cells.length);

        assertEquals(6, grid.cells[0].length);

        int openEdges = 0;

        for (Cell cell : grid.getAllCells()) {
            openEdges += cell.getOpenDegree();
        }

        assertEquals(
                grid.getCellCount() - 1,
                openEdges / 2,
                generator.getClass().getSimpleName()
                        + " rectangular tree"
        );
    }

    private static void assertGeneratorProducesTree(
            MazeGenerator generator
    ) {

        MazeGrid grid = new MazeGrid(
                10,
                7,
                TopologyType.CIRCULAR
        );

        runGenerator(generator, grid);

        int openUndirectedEdges = 0;

        for (Cell cell : grid.getAllCells()) {

            for (boolean open : cell.getNeighbors().values()) {

                if (open) {
                    openUndirectedEdges++;
                }
            }
        }

        assertEquals(
                grid.getCellCount() - 1,
                openUndirectedEdges / 2,
                generator.getClass().getSimpleName()
                        + " must produce a spanning tree"
        );

        assertEquals(
                grid.getCellCount(),
                reachableCount(
                        grid,
                        grid.getAllCells().get(0)
                ),
                generator.getClass().getSimpleName()
                        + " must connect every cell"
        );
    }

    private static void runGenerator(
            MazeGenerator generator,
            MazeGrid grid
    ) {

        generator.initialize(grid);

        int steps = 0;

        int maximumSteps = 2_000_000;

        while (generator.step()) {

            if (++steps > maximumSteps) {

                fail(
                        generator.getClass().getSimpleName()
                                + " exceeded generation step limit"
                );
            }
        }
    }

    private static int reachableCount(
            MazeGrid grid,
            Cell start
    ) {

        Set<Cell> visited = new HashSet<>();

        Queue<Cell> queue = new ArrayDeque<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {

            Cell current = queue.remove();

            for (Cell neighbor : current.getOpenNeighbors()) {

                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        return visited.size();
    }

    private static void assertValidPath(
            MazeGrid grid,
            java.util.List<Cell> path
    ) {

        assertNotNull(path);

        assertSame(
                grid.getEntranceCell(),
                path.get(0)
        );

        assertSame(
                grid.getExitCell(),
                path.get(path.size() - 1)
        );

        for (int i = 1; i < path.size(); i++) {

            assertTrue(
                    path.get(i - 1).isPassageOpenTo(path.get(i))
            );
        }
    }
    @Test
    void circularOpeningsChooseTheMostDistantOuterRingPair() {

        MazeGrid grid = new MazeGrid(
                16,
                8,
                TopologyType.CIRCULAR
        );

        MazeGenerator generator = new DfsMazeGenerator();

        runGenerator(generator, grid);

        // Match the application's generation-completion order.
        grid.applyComplexityFactor(0.5);

        grid.createCircularOpenings();

        Cell entrance = grid.getEntranceCell();
        Cell exit = grid.getExitCell();

        assertNotNull(entrance);
        assertNotNull(exit);
        assertNotSame(entrance, exit);

        int selectedDistance = shortestPassageDistance(
                entrance,
                exit
        );

        int maximumDistance = -1;

        java.util.List<Cell> outerRing = grid.getAllCells()
                .stream()
                .filter(c -> c instanceof PolarCell polar
                        && polar.getRingIndex()
                        == grid.getPolarRingCount())
                .toList();

        for (int i = 0; i < outerRing.size(); i++) {

            for (int j = i + 1; j < outerRing.size(); j++) {

                maximumDistance = Math.max(
                        maximumDistance,
                        shortestPassageDistance(
                                outerRing.get(i),
                                outerRing.get(j)
                        )
                );
            }
        }

        assertEquals(
                maximumDistance,
                selectedDistance,
                "Entrance and exit must be a diameter pair restricted to the outer ring"
        );
    }

    private static int shortestPassageDistance(
            Cell start,
            Cell target
    ) {

        java.util.Map<Cell, Integer> distances =
                new java.util.HashMap<>();

        java.util.ArrayDeque<Cell> queue =
                new java.util.ArrayDeque<>();

        distances.put(start, 0);
        queue.add(start);

        while (!queue.isEmpty()) {

            Cell current = queue.remove();

            int distance = distances.get(current);

            if (current == target) {
                return distance;
            }

            for (Cell neighbor : current.getOpenNeighbors()) {

                if (!distances.containsKey(neighbor)) {

                    distances.put(neighbor, distance + 1);
                    queue.add(neighbor);
                }
            }
        }

        return -1;
    }
}