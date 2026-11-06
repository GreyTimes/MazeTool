package view;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import model.MazeThemeConfig;

public class ThemeCustomizerModal {

    public static void display(
            Stage owner,
            MazeThemeConfig currentConfig,
            Runnable onThemeUpdated
    ) {

        // WORK ON A COPY UNTIL THE USER PRESSES APPLY

        MazeThemeConfig draft = currentConfig.copy();

        Stage modal = new Stage();

        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initOwner(owner);

        modal.setTitle("Maze Appearance Customizer (#29)");

        VBox root = new VBox(12);
        root.setPadding(new Insets(15));

        // PRESET SELECTOR

        ComboBox<MazeThemeConfig.ThemePreset> presetCombo =
                new ComboBox<>();

        presetCombo.getItems().setAll(
                MazeThemeConfig.ThemePreset.values()
        );

        presetCombo.setMaxWidth(Double.MAX_VALUE);
        presetCombo.setValue(null);

        // COLOR PICKERS

        ColorPicker bgPicker =
                new ColorPicker(draft.getBackgroundColor());

        ColorPicker floorPicker =
                new ColorPicker(draft.getFloorColor());

        ColorPicker wallPicker =
                new ColorPicker(draft.getWallColor());

        ColorPicker pathPicker =
                new ColorPicker(draft.getPathColor());

        ColorPicker playerPicker =
                new ColorPicker(draft.getPlayerColor());

        ColorPicker headPicker =
                new ColorPicker(draft.getHeadColor());

        ColorPicker entrancePicker =
                new ColorPicker(draft.getEntranceColor());

        ColorPicker exitPicker =
                new ColorPicker(draft.getExitColor());

        // WALL WIDTH

        Slider wallWidthSlider = new Slider(
                0.5,
                3.0,
                draft.getWallWidthMultiplier()
        );

        wallWidthSlider.setShowTickLabels(true);
        wallWidthSlider.setShowTickMarks(true);
        wallWidthSlider.setMajorTickUnit(0.5);
        wallWidthSlider.setMinorTickCount(4);
        wallWidthSlider.setBlockIncrement(0.1);

        Label wallWidthValue = new Label(
                String.format("%.1fx", wallWidthSlider.getValue())
        );

        wallWidthSlider.valueProperty().addListener(
                (obs, oldValue, newValue) ->
                        wallWidthValue.setText(
                                String.format("%.1fx", newValue.doubleValue())
                        )
        );

        // WALL CORNERS

        CheckBox roundedCornersCheck = new CheckBox(
                "Use Rounded Wall Corners"
        );

        roundedCornersCheck.setSelected(
                draft.hasRoundedWallCorners()
        );

        // WALL TEXTURE

        ComboBox<MazeThemeConfig.WallTexture> textureCombo =
                new ComboBox<>();

        textureCombo.getItems().setAll(
                MazeThemeConfig.WallTexture.values()
        );

        textureCombo.setValue(draft.getWallTexture());
        textureCombo.setMaxWidth(Double.MAX_VALUE);

        // COLOR GRID

        GridPane colorGrid = new GridPane();

        colorGrid.setHgap(12);
        colorGrid.setVgap(10);

        colorGrid.add(new Label("Canvas Background:"), 0, 0);
        colorGrid.add(bgPicker, 1, 0);

        colorGrid.add(new Label("Floor Color:"), 0, 1);
        colorGrid.add(floorPicker, 1, 1);

        colorGrid.add(new Label("Wall Color:"), 0, 2);
        colorGrid.add(wallPicker, 1, 2);

        colorGrid.add(new Label("Solution Highlight:"), 0, 3);
        colorGrid.add(pathPicker, 1, 3);

        colorGrid.add(new Label("Player Tint:"), 0, 4);
        colorGrid.add(playerPicker, 1, 4);

        colorGrid.add(new Label("Generation Head:"), 0, 5);
        colorGrid.add(headPicker, 1, 5);

        colorGrid.add(new Label("Entrance Shade:"), 0, 6);
        colorGrid.add(entrancePicker, 1, 6);

        colorGrid.add(new Label("Exit Shade:"), 0, 7);
        colorGrid.add(exitPicker, 1, 7);

        // PRESET APPLICATION

        presetCombo.setOnAction(event -> {

            MazeThemeConfig.ThemePreset selected =
                    presetCombo.getValue();

            if (selected == null) {
                return;
            }

            draft.applyPreset(selected);

            // Refresh all color pickers.

            bgPicker.setValue(draft.getBackgroundColor());
            floorPicker.setValue(draft.getFloorColor());
            wallPicker.setValue(draft.getWallColor());

            pathPicker.setValue(draft.getPathColor());
            playerPicker.setValue(draft.getPlayerColor());
            headPicker.setValue(draft.getHeadColor());

            entrancePicker.setValue(draft.getEntranceColor());
            exitPicker.setValue(draft.getExitColor());
        });

        // APPLY BUTTON

        Button applyBtn = new Button("Apply Customizations");

        applyBtn.setMaxWidth(Double.MAX_VALUE);

        applyBtn.setStyle(
                "-fx-base: #00acc1; -fx-font-weight: bold;"
        );

        applyBtn.setOnAction(event -> {

            // SAVE COLORS TO THE TEMPORARY CONFIGURATION

            draft.setBackgroundColor(bgPicker.getValue());
            draft.setFloorColor(floorPicker.getValue());
            draft.setWallColor(wallPicker.getValue());

            draft.setPathColor(pathPicker.getValue());
            draft.setPlayerColor(playerPicker.getValue());
            draft.setHeadColor(headPicker.getValue());

            draft.setEntranceColor(entrancePicker.getValue());
            draft.setExitColor(exitPicker.getValue());

            // SAVE WALL OPTIONS

            draft.setWallWidthMultiplier(wallWidthSlider.getValue());

            draft.setRoundedWallCorners(
                    roundedCornersCheck.isSelected()
            );

            draft.setWallTexture(textureCombo.getValue());

            // UPDATE THE REAL CONFIGURATION

            currentConfig.copyFrom(draft);

            // Persist the newly applied configuration.
            currentConfig.save();

            if (onThemeUpdated != null) {
                onThemeUpdated.run();
            }

            modal.close();
        });

        // CANCEL BUTTON

        Button cancelBtn = new Button("Cancel");

        cancelBtn.setMaxWidth(Double.MAX_VALUE);

        cancelBtn.setOnAction(event -> modal.close());

        HBox buttons = new HBox(10, cancelBtn, applyBtn);

        HBox.setHgrow(cancelBtn, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(applyBtn, javafx.scene.layout.Priority.ALWAYS);

        // FINAL LAYOUT

        root.getChildren().addAll(
                new Label("Preset Visual Themes:"),
                presetCombo,
                new Separator(),

                new Label("Color Customization:"),
                colorGrid,

                new Separator(),

                new Label("Wall Rendering:"),
                new Label("Wall Width:"),
                wallWidthSlider,
                wallWidthValue,
                roundedCornersCheck,

                new Label("Wall Texture:"),
                textureCombo,

                new Separator(),

                buttons
        );

        Scene scene = new Scene(root, 470, 650);

        if (draft.isDarkMode()) {
            String stylesheet =
                    ThemeCustomizerModal.class
                            .getResource("/style.css")
                            .toExternalForm();

            scene.getStylesheets().add(stylesheet);
        }

        modal.setScene(scene);
        modal.setResizable(false);
        modal.showAndWait();
    }
}
