package model;

/**
 * Cell in a pointy-top hexagonal maze.
 *
 * Interior cells are regular hexagons. Boundary cells are clipped
 * against the outer hexagon so the whole board has a regular hexagonal shape.
 */
public final class HexagonalCell extends PolygonCell {

    private final int axialQ;
    private final int axialR;

    /** Creates a regular hexagonal cell. */
    public HexagonalCell(
            int axialQ,
            int axialR,
            double centerX,
            double centerY,
            double radius
    ) {
        this(
                axialQ,
                axialR,
                createX(centerX, radius),
                createY(centerY, radius)
        );
    }

    /** Creates a full or clipped hexagonal cell. */
    public HexagonalCell(
            int axialQ,
            int axialR,
            double[] verticesX,
            double[] verticesY
    ) {
        super(axialQ, axialR, verticesX, verticesY);

        this.axialQ = axialQ;
        this.axialR = axialR;
    }

    private static double[] createX(
            double centerX,
            double radius
    ) {
        double[] values = new double[6];

        for (int i = 0; i < 6; i++) {
            double angle = -Math.PI / 2.0
                    + i * Math.PI / 3.0;

            values[i] = centerX
                    + radius * Math.cos(angle);
        }

        return values;
    }

    private static double[] createY(
            double centerY,
            double radius
    ) {
        double[] values = new double[6];

        for (int i = 0; i < 6; i++) {
            double angle = -Math.PI / 2.0
                    + i * Math.PI / 3.0;

            values[i] = centerY
                    + radius * Math.sin(angle);
        }

        return values;
    }

    public int getAxialQ() {
        return axialQ;
    }

    public int getAxialR() {
        return axialR;
    }
}