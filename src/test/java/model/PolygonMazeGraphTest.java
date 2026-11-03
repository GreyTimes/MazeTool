
package model;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PolygonMazeGraphTest {


    @Test
    void polygonGridsHaveExpectedCellCounts() {

        MazeGrid hexagonal = new MazeGrid(
                4,
                4,
                TopologyType.HEXAGONAL
        );

        MazeGrid triangular = new MazeGrid(
                10,
                10,
                TopologyType.TRIANGULAR
        );

        assertEquals(91, hexagonal.getCellCount());

        assertEquals(100, triangular.getCellCount());

        assertTrue(hexagonal.isPolygonal());

        assertTrue(triangular.isPolygonal());

        assertEquals(0, hexagonal.cells.length);

        assertEquals(0, triangular.cells.length);
    }


    @Test
    void polygonAdjacencyIsSymmetric() {

        for (TopologyType topology : List.of(
                TopologyType.HEXAGONAL,
                TopologyType.TRIANGULAR
        )) {

            MazeGrid grid = new MazeGrid(
                    8,
                    6,
                    topology
            );

            for (Cell cell : grid.getAllCells()) {

                assertFalse(
                        cell.getAdjacentCells().isEmpty()
                );

                PolygonCell polygon = (PolygonCell) cell;

                for (int edge = 0;
                     edge < polygon.getEdgeCount();
                     edge++) {

                    Cell neighbor =
                            polygon.getNeighborAcrossEdge(edge);

                    if (neighbor != null) {

                        assertTrue(
                                neighbor.getNeighbors().containsKey(cell)
                        );

                        assertTrue(
                                cell.getNeighbors().containsKey(neighbor)
                        );
                    }
                }
            }

            assertEquals(
                    grid.getCellCount(),
                    reachableCount(
                            grid,
                            grid.getAllCells().get(0),
                            false
                    )
            );
        }
    }


    @Test
    void allFiveGeneratorsAndThreeSolversWorkOnBothTopologies() {

        List<MazeGenerator> generators = List.of(

                new DfsMazeGenerator(),

                new PrimsMazeGenerator(),

                new KruskalsMazeGenerator(),

                new AldousBroderMazeGenerator(),

                new WilsonsMazeGenerator()
        );


        for (TopologyType topology : List.of(
                TopologyType.HEXAGONAL,
                TopologyType.TRIANGULAR
        )) {

            for (MazeGenerator generator : generators) {

                MazeGrid grid = new MazeGrid(
                        8,
                        6,
                        topology
                );

                grid.layoutPolygonGrid(
                        600,
                        600,
                        22
                );

                runGenerator(
                        generator,
                        grid
                );


                int openEdges =
                        countOpenUndirectedEdges(grid);

                assertEquals(
                        grid.getCellCount() - 1,
                        openEdges,
                        generator.getClass().getSimpleName()
                                + " must generate a spanning tree"
                );


                assertEquals(
                        grid.getCellCount(),
                        reachableCount(
                                grid,
                                grid.getAllCells().get(0),
                                true
                        )
                );


                // Reuse the same complexity adjustment as the application.

                grid.applyComplexityFactor(0.45);


                if (topology == TopologyType.HEXAGONAL) {

                    grid.createHexagonalOpenings();

                } else {

                    grid.createTriangularOpenings();
                }


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
        }
    }


    @Test
    void polygonOpeningsChooseTheFarthestBoundaryPair() {

        for (TopologyType topology : List.of(
                TopologyType.HEXAGONAL,
                TopologyType.TRIANGULAR
        )) {

            MazeGrid grid = new MazeGrid(
                    9,
                    7,
                    topology
            );

            newMaze(grid);


            if (topology == TopologyType.HEXAGONAL) {

                grid.createHexagonalOpenings();

            } else {

                grid.createTriangularOpenings();
            }


            PolygonCell entrance =
                    (PolygonCell) grid.getEntranceCell();

            PolygonCell exit =
                    (PolygonCell) grid.getExitCell();


            assertNotNull(entrance);

            assertNotNull(exit);

            assertNotSame(entrance, exit);


            assertTrue(
                    entrance.isBoundaryEdge(
                            entrance.getOpeningEdgeIndex()
                    )
            );

            assertTrue(
                    exit.isBoundaryEdge(
                            exit.getOpeningEdgeIndex()
                    )
            );


            int selectedDistance =
                    shortestPassageDistance(
                            entrance,
                            exit
                    );


            int maximumDistance = -1;

            List<Cell> boundaryCells =
                    new ArrayList<>();


            for (Cell cell : grid.getAllCells()) {

                if (cell instanceof PolygonCell polygon
                        && !polygon.getBoundaryEdgeIndices().isEmpty()) {

                    boundaryCells.add(cell);
                }
            }


            for (int i = 0;
                 i < boundaryCells.size();
                 i++) {

                for (int j = i + 1;
                     j < boundaryCells.size();
                     j++) {

                    maximumDistance = Math.max(
                            maximumDistance,

                            shortestPassageDistance(
                                    boundaryCells.get(i),
                                    boundaryCells.get(j)
                            )
                    );
                }
            }


            assertEquals(
                    maximumDistance,
                    selectedDistance
            );
        }
    }


    @Test
    void maximumPolygonSizesRemainConnected() {

        MazeGrid hexagonal = new MazeGrid(
                7,
                7,
                TopologyType.HEXAGONAL
        );

        MazeGrid triangular = new MazeGrid(
                16,
                16,
                TopologyType.TRIANGULAR
        );

        assertEquals(241, hexagonal.getCellCount());

        assertEquals(256, triangular.getCellCount());


        assertEquals(
                hexagonal.getCellCount(),

                reachableCount(
                        hexagonal,
                        hexagonal.getAllCells().get(0),
                        false
                )
        );


        assertEquals(
                triangular.getCellCount(),

                reachableCount(
                        triangular,
                        triangular.getAllCells().get(0),
                        false
                )
        );
    }


    // ============================================================
    // TEST HELPERS
    // ============================================================

    private static void newMaze(MazeGrid grid) {

        runGenerator(
                new DfsMazeGenerator(),
                grid
        );

        grid.applyComplexityFactor(0.5);
    }


    private static void runGenerator(
            MazeGenerator generator,
            MazeGrid grid
    ) {

        generator.initialize(grid);

        int steps = 0;

        while (generator.step()) {

            if (++steps > 2_000_000) {

                fail(
                        generator.getClass().getSimpleName()
                                + " exceeded the generation step limit"
                );
            }
        }
    }


    private static int countOpenUndirectedEdges(
            MazeGrid grid
    ) {

        int directed = 0;

        for (Cell cell : grid.getAllCells()) {

            for (boolean open : cell.getNeighbors().values()) {

                if (open) {
                    directed++;
                }
            }
        }

        return directed / 2;
    }


    private static int reachableCount(
            MazeGrid grid,
            Cell start,
            boolean openOnly
    ) {

        Set<Cell> visited = new HashSet<>();

        Queue<Cell> queue = new ArrayDeque<>();

        queue.add(start);

        visited.add(start);


        while (!queue.isEmpty()) {

            Cell current = queue.remove();

            List<Cell> nextCells = openOnly

                    ? current.getOpenNeighbors()

                    : current.getAdjacentCells();


            for (Cell next : nextCells) {

                if (visited.add(next)) {

                    queue.add(next);
                }
            }
        }

        return visited.size();
    }


    private static int shortestPassageDistance(
            Cell start,
            Cell target
    ) {

        Map<Cell, Integer> distances =
                new HashMap<>();

        Queue<Cell> queue =
                new ArrayDeque<>();

        queue.add(start);

        distances.put(start, 0);


        while (!queue.isEmpty()) {

            Cell current = queue.remove();

            if (current == target) {

                return distances.get(current);
            }

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

        return -1;
    }


    private static void assertValidPath(
            MazeGrid grid,
            List<Cell> path
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
}
