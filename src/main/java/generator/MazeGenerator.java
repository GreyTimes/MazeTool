package generator;

import model.Cell;
import model.MazeGrid;

public interface MazeGenerator {
    void initialize(MazeGrid grid);

    boolean step();

    Cell getCurrentHead();
}