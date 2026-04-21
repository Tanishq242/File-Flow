package com.filescan.fileorganizer.ui.components;

import com.filescan.fileorganizer.controller.DuplicateScanController;
import com.filescan.fileorganizer.model.FileType;
import com.filescan.fileorganizer.service.DuplicateFinder;
import com.filescan.fileorganizer.service.JunkFilesFinder;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.filescan.fileorganizer.service.FileService.categorizedFiles;
import static com.filescan.fileorganizer.service.FileService.mediaFiles;
import static com.filescan.fileorganizer.ui.components.dropDown.createCategory;

public class scanResultDialogBox {
    static ArrayList<String> pathList = new ArrayList<>();
    private static final Set<Path> selectedFiles = new HashSet<>();
    private static boolean mediaAvailable = false;
    private static Node nodeRef;
    private static Task<Void> currentTask;

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

    private static Label getSizeLabel(Path path) throws IOException {
        // 👉 get file size
        long size = Files.size(path);

        Label sizeLabel = new Label(formatSize(size));
        sizeLabel.setStyle("""
                    -fx-background-color: rgba(0,0,0,0.6);
                    -fx-text-fill: white;
                    -fx-padding: 3 6;
                    -fx-background-radius: 5;
                """);

        sizeLabel.setVisible(false);

        return sizeLabel;
    }

