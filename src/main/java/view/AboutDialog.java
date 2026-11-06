
package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;

public final class AboutDialog {

    private static final String REPOSITORY_URL =
            "https://github.com/GreyTimes/MazinStuff";

    private static final String FALLBACK_VERSION = "2.0.0";

    private AboutDialog() {
    }

    public static void display(Stage owner, boolean darkMode) {

        Stage dialog = new Stage();

        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);

        dialog.setTitle("About MazinStuff");
        dialog.setResizable(false);

        // COLORS

        String background = darkMode ? "#151a21" : "#f1f4f8";
        String card = darkMode ? "#202832" : "#ffffff";
        String primaryText = darkMode ? "#edf2f7" : "#1f2937";
        String secondaryText = darkMode ? "#aab6c5" : "#64748b";
        String accent = "#00acc1";

        // MAIN CONTAINER

        VBox root = new VBox(15);
        root.setPadding(new Insets(22));
        root.setAlignment(Pos.CENTER);

        root.setStyle(
                "-fx-background-color: " + background + ";"
                        + "-fx-font-family: 'Segoe UI';"
        );

        // LOGO

        VBox logoContainer = new VBox(8);
        logoContainer.setAlignment(Pos.CENTER);

        URL logoResource =
                AboutDialog.class.getResource("/MazinLogo.jpg");

        if (logoResource != null) {

            Image logoImage = new Image(
                    logoResource.toExternalForm(),
                    400,
                    180,
                    true,
                    true
            );

            ImageView logoView = new ImageView(logoImage);

            logoView.setPreserveRatio(true);
            logoView.setFitWidth(400);
            logoView.setFitHeight(180);

            logoContainer.getChildren().add(logoView);

        } else {

            Label fallbackLogo = new Label("MazinStuff");

            fallbackLogo.setStyle(
                    "-fx-font-size: 30px;"
                            + "-fx-font-weight: bold;"
                            + "-fx-text-fill: " + primaryText + ";"
            );

            logoContainer.getChildren().add(fallbackLogo);
        }

        Label subtitle = new Label(
                "Maze Generator & Solver"
        );

        subtitle.setStyle(
                "-fx-font-size: 15px;"
                        + "-fx-text-fill: " + secondaryText + ";"
        );

        logoContainer.getChildren().add(subtitle);

        // VERSION

        String version = getApplicationVersion();

        Label versionLabel = new Label(
                "Version " + version
        );

        versionLabel.setStyle(
                "-fx-background-color: " + card + ";"
                        + "-fx-background-radius: 20;"
                        + "-fx-padding: 6 16;"
                        + "-fx-text-fill: " + accent + ";"
                        + "-fx-font-size: 12px;"
                        + "-fx-font-weight: bold;"
        );

        // DESCRIPTION

        Label description = new Label(
                "Create, explore, solve and play with mazes "
                        + "of different shapes and complexities."
        );

        description.setWrapText(true);
        description.setMaxWidth(410);
        description.setAlignment(Pos.CENTER);

        description.setStyle(
                "-fx-font-size: 13px;"
                        + "-fx-text-fill: " + primaryText + ";"
        );

        // FEATURES CARD

        VBox featureCard = new VBox(10);

        featureCard.setPadding(new Insets(15));
        featureCard.setMaxWidth(420);

        featureCard.setStyle(
                "-fx-background-color: " + card + ";"
                        + "-fx-background-radius: 10;"
        );

        Label featuresTitle = new Label("Application Features");

        featuresTitle.setStyle(
                "-fx-font-size: 15px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: " + primaryText + ";"
        );

        Label features = new Label(
                "• Four maze geometries: Rectangular, Circular, "
                        + "Hexagonal and Triangular.\n\n"

                        + "• Five generation algorithms and three "
                        + "pathfinding algorithms.\n\n"

                        + "• Interactive playable mode with Fog of War.\n\n"

                        + "• Custom visual themes, color palettes and "
                        + "wall rendering options.\n\n"

                        + "• Maze saving, loading and PNG exporting.\n\n"

                        + "• Performance benchmarking and generation seeds."
        );

        features.setWrapText(true);
        features.setMaxWidth(390);

        features.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-text-fill: " + secondaryText + ";"
        );

        featureCard.getChildren().addAll(
                featuresTitle,
                features
        );

        // APPLICATION INFORMATION

        VBox information = new VBox(5);

        information.setAlignment(Pos.CENTER);

        Label developer = new Label("Developed by GreyTimes");

        developer.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: " + primaryText + ";"
        );

        Label technologies = new Label(
                "Built with Java 21, JavaFX 21 and Gradle"
        );

        technologies.setStyle(
                "-fx-font-size: 11px;"
                        + "-fx-text-fill: " + secondaryText + ";"
        );

        Label copyright = new Label(
                "© 2026 GreyTimes"
        );

        copyright.setStyle(
                "-fx-font-size: 10px;"
                        + "-fx-text-fill: " + secondaryText + ";"
        );

        information.getChildren().addAll(
                developer,
                technologies,
                copyright
        );

        // GITHUB LINK

        Hyperlink repositoryLink = new Hyperlink(
                "Visit GitHub Repository"
        );

        repositoryLink.setStyle(
                "-fx-text-fill: " + accent + ";"
                        + "-fx-font-size: 12px;"
        );

        repositoryLink.setOnAction(event -> {

            try {

                if (!Desktop.isDesktopSupported()) {
                    throw new UnsupportedOperationException(
                            "Desktop browsing is not supported."
                    );
                }

                Desktop.getDesktop().browse(
                        URI.create(REPOSITORY_URL)
                );

            } catch (Exception ex) {

                Alert alert = new Alert(
                        Alert.AlertType.ERROR
                );

                alert.initOwner(dialog);

                alert.setTitle("Unable to Open Repository");
                alert.setHeaderText("Could not open GitHub.");

                alert.setContentText(ex.getMessage());

                alert.showAndWait();
            }
        });

        // CLOSE BUTTON

        Button closeButton = new Button("Close");

        closeButton.setDefaultButton(true);
        closeButton.setMinWidth(110);

        closeButton.setStyle(
                "-fx-background-color: " + accent + ";"
                        + "-fx-text-fill: white;"
                        + "-fx-font-weight: bold;"
                        + "-fx-background-radius: 5;"
                        + "-fx-padding: 8 18;"
        );

        closeButton.setOnAction(event -> dialog.close());

        // FINAL LAYOUT

        root.getChildren().addAll(
                logoContainer,
                versionLabel,
                description,
                featureCard,
                information,
                repositoryLink,
                closeButton
        );

        Scene scene = new Scene(root, 480, 680);

        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private static String getApplicationVersion() {

        Package appPackage = AboutDialog.class.getPackage();

        String version = appPackage.getImplementationVersion();

        if (version == null || version.isBlank()) {
            return FALLBACK_VERSION;
        }

        return version;
    }
}
