package view;

import javafx.scene.canvas.GraphicsContext;
import model.Cell;
import model.MazeGrid;
import java.util.List;

public class TriangularRenderer implements MazeRenderer {
    private boolean darkMode = true;

    @Override
    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    @Override
    public void render(GraphicsContext gc, double width, double height, MazeGrid grid,
                       Cell playerCell, List<Cell> solvedPath, Cell currentHead,
                       boolean fogOfWarActive, int fogRadius) {
        // Η λογική σχεδίασης για τα εξάγωνα θα προστεθεί εδώ αργότερα
    }
}