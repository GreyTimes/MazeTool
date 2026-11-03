package view;

import javafx.scene.canvas.GraphicsContext;

import model.Cell;
import model.MazeGrid;

import java.util.Collection;
import java.util.List;

public interface MazeRenderer {

    void render(
            GraphicsContext gc,
            double width,
            double height,
            MazeGrid grid,
            Cell playerCell,
            List<Cell> solvedPath,
            Cell currentHead,
            boolean fogOfWarActive,
            int fogRadius
    );

    void setDarkMode(boolean darkMode);


    // Optional topology-specific interaction hooks.

    default void setHighlightedCells(Collection<Cell> cells) {
    }

    default void setHoveredCell(Cell cell) {
    }

    default void setPlayerScreenPosition(double x, double y) {
    }

    default void clearPlayerScreenPosition() {
    }


    // Mouse hit testing.

    default Cell findCellAt(
            double x,
            double y,
            double width,
            double height,
            MazeGrid grid
    ) {
        return null;
    }
}
