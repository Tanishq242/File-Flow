package com.filescan.fileorganizer.ui.components;

import com.filescan.fileorganizer.controller.DuplicateScanController;
import com.filescan.fileorganizer.model.FileType;
import com.filescan.fileorganizer.service.DuplicateFinder;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DuplicateFileDialog {
    private static Task<Void> currentTask;

    private static int showAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete selected files?");
        alert.setContentText("File will delete Permanently");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return 1;
        }
        return 0;
    }

    private static VBox createCategoryTile(String name, FontIcon icon, Runnable action) {

        Label label = new Label(name);

        VBox box = new VBox(8, icon, label);
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(120, 120);
        box.setPadding(new Insets(10));

        // ✅ Rounded background
        box.setStyle("""
                    -fx-background-color: #99A1AF;
                    -fx-background-radius: 15;
                    -fx-border-radius: 15;
                """);

        // ✅ Click action (unchanged)
        box.setOnMouseClicked(e -> action.run());

        // ✅ Hover effect (optional but recommended)
        box.setOnMouseEntered(e -> box.setStyle("""
                    -fx-background-color: #3a3a3a;
                    -fx-background-radius: 15;
                """));

        box.setOnMouseExited(e -> box.setStyle("""
                    -fx-background-color: #99A1AF;
                    -fx-background-radius: 15;
                """));

        return box;
    }

    private static void loadFiles(VBox vbox, FileType selectedType) {

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {

                for (Map.Entry<FileType, List<List<Path>>> entry : DuplicateFinder.filesByType.entrySet()) {

                    FileType type = entry.getKey();

                    // 🔥 FILTER (if needed)
                    if (selectedType != null && type != selectedType) continue;

                    List<List<Path>> groups = entry.getValue();

                    // 🔥 CATEGORY HEADER (NEW)
                    Platform.runLater(() -> {
                        Label categoryLabel = new Label(type.name());
                        categoryLabel.setStyle("""
                            -fx-text-fill: #E8E6E3;
                            -fx-font-size: 18px;
                            -fx-font-weight: bold;
                            -fx-padding: 10 0 5 5;
                        """);
                        vbox.getChildren().add(categoryLabel);
                    });

                    for (List<Path> group : groups) {

                        VBox groupCard = new VBox();
                        groupCard.setStyle("""
                            -fx-background-color: #2E2C2A;
                            -fx-background-radius: 12;
                            -fx-border-color: #444240;
                            -fx-border-width: 0.5;
                            -fx-border-radius: 12;
                        """);

                        // ── HEADER ─────────────────────────
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

                        String firstName = group.isEmpty() ? "Unknown"
                                : group.get(0).getFileName().toString();

                        Label groupLabel = new Label(firstName);
                        groupLabel.setStyle("-fx-text-fill: #E8E6E3; -fx-font-size: 13px; -fx-font-weight: bold;");
                        HBox.setHgrow(groupLabel, Priority.ALWAYS);

                        Label groupMeta = new Label(group.size() + " copies");
                        groupMeta.setStyle("-fx-text-fill: #888680; -fx-font-size: 12px;");

                        Label chevron = new Label("▼");

                        header.getChildren().addAll(groupCheckBox, groupLabel, groupMeta, chevron);

                        // ── FILE LIST ─────────────────────────
                        VBox fileList = new VBox(6);
                        fileList.setPadding(new Insets(8));

                        List<CheckBox> fileCheckBoxes = new ArrayList<>();

                        for (int i = 0; i < group.size(); i++) {

                            Path filePath = group.get(i);
                            boolean isOriginal = (i == 0);

                            HBox fileRow = new HBox(10);
                            fileRow.setAlignment(Pos.CENTER_LEFT);
                            fileRow.setPadding(new Insets(8));

                            // Hover effect
                            fileRow.setOnMouseEntered(e -> fileRow.setStyle("""
                                -fx-background-color: #333130;
                                -fx-background-radius: 8;
                            """));
                            fileRow.setOnMouseExited(e -> fileRow.setStyle("""
                                -fx-background-color: transparent;
                            """));

                            // Checkbox
                            CheckBox fileCheckBox = new CheckBox();
                            fileCheckBox.setDisable(isOriginal);
                            fileCheckBoxes.add(fileCheckBox);

                            // 🔥 THUMBNAIL (UPDATED)
                            StackPane thumb = new StackPane();
                            thumb.setMinSize(50, 50);
                            thumb.setStyle("""
                                -fx-background-color: #3A3835;
                                -fx-background-radius: 6;
                            """);

                            FileType fileType = FileType.fromPath(filePath);
                            thumb.getChildren().add(
                                    DuplicateScanController.getFileIcon(fileType, filePath)
                            );

                            // File info
                            VBox fileInfo = new VBox(3);
                            HBox.setHgrow(fileInfo, Priority.ALWAYS);

                            Label fileName = new Label(filePath.getFileName().toString());
                            fileName.setStyle("-fx-text-fill: #E8E6E3; -fx-font-size: 13px; -fx-font-weight: bold;");

                            Label filePathLabel = new Label(
                                    filePath.getParent() != null ? filePath.getParent().toString() : ""
                            );
                            filePathLabel.setStyle("-fx-text-fill: #666460; -fx-font-size: 11px;");

                            fileInfo.getChildren().addAll(fileName, filePathLabel);

                            // Size
                            long size = 0;
                            try { size = Files.size(filePath); } catch (Exception ignored) {}
                            Label sizeLabel = new Label(formatSize(size));
                            sizeLabel.setStyle("-fx-text-fill: #888680; -fx-font-size: 12px;");

                            // 🔥 BADGE (UPDATED STYLE)
                            Label badge = new Label(isOriginal ? "Original" : "Duplicate");
                            badge.setPadding(new Insets(2, 8, 2, 8));
                            badge.setStyle(isOriginal
                                    ? "-fx-background-color: #1A3A2A; -fx-text-fill: #4CAF7D; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 20;"
                                    : "-fx-background-color: #3A2E10; -fx-text-fill: #D4A017; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 20;"
                            );

                            fileRow.getChildren().addAll(fileCheckBox, thumb, fileInfo, sizeLabel, badge);
                            fileList.getChildren().add(fileRow);
                        }

                        // Select all duplicates
                        groupCheckBox.setOnAction(e -> {
                            boolean selected = groupCheckBox.isSelected();
                            fileCheckBoxes.forEach(cb -> {
                                if (!cb.isDisabled()) cb.setSelected(selected);
                            });
                        });

                        // Collapse / expand
                        header.setOnMouseClicked(e -> {
                            if (e.getTarget() instanceof CheckBox) return;
                            boolean visible = fileList.isVisible();
                            fileList.setVisible(!visible);
                            fileList.setManaged(!visible);
                            chevron.setText(visible ? "▶" : "▼");
                        });

                        groupCard.getChildren().addAll(header, fileList);

                        Platform.runLater(() -> vbox.getChildren().add(groupCard));
                    }
                }
                return null;
            }
        };

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private static void loadDuplicateFiles(VBox vbox,
                                           Map<FileType, List<List<Path>>> data) {

        // Clear old UI
        vbox.getChildren().clear();

        for (Map.Entry<FileType, List<List<Path>>> entry : data.entrySet()) {

            FileType type = entry.getKey();
            List<List<Path>> groups = entry.getValue();

            // 🔥 CATEGORY HEADER (IMPORTANT)
            Label categoryLabel = new Label(type.name());
            categoryLabel.setStyle("""
                        -fx-text-fill: #E8E6E3;
                        -fx-font-size: 18px;
                        -fx-font-weight: bold;
                        -fx-padding: 10 0 5 5;
                    """);

            vbox.getChildren().add(categoryLabel);

            for (List<Path> group : groups) {

                // ── GROUP CARD ─────────────────────────
                VBox groupCard = new VBox(5);
                groupCard.setStyle("""
                            -fx-background-color: #2E2C2A;
                            -fx-background-radius: 10;
                            -fx-padding: 8;
                            -fx-border-color: #444240;
                            -fx-border-radius: 10;
                        """);

                for (int i = 0; i < group.size(); i++) {

                    Path filePath = group.get(i);
                    boolean isOriginal = (i == 0);

                    // ── FILE ROW ─────────────────────────
                    HBox fileRow = new HBox(10);
                    fileRow.setAlignment(Pos.CENTER_LEFT);
                    fileRow.setPadding(new Insets(5));

                    // Checkbox
                    CheckBox checkBox = new CheckBox();
                    checkBox.setDisable(isOriginal);

                    // Thumbnail
                    StackPane thumb = new StackPane();
                    thumb.setPrefSize(40, 40);
                    thumb.setStyle("-fx-background-color: #3A3835; -fx-background-radius: 5;");

                    FileType fileType = FileType.fromPath(filePath);
                    thumb.getChildren().add(DuplicateScanController.getFileIcon(fileType, filePath));

                    // File Info
                    VBox info = new VBox(2);

                    Label name = new Label(filePath.getFileName().toString());
                    name.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");

                    Label pathLabel = new Label(
                            filePath.getParent() != null ? filePath.getParent().toString() : ""
                    );
                    pathLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 10px;");

                    info.getChildren().addAll(name, pathLabel);

                    // Size
                    long size = 0;
                    try {
                        size = Files.size(filePath);
                    } catch (Exception ignored) {
                    }

                    Label sizeLabel = new Label(formatSize(size));
                    sizeLabel.setStyle("-fx-text-fill: #aaa;");

                    // Badge
                    Label badge = new Label(isOriginal ? "Original" : "Duplicate");
                    badge.setStyle(isOriginal
                            ? "-fx-background-color: green; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 10;"
                            : "-fx-background-color: orange; -fx-text-fill: black; -fx-padding: 2 6; -fx-background-radius: 10;"
                    );

                    // 🔥 ADD EVERYTHING (IMPORTANT)
                    fileRow.getChildren().addAll(checkBox, thumb, info, sizeLabel, badge);

                    groupCard.getChildren().add(fileRow); // ✅ attach row
                }

                vbox.getChildren().add(groupCard); // ✅ attach group
            }
        }
    }

    // ── Helper: format bytes to human-readable size ───────────────────────────────
    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private static void showImages(VBox root, StackPane container, HBox button) {
//        loadFiles(tile, FileType.IMAGE);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        ScrollPane scroll = new ScrollPane(vbox);
        scroll.setFitToWidth(true);
        scroll.setStyle("""
                    -fx-background: transparent;
                    -fx-background-color: transparent;
                """);

        loadFiles(vbox, FileType.IMAGE);

        container.getChildren().setAll(scroll);
        if (!root.getChildren().contains(button)) {
            root.getChildren().add(button);
        }
    }

    private static void showVideos(VBox root, StackPane container, HBox button) {
//        loadFiles(tile, FileType.VIDEO);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        ScrollPane scroll = new ScrollPane(vbox);
        scroll.setFitToWidth(true);
        scroll.setStyle("""
                    -fx-background: transparent;
                    -fx-background-color: transparent;
                """);

        loadFiles(vbox, FileType.VIDEO);

        container.getChildren().setAll(scroll);
        if (!root.getChildren().contains(button)) {
            root.getChildren().add(button);
        }
    }

    private static void showMusic(VBox root, StackPane container, HBox button) {
//        loadFiles(tile, FileType.MUSIC);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        ScrollPane scroll = new ScrollPane(vbox);
        scroll.setFitToWidth(true);
        scroll.setStyle("""
                    -fx-background: transparent;
                    -fx-background-color: transparent;
                """);

        loadFiles(vbox, FileType.MUSIC);

        container.getChildren().setAll(scroll);
        if (!root.getChildren().contains(button)) {
            root.getChildren().add(button);
        }
    }

    private static void showDocuments(VBox root, StackPane container, HBox button) {
//        loadFiles(tile, FileType.DOCUMENT);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        ScrollPane scroll = new ScrollPane(vbox);
        scroll.setFitToWidth(true);
        scroll.setStyle("""
                    -fx-background: transparent;
                    -fx-background-color: transparent;
                """);

        loadFiles(vbox, FileType.DOCUMENT);

        container.getChildren().setAll(scroll);
        if (!root.getChildren().contains(button)) {
            root.getChildren().add(button);
        }
    }

    public static void show(Stage owner) {

        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Duplicate Files");

        TilePane categories = new TilePane();
        categories.setHgap(20);
        categories.setVgap(20);
        categories.setAlignment(Pos.CENTER);

        StackPane mainContainer = new StackPane();
        mainContainer.getChildren().add(categories);


        TilePane tile = new TilePane();
        tile.setPrefColumns(5);
        tile.setHgap(10);
        tile.setVgap(10);

        Button deleteBtn = new Button("Delete Selected");
        Button backBtn = new Button("Back");
        HBox buttons = new HBox(10, deleteBtn, backBtn);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(10));

//        deleteBtn.setOnAction(e -> {
//            if (showAlert() != 1) {
//                for (Path path : selectedFiles) {
//                    try {
//                        Files.deleteIfExists(path);
//                    } catch (Exception ex) {
//                        ex.printStackTrace();
//                    }
//                }
//
//                // remove from UI
//                tile.getChildren().removeIf(node -> {
//                    if (node instanceof StackPane sp) {
//                        return selectedFiles.contains((Path) sp.getUserData());
//                    }
//                    return false;
//                });
//
//                selectedFiles.clear();
//            }
//        });


        FontIcon imageIcon = new FontIcon("fas-image");
        imageIcon.setIconSize(40);
        FontIcon videoIcon = new FontIcon("fas-video");
        videoIcon.setIconSize(40);
        FontIcon musicIcon = new FontIcon("fas-music");
        musicIcon.setIconSize(40);
        FontIcon docIcon = new FontIcon("fas-file-alt");
        docIcon.setIconSize(40);

        VBox root = new VBox(mainContainer);
        VBox.setVgrow(mainContainer, Priority.ALWAYS);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.CENTER);

        backBtn.setOnAction(e -> {
            if (currentTask != null && currentTask.isRunning()) {
                currentTask.cancel();   // 🔥 stop task
            }

            tile.getChildren().clear();

            // Reset main container properly
            mainContainer.getChildren().setAll(categories);

            // Remove buttons safely
            root.getChildren().remove(buttons);
        });

        categories.getChildren().addAll(
                createCategoryTile("Image", imageIcon, () -> showImages(root, mainContainer, buttons)),
                createCategoryTile("Music", musicIcon, () -> showMusic(root, mainContainer, buttons)),
                createCategoryTile("Videos", videoIcon, () -> showVideos(root, mainContainer, buttons)),
                createCategoryTile("Document", docIcon, () -> showDocuments(root, mainContainer, buttons))
        );

        Scene scene = new Scene(root, 1000, 700);

        // Apply dark theme
        scene.getStylesheets().add(DuplicateFileDialog.class.getResource("/css/dark-theme.css").toExternalForm());

        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
