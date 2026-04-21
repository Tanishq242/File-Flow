package com.filescan.fileorganizer.testProgram;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Random;

public class FileGeneratorUI extends Application {

    private TextArea logArea;
    private ProgressBar progressBar;

    private final String[] extensions = {
            ".txt", ".json", ".xml", ".java",
            ".jpg", ".png",
            ".mp4", ".mkv",
            ".mp3",
            ".zip"
    };

    private final Random random = new Random();

    @Override
    public void start(Stage stage) {

        TextField folderField = new TextField();
        folderField.setPromptText("Select folder...");

        Button browseBtn = new Button("Browse");

        browseBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            File dir = chooser.showDialog(stage);
            if (dir != null) {
                folderField.setText(dir.getAbsolutePath());
            }
        });

        TextField fileCountField = new TextField();
        fileCountField.setPromptText("Enter number of files");

        Button generateBtn = new Button("Generate Files");

        progressBar = new ProgressBar(0);

        logArea = new TextArea();
        logArea.setPrefHeight(200);

        generateBtn.setOnAction(e -> {

            String path = folderField.getText();
            int count;

            try {
                count = Integer.parseInt(fileCountField.getText());
            } catch (Exception ex) {
                log("❌ Invalid number");
                return;
            }

            File folder = new File(path);

            if (!folder.exists()) {
                log("❌ Folder not found");
                return;
            }

            runGenerator(folder, count);
        });

        VBox root = new VBox(10,
                folderField, browseBtn,
                fileCountField,
                generateBtn,
                progressBar,
                logArea
        );

        root.setPadding(new Insets(15));
        root.setAlignment(Pos.CENTER);

        stage.setScene(new Scene(root, 400, 400));
        stage.setTitle("Test File Generator");
        stage.show();
    }

    private void runGenerator(File folder, int count) {

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {

                for (int i = 1; i <= count; i++) {

                    String ext = extensions[random.nextInt(extensions.length)];
                    String fileName = "test_" + i + ext;

                    File file = new File(folder, fileName);

                    createRandomFile(file);

                    updateProgress(i, count);
                    updateMessage("Created: " + fileName);

                    Thread.sleep(50);
                }

                updateMessage("✅ Done generating files!");
                return null;
            }
        };

        task.messageProperty().addListener((obs, old, msg) -> log(msg));
        progressBar.progressProperty().bind(task.progressProperty());

        new Thread(task).start();
    }

    private void createRandomFile(File file) {
        try {
            byte[] data = new byte[random.nextInt(5000) + 100]; // 100B–5KB
            random.nextBytes(data);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(data);
            }

        } catch (Exception e) {
            log("❌ Error creating file: " + file.getName());
        }
    }

    private void log(String msg) {
        Platform.runLater(() -> logArea.appendText(msg + "\n"));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