    private static Image getMusicArtwork(Path path) {
        try {
            Mp3File mp3 = new Mp3File(path.toFile());

            if (mp3.hasId3v2Tag()) {
                ID3v2 tag = mp3.getId3v2Tag();
                byte[] imageData = tag.getAlbumImage();

                if (imageData != null) {
                    return new Image(new ByteArrayInputStream(imageData));
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private static ImageView createPdfThumbnail(Path path) {
        try (PDDocument doc = PDDocument.load(path.toFile())) {

            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage bim = renderer.renderImageWithDPI(0, 100);

            Image fxImage = SwingFXUtils.toFXImage(bim, null);

            ImageView iv = new ImageView(fxImage);
            iv.setFitWidth(100);
            iv.setFitHeight(100);
            iv.setPreserveRatio(true);

            return iv;

        } catch (Exception e) {
            e.printStackTrace();
            return new ImageView(new Image("file:icons/pdf.png", 100, 100, true, true));
        }
    }

    private static Node createFileNode(Path i, FileType type) throws IOException {
//        System.out.println("Creating node for: " + i);

        Label sizeLabel = getSizeLabel(i);
        ImageView iv = null;

        StackPane wrapper = new StackPane();
        wrapper.setPadding(new Insets(2));

        try {

            // ───── IMAGE ─────
            if (type == FileType.IMAGE) {

                Image img = new Image(i.toUri().toString(), 100, 100, true, true, true);

                if (img.isError()) {
                    System.out.println("⚠ Failed image: " + i);

                    // fallback placeholder
                    var url = LargeFileDialog.class
                            .getResource("/icons/image.png");

                    if (url != null) {
                        img = new Image(url.toExternalForm(), 100, 100, true, true);
                    } else {
                        img = new ImageView().getImage(); // empty fallback
                    }
                }

                iv = new ImageView(img);

                wrapper.setUserData(i);
                wrapper.getChildren().addAll(iv, sizeLabel);
                StackPane.setAlignment(sizeLabel, Pos.BOTTOM_CENTER);

            }

            // ───── MUSIC ─────
            else if (type == FileType.MUSIC) {

                Image img = getMusicArtwork(i);

                if (img == null) {
                    var url = LargeFileDialog.class
                            .getResource("/icons/record.png");

                    if (url != null) {
                        img = new Image(url.toExternalForm(), 50, 50, true, true, false);
                    }
                }

                if (img != null) {
                    iv = new ImageView(img);
                } else {
                    return null;
                }

                iv.setFitWidth(50);
                iv.setFitHeight(50);
                iv.setPreserveRatio(true);

                Label songLabel = new Label(i.getFileName().toString());

                StackPane imagePane = new StackPane(iv);
                imagePane.setStyle("-fx-background-color: #9F9FA9; -fx-border-radius: 15; -fx-background-radius: 15;");
                imagePane.setPrefSize(60, 60);

                VBox box = new VBox(5, imagePane, songLabel);
                box.setAlignment(Pos.CENTER);
                box.setPrefSize(80, 80);

                wrapper.getChildren().addAll(box, sizeLabel);
            }

            // ───── DOCUMENT ─────
            else if (type == FileType.DOCUMENT) {
                System.out.println(i.toAbsolutePath());
                if (i.toString().toLowerCase().endsWith(".pdf")) {
                    iv = createPdfThumbnail(i);
                } else {
                    var url = LargeFileDialog.class
                            .getResource("/file_ext_icon/txt.png");

                    if (url != null) {
                        iv = new ImageView(new Image(url.toExternalForm(), 100, 100, true, true));
                    }
                }

                if (iv != null) {
                    wrapper.getChildren().addAll(iv, sizeLabel);
                }
            }

            // ───── VIDEO ─────
            else if (type == FileType.VIDEO) {

                var url = LargeFileDialog.class
                        .getResource("/icons/movie.png");

                if (url == null) return null;

                iv = new ImageView(new Image(url.toExternalForm(), 50, 50, true, true, false));

                iv.setFitWidth(50);
                iv.setFitHeight(50);
                iv.setPreserveRatio(true);

                Label label = new Label(i.getFileName().toString());

                StackPane imagePane = new StackPane(iv);
                imagePane.setStyle("-fx-background-color: #9F9FA9;");
                imagePane.setPrefSize(60, 60);

                VBox box = new VBox(5, imagePane, label);
                box.setAlignment(Pos.CENTER);
                box.setPrefSize(80, 80);

                wrapper.getChildren().addAll(box, sizeLabel);
            }

            // ───── DEFAULT ─────
            else {
                var url = LargeFileDialog.class
                        .getResource("/file_ext_icon/docx.png");

                if (url != null) {
                    iv = new ImageView(new Image(url.toExternalForm(), 100, 100, true, true));
                    wrapper.getChildren().addAll(iv, sizeLabel);
                }
            }

            // ───── COMMON EVENTS ─────
            wrapper.setOnMouseEntered(e -> sizeLabel.setVisible(true));
            wrapper.setOnMouseExited(e -> sizeLabel.setVisible(false));

            wrapper.setOnMouseClicked(e -> {
                if (selectedFiles.contains(i)) {
                    selectedFiles.remove(i);
                    wrapper.setStyle("");
                } else {
                    selectedFiles.add(i);
                    wrapper.setStyle("""
                        -fx-effect: dropshadow(gaussian, #3b82f6, 20, 0.5, 0, 0);
                        """);
                }
            });

            return wrapper;

        } catch (Exception e) {
            System.out.println("❌ UI build failed: " + i);
            e.printStackTrace();
            return null;
        }
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
                    fileCheckBox.setId(filePath.getParent().toString());
//                    fileCheckBox.setDisable(isOriginal);
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

                    fileCheckBox.setOnAction(e -> {
                        if (!pathList.contains(filePathLabel.getText()) && fileCheckBox.isSelected()) {
                            pathList.add(filePathLabel.getText());
                        } else if (!fileCheckBox.isSelected()) {
                            pathList.remove(filePathLabel.getText());
                        }
                        System.out.println(pathList.size());
                    });
                }

                // ── Group checkbox → toggles all duplicate checkboxes ─────
                groupCheckBox.setOnAction(e -> {
                    boolean selected = groupCheckBox.isSelected();
                    fileCheckBoxes.forEach(cb -> {
                        if (!cb.isDisabled()) {
                            cb.setSelected(selected);
                            if (!pathList.contains(cb.getId()) && cb.isSelected()) {
                                pathList.add(cb.getId());
                            } else if (!cb.isSelected()) {
                                pathList.remove(cb.getId());
                            }
                            System.out.println(pathList.size());
                        }
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

    private static void loadFiles(TilePane tile, FileType type) {
//        System.out.println("TYPE = " + type);
//        System.out.println("MAP KEYS = " + categorizedFiles.keySet());

        tile.getChildren().clear();

        List<Path> files;
        if (mediaAvailable) {
            files = mediaFiles.get(type);
        } else {
            files = categorizedFiles.get(type);
        }
//        System.out.println("FILES = " + files);

        // ✅ Safety check
        if (files == null || files.isEmpty()) {
            System.out.println("⚠ No files for type: " + type);
            return;
        }

        Task<List<Node>> task = new Task<>() {
            @Override
            protected List<Node> call() {

                List<Node> nodes = new ArrayList<>();

                for (Path file : files) {

                    if (isCancelled()) break;

                    try {
                        Node node = createFileNode(file, type);
                        if (node != null) {
                            nodes.add(node);
                        }

                    } catch (Exception e) {
                        System.out.println("❌ Error processing: " + file);
                        e.printStackTrace();
                    }
                }

                return nodes;
            }
        };

        task.setOnSucceeded(e -> {
            tile.getChildren().setAll(task.getValue());
        });

        task.setOnFailed(e -> {
            System.out.println("❌ Task failed");
            task.getException().printStackTrace();
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private static void loadJunkFiles(StackPane root) {
        VBox junkCategoryBox = new VBox(10);
        junkCategoryBox.setPrefWidth(950);
        ScrollPane scrollPane = new ScrollPane(junkCategoryBox);
        scrollPane.setPrefHeight(300);
        scrollPane.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-background: transparent;" +
                        "-fx-border: none"
        );
        scrollPane.getStyleClass().add("duplicate-Card-Container-Scroll");

        Accordion accordion = new Accordion();
        junkCategoryBox.getChildren().add(accordion);

        for (File i : JunkFilesFinder.junkFiles.keySet()) {
            TitledPane tempPane = createCategory(
                    i.getName(), JunkFilesFinder.junkFiles.get(i).size(), JunkFilesFinder.junkFiles.get(i)
            );
            tempPane.setCollapsible(true);
            accordion.getPanes().add(tempPane);
        }

        root.getChildren().add(junkCategoryBox);
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

    private static void showImages(VBox root, TilePane tile, StackPane container, HBox button) {
        loadFiles(tile, FileType.IMAGE);

        VBox vbox = new VBox(10, tile);
        vbox.setPadding(new Insets(10));
        ScrollPane scroll = new ScrollPane(vbox);
        scroll.setFitToWidth(true);
        scroll.setStyle("""
                    -fx-background: transparent;
                    -fx-background-color: transparent;
                """);

        container.getChildren().setAll(scroll);

        nodeRef = root.getChildren().getLast();
        root.getChildren().removeLast();

        if (!root.getChildren().contains(button)) {
            root.getChildren().add(button);
        }
    }

    private static void showVideos(VBox root, TilePane tile, StackPane container, HBox button) {
        loadFiles(tile, FileType.VIDEO);

        VBox vbox = new VBox(10, tile);
        vbox.setPadding(new Insets(10));
        ScrollPane scroll = new ScrollPane(vbox);
        scroll.setFitToWidth(true);
        scroll.setStyle("""
                    -fx-background: transparent;
                    -fx-background-color: transparent;
                """);

//        loadDuplicateFiles(root, DuplicateFinder.filesByType);

        nodeRef = root.getChildren().getLast();
        root.getChildren().removeLast();

        container.getChildren().setAll(scroll);
        if (!root.getChildren().contains(button)) {
            root.getChildren().add(button);
        }
    }

    private static void showMusic(VBox root, TilePane tile, StackPane container, HBox button) {
        loadFiles(tile, FileType.MUSIC);

        VBox vbox = new VBox(10, tile);
        vbox.setPadding(new Insets(10));
        ScrollPane scroll = new ScrollPane(vbox);
        scroll.setFitToWidth(true);
        scroll.setStyle("""
                    -fx-background: transparent;
                    -fx-background-color: transparent;
                """);

//        loadDuplicateFiles(root, DuplicateFinder.filesByType);
        nodeRef = root.getChildren().getLast();
        root.getChildren().removeLast();

        container.getChildren().setAll(scroll);
        if (!root.getChildren().contains(button)) {
            root.getChildren().add(button);
        }
    }

    private static void showDocuments(VBox root, TilePane tile, StackPane container, HBox button) {
        loadFiles(tile, FileType.DOCUMENT);

        VBox vbox = new VBox(10, tile);
        vbox.setPadding(new Insets(10));
        ScrollPane scroll = new ScrollPane(vbox);
        scroll.setFitToWidth(true);
        scroll.setStyle("""
                    -fx-background: transparent;
                    -fx-background-color: transparent;
                """);

//        loadDuplicateFiles(root, DuplicateFinder.filesByType);
        nodeRef = root.getChildren().getLast();
        root.getChildren().removeLast();

        container.getChildren().setAll(scroll);
        if (!root.getChildren().contains(button)) {
            root.getChildren().add(button);
        }
    }

    private static void showDuplicate(StackPane container, TilePane categories, VBox root) {
//        container.getChildren().clear();

        VBox cardsContainer = new VBox(15);
        ScrollPane scroll = new ScrollPane(cardsContainer);
        scroll.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-background: transparent;"
        );
        scroll.getStyleClass().add("duplicate-Card-Container-Scroll");
        scroll.setFitToWidth(true);

        HBox bottomBtn = new HBox(10);
        bottomBtn.setAlignment(Pos.CENTER);

        Button backBtn = new Button("Back");
        Button deleteSelected = new Button("Delete Selected");
        Button deleteAll = new Button("Delete all duplicates");

        bottomBtn.getChildren().addAll(deleteSelected, deleteAll, backBtn);
        root.getChildren().add(bottomBtn);

        deleteSelected.setOnAction(e -> {
            System.out.println(pathList);
        });

        deleteAll.setOnAction(e -> {
            for (Node i : cardsContainer.getChildren()) {
                ((CheckBox)((HBox)((VBox)i).getChildren().getFirst()).getChildren().getFirst()).setSelected(true);
                ((CheckBox)((HBox)((VBox)(((VBox)i).getChildren().getLast())).getChildren().getLast()).getChildren().getFirst()).setSelected(true);
            }
        });

        backBtn.setOnAction(e -> {
            root.getChildren().remove(bottomBtn);
            container.getChildren().remove(scroll);
            container.getChildren().add(categories);
        });

        container.getChildren().clear();
        container.getChildren().add(scroll);

        loadDuplicateFiles(cardsContainer, DuplicateFinder.filesByType);

        if (mediaAvailable) mediaAvailable = false;
    }

    private static void showLarge(StackPane container, TilePane categories, TilePane categories2, VBox root) {
//        container.getChildren().clear();
        Button BackBtn = new Button("Back");
        root.getChildren().add(BackBtn);

        BackBtn.setOnAction(e -> {
            System.out.println("back button in large method");
            container.getChildren().remove(categories2);
            root.getChildren().remove(BackBtn);
            container.getChildren().add(categories);
        });

        container.getChildren().add(categories2);
        if (!categories2.getChildren().getLast().isVisible()) categories2.getChildren().getLast().setVisible(true);

        if (mediaAvailable) mediaAvailable = false;
    }

    private static void showJunk(StackPane container, TilePane categories, VBox root) {
        HBox bottomBtn = new HBox(10);
        bottomBtn.setAlignment(Pos.CENTER);

        container.getChildren().clear();
        loadJunkFiles(container);

        Button backBtn = new Button("Back");
        Button deleteSelected = new Button("Delete Selected");
        Button deleteAll = new Button("Delete all duplicates");

        backBtn.setOnAction(e -> {
            root.getChildren().remove(bottomBtn);
            container.getChildren().removeLast();
            container.getChildren().add(categories);
        });

        bottomBtn.getChildren().addAll(deleteSelected, deleteAll, backBtn);
        root.getChildren().add(bottomBtn);
    }

    private static void showMedia(StackPane container, TilePane categories, TilePane categories2, VBox root) {
//        container.getChildren().clear();
        Button BackBtn = new Button("Back");
        root.getChildren().add(BackBtn);

        BackBtn.setOnAction(e -> {
            container.getChildren().remove(categories2);
            container.getChildren().add(categories);
            root.getChildren().remove(BackBtn);
        });

        container.getChildren().removeLast();
        categories2.getChildren().getLast().setVisible(false);
        container.getChildren().add(categories2);

        mediaAvailable = true;
    }

    public static void show(Stage owner, VBox box) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Duplicate Files");

        dialog.setOnCloseRequest(e -> {
            System.out.println("box is closed");
            box.setOpacity(0);          // hidden initially
            box.setTranslateY(-30);     // start slightly above
        });

        TilePane categories = new TilePane();
        categories.setHgap(20);
        categories.setVgap(20);
        categories.setAlignment(Pos.CENTER);

        TilePane categories2 = new TilePane();
        categories2.setHgap(20);
        categories2.setVgap(20);
        categories2.setAlignment(Pos.CENTER);

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

        FontIcon duplicateFileIcon = new FontIcon("fas-copy");
        duplicateFileIcon.setIconSize(40);
        FontIcon largeFileIcon = new FontIcon("fas-file-alt");
        largeFileIcon.setIconSize(40);
        FontIcon junkFileIcon = new FontIcon("fas-trash-alt");
        junkFileIcon.setIconSize(40);
        FontIcon mediaFileIcon = new FontIcon("fas-photo-video");
        mediaFileIcon.setIconSize(40);

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
            System.out.println("clicked on the back button");
            if (currentTask != null && currentTask.isRunning()) {
                currentTask.cancel();   // 🔥 stop task
            }

            tile.getChildren().clear();

            // Reset main container properly
            mainContainer.getChildren().setAll(categories2);

            // Remove buttons safely
            root.getChildren().remove(buttons);

            root.getChildren().add(nodeRef);
        });

        categories.getChildren().addAll(
                createCategoryTile("Duplicate File", duplicateFileIcon, () -> showDuplicate(mainContainer, categories, root)),
                createCategoryTile("Large File", largeFileIcon, () -> showLarge(mainContainer, categories, categories2, root)),
                createCategoryTile("Junk File", junkFileIcon, () -> showJunk(mainContainer, categories, root)),
                createCategoryTile("Media File", mediaFileIcon, () -> showMedia(mainContainer, categories, categories2, root))
        );

        categories2.getChildren().addAll(
                createCategoryTile("Image", imageIcon, () -> showImages(root, tile, mainContainer, buttons)),
                createCategoryTile("Music", musicIcon, () -> showMusic(root, tile, mainContainer, buttons)),
                createCategoryTile("Videos", videoIcon, () -> showVideos(root, tile, mainContainer, buttons)),
                createCategoryTile("Document", docIcon, () -> showDocuments(root, tile, mainContainer, buttons))
        );

        Scene scene = new Scene(root, 1000, 700);

        // Apply dark theme
        scene.getStylesheets().add(DuplicateFileDialog.class.getResource("/css/dark-theme.css").toExternalForm());
        scene.getStylesheets().add(DuplicateFileDialog.class.getResource("/css/style.css").toExternalForm());

        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.setFullScreen(false);
        dialog.showAndWait();
    }
}