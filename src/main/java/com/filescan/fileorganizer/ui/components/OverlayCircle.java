package com.filescan.fileorganizer.ui.components;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public class OverlayCircle {
    public static StackPane transparentCircle(int size, int posX, int posY, String hexColor, VBox inputBox) {
        Circle circle = new Circle(size);
        circle.setFill(new RadialGradient(
                0, 0,
                0.5, 0.5,
                1,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.web(hexColor)),
                new Stop(1, Color.TRANSPARENT)
        ));
        circle.setTranslateX(posX);
        circle.setTranslateY(posY);

        StackPane sepBox = new StackPane(inputBox, circle);
//        sepBox.setPrefSize(450, 500);
        StackPane stackPane = new StackPane(sepBox);
        StackPane.setAlignment(circle, Pos.TOP_RIGHT);
        stackPane.setStyle("""
                    -fx-background-color: #1e1e28;
                    -fx-background-radius: 15;
                    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 5);
                """);
        stackPane.setPrefSize(200, 200);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(30);   // 2 × background radius
        clip.setArcHeight(30);

        stackPane.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            clip.setWidth(newBounds.getWidth());
            clip.setHeight(newBounds.getHeight());
        });

        stackPane.setClip(clip);
        return stackPane;
    }
}
