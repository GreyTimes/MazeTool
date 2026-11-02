package model;

public enum TopologyType {
    RECTANGULAR("Rectangular"),
    CIRCULAR("Circular / Polar"),
    TRIANGULAR("Triangular"),
    HEXAGONAL("Hexagonal");

    private final String displayName;

    TopologyType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}