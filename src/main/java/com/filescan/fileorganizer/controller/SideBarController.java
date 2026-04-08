package com.filescan.fileorganizer.controller;

import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.function.Supplier;

public class SideBarController {
    public static Label selectedLabel = null; // tracks currently active label

    public static Label createNavLabel(String text, Node icon, Runnable action) {
        Label label = new Label(text, icon);
        label.getStyleClass().add("nav-label");

        label.setOnMouseClicked(e -> {
            // Remove active style from previous
            if (selectedLabel != null) {
                selectedLabel.getStyleClass().remove("menu-time-active");
            }
            // Set active style on current
            label.getStyleClass().add("menu-time-active");
            selectedLabel = label;

            // Run the action for this label
            action.run();
        });

        // Hover effects
        label.setOnMouseEntered(e -> label.setCursor(Cursor.HAND));
        label.setOnMouseExited(e -> label.setCursor(Cursor.DEFAULT));

        return label;
    }

    public static void changeContent(VBox box, Supplier<Parent> uiFunction) {
        box.getChildren().clear();
        box.getChildren().add(uiFunction.get());
    }
}
