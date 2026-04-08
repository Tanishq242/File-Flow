package com.filescan.fileorganizer.ui.components;

import javafx.animation.TranslateTransition;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class ToggleSwitch extends Pane {
    private final Rectangle background;
    private final Circle trigger;

    private boolean isOn = false;
    private final double width;
    private final double height;
    private final double radius;
    private final double thumbMargin;

    public ToggleSwitch(double width, double height) {
        this.width = width;
        this.height = height;
        this.radius = height / 2;
        this.thumbMargin = height * 0.05;

        background = new Rectangle(width, height);
        background.setArcWidth(height);
        background.setArcHeight(height);
        background.setFill(Color.LIGHTGRAY);

        trigger = new Circle(radius - thumbMargin);
        trigger.setFill(Color.WHITE);
        trigger.setStroke(Color.LIGHTGRAY);
        trigger.setCenterX(radius);
        trigger.setCenterY(radius);

        getChildren().addAll(background, trigger);

        setMinSize(width, height);
        setMaxSize(width, height);

        setOnMouseClicked(this::toggle);
    }

    private void toggle(MouseEvent e) {
        TranslateTransition transition = new TranslateTransition(Duration.seconds(0.2), trigger);

        double toX = isOn ? 0 : width - height;

        transition.setToX(toX);
        transition.play();

        background.setFill(isOn ? Color.LIGHTGRAY : Color.web("#6bcb77"));
        isOn = !isOn;

    }

    public boolean isOn() {
        return isOn;
    }
}