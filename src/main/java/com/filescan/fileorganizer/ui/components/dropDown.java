package com.filescan.fileorganizer.ui.components;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

public class dropDown {
    private static String truncate(String s, int max) {
        return s.length() <= max ? s : "..." + s.substring(s.length() - (max - 3));
    }

    private static String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int idx = (int) (Math.log10(bytes) / Math.log10(1024));
        idx = Math.min(idx, units.length - 1);
        double value = bytes / Math.pow(1024, idx);
        return new DecimalFormat("#,##0.##").format(value) + " " + units[idx];
    }

    public static TitledPane createCategory(String name, int files, List<File> listFiles) {

        // ✅ Replace VBox + Labels with ListView
        ListView<String> fileList = new ListView<>();
        fileList.getStyleClass().add("dark-list");
        fileList.setPrefHeight(150);

        long totalSize = 0;
        for (File file : listFiles) {
            totalSize += file.length();
            fileList.getItems().add(file.getName()); // ✅ just strings, no Label nodes
        }

        String correctSize = formatSize(totalSize);

        String label = String.format("%-30s %-15s %d files", name, correctSize, files);
        CheckBox checkBox = new CheckBox(label);
        checkBox.getStyleClass().add("junkCategory-CheckBox");
        checkBox.setStyle("-fx-text-fill: white;" +
                "-fx-font-family: 'Consolas';");

        // expandable pane
        TitledPane pane = new TitledPane();
        pane.setGraphicTextGap(20);
        pane.getStyleClass().add("dropDownBox");
        pane.setExpanded(false);
        pane.setGraphic(checkBox);
        pane.setContent(fileList); // ✅ ListView goes directly, no ScrollPane needed

        return pane;
    }
}
