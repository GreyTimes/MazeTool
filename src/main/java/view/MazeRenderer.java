package view;

import javafx.scene.canvas.GraphicsContext;
import model.Cell;
import model.MazeGrid;
import java.util.List;

public interface MazeRenderer {
    void render(GraphicsContext gc, double width, double height, MazeGrid grid,
                Cell playerCell, List<Cell> solvedPath, Cell currentHead,
                boolean fogOfWarActive, int fogRadius);
    void setDarkMode(boolean darkMode);
}