package com.filescan.fileorganizer.ui.components;

import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class CompletionDialog {
    public static void showCompletionDialog(Stage owner) {

        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        // ✔ Check icon (SVG)
        SVGPath check = new SVGPath();
        check.setContent("M10 50 L40 80 L90 20"); // check shape
        check.setStroke(Color.WHITE);
        check.setStrokeWidth(6);
        check.setFill(null);

        Circle circle = new Circle(45, Color.web("#22c55e"));

        StackPane icon = new StackPane(circle, check);

        // ✨ Animation
        ScaleTransition scale = new ScaleTransition(Duration.millis(400), icon);
        scale.setFromX(0);
        scale.setFromY(0);
        scale.setToX(1);
        scale.setToY(1);

        // Title
        Label title = new Label("Task Completed!");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        // Subtitle
        Label subtitle = new Label("Your files have been organized successfully.");
        subtitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

        // Button
        Button closeBtn = new Button("Awesome 👍");
        closeBtn.setStyle("""
                    -fx-background-color: linear-gradient(to right, #22c55e, #16a34a);
                    -fx-text-fill: white;
                    -fx-font-size: 13px;
                    -fx-padding: 8 20;
                    -fx-background-radius: 10;
                """);

        closeBtn.setOnAction(e -> dialog.close());

        VBox box = new VBox(15, icon, title, subtitle, closeBtn);
        box.setAlignment(Pos.CENTER);

        box.setStyle("""
                    -fx-background-color: rgba(15,23,42,0.95);
                    -fx-padding: 30;
                    -fx-background-radius: 20;
                    -fx-border-radius: 20;
                    -fx-border-color: rgba(255,255,255,0.08);
                """);

        StackPane root = new StackPane(box);
        root.setStyle("-fx-background-color: rgba(0,0,0,0.6);");

        Scene scene = new Scene(root, 350, 300);
        scene.setFill(Color.TRANSPARENT);

        dialog.setScene(scene);
        dialog.show();

        scale.play(); // run animation
    }
}
