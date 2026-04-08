package com.filescan.fileorganizer;

import com.filescan.fileorganizer.controller.*;
import com.filescan.fileorganizer.model.DuplicateFile;
import com.filescan.fileorganizer.model.FileData;
import com.filescan.fileorganizer.model.FileType;
import com.filescan.fileorganizer.model.JunkCategory;
import com.filescan.fileorganizer.service.*;
import com.filescan.fileorganizer.ui.components.*;
import com.sun.tools.javac.Main;
import com.filescan.fileorganizer.ui.components.CodeFileUI;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.filescan.fileorganizer.ui.components.CodeFileUI.buildCodeFinderUI;
import static com.filescan.fileorganizer.ui.components.OverlayCircle.transparentCircle;
import static com.filescan.fileorganizer.ui.components.dropDown.createCategory;

public class MainApp extends Application {
    private int WINDOW_WIDTH = 1366;
    private int WINDOW_HEIGHT = 768;
    static Timeline[] holder = new Timeline[1];
    private static int step = 0;


    public static void main(String[] args) {
        launch(args);
    }

    public static void countUpAnimation(Label label, long max, Runnable onDone) {

        final long[] current = {0};

        final double MIN_DELAY = 0.01;   // seconds — fast at start
        final double MAX_DELAY = 0.05;   // seconds — slow near end

        Runnable scheduleNext = new Runnable() {
            @Override
            public void run() {

                // Update the label
                label.setText(String.valueOf(current[0]));

                // Stop once we've reached max
                if (current[0] >= max) {
                    if (onDone != null) onDone.run();
                    return;
                }

                current[0]++;

                // progress: 0.0 (start) → 1.0 (end)
                double progress = (double) current[0] / max;

                // Quadratic ease-out: delay grows as progress increases
                double delay = MIN_DELAY + (MAX_DELAY - MIN_DELAY) * (progress * progress);

                // Schedule the next tick with the computed delay
                holder[0] = new Timeline(
                        new KeyFrame(Duration.seconds(delay), e -> this.run())
                );
                holder[0].play();
            }
        };

        // Kick off immediately
        scheduleNext.run();
    }

    public static void scanningBoxAnimation(Node node, int time) {
        FadeTransition fade = new FadeTransition(Duration.millis(time), node);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(time), node);
        slide.setFromY(-30);
        slide.setToY(0);

        ParallelTransition animation = new ParallelTransition(fade, slide);
        animation.play();
    }
//    public static void countDownAnimation(Label label, long max) {
//
//        final int[] tempNum = {0}; // local variable (safe)
//
//        Timeline timeline = new Timeline();
//
//        Timeline timeline1 = new Timeline();
//
//        KeyFrame frame = new KeyFrame(Duration.seconds(0.01), e -> {
//            if (tempNum[0] > max) {
//                timeline.stop(); // ✅ stop properly
//                System.out.println("Timeline Stopped");
//                return;
//            }
//
//            label.setText(String.valueOf(tempNum[0]++));
//        });
//
//        timeline.getKeyFrames().add(frame);
//        timeline.setCycleCount(Animation.INDEFINITE);
//        timeline.play();
//    }

    public static Parent largeFileDashBoard() {
        Label firstText = new Label("Large Files");
        firstText.getStyleClass().add("topTextLabel");

        VBox choiceBox = new VBox();
        Label driveListHeadLabel = new Label("Select the drive");
        driveListHeadLabel.getStyleClass().add("driveHeadLabel");
        ChoiceBox<String> driveList = new ChoiceBox<>();
        driveList.getStyleClass().add("driveListChoiceBox");
        ObservableList<String> list = driveList.getItems();

        for (File i : File.listRoots()) {
            if (i.toString().contains("C")) continue;
            list.add("Local Disk " + i.toString().replace("\\", "").trim());
        }

        ProgressIndicator pi = new ProgressIndicator();
        pi.setMaxSize(30, 30);
        pi.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        Label scanLabel = new Label("Scanning...");
        scanLabel.getStyleClass().add("summaryLabel");

        HBox hb1 = new HBox(10, pi, scanLabel);
        hb1.setAlignment(Pos.CENTER);
        HBox hb2 = new HBox(30, driveList);

        choiceBox.getChildren().addAll(driveListHeadLabel, hb2);

        TableView<FileData> table = new TableView<>();
        table.setEditable(true);

        TableColumn<FileData, Boolean> selectCol = new TableColumn<>("Select");
        selectCol.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
//        selectCol.setEditable(true);

        TableColumn<FileData, String> nameCol = new TableColumn<>("File Name");
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());

        TableColumn<FileData, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> data.getValue().typeProperty());

        TableColumn<FileData, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(data -> data.getValue().sizeProperty());

        TableColumn<FileData, String> pathCol = new TableColumn<>("Location");
        pathCol.setCellValueFactory(data -> data.getValue().pathProperty());

        table.getColumns().addAll(selectCol, nameCol, typeCol, sizeCol, pathCol);

//        table.getItems().addAll(
//                new FileData("movie.mkv", "Video", "3.4 GB", "D:/Movies"),
//                new FileData("backup.zip", "Archive", "1.8 GB", "C:/Downloads"),
//                new FileData("dataset.csv", "Data", "950 MB", "C:/Projects")
//        );

        table.getStyleClass().add("largeFilesTable");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        RadioButton showAll = new RadioButton("Show All");
        RadioButton images = new RadioButton("Images");
        images.setUserData("IMAGES");
        RadioButton musics = new RadioButton("Musics");
        musics.setUserData("MUSICS");
        RadioButton videos = new RadioButton("Videos");
        videos.setUserData("VIDEOS");
        RadioButton documents = new RadioButton("Documents");
        documents.setUserData("DOCUMENTS");
        RadioButton archives = new RadioButton("Archives");
        archives.setUserData("ARCHIVES");

        ToggleGroup fileTypeGroup = new ToggleGroup();

        showAll.setToggleGroup(fileTypeGroup);
        images.setToggleGroup(fileTypeGroup);
        musics.setToggleGroup(fileTypeGroup);
        videos.setToggleGroup(fileTypeGroup);
        documents.setToggleGroup(fileTypeGroup);
        archives.setToggleGroup(fileTypeGroup);

        fileTypeGroup.getToggles().getFirst().setSelected(true);

        fileTypeGroup.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> {
            if (!table.getItems().isEmpty()) {
                System.out.println("only images");
                LargeScanController.showOnlyImages(table);
            } else {
                fileTypeGroup.getToggles().getFirst().setSelected(true);
                LargeScanController.showAllFiles(table);
            }
        });

        HBox fileTypes = new HBox(10);
        fileTypes.setAlignment(Pos.CENTER);
        fileTypes.getChildren().addAll(showAll, images, musics, videos, documents, archives);

        Label totalFilesLabel = new Label("Total Files: 0");
        Label totalSizeLabel = new Label("Total Size: 0");
        totalFilesLabel.getStyleClass().add("summaryLabel");
        totalSizeLabel.getStyleClass().add("summaryLabel");

        HBox summaryBox = new HBox(30);
        summaryBox.getChildren().addAll(totalFilesLabel, totalSizeLabel);

        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        Button startButton = new Button("Start Button");
        startButton.getStyleClass().add("themeButton");
        Button compressFile = new Button("Compress File");
        compressFile.getStyleClass().add("themeButton");
        Button deleteFile = new Button("Delete Selected Files");
        deleteFile.getStyleClass().add("themeButton");
        Button deleteAllFile = new Button("Delete All Files");
        deleteAllFile.getStyleClass().add("themeButton");
        deleteFile.setDisable(true);

        startButton.setOnAction(e -> {
            startButton.setDisable(true);
            String selected = driveList.getValue().trim();
            String driveLetter = selected.split(" ")[2];

            hb2.getChildren().add(hb1);
            int value = LargeScanController.largeFileScan(driveLetter, table);
            if (value == 0) {
                pi.setProgress(1.0);
                scanLabel.setText("Scanning Done");
                totalFilesLabel.setText("Total Files: " + FileService.largeFilesList.size());
                startButton.setDisable(false);
            }
        });

        buttonBox.getChildren().add(startButton);
//        buttonBox.getChildren().addAll(compressFile, deleteFile, deleteAllFile);

        Label infoLabel = new Label("Tip: Double-click a file name to open it.");
        infoLabel.setAlignment(Pos.CENTER);
        infoLabel.setMaxWidth(Double.MAX_VALUE);
        infoLabel.getStyleClass().add("infoLabel");

        return new VBox(10, firstText, choiceBox, table, fileTypes, summaryBox, buttonBox, infoLabel);
    }

    public static Parent duplicateFileDashBoard() {
        Label firstText = new Label("Duplicate Files");
        firstText.getStyleClass().add("topTextLabel");

        VBox choiceBox = new VBox();
        Label driveListHeadLabel = new Label("Select the drive");
        driveListHeadLabel.getStyleClass().add("driveHeadLabel");
        ChoiceBox<String> driveList = new ChoiceBox<>();
        driveList.getStyleClass().add("driveListChoiceBox");
        ObservableList<String> list = driveList.getItems();
        for (File i : File.listRoots()) {
            if (i.toString().contains("C")) continue;
            list.add("Local Disk " + i.toString().replace("\\", "").trim());
        }
        ProgressIndicator pi = new ProgressIndicator();
        pi.setMaxSize(30, 30);
        pi.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        Label scanLabel = new Label("Scanning...");
        scanLabel.getStyleClass().add("summaryLabel");

        HBox hb1 = new HBox(10, pi, scanLabel);
        hb1.setAlignment(Pos.CENTER);
        HBox hb2 = new HBox(30, driveList);

        choiceBox.getChildren().addAll(driveListHeadLabel, hb2);

        VBox cardsContainer = new VBox(15);
        ScrollPane scroll = new ScrollPane(cardsContainer);
        scroll.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-background: transparent;"
        );
        scroll.getStyleClass().add("duplicate-Card-Container-Scroll");
        scroll.setFitToWidth(true);
//        for (int i = 0; i < 10; i++) {
//            VBox card = new VBox(10);
//            card.getStyleClass().add("duplicateCard");
//
//            Label title = new Label("Duplicate File: photo.png");
//            Label size = new Label("Size: 4.2 MB");
//
//            CheckBox file1 = new CheckBox("C:/Pictures/photo.png");
//            CheckBox file2 = new CheckBox("D:/Backup/photo.png");
//            CheckBox file3 = new CheckBox("D:/Backup/photo.png");
//            CheckBox file4 = new CheckBox("D:/Backup/photo.png");
//
//            Button deleteBtn = new Button("Delete Selected");
//
//            card.getChildren().addAll(title, size, file1, file2, file3, file4, deleteBtn);
//            cardsContainer.getChildren().addAll(card);
//        }

        Button startButton = new Button("Start Button");
        startButton.getStyleClass().add("themeButton");
        BorderPane btnCenter = new BorderPane();
        btnCenter.setCenter(startButton);

        startButton.setOnAction(e -> {
            if (hb2.getChildren().contains(hb1)) {
                hb2.getChildren().remove(hb1);
                pi.setProgress(-1);
            }

            String selected = driveList.getValue().trim();
            String driveLetter = selected.split(" ")[2];

            startButton.setDisable(true);

            // Show loader immediately
            hb2.getChildren().add(hb1);
            scanLabel.setText("Scanning...");

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    DuplicateScanController.startScan(driveLetter, cardsContainer);
                    return null;
                }
            };

            task.setOnSucceeded(ev -> {
                pi.setProgress(1.0);
                scanLabel.setText("Scanning Completed");
                startButton.setDisable(false);
            });

            task.setOnFailed(ev -> {
                scanLabel.setText("Error occurred");
                startButton.setDisable(false);
                task.getException().printStackTrace();
            });

            new Thread(task).start();
        });

        return new VBox(10, firstText, choiceBox, scroll, btnCenter);
    }

    public static Parent junkFileDashBoard() {
        String[] junkCategory = {"Temporary Files", "Crash Dumps", "Windows Logs", "Browser Cache", "Recycle Bin"};

        Label firstText = new Label("Junk Files");
        firstText.getStyleClass().add("topTextLabel");

        HBox junkStatBox = new HBox(20);
        junkStatBox.setPrefHeight(150);
        junkStatBox.setAlignment(Pos.CENTER);

        Label h1 = new Label("Total Junk");
        h1.getStyleClass().add("junkStatHead");
        Label s1 = new Label("3.4 GB");
        s1.getStyleClass().add("junkStatResult");
        VBox v1 = new VBox(10, h1, s1);
        v1.getStyleClass().add("vboxStatBox");

        Label h2 = new Label("Total Files");
        h2.getStyleClass().add("junkStatHead");
        Label s2 = new Label("0");
        s2.getStyleClass().add("junkStatResult");
        VBox v2 = new VBox(10, h2, s2);
        v2.getStyleClass().add("vboxStatBox");

        Label h3 = new Label("Safe to Clean");
        h3.getStyleClass().add("junkStatHead");
        Label s3 = new Label("0");
        s3.getStyleClass().add("junkStatResult");
        VBox v3 = new VBox(10, h3, s3);
        v3.getStyleClass().add("vboxStatBox");

        junkStatBox.getChildren().addAll(v1, v2, v3);

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

        BorderPane buttonBox = new BorderPane();
        Button startScanButton = new Button("Scan Junk Files");
        startScanButton.getStyleClass().add("junkScanButton");
        buttonBox.setCenter(startScanButton);

        Button deleteFileButton = new Button("Delete Files");
        deleteFileButton.getStyleClass().add("deleteFileButton");
        BorderPane deleteFileButtonBox = new BorderPane();
        deleteFileButtonBox.setCenter(deleteFileButton);

        VBox root = new VBox(30, firstText, junkStatBox, buttonBox, scrollPane);

        startScanButton.setOnAction(e -> {
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    startScanButton.setDisable(true);
                    JunkFilesFinder.startScan();
                    return null;
                }
            };

            task.setOnSucceeded(ex -> {
                s2.setText(String.valueOf(JunkFilesFinder.totalFiles));
                s3.setText(JunkFilesFinder.junkFileSize);
                startScanButton.setDisable(false);
                JunkFilesFinder.totalFiles = 0;
                JunkFilesFinder.totalSize = 0;

                for (File i : JunkFilesFinder.junkFiles.keySet()) {
                    TitledPane tempPane = createCategory(
                            i.getName(), JunkFilesFinder.junkFiles.get(i).size(), JunkFilesFinder.junkFiles.get(i)
                    );
                    tempPane.setCollapsible(true);
                    junkCategoryBox.getChildren().add(tempPane);
                }

                root.getChildren().add(deleteFileButtonBox);

            });

            Thread thread = new Thread(task);
            thread.start();
        });

//        for (int i = 0; i < junkCategory.length; i++) {
//            HBox junkCategory1 = new HBox();
//            CheckBox c1 = new CheckBox(junkCategory[i]);
//            Label sizeLabel = new Label("1.2 GB");
//            Label fileCountLabel = new Label("523");
//            junkCategory1.getChildren().addAll(c1, new HBox(sizeLabel, fileCountLabel));
//            junkCategoryBox.getChildren().add(junkCategory1);
//            TitledPane tempPane = createCategory(
//                    junkCategory[i], "1.2 GB", 523
//            );
//            tempPane.setCollapsible(true);
//            junkCategoryBox.getChildren().add(tempPane);
//        }

        return root;
    }

    public static Parent fileTransferDashBoard() {
        Label firstText = new Label("File Transfer");
        firstText.getStyleClass().add("topTextLabel");


        VBox sourceBox = new VBox(10);
        sourceBox.setStyle("-fx-background-color: #31313B;" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 10;");
        sourceBox.setFillWidth(false);
//        sourceBox.setPrefSize(450, 500);

        ChoiceBox<String> driveList1 = new ChoiceBox<>();
        driveList1.setValue("Select the drive");
        driveList1.getStyleClass().add("driveListChoiceBox");
        ObservableList<String> list = driveList1.getItems();
        list.addAll("Local Disk C:", "Local Disk D:", "Local Disk E:", "Local Disk F:");

        String[] junkCategory = {"Document", "Music", "Pictures", "Videos"};

        sourceBox.getChildren().add(driveList1);
//        for (int i = 0; i < junkCategory.length; i++) {
//            TitledPane tempPane = createCategory(
//                    junkCategory[i], "1.2 GB", 523,
//                    "temp1.tmp", "temp2.tmp", "temp3.tmp", "temp1.tmp", "temp2.tmp", "temp3.tmp", "temp1.tmp", "temp2.tmp", "temp3.tmp"
//            );
//            sourceBox.getChildren().add(tempPane);
//        }

        VBox destinationBox = new VBox(10);
        destinationBox.setFillWidth(false);
        destinationBox.setStyle("-fx-background-color: #31313B;" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 10;");
//        destinationBox.setPrefSize(450, 500);

        ChoiceBox<String> driveList2 = new ChoiceBox<>();
        driveList2.setValue("Select the drive");
        driveList2.getStyleClass().add("driveListChoiceBox");
        ObservableList<String> list2 = driveList2.getItems();
        list2.addAll("Select the drive", "Local Disk C:", "Local Disk D:", "Local Disk E:", "Local Disk F:");

        destinationBox.getChildren().add(driveList2);
//        for (int i = 0; i < junkCategory.length; i++) {
//            TitledPane tempPane = createCategory(
//                    junkCategory[i], "1.2 GB", 523,
//                    "temp1.tmp", "temp2.tmp", "temp3.tmp", "temp1.tmp", "temp2.tmp", "temp3.tmp", "temp1.tmp", "temp2.tmp", "temp3.tmp"
//            );
//            destinationBox.getChildren().add(tempPane);
//        }

        StackPane stackPane1 = transparentCircle(150, 80, 300, "#ff6b6b26", sourceBox);
        StackPane stackPane2 = transparentCircle(150, 80, 300, "#4d96ff26", destinationBox);

        HBox hBox = new HBox(50, stackPane1, stackPane2);
        hBox.setPrefHeight(500);
        hBox.setAlignment(Pos.CENTER);

        Button moveFileButton = new Button("Move Files");
        moveFileButton.getStyleClass().add("moveFileButton");
        BorderPane moveFileButtonBox = new BorderPane();
        moveFileButtonBox.setCenter(moveFileButton);

        return new VBox(30, firstText, hBox, moveFileButtonBox);
    }

    public static Parent scanSystemDashBoard(Stage stage) {
        Label firstText = new Label("Scan Center");
        firstText.getStyleClass().add("topTextLabel");

        Label scanFilterLabel = new Label("Scan Filter");
        scanFilterLabel.getStyleClass().add("scanFilter");

        CheckBox cb1 = new CheckBox("Duplicate Files");
        cb1.setId("DUPLICATE");
        CheckBox cb2 = new CheckBox("Large Files");
        cb2.setId("LARGE");
        CheckBox cb3 = new CheckBox("Junk Files");
        cb3.setId("JUNK");
        CheckBox cb4 = new CheckBox("Media Files");
        cb4.setId("MEDIA");
        CheckBox cb6 = new CheckBox("Select All");

        CheckBox organizeCheckBox = new CheckBox("Do you want to organize files");

        List<CheckBox> filters = List.of(cb1, cb2, cb3, cb4);

        cb6.setOnAction(e -> {
            boolean selected = cb6.isSelected();
            filters.forEach(cb -> cb.setSelected(selected));
        });

        cb1.getStyleClass().add("filter-box");
        cb2.getStyleClass().add("filter-box");
        cb3.getStyleClass().add("filter-box");
        cb4.getStyleClass().add("filter-box");
        cb6.getStyleClass().add("filter-box");
        organizeCheckBox.getStyleClass().add("filter-box");

        Label specificPathLabel = new Label("Select Specific Folder or Drive");
        specificPathLabel.getStyleClass().add("specificLabel");

        Label pathLabel = new Label();
        pathLabel.setStyle("-fx-text-fill: #94a3b8;\n" +
                "    -fx-font-size: 13px;");

        HBox pathBox = new HBox(10, specificPathLabel, pathLabel);
        pathBox.setAlignment(Pos.CENTER_LEFT);

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Drive or Folder");
        specificPathLabel.setOnMouseClicked(e -> {
            File file = directoryChooser.showDialog(stage);
            if (file != null) {
                pathLabel.setText(file.getAbsolutePath());
            }
        });

        Button scanButton = new Button("Apply and Scan");
        scanButton.getStyleClass().add("scanningButton");

        BorderPane scanButtonBox = new BorderPane();
        scanButtonBox.setCenter(scanButton);

        VBox scanFilterBox = new VBox(10, scanFilterLabel, new HBox(20, cb1, cb2, cb3, cb4, cb6), organizeCheckBox, pathBox, scanButtonBox);
//        scanFilterBox.setPrefSize(500, 250);
        scanFilterBox.setStyle("-fx-background-color: #1e1e28;\n" +
                "    -fx-background-radius: 15;\n" +
                "    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 5);" +
                "    -fx-padding: 20;");
        scanFilterBox.setTranslateY(-250);


        StackPane scanComponent = CircularScan.circularBuffer();
        Label currentScanLabel = new Label("Starting...");
        currentScanLabel.setStyle("-fx-text-fill: #94a3b8;\n" +
                "    -fx-font-size: 13px;");
        BorderPane currentScanBox = new BorderPane();
        currentScanBox.setCenter(currentScanLabel);
        VBox scanInfoBox = new VBox(15, scanComponent, currentScanBox);
        scanInfoBox.setStyle("-fx-background-color: #1e1e28;\n" +
                "    -fx-background-radius: 15;\n" +
                "    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 5);" +
                "    -fx-padding: 20;");

        scanInfoBox.setOpacity(0);          // hidden initially
        scanInfoBox.setTranslateY(-30);     // start slightly above

//        scanButton.setOnAction(e -> {
//            DuplicateDialog.show(stage);
//        });

        scanButton.setOnAction(e -> {
            scanningBoxAnimation(scanInfoBox, 400);
            TranslateTransition slide2 = new TranslateTransition(Duration.millis(350), scanFilterBox);
            slide2.setFromY(-250);
            slide2.setToY(0);
            slide2.play();

            File[] file = File.listRoots();
            StringBuilder str = new StringBuilder();
            for (File i : file) {
                str.append(i.getAbsolutePath()).append(" ");
            }

            PauseTransition delay = new PauseTransition(Duration.seconds(2));
            delay.setOnFinished(event -> {

                currentScanLabel.setText("Scanning " + str + " drives");

                int totalTasks = 0;

                if (cb1.isSelected()) totalTasks++;
                if (cb3.isSelected()) totalTasks++;
                if (cb2.isSelected() || cb4.isSelected()) totalTasks++;

                final int finalTotalTasks = totalTasks;

                // If nothing selected, do nothing
                if (finalTotalTasks == 0) return;

                AtomicInteger completedTasks = new AtomicInteger(0);

                Runnable onTaskFinished = () -> {
                    if (completedTasks.incrementAndGet() == finalTotalTasks) {
                        Platform.runLater(() -> scanResultDialogBox.show(stage));
                    }
                };

                // 🔹 Task 1: Duplicate scan
                if (cb1.isSelected()) {
                    Task<Void> task1 = new Task<>() {
                        @Override
                        protected Void call() throws IOException {
                            for (File i : file) {
                                if (i.getAbsolutePath().contains("C") || i.getAbsolutePath().contains("F"))
                                    continue;

                                System.out.println(i.getAbsolutePath());
                                DuplicateFinder.showDuplicate(i.getAbsolutePath());
                            }
                            return null;
                        }
                    };

                    task1.setOnSucceeded(e1 -> onTaskFinished.run());
                    task1.setOnFailed(e2 -> onTaskFinished.run());

                    new Thread(task1).start();
                }

                // 🔹 Task 2: Junk scan
                if (cb3.isSelected()) {
                    Task<Void> task2 = new Task<>() {
                        @Override
                        protected Void call() {
                            JunkFilesFinder.startScan();
                            return null;
                        }
                    };

                    task2.setOnSucceeded(e3 -> onTaskFinished.run());
                    task2.setOnFailed(e4 -> onTaskFinished.run());

                    new Thread(task2).start();
                }

                // 🔹 Task 3: File scan
                if (cb2.isSelected() || cb4.isSelected()) {
                    Task<Void> task3 = new Task<>() {
                        @Override
                        protected Void call() {
                            FileService.scanPath(cb2.isSelected(), cb4.isSelected());
                            return null;
                        }
                    };

                    task3.setOnSucceeded(e5 -> onTaskFinished.run());
                    task3.setOnFailed(e6 -> onTaskFinished.run());

                    new Thread(task3).start();
                }

            });

            delay.play();
        });

        HBox statBox = new HBox(15);

        String[] category = {"Documents", "Musics", "Videos", "Images", "Programming"};
        String[] imgIcon = {"folder.png", "ring.png", "video.png", "picture.png", "programming.png"};
        String[] hexColor = {"#ff6b6b26", "#4d96ff26", "#6bcb7726", "#c77dff26", "#ffd93d26"};
        for (int i = 0; i < 5; i++) {
            ImageView docIcon = new ImageView(
                    new Image(MainApp.class.getResourceAsStream("/icons/" + imgIcon[i]))
            );

            docIcon.setSmooth(true);
            docIcon.setPreserveRatio(true);

            StackPane imgBackground = new StackPane(docIcon);
            imgBackground.setAlignment(Pos.CENTER);
            imgBackground.setPrefSize(50, 60);
            imgBackground.setPadding(new Insets(15));

            imgBackground.setStyle(
                    "-fx-background-color: " + hexColor[i] + ";" +
                            "-fx-background-radius: 15;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 5);"
            );


            Label fileCountLabel = new Label();
            fileCountLabel.getStyleClass().add("fileCountLabel");

            Label totalLabel = new Label("Total " + category[i]);
            totalLabel.getStyleClass().add("totalLabel");
            VBox v1 = new VBox(5, imgBackground, fileCountLabel, totalLabel);
            v1.setPadding(new Insets(0, 0, 0, 10));
            v1.setAlignment(Pos.BASELINE_LEFT);
            v1.setFillWidth(false);

            Circle circle = new Circle(60);
            circle.setFill(new RadialGradient(
                    0, 0,
                    0.5, 0.5,
                    1,
                    true,
                    CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web(hexColor[i])),
                    new Stop(1, Color.TRANSPARENT)
            ));
            circle.setTranslateX(45);
            circle.setTranslateY(-45);

            StackPane sepBox = new StackPane(v1, circle);
//            sepBox.setPrefSize(200, 200);
            StackPane stackPane = new StackPane(sepBox);
            StackPane.setAlignment(circle, Pos.TOP_RIGHT);
            stackPane.setStyle("""
                        -fx-background-color: #1e1e28;
                        -fx-background-radius: 15;
                        -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 5);
                    """);
            stackPane.setPrefSize(200, 150);

            Rectangle clip = new Rectangle();
            clip.setArcWidth(30);   // 2 × background radius
            clip.setArcHeight(30);

            stackPane.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
                clip.setWidth(newBounds.getWidth());
                clip.setHeight(newBounds.getHeight());
            });

            stackPane.setClip(clip);
            statBox.getChildren().add(stackPane);
        }

        return new VBox(20, firstText, scanInfoBox, scanFilterBox, statBox);
    }

    public static Parent mainDashBoard() {
        VBox content = new VBox();

        Label firstText = new Label("DashBoard ");
        Label secondText = new Label("/ Overview");
        firstText.getStyleClass().add("topTextLabel");
        secondText.getStyleClass().add("topSecondTextLabel");
        HBox topTextBox = new HBox(firstText, secondText);

        HBox statBox = new HBox(15);

        String[] category = {"Documents", "Musics", "Videos", "Images", "Programming"};
        String[] imgIcon = {"folder.png", "ring.png", "video.png", "picture.png", "programming.png"};
        String[] hexColor = {"#ff6b6b26", "#4d96ff26", "#6bcb7726", "#c77dff26", "#ffd93d26"};
        for (int i = 0; i < 5; i++) {
            ImageView docIcon = new ImageView(
                    new Image(MainApp.class.getResourceAsStream("/icons/" + imgIcon[i]))
            );

            docIcon.setSmooth(true);
            docIcon.setPreserveRatio(true);

            StackPane imgBackground = new StackPane(docIcon);
            imgBackground.setAlignment(Pos.CENTER);
            imgBackground.setPrefSize(50, 60);
            imgBackground.setPadding(new Insets(15));

            imgBackground.setStyle(
                    "-fx-background-color: " + hexColor[i] + ";" +
                            "-fx-background-radius: 15;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 5);"
            );


            Label fileCountLabel = new Label();
            fileCountLabel.getStyleClass().add("fileCountLabel");

            countUpAnimation(fileCountLabel, 10, () -> {
            });

            Label totalLabel = new Label("Total " + category[i]);
            totalLabel.getStyleClass().add("totalLabel");
            Label percentageLabel = new Label("↑ 12% this week");
            percentageLabel.getStyleClass().add("percentageLabel");
            VBox v1 = new VBox(5, imgBackground, fileCountLabel, totalLabel, percentageLabel);
            v1.setPadding(new Insets(0, 0, 0, 10));
            v1.setAlignment(Pos.BASELINE_LEFT);
            v1.setFillWidth(false);

            Circle circle = new Circle(60);
            circle.setFill(new RadialGradient(
                    0, 0,
                    0.5, 0.5,
                    1,
                    true,
                    CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web(hexColor[i])),
                    new Stop(1, Color.TRANSPARENT)
            ));
            circle.setTranslateX(45);
            circle.setTranslateY(-45);

            StackPane sepBox = new StackPane(v1, circle);
//            sepBox.setPrefSize(200, 200);
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
            statBox.getChildren().add(stackPane);
        }

        HBox midGrid = new HBox(10);
        midGrid.setPadding(new Insets(10, 0, 0, 0));
        VBox recentFileBox = new VBox(10);
        recentFileBox.getStyleClass().add("recentFileBox");
        recentFileBox.setPadding(new Insets(10));
        recentFileBox.setPrefSize(750, 350);

        HBox recentFileHead = new HBox(450);
        recentFileHead.setAlignment(Pos.CENTER);
        Label recentFileLabel = new Label("Recent Files");
        recentFileLabel.getStyleClass().add("recentFileLabel");
        Label viewFileLabel = new Label("View all →");
        viewFileLabel.getStyleClass().add("viewFileLabel");
        recentFileHead.getChildren().addAll(recentFileLabel, viewFileLabel);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.getStyleClass().add("recentFileScrollBar");
        scrollPane.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-background: transparent;"
        );

        VBox container = new VBox(10); // 10px spacing between items
        container.setPadding(new Insets(10));

        for (int i = 0; i < 30; i++) {
            HBox recentFileInfoBox = new HBox(280);
            recentFileInfoBox.getStyleClass().add("recentFileInfoBox");
            recentFileInfoBox.setAlignment(Pos.CENTER);
            recentFileInfoBox.setFillHeight(false);

            ImageView docIcon = new ImageView(
                    new Image(MainApp.class.getResourceAsStream("/file_ext_icon/docx.png"))
            );
            docIcon.setSmooth(true);
            docIcon.setPreserveRatio(true);

            StackPane imgBackground = new StackPane(docIcon);
            imgBackground.setAlignment(Pos.CENTER);
            imgBackground.setPrefSize(50, 50);
            imgBackground.setPadding(new Insets(5));
            imgBackground.setStyle(
                    "-fx-background-color: #ff6b6b26;" +
                            "-fx-background-radius: 15;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 5);"
            );

            VBox fileInfoBox = new VBox();
            Label fileNameLabel = new Label("Practical.pdf");
            fileNameLabel.getStyleClass().add("fileNameLabel");
            Label fileLocationDetailLabel = new Label("Modified 2 hours ago | C:/users/tanis/Documents");
            fileLocationDetailLabel.getStyleClass().add("fileLocationDetailLabel");
            fileInfoBox.getChildren().addAll(fileNameLabel, fileLocationDetailLabel);

            HBox fileSizeBox = new HBox(10);
            fileSizeBox.setPrefWidth(120);
            Label fileSizeLabel = new Label("4.3 MB");
            fileSizeLabel.getStyleClass().add("fileSizeLabel");
            StackPane extensionBackground = new StackPane();
            extensionBackground.setPrefWidth(40);
            extensionBackground.setStyle(
                    "-fx-background-color: #ff6b6b26;" +
                            "-fx-background-radius: 15;");
            Label fileExtLabel = new Label("PDF");
            fileExtLabel.getStyleClass().add("fileExtLabel");
            extensionBackground.getChildren().add(fileExtLabel);
            fileSizeBox.getChildren().addAll(fileSizeLabel, extensionBackground);

            HBox pane = new HBox(10, imgBackground, fileInfoBox);
            pane.setAlignment(Pos.CENTER_LEFT);
            recentFileInfoBox.getChildren().addAll(pane, fileSizeBox);

            container.getChildren().add(recentFileInfoBox); // ✅ add to VBox
        }

        scrollPane.setContent(container); // ✅ set ONCE after loop
        scrollPane.setFitToWidth(true);

        recentFileBox.getChildren().addAll(recentFileHead, scrollPane);


        VBox categoryFolderBox = new VBox(10);
        categoryFolderBox.setPadding(new Insets(15));
        categoryFolderBox.setPrefSize(350, 350);
        categoryFolderBox.setStyle("""
                    -fx-background-color: #1e1e28;
                    -fx-background-radius: 15;
                    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 5);
                """);

        HBox categoryFileHead = new HBox();
        Label categoryFileLabel = new Label("By Category");
        categoryFileLabel.getStyleClass().add("recentFileLabel");
        categoryFileHead.getChildren().addAll(categoryFileLabel);

        categoryFolderBox.getChildren().addAll(categoryFileHead);
        for (int i = 0; i < 5; i++) {
            VBox storageDetailBox = new VBox(5);
            storageDetailBox.setPadding(new Insets(0, 0, 10, 0));

            FontIcon orangeCircle = new FontIcon("fas-circle");
            orangeCircle.setIconColor(Color.web("#ff6b6b"));
            orangeCircle.setIconSize(7);

            Label documentLabel = new Label(category[i], orangeCircle);
            documentLabel.getStyleClass().add("documentCategoryLabel");
            HBox hb1 = new HBox(documentLabel);
            Label fileCountSizeLabel = new Label("1,248 files | 48 GB");
            fileCountSizeLabel.getStyleClass().add("fileCountSizeLabel");

            Region region = new Region();
            HBox.setHgrow(region, Priority.ALWAYS);

            HBox hb2 = new HBox(hb1, region, fileCountSizeLabel);
            hb2.setAlignment(Pos.CENTER);

            ProgressBar sizeProgressBar = new ProgressBar();
            sizeProgressBar.setProgress(new Random().nextFloat());
            sizeProgressBar.getStyleClass().add("sizeProgressBar");
            storageDetailBox.getChildren().addAll(hb2, sizeProgressBar);
            categoryFolderBox.getChildren().add(storageDetailBox);
        }

        BorderPane buttonBox = new BorderPane();
        buttonBox.getStyleClass().add("gradient-button-box");
        buttonBox.setPrefSize(350, 350);
        buttonBox.setCenter(GradientButton.ScanButton());

        midGrid.getChildren().addAll(recentFileBox, categoryFolderBox);

        HBox bottomGrid = new HBox(10);
        bottomGrid.setPadding(new Insets(10, 0, 0, 0));

        HBox recentActivityHead = new HBox(250);
        recentActivityHead.setAlignment(Pos.CENTER);
        Label recentActivityLabel = new Label("Recent Activity");
        recentActivityLabel.getStyleClass().add("recentFileLabel");
        Label recentActivityLogLabel = new Label("Full log →");
        recentActivityLogLabel.getStyleClass().add("viewFileLabel");
        recentActivityHead.getChildren().addAll(recentActivityLabel, recentActivityLogLabel);

        VBox recentActivityBox = new VBox(10);
        recentActivityBox.setPrefSize(580, 200);
        recentActivityBox.setPadding(new Insets(8));
        recentActivityBox.setStyle("""
                    -fx-background-color: #1e1e28;
                    -fx-background-radius: 15;
                    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 5);
                """);

        ScrollPane activityscrollPane = new ScrollPane();
        activityscrollPane.getStyleClass().add("recentFileScrollBar");
        activityscrollPane.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-background: transparent;"
        );

        VBox recentActivityList = new VBox(8);

        for (int i = 0; i < 10; i++) {
            ImageView deleteIcon = new ImageView(new Image(MainApp.class.getResourceAsStream("/icons/delete.png")));
            deleteIcon.setFitWidth(19);
            deleteIcon.setFitHeight(19);
            StackPane circleBg = new StackPane(deleteIcon);
            circleBg.setAlignment(Pos.CENTER);
            circleBg.setPrefSize(33, 30);
//        circleBg.setPadding(new Insets(15));

            circleBg.setStyle(
                    "-fx-background-color: #6bcb7726;" +
                            "-fx-background-radius: 15;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 5);"
            );

            Label activityDescriptionLabel = new Label("Moved 23 files to Archive/2024 via auto-rule");
            activityDescriptionLabel.getStyleClass().add("activityDescriptionLabel");
            Label activityTimeLabel = new Label("2 minutes ago");
            activityTimeLabel.getStyleClass().add("activityTimeLabel");

            VBox recentActivityDetailBox = new VBox();
            recentActivityDetailBox.getChildren().addAll(activityDescriptionLabel, activityTimeLabel);

            HBox activityInfoBox = new HBox(10);
            activityInfoBox.getChildren().addAll(circleBg, recentActivityDetailBox);
            recentActivityList.getChildren().add(activityInfoBox);
        }
        activityscrollPane.setContent(recentActivityList);

        recentActivityBox.getChildren().addAll(recentActivityHead, activityscrollPane);

        HBox quickSettingHead = new HBox();
        Label quickSettingLabel = new Label("Quick Setting");
        quickSettingLabel.getStyleClass().add("recentFileLabel");
        quickSettingHead.getChildren().addAll(quickSettingLabel);

        HBox autoScanSettingBox = new HBox();
        autoScanSettingBox.setAlignment(Pos.CENTER_LEFT);
        Label autoScanLabel = new Label("Auto scan and Organize");
        autoScanLabel.getStyleClass().add("quickSettingLabels");
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        autoScanSettingBox.getChildren().addAll(autoScanLabel, spacer1, new ToggleSwitch(36, 20));

        HBox tempFileSettingBox = new HBox();
        tempFileSettingBox.setAlignment(Pos.CENTER_LEFT);
        Label tempFileLabel = new Label("Auto delete temp files");
        tempFileLabel.getStyleClass().add("quickSettingLabels");
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        tempFileSettingBox.getChildren().addAll(tempFileLabel, spacer2, new ToggleSwitch(36, 20));

        VBox quickSettingBox = new VBox(10);
        quickSettingBox.setStyle("""
                    -fx-background-color: #1e1e28;
                    -fx-background-radius: 15;
                    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 5);
                """);
        quickSettingBox.setPrefSize(580, 200);
        quickSettingBox.setPadding(new Insets(8));
        quickSettingBox.getChildren().addAll(quickSettingHead, autoScanSettingBox, tempFileSettingBox);

        bottomGrid.getChildren().addAll(recentActivityBox, quickSettingBox);

        content.getChildren().addAll(topTextBox, statBox, midGrid, bottomGrid);

        return content;
    }

    @Override
    public void start(Stage primaryStage) {
        Font font = Font.loadFont(
                getClass().getResourceAsStream("/fonts/Syne-ExtraBold.ttf"),
                14
        );

        font = Font.loadFont(getClass().getResourceAsStream("/fonts/DMSans-VariableFont.ttf"), 54);

        BorderPane root = new BorderPane();

        //Left Side Content
        VBox sidebar = new VBox(40);
        sidebar.getStyleClass().add("left-side-vbox");
        sidebar.setPrefWidth(250);

        //Right Side Content
        VBox content = new VBox();
        content.getStyleClass().add("right-side-vbox");
        content.getChildren().addAll(mainDashBoard());

        Label appName = new Label("FileFlow");
        appName.getStyleClass().add("appName");

        ImageView dashboardIcon = new ImageView(new Image(getClass().getResourceAsStream("/icons/dashboard.png")));
        dashboardIcon.setFitWidth(19);
        dashboardIcon.setFitHeight(19);

        ImageView scanIcon = new ImageView(new Image(getClass().getResourceAsStream("/icons/scan-folder.png")));
        scanIcon.setFitWidth(24);
        scanIcon.setFitHeight(24);

        ImageView largeFileIcon = new ImageView(new Image(getClass().getResourceAsStream("/icons/largefile.png")));
        largeFileIcon.setFitWidth(19);
        largeFileIcon.setFitHeight(19);

        ImageView duplicateFileIcon = new ImageView(new Image(getClass().getResourceAsStream("/icons/duplicate.png")));
        duplicateFileIcon.setFitWidth(19);
        duplicateFileIcon.setFitHeight(19);

        ImageView compressFileIcon = new ImageView(new Image(getClass().getResourceAsStream("/icons/compressfile.png")));
        compressFileIcon.setFitWidth(19);
        compressFileIcon.setFitHeight(19);

        ImageView junkFileIcon = new ImageView(new Image(getClass().getResourceAsStream("/icons/junkfile.png")));
        junkFileIcon.setFitWidth(19);
        junkFileIcon.setFitHeight(19);

        ImageView codeFileIcon = new ImageView(new Image(getClass().getResourceAsStream("/lang_icon/programming.png")));
        codeFileIcon.setFitWidth(19);
        codeFileIcon.setFitHeight(19);

        ImageView fileTransferIcon = new ImageView(new Image(getClass().getResourceAsStream("/icons/filetransfer.png")));
        fileTransferIcon.setFitWidth(19);
        fileTransferIcon.setFitHeight(19);

        FontIcon settingIcon = new FontIcon("fas-cog");
        settingIcon.setIconColor(Color.WHITE);
        settingIcon.setIconSize(19);

        Label labMain = new Label("MAIN");
        Label labDashboard = SideBarController.createNavLabel("Dashboard", dashboardIcon, () -> SideBarController.changeContent(content, MainApp::mainDashBoard));
        Label labScan = SideBarController.createNavLabel("Scan System", scanIcon, () -> SideBarController.changeContent(content, () -> scanSystemDashBoard(primaryStage)));
        Label labLargeFiles = SideBarController.createNavLabel("Large Files", largeFileIcon, () -> SideBarController.changeContent(content, MainApp::largeFileDashBoard));
        Label labDuplicates = SideBarController.createNavLabel("Duplicates", duplicateFileIcon, () -> SideBarController.changeContent(content, MainApp::duplicateFileDashBoard));
        Label labCompress = SideBarController.createNavLabel("Compress Files", compressFileIcon, () -> SideBarController.changeContent(content, null));
        Label labJunk = SideBarController.createNavLabel("Junk Files", junkFileIcon, () -> SideBarController.changeContent(content, MainApp::junkFileDashBoard));
        Label labCode = SideBarController.createNavLabel("Code Files", codeFileIcon, () -> SideBarController.changeContent(content, CodeFileUI::buildCodeFinderUI));
        Label labTransfer = SideBarController.createNavLabel("File Transfer", fileTransferIcon, () -> SideBarController.changeContent(content, MainApp::fileTransferDashBoard));
        Label labSetting = SideBarController.createNavLabel("Setting", settingIcon, () -> SideBarController.changeContent(content, null));
        SideBarController.selectedLabel = labDashboard;

        labDashboard.setGraphicTextGap(13);
        labScan.setGraphicTextGap(13);
        labLargeFiles.setGraphicTextGap(13);
        labDuplicates.setGraphicTextGap(13);
        labCompress.setGraphicTextGap(13);
        labJunk.setGraphicTextGap(13);
        labCode.setGraphicTextGap(13);
        labTransfer.setGraphicTextGap(13);
        labSetting.setGraphicTextGap(13);

        // Add same style class to all
        labMain.getStyleClass().add("menu-item-heading");
        labDashboard.getStyleClass().addAll("left-menu-item", "menu-time-active");
        labScan.getStyleClass().add("left-menu-item");
        labLargeFiles.getStyleClass().add("left-menu-item");
        labDuplicates.getStyleClass().add("left-menu-item");
        labCompress.getStyleClass().add("left-menu-item");
        labJunk.getStyleClass().add("left-menu-item");
        labCode.getStyleClass().add("left-menu-item");
        labTransfer.getStyleClass().add("left-menu-item");
        labSetting.getStyleClass().add("left-menu-item");

        VBox mainOptionBox = new VBox(12,
                labMain,
                labDashboard,
                labScan,
                labLargeFiles,
                labDuplicates,
                labCompress,
                labJunk,
                labCode,
                labTransfer,
                labSetting
        );

        FontIcon orangeDot = new FontIcon("fas-circle");
        orangeDot.setIconColor(Color.web("#ff6b6b"));
        orangeDot.setIconSize(7);

        FontIcon blueDot = new FontIcon("fas-circle");
        blueDot.setIconColor(Color.web("#4d96ff"));
        blueDot.setIconSize(7);

        FontIcon yellowDot = new FontIcon("fas-circle");
        yellowDot.setIconColor(Color.web("#ffd93d"));
        yellowDot.setIconSize(7);

        FontIcon greenDot = new FontIcon("fas-circle");
        greenDot.setIconColor(Color.web("#6bc77b"));
        greenDot.setIconSize(7);

        FontIcon purpleDot = new FontIcon("fas-circle");
        purpleDot.setIconColor(Color.web("#c77dff"));
        purpleDot.setIconSize(7);

        Label labCategory = new Label("CATEGORIES");
        Label labDocuments = new Label("Documents", orangeDot);
        Label labImages = new Label("Images", blueDot);
        Label labVideos = new Label("Videos", yellowDot);
        Label labAudio = new Label("Audio", greenDot);
        Label labArchives = new Label("Archives", purpleDot);

        labDocuments.setGraphicTextGap(13);
        labImages.setGraphicTextGap(13);
        labVideos.setGraphicTextGap(13);
        labAudio.setGraphicTextGap(13);
        labArchives.setGraphicTextGap(13);

        labCategory.getStyleClass().add("menu-item-heading");
        labDocuments.getStyleClass().add("left-menu-item");
        labImages.getStyleClass().add("left-menu-item");
        labVideos.getStyleClass().add("left-menu-item");
        labAudio.getStyleClass().add("left-menu-item");
        labArchives.getStyleClass().add("left-menu-item");

        VBox categoryBox = new VBox(8,
                labCategory,
                labDocuments,
                labImages,
                labVideos,
                labAudio,
                labArchives
        );

        sidebar.getChildren().addAll(appName, mainOptionBox, categoryBox);

//        DuplicateFileDialog.show(primaryStage);
//        scanResultDialogBox.show(primaryStage);

        root.setLeft(sidebar);
        root.setCenter(content);
        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm()
        );
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}