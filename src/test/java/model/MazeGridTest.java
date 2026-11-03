package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MazeGridTest {

    @Test
    void testNonSquareRectangularGrid() {
        int customWidth = 35;
        int customHeight = 15;
        MazeGrid rectGrid = new MazeGrid(customWidth, customHeight);

        // Verify independent dimensions
        assertEquals(customWidth, rectGrid.cols, "Grid width should match custom cols");
        assertEquals(customHeight, rectGrid.rows, "Grid height should match custom rows");

        // Verify corners and edge boundaries are correct
        assertNotNull(rectGrid.getCell(0, 0), "Top-left corner should exist");
        assertNotNull(rectGrid.getCell(customWidth - 1, customHeight - 1), "Bottom-right corner should exist");
        assertNull(rectGrid.getCell(customWidth, customHeight), "Out of bounds check should return null");
    }
}