package service;

import generator.*;
import model.*;
import solver.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BenchmarkRunner {

    public static List<BenchmarkResult> runBatch(
            List<String> generators,
            List<String> solvers,
            TopologyType topology,
            int iterations,
            long baseSeed
    ) {
        List<BenchmarkResult> results = new ArrayList<>();
        Random random = new Random(baseSeed);

        List<int[]> gridSizes = switch (topology) {
            case CIRCULAR -> List.of(new int[]{12, 6}, new int[]{16, 8}, new int[]{20, 10});
            case HEXAGONAL -> List.of(new int[]{3, 3}, new int[]{4, 4}, new int[]{5, 5});
            case TRIANGULAR -> List.of(new int[]{6, 6}, new int[]{10, 10}, new int[]{14, 14});
            default -> List.of(new int[]{20, 20}, new int[]{35, 35}, new int[]{50, 50});
        };

        for (int[] size : gridSizes) {
            int w = size[0];
            int h = size[1];

            for (String genName : generators) {
                for (String solverName : solvers) {
                    long totalGenTime = 0;
                    long totalSolveTime = 0;
                    long totalNodesVisited = 0;
                    long totalPathLen = 0;
                    long totalReachable = 0;
                    int successfulIterations = 0;

                    for (int i = 0; i < iterations; i++) {
                        try {
                            long seed = random.nextLong();
                            Random iterRand = new Random(seed);

                            MazeGrid grid = switch (topology) {
                                case HEXAGONAL, TRIANGULAR -> new MazeGrid(w, w, topology);
                                default -> new MazeGrid(w, h, topology);
                            };

                            if (topology == TopologyType.CIRCULAR) {
                                grid.layoutCircularGrid(600, 600, 0.0);
                            } else if (topology == TopologyType.HEXAGONAL) {
                                grid.layoutHexagonalGrid(600, 600, 0.0);
                            } else if (topology == TopologyType.TRIANGULAR) {
                                grid.layoutTriangularGrid(600, 600, 0.0);
                            }

                            MazeGenerator generator = switch (genName) {
                                case "Randomized Prim's" -> new PrimsMazeGenerator();
                                case "Randomized Kruskal's" -> new KruskalsMazeGenerator();
                                case "Aldous-Broder" -> new AldousBroderMazeGenerator();
                                case "Wilson's" -> new WilsonsMazeGenerator();
                                default -> new DfsMazeGenerator();
                            };

                            long startGen = System.nanoTime();
                            generator.initialize(grid, iterRand);
                            while (generator.step()) {
                                // complete generation loop
                            }
                            grid.createOpenings(EntranceExitMode.LEFT_TO_RIGHT, iterRand);
                            long endGen = System.nanoTime();
                            totalGenTime += (endGen - startGen) / 1_000_000;

                            for (Cell c : grid.getAllCells()) {
                                c.visited = false;
                            }

                            long startSolve = System.nanoTime();
                            List<Cell> path = switch (solverName) {
                                case "Depth-First Search (DFS)" -> DfsMazeSolver.solve(grid);
                                case "Dijkstra's Algorithm" -> DijkstraMazeSolver.solve(grid);
                                default -> BfsMazeSolver.solve(grid);
                            };
                            long endSolve = System.nanoTime();
                            totalSolveTime += (endSolve - startSolve) / 1_000_000;

                            int nodesVisited = countVisitedNodes(grid);
                            int pathLen = (path != null) ? path.size() : 0;
                            int reachable = countReachableCells(grid);

                            totalNodesVisited += nodesVisited;
                            totalPathLen += pathLen;
                            totalReachable += reachable;
                            successfulIterations++;
                        } catch (Exception ex) {
                            System.err.println("Warning: Benchmark iteration failed for " + genName + " / " + solverName + ": " + ex.getMessage());
                        }
                    }

                    if (successfulIterations > 0) {
                        results.add(new BenchmarkResult(
                                topology.name(),
                                genName,
                                solverName,
                                w,
                                h,
                                totalGenTime / successfulIterations,
                                totalSolveTime / successfulIterations,
                                (int)(totalNodesVisited / successfulIterations),
                                (int)(totalPathLen / successfulIterations),
                                (int)(totalReachable / successfulIterations)
                        ));
                    }
                }
            }
        }
        return results;
    }

    private static int countVisitedNodes(MazeGrid grid) {
        int count = 0;
        for (Cell c : grid.getAllCells()) {
            if (c.visited) {
                count++;
            }
        }
        return Math.max(1, count);
    }

    private static int countReachableCells(MazeGrid grid) {
        int count = 0;
        for (Cell c : grid.getAllCells()) {
            if (!c.getOpenNeighbors().isEmpty()) {
                count++;
            }
        }
        return Math.max(1, count);
    }
}