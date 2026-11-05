package generator;

import model.Cell;
import model.MazeGrid;
import java.util.Random;

public interface MazeGenerator {
    void initialize(MazeGrid grid, Random random);

    boolean step();

    Cell getCurrentHead();
}