package view;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public final class EndpointMarkerRenderer {

    private EndpointMarkerRenderer() {
    }

    public static void draw(GraphicsContext gc, double x, double y, double diameter, Color color, String label) {
        gc.save();
        double size = Math.max(6.0, diameter);
        gc.setGlobalAlpha(1.0);
        gc.setFill(color);
        gc.fillOval(x - size / 2.0, y - size / 2.0, size, size);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("System", FontWeight.BOLD, Math.max(6.0, size * 0.62)));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(label, x, y + 0.2);
        gc.restore();
    }
}