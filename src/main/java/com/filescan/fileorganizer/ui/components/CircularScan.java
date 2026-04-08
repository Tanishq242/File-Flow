package com.filescan.fileorganizer.ui.components;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class CircularScan {
    public static StackPane circularBuffer() {
        // Arc (scanner ring)
        Arc arc = new Arc();
        arc.setRadiusX(80);
        arc.setRadiusY(80);
        arc.setStartAngle(0);
        arc.setLength(270);
        arc.setType(ArcType.OPEN);
        arc.setStrokeWidth(8);
        arc.setFill(null);

        // Gradient stroke
        arc.setStroke(new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#6366f1"))
        ));

        // Center text
        Text text = new Text("Scanning...");
        text.setStyle("-fx-font-weight: bold;");
        text.setFill(Color.WHITE);

        // Layout
        StackPane root = new StackPane(arc, text);
        root.setAlignment(Pos.CENTER);

        // Rotation animation
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(arc.rotateProperty(), 0)),
                new KeyFrame(Duration.seconds(1),
                        new KeyValue(arc.rotateProperty(), 360))
        );

        timeline.setCycleCount(Animation.INDEFINITE);
//        timeline.setInterpolator(javafx.animation.Interpolator.LINEAR);
        timeline.play();

        return root;
    }
}
