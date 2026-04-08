package com.filescan.fileorganizer.controller;

import com.filescan.fileorganizer.model.FileType;
import com.filescan.fileorganizer.service.DuplicateFinder;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class DuplicateScanController {
    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public static ImageView getFileIcon(FileType type, Path filePath) {

        ImageView iv;

        // ✅ If image → show actual image preview
        if (type == FileType.IMAGE) {
            iv = new ImageView(new Image(
                    filePath.toUri().toString(),
                    48, 48, true, true, true
            ));
            iv.setFitWidth(48);
            iv.setFitHeight(48);
            return iv;
        }

        // 🔥 For other types → use icons
        String iconPath;

        switch (type) {
            case MUSIC:
                iconPath = "/icons/record.png";
                break;
            case DOCUMENT:
                iconPath = "/icons/txt.png";
                break;
            case VIDEO:
                iconPath = "/icons/movie.png";
                break;
            default:
                iconPath = "/icons/file.png";
        }

        // ✅ Load resource safely
        var resource = DuplicateScanController.class.getResource(iconPath);

        if (resource == null) {
            System.out.println("❌ Missing icon: " + iconPath);
            return new ImageView(); // fallback (prevents crash)
        }

        iv = new ImageView(new Image(resource.toExternalForm()));
        iv.setFitWidth(26);
        iv.setFitHeight(26);

        return iv;
    }

    private static void loadDuplicateFiles(VBox vbox,
                                           Map<FileType, List<List<Path>>> data) {
        vbox.getChildren().clear();

        for (Map.Entry<FileType, List<List<Path>>> entry : data.entrySet()) {

            FileType type = entry.getKey();
            System.out.println(type); // ✅ now will print ALL

            List<List<Path>> groups = entry.getValue();

            for (List<Path> group : groups) {

                VBox groupCard = new VBox();
                groupCard.setStyle("""
                        -fx-background-color: #2E2C2A;
                        -fx-background-radius: 12;
                        -fx-border-color: #444240;
                        -fx-border-width: 0.5;
                        -fx-border-radius: 12;
                        """);

                // ── Group header ──────────────────────────────────────────
                HBox header = new HBox(10);
                header.setAlignment(Pos.CENTER_LEFT);
                header.setPadding(new Insets(10, 14, 10, 14));
                header.setStyle("""
                        -fx-background-color: #252422;
                        -fx-background-radius: 12 12 0 0;
                        -fx-border-color: #444240;
                        -fx-border-width: 0 0 0.5 0;
                        """);

                CheckBox groupCheckBox = new CheckBox();
                groupCheckBox.setStyle("-fx-cursor: hand;");

                String firstName = group.isEmpty() ? "Unknown" : group.get(0).getFileName().toString();
                Label groupLabel = new Label(firstName);
                groupLabel.setStyle("-fx-text-fill: #E8E6E3; -fx-font-size: 13px; -fx-font-weight: bold;");
                HBox.setHgrow(groupLabel, Priority.ALWAYS);

                Label groupMeta = new Label(group.size() + " copies");
                groupMeta.setStyle("-fx-text-fill: #888680; -fx-font-size: 12px;");

                Label chevron = new Label("▼");
                chevron.setStyle("-fx-text-fill: #666460; -fx-font-size: 10px;");

                header.getChildren().addAll(groupCheckBox, groupLabel, groupMeta, chevron);

                // ── File rows container ───────────────────────────────────
                VBox fileList = new VBox(6);
                fileList.setPadding(new Insets(8, 8, 10, 8));

                List<CheckBox> fileCheckBoxes = new ArrayList<>();

                for (int i = 0; i < group.size(); i++) {
                    Path filePath = group.get(i);
                    boolean isOriginal = (i == 0);

                    HBox fileRow = new HBox(10);
                    fileRow.setAlignment(Pos.CENTER_LEFT);
                    fileRow.setPadding(new Insets(8, 10, 8, 10));
                    fileRow.setStyle("""
                            -fx-background-color: transparent;
                            -fx-background-radius: 8;
                            -fx-border-color: transparent;
                            -fx-border-radius: 8;
                            -fx-border-width: 0.5;
                            """);

                    // Hover effect
                    fileRow.setOnMouseEntered(e -> fileRow.setStyle("""
                            -fx-background-color: #333130;
                            -fx-background-radius: 8;
                            -fx-border-color: #444240;
                            -fx-border-radius: 8;
                            -fx-border-width: 0.5;
                            """));
                    fileRow.setOnMouseExited(e -> fileRow.setStyle("""
                            -fx-background-color: transparent;
                            -fx-background-radius: 8;
                            -fx-border-color: transparent;
                            -fx-border-radius: 8;
                            -fx-border-width: 0.5;
                            """));

                    // File checkbox (disabled for original)
                    CheckBox fileCheckBox = new CheckBox();
                    fileCheckBox.setDisable(isOriginal);
                    fileCheckBox.setStyle("-fx-cursor: hand;");
                    fileCheckBoxes.add(fileCheckBox);

                    // Thumbnail
                    StackPane thumb = new StackPane();
                    thumb.setMinSize(52, 52);
                    thumb.setMaxSize(52, 52);
                    thumb.setStyle("""
                            -fx-background-color: #3A3835;
                            -fx-background-radius: 6;
                            -fx-border-color: #4A4846;
                            -fx-border-radius: 6;
                            -fx-border-width: 0.5;
                            """);

                    FileType fileType = FileType.fromPath(filePath);
                    thumb.getChildren().add(getFileIcon(fileType, filePath));

                    // File name + path
                    VBox fileInfo = new VBox(3);
                    HBox.setHgrow(fileInfo, Priority.ALWAYS);
                    fileInfo.setMinWidth(0);

                    Label fileName = new Label(filePath.getFileName().toString());
                    fileName.setStyle("-fx-text-fill: #E8E6E3; -fx-font-size: 13px; -fx-font-weight: bold;");
                    fileName.setMaxWidth(Double.MAX_VALUE);
                    fileName.setEllipsisString("…");

                    Label filePathLabel = new Label(filePath.getParent() != null ? filePath.getParent().toString() : "");
                    filePathLabel.setStyle("-fx-text-fill: #666460; -fx-font-size: 11px;");
                    filePathLabel.setMaxWidth(Double.MAX_VALUE);
                    filePathLabel.setEllipsisString("…");

                    fileInfo.getChildren().addAll(fileName, filePathLabel);

                    // File size
                    long sizeBytes = 0;
                    try {
                        sizeBytes = Files.size(filePath);
                    } catch (Exception ignored) {
                    }
                    String sizeStr = formatSize(sizeBytes);
                    Label sizeLabel = new Label(sizeStr);
                    sizeLabel.setStyle("-fx-text-fill: #888680; -fx-font-size: 12px;");

                    // Original / Duplicate badge
                    Label badge = new Label(isOriginal ? "original" : "duplicate");
                    badge.setPadding(new Insets(2, 8, 2, 8));
                    badge.setStyle(isOriginal
                            ? "-fx-background-color: #1A3A2A; -fx-text-fill: #4CAF7D; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 20;"
                            : "-fx-background-color: #3A2E10; -fx-text-fill: #D4A017; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 20;"
                    );

                    fileRow.getChildren().addAll(fileCheckBox, thumb, fileInfo, sizeLabel, badge);
                    fileList.getChildren().add(fileRow);
                }

                // ── Group checkbox → toggles all duplicate checkboxes ─────
                groupCheckBox.setOnAction(e -> {
                    boolean selected = groupCheckBox.isSelected();
                    fileCheckBoxes.forEach(cb -> {
                        if (!cb.isDisabled()) cb.setSelected(selected);
                    });
                });

                // ── Collapse / expand on header click ─────────────────────
                header.setOnMouseClicked(e -> {
                    if (e.getTarget() instanceof CheckBox) return;
                    boolean visible = fileList.isVisible();
                    fileList.setVisible(!visible);
                    fileList.setManaged(!visible);
                    chevron.setText(visible ? "▶" : "▼");
                });
                header.setStyle(header.getStyle() + "-fx-cursor: hand;");

                groupCard.getChildren().addAll(header, fileList);

                vbox.getChildren().add(groupCard);
            }
        }
    }

    public static void startScan(String drive, VBox vBox) throws IOException {
        String normalizedDrive = drive.toUpperCase().replace(":", "") + ":\\";

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                DuplicateFinder.showDuplicate(normalizedDrive); // ✅ ONLY scanning
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            // ✅ FX thread safe
            DuplicateScanController.loadDuplicateFiles(vBox, DuplicateFinder.filesByType);
        });

        new Thread(task).start();

    }
}
