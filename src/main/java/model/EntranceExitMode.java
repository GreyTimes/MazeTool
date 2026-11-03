package model;

public enum EntranceExitMode {

    LEFT_TO_RIGHT("Left to Right"),

    TOP_TO_BOTTOM("Top to Bottom"),

    CENTER_TO_OUTER("Center to Outer Edge"),

    CUSTOM("Custom Click Selection");

    private final String displayName;

    EntranceExitMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}