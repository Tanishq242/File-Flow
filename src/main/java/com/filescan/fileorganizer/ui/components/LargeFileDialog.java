package com.filescan.fileorganizer.ui.components;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import javafx.embed.swing.SwingFXUtils;
import com.filescan.fileorganizer.model.FileType;
import com.filescan.fileorganizer.service.FileService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.filescan.fileorganizer.service.FileService.categorizedFiles;

public class LargeFileDialog {
    private static Task<Void> currentTask;
    private static final Set<Path> selectedFiles = new HashSet<>();

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

    private static String formatSize(long bytes) {
        if (bytes >= 1024 * 1024 * 1024)
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        else if (bytes >= 1024 * 1024)
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        else if (bytes >= 1024)
            return String.format("%.2f KB", bytes / 1024.0);
        else
            return bytes + " B";
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

    private static void loadFiles(TilePane tile, FileType type) {
        tile.getChildren().clear();

        Task<Void> currentTask = new Task<>() {
            @Override
            protected Void call() throws IOException {
                List<Path> files = categorizedFiles.getOrDefault(type, List.of());
                System.out.println(type);
                System.out.println(files.size());
                for (Path i : files) {
                    if (isCancelled()) {
                        break; // or return null;
                    }
                    try {
                        Label sizeLabel = getSizeLabel(i);

                        ImageView iv = null;
                        StackPane wrapper = new StackPane();
                        wrapper.setPadding(new Insets(2));

                        if (type == FileType.IMAGE) {

                            iv = new ImageView(new Image(
                                    i.toUri().toString(),
                                    100, 100, true, true, true
                            ));

                            wrapper.setUserData(i);
                            wrapper.getChildren().addAll(iv, sizeLabel);
                            StackPane.setAlignment(sizeLabel, Pos.BOTTOM_CENTER);

                            wrapper.setOnMouseClicked(e -> {
                                System.out.println(wrapper.getChildren());
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

                        } else if (type == FileType.MUSIC) {

                            Image img = getMusicArtwork(i);

                            if (img != null) {
                                iv = new ImageView(img);
                            } else {
                                String url = LargeFileDialog.class
                                        .getResource("/icons/record.png")
                                        .toExternalForm();

                                iv = new ImageView(new Image(url, 50, 50, true, true, false)); // 🔥 fixed
                            }

                            // ✅ common settings
                            iv.setFitWidth(50);
                            iv.setFitHeight(50);
                            iv.setPreserveRatio(true);
                            iv.setSmooth(false);

                            // ✅ consistent UI
                            Label songLabel = new Label(i.getFileName().toString());

                            StackPane imagePane = new StackPane(iv);
                            imagePane.setStyle("-fx-background-color: #9F9FA9;" +
                                    "-fx-border-radius: 15;" +
                                    "-fx-background-radius: 15;");
                            imagePane.setPrefSize(60, 60);

                            VBox box = new VBox(5, imagePane, songLabel);
                            box.setAlignment(Pos.CENTER);
                            box.setPrefSize(80, 80);

                            wrapper.getChildren().add(box);
                            wrapper.getChildren().add(sizeLabel);

                            wrapper.setOnMouseClicked(e -> {
                                System.out.println(wrapper.getChildren());
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
                        } else if (type == FileType.DOCUMENT) {

                            if (i.toString().endsWith(".pdf")) {
                                iv = createPdfThumbnail(i);
                            } else {
                                iv = new ImageView(new Image(
                                        "D:\\fileOrganizer\\src\\main\\resources\\file_ext_icon\\txt.png",
                                        100, 100, true, true
                                ));
                            }

                            wrapper.getChildren().addAll(iv, sizeLabel);
                            wrapper.setPrefSize(110, 110);
                            wrapper.setPadding(new Insets(5));
                        } else if (type == FileType.VIDEO) {
                            String url = LargeFileDialog.class
                                    .getResource("/icons/movie.png")
                                    .toExternalForm();

                            iv = new ImageView(new Image(url, 50, 50, true, true, false));

                            iv.setFitWidth(50);
                            iv.setFitHeight(50);
                            iv.setPreserveRatio(true);
                            iv.setSmooth(false);

                            // ✅ consistent UI
                            Label songLabel = new Label(i.getFileName().toString());

                            StackPane imagePane = new StackPane(iv);
                            imagePane.setStyle("-fx-background-color: #9F9FA9;");
                            imagePane.setPrefSize(60, 60);

                            VBox box = new VBox(5, imagePane, songLabel);
                            box.setAlignment(Pos.CENTER);
                            box.setPrefSize(80, 80);

                            wrapper.getChildren().add(box);
                            wrapper.getChildren().add(sizeLabel);

                            wrapper.setOnMouseClicked(e -> {
                                System.out.println(wrapper.getChildren());
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
                        } else {

                            iv = new ImageView(new Image(
                                    "D:\\fileOrganizer\\src\\main\\resources\\file_ext_icon\\docx.png",
                                    100, 100, true, true
                            ));

                            wrapper.getChildren().addAll(iv, sizeLabel);
                            wrapper.setPrefSize(110, 110);
                            wrapper.setPadding(new Insets(5));
                        }

                        wrapper.setOnMouseEntered(e -> sizeLabel.setVisible(true));
                        wrapper.setOnMouseExited(e -> sizeLabel.setVisible(false));

                        Platform.runLater(() -> tile.getChildren().add(wrapper));

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        };

        Thread t = new Thread(currentTask);
        t.setDaemon(true);
        t.start();

    }

    private static void showImages(TilePane tile, StackPane container, VBox root, HBox button) {
        loadFiles(tile, FileType.IMAGE);

        ScrollPane scroll = new ScrollPane(tile);
        scroll.setFitToWidth(true);
        scroll.setStyle("""
                    -fx-background: transparent;
                    -fx-background-color: transparent;
                """);

        container.getChildren().setAll(scroll);
        if (!root.getChildren().contains(button)) {
            root.getChildren().add(button);
        }
    }

    private static void showVideos(TilePane tile, StackPane container, VBox root, HBox button) {
        loadFiles(tile, FileType.VIDEO);

        ScrollPane scroll = new ScrollPane(tile);
        scroll.setFitToWidth(true);
        scroll.setStyle("""
                    -fx-background: transparent;
                    -fx-background-color: transparent;
                """);

        container.getChildren().setAll(scroll);
        if (!root.getChildren().contains(button)) {
            root.getChildren().add(button);
        }
    }

    private static void showMusic(TilePane tile, StackPane container, VBox root, HBox button) {
        loadFiles(tile, FileType.MUSIC);

        ScrollPane scroll = new ScrollPane(tile);
        scroll.setFitToWidth(true);
        scroll.setStyle("""
                    -fx-background: transparent;
                    -fx-background-color: transparent;
                """);

        container.getChildren().setAll(scroll);
        if (!root.getChildren().contains(button)) {
            root.getChildren().add(button);
        }
    }

    private static void showDocuments(TilePane tile, StackPane container, VBox root, HBox button) {
        loadFiles(tile, FileType.DOCUMENT);

        ScrollPane scroll = new ScrollPane(tile);
        scroll.setFitToWidth(true);
        scroll.setStyle("""
                    -fx-background: transparent;
                    -fx-background-color: transparent;
                """);

        container.getChildren().setAll(scroll);
        if (!root.getChildren().contains(button)) {
            root.getChildren().add(button);
        }
    }

    public static void show(Stage owner) {

        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Large Files");

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

        deleteBtn.setOnAction(e -> {
            if (showAlert() != 1) {
                for (Path path : selectedFiles) {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                // remove from UI
                tile.getChildren().removeIf(node -> {
                    if (node instanceof StackPane sp) {
                        return selectedFiles.contains((Path) sp.getUserData());
                    }
                    return false;
                });

                selectedFiles.clear();
            }
        });


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
                createCategoryTile("Image", imageIcon, () -> showImages(tile, mainContainer, root, buttons)),
                createCategoryTile("Music", musicIcon, () -> showMusic(tile, mainContainer, root, buttons)),
                createCategoryTile("Videos", videoIcon, () -> showVideos(tile, mainContainer, root, buttons)),
                createCategoryTile("Document", docIcon, () -> showDocuments(tile, mainContainer, root, buttons))
        );

        Scene scene = new Scene(root, 1000, 700);

        // Apply dark theme
        scene.getStylesheets().add(LargeFileDialog.class.getResource("/css/dark-theme.css").toExternalForm());

        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
