package model;

/**
 * Equilateral triangle within a triangular tessellation.
 */
public final class TriangularCell extends PolygonCell {

    private final int column;
    private final int row;
    private final boolean firstOrientation;

    public TriangularCell(
            int column,
            int row,
            boolean firstOrientation,
            double[] verticesX,
            double[] verticesY
    ) {

        super(
                column * 2 + (firstOrientation ? 0 : 1),
                row,
                verticesX,
                verticesY
        );

        this.column = column;
        this.row = row;
        this.firstOrientation = firstOrientation;
    }

    public int getColumn() {
        return column;
    }

    public int getRow() {
        return row;
    }

    public boolean isFirstOrientation() {
        return firstOrientation;
    }
}