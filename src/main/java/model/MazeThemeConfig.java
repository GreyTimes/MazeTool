package model;

import javafx.scene.paint.Color;
import java.util.Objects;

import java.util.Locale;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;
import java.util.logging.Logger;
import java.util.logging.Level;

public class MazeThemeConfig {
    private static final Logger LOGGER =
            Logger.getLogger(MazeThemeConfig.class.getName());

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(MazeThemeConfig.class)
                    .node("appearance");

    public void save() {

        try {
            PREFS.putBoolean("darkMode", darkMode);

            PREFS.put("backgroundColor", colorToHex(backgroundColor));
            PREFS.put("floorColor", colorToHex(floorColor));
            PREFS.put("wallColor", colorToHex(wallColor));

            PREFS.put("playerColor", colorToHex(playerColor));
            PREFS.put("pathColor", colorToHex(pathColor));
            PREFS.put("headColor", colorToHex(headColor));

            PREFS.put("entranceColor", colorToHex(entranceColor));
            PREFS.put("exitColor", colorToHex(exitColor));

            PREFS.putDouble("wallWidthMultiplier", wallWidthMultiplier);

            PREFS.putBoolean("roundedWallCorners", roundedWallCorners);

            PREFS.put("wallTexture", wallTexture.name());

            PREFS.flush();

        } catch (BackingStoreException | SecurityException ex) {

            LOGGER.log(
                    Level.WARNING,
                    "Could not save theme preferences.",
                    ex
            );
        }
    }

    public static MazeThemeConfig load() {

        MazeThemeConfig config = new MazeThemeConfig(
                ThemePreset.DEFAULT_DARK
        );

        try {

            config.darkMode = PREFS.getBoolean(
                    "darkMode",
                    config.darkMode
            );

            config.backgroundColor = readColor(
                    "backgroundColor",
                    config.backgroundColor
            );

            config.floorColor = readColor(
                    "floorColor",
                    config.floorColor
            );

            config.wallColor = readColor(
                    "wallColor",
                    config.wallColor
            );

            config.playerColor = readColor(
                    "playerColor",
                    config.playerColor
            );

            config.pathColor = readColor(
                    "pathColor",
                    config.pathColor
            );

            config.headColor = readColor(
                    "headColor",
                    config.headColor
            );

            config.entranceColor = readColor(
                    "entranceColor",
                    config.entranceColor
            );

            config.exitColor = readColor(
                    "exitColor",
                    config.exitColor
            );

            config.setWallWidthMultiplier(
                    PREFS.getDouble("wallWidthMultiplier", 1.0)
            );

            config.roundedWallCorners = PREFS.getBoolean(
                    "roundedWallCorners",
                    true
            );

            try {
                config.wallTexture = WallTexture.valueOf(
                        PREFS.get("wallTexture", "SOLID")
                );
            } catch (IllegalArgumentException ex) {
                config.wallTexture = WallTexture.SOLID;
            }

        } catch (SecurityException ex) {

            LOGGER.log(
                    Level.WARNING,
                    "Could not load theme preferences. Using defaults.",
                    ex
            );
        }

        return config;
    }

    public enum WallTexture {
        SOLID("Solid"),
        DASHED("Dashed"),
        DOTTED("Dotted");

        private final String displayName;

        WallTexture(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum ThemePreset {

        DEFAULT_DARK(
                "Default Dark", true,
                "#1e1e1e", "#242424", "#4a4a4a",
                "#e91e63", "#00bcd4", "#ffeb3b",
                "#4caf50", "#f44336"
        ),

        CYBERPUNK(
                "Cyberpunk", true,
                "#0b032d", "#12083d", "#ff0055",
                "#ff00ff", "#00ffcc", "#ffff00",
                "#39ff14", "#ff3131"
        ),

        RETRO_ARCADE(
                "Retro Arcade", true,
                "#000000", "#101010", "#2196f3",
                "#ff9800", "#ffeb3b", "#e91e63",
                "#4caf50", "#f44336"
        ),

        CLASSIC_BLUEPRINT(
                "Classic Blueprint", true,
                "#0a192f", "#112240", "#64ffda",
                "#64ffda", "#8892b0", "#ccd6f6",
                "#57cbff", "#ff5757"
        ),

        FOREST_DUNGEON(
                "Forest Dungeon", false,
                "#2d3830", "#354438", "#5c4033",
                "#ffb74d", "#81c784", "#fff176",
                "#81c784", "#e53935"
        );

        private final String displayName;
        private final boolean darkMode;

        private final String background;
        private final String floor;
        private final String wall;
        private final String player;
        private final String path;
        private final String head;
        private final String entrance;
        private final String exit;

        ThemePreset(
                String displayName,
                boolean darkMode,
                String background,
                String floor,
                String wall,
                String player,
                String path,
                String head,
                String entrance,
                String exit
        ) {
            this.displayName = displayName;
            this.darkMode = darkMode;
            this.background = background;
            this.floor = floor;
            this.wall = wall;
            this.player = player;
            this.path = path;
            this.head = head;
            this.entrance = entrance;
            this.exit = exit;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private boolean darkMode;

    private Color backgroundColor;
    private Color floorColor;
    private Color wallColor;
    private Color playerColor;
    private Color pathColor;
    private Color headColor;
    private Color entranceColor;
    private Color exitColor;

    private double wallWidthMultiplier = 1.0;

    private boolean roundedWallCorners = true;

    private WallTexture wallTexture = WallTexture.SOLID;

    public MazeThemeConfig(ThemePreset preset) {
        applyPreset(preset);
    }

    private static Color readColor(String key, Color fallback) {

        String savedColor = PREFS.get(
                key,
                colorToHex(fallback)
        );

        try {
            return Color.web(savedColor);

        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private static String colorToHex(Color color) {

        return String.format(
                Locale.ROOT,
                "#%02X%02X%02X%02X",

                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255),
                (int) Math.round(color.getOpacity() * 255)
        );
    }

    // Creates an independent copy for editing in the customizer.
    public MazeThemeConfig copy() {
        return new MazeThemeConfig(this);
    }

    private MazeThemeConfig(MazeThemeConfig source) {
        copyFrom(source);
    }

    // Copies all settings while preserving the current object reference.
    public void copyFrom(MazeThemeConfig source) {
        Objects.requireNonNull(source, "Source configuration cannot be null");

        darkMode = source.darkMode;

        backgroundColor = source.backgroundColor;
        floorColor = source.floorColor;
        wallColor = source.wallColor;
        playerColor = source.playerColor;
        pathColor = source.pathColor;
        headColor = source.headColor;
        entranceColor = source.entranceColor;
        exitColor = source.exitColor;

        wallWidthMultiplier = source.wallWidthMultiplier;
        roundedWallCorners = source.roundedWallCorners;
        wallTexture = source.wallTexture;
    }

    public void applyPreset(ThemePreset preset) {
        Objects.requireNonNull(preset, "Theme preset cannot be null");

        darkMode = preset.darkMode;

        backgroundColor = Color.web(preset.background);
        floorColor = Color.web(preset.floor);
        wallColor = Color.web(preset.wall);

        playerColor = Color.web(preset.player);
        pathColor = Color.web(preset.path);
        headColor = Color.web(preset.head);

        entranceColor = Color.web(preset.entrance);
        exitColor = Color.web(preset.exit);
    }

    // DARK / LIGHT MODE

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    // COLORS

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color color) {
        backgroundColor = Objects.requireNonNull(color);
    }

    public Color getFloorColor() {
        return floorColor;
    }

    public void setFloorColor(Color color) {
        floorColor = Objects.requireNonNull(color);
    }

    public Color getWallColor() {
        return wallColor;
    }

    public void setWallColor(Color color) {
        wallColor = Objects.requireNonNull(color);
    }

    public Color getPlayerColor() {
        return playerColor;
    }

    public void setPlayerColor(Color color) {
        playerColor = Objects.requireNonNull(color);
    }

    public Color getPathColor() {
        return pathColor;
    }

    public void setPathColor(Color color) {
        pathColor = Objects.requireNonNull(color);
    }

    public Color getHeadColor() {
        return headColor;
    }

    public void setHeadColor(Color color) {
        headColor = Objects.requireNonNull(color);
    }

    public Color getEntranceColor() {
        return entranceColor;
    }

    public void setEntranceColor(Color color) {
        entranceColor = Objects.requireNonNull(color);
    }

    public Color getExitColor() {
        return exitColor;
    }

    public void setExitColor(Color color) {
        exitColor = Objects.requireNonNull(color);
    }

    // WALL SETTINGS

    public double getWallWidthMultiplier() {
        return wallWidthMultiplier;
    }

    public void setWallWidthMultiplier(double multiplier) {
        if (!Double.isFinite(multiplier)) {
            throw new IllegalArgumentException("Wall width must be finite.");
        }

        wallWidthMultiplier = Math.max(0.5, Math.min(3.0, multiplier));
    }

    public boolean hasRoundedWallCorners() {
        return roundedWallCorners;
    }

    public void setRoundedWallCorners(boolean rounded) {
        roundedWallCorners = rounded;
    }

    public WallTexture getWallTexture() {
        return wallTexture;
    }

    public void setWallTexture(WallTexture texture) {
        wallTexture = Objects.requireNonNull(texture);
    }
}
