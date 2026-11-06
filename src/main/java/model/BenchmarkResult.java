package model;

public class BenchmarkResult {
    private final String topology;
    private final String generatorName;
    private final String solverName;
    private final int width;
    private final int height;
    private final long generationTimeMs;
    private final long solvingTimeMs;
    private final int nodesVisited;
    private final int pathLength;
    private final int totalReachableCells;
    private final double pathEfficiencyRatio;

    public BenchmarkResult(String topology, String generatorName, String solverName, int width, int height,
                           long generationTimeMs, long solvingTimeMs, int nodesVisited, int pathLength, int totalReachableCells) {
        this.topology = topology;
        this.generatorName = generatorName;
        this.solverName = solverName;
        this.width = width;
        this.height = height;
        this.generationTimeMs = generationTimeMs;
        this.solvingTimeMs = solvingTimeMs;
        this.nodesVisited = nodesVisited;
        this.pathLength = pathLength;
        this.totalReachableCells = totalReachableCells;
        this.pathEfficiencyRatio = totalReachableCells > 0 ? (double) pathLength / totalReachableCells : 0.0;
    }

    public String getTopology() { return topology; }
    public String getGeneratorName() { return generatorName; }
    public String getSolverName() { return solverName; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public long getGenerationTimeMs() { return generationTimeMs; }
    public long getSolvingTimeMs() { return solvingTimeMs; }
    public int getNodesVisited() { return nodesVisited; }
    public int getPathLength() { return pathLength; }
    public int getTotalReachableCells() { return totalReachableCells; }
    public double getPathEfficiencyRatio() { return pathEfficiencyRatio; }

    public String toCsvRow() {
        return String.format("\"%s\",\"%s\",\"%s\",%d,%d,%d,%d,%d,%d,%d,%.4f",
                topology, generatorName, solverName, width, height,
                generationTimeMs, solvingTimeMs, nodesVisited, pathLength, totalReachableCells, pathEfficiencyRatio);
    }

    public static String getCsvHeader() {
        return "Topology,Generator,Solver,Width,Height,GenerationTimeMs,SolvingTimeMs,NodesVisited,PathLength,TotalReachableCells,PathEfficiencyRatio";
    }
}