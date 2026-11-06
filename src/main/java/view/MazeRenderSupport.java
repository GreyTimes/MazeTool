
package view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import model.MazeThemeConfig;

public final class MazeRenderSupport {

    private MazeRenderSupport() {
    }

    public static void configureWallStroke(
            GraphicsContext gc,
            MazeThemeConfig config,
            double baseWidth
    ) {

        double width = Math.max(0.5, baseWidth);

        boolean dotted =
                config.getWallTexture() == MazeThemeConfig.WallTexture.DOTTED;

        gc.setLineWidth(width);

        gc.setLineCap(
                config.hasRoundedWallCorners() || dotted
                        ? StrokeLineCap.ROUND
                        : StrokeLineCap.BUTT
        );

        gc.setLineJoin(
                config.hasRoundedWallCorners()
                        ? StrokeLineJoin.ROUND
                        : StrokeLineJoin.MITER
        );

        gc.setLineDashOffset(0);

        switch (config.getWallTexture()) {

            case SOLID -> gc.setLineDashes();

            case DASHED -> gc.setLineDashes(
                    width * 4.0,
                    width * 2.0
            );

            case DOTTED -> gc.setLineDashes(
                    Math.max(0.1, width * 0.05),
                    width * 2.0
            );
        }
    }
}
