package model;

/**
 * A cell in a polar maze. Ring zero is the central disk; rings one onward are
 * annular sectors. Angles are in radians and increase clockwise on screen.
 * Radii are normalized to the range [0, 1] and scaled by the renderer/canvas.
 */
public final class PolarCell extends Cell {
    private final int ringIndex;
    private final int sectorIndex;
    private final int sectorsInRing;

    private final double innerRadius;
    private final double outerRadius;

    private final double startAngle;
    private final double endAngle;

    public PolarCell(
            int ringIndex,
            int sectorIndex,
            int sectorsInRing,
            double innerRadius,
            double outerRadius,
            double startAngle,
            double endAngle
    ) {
        // x/y remain useful for diagnostics, but polar code must use the
        // explicit ring/sector fields rather than treating them as Cartesian.
        super(sectorIndex, ringIndex);

        this.ringIndex = ringIndex;
        this.sectorIndex = sectorIndex;
        this.sectorsInRing = sectorsInRing;

        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;

        this.startAngle = startAngle;
        this.endAngle = endAngle;
    }

    public int getRingIndex() {
        return ringIndex;
    }

    public int getSectorIndex() {
        return sectorIndex;
    }

    public int getSectorsInRing() {
        return sectorsInRing;
    }

    public double getInnerRadius() {
        return innerRadius;
    }

    public double getOuterRadius() {
        return outerRadius;
    }

    public double getStartAngle() {
        return startAngle;
    }

    public double getEndAngle() {
        return endAngle;
    }

    public double getCenterRadius() {
        return isCore() ? 0.0 : (innerRadius + outerRadius) / 2.0;
    }

    public double getCenterAngle() {return (startAngle + endAngle) / 2.0;}

    public boolean isCore() {
        return ringIndex == 0;
    }
}