package com.filescan.fileorganizer.controller;

import com.filescan.fileorganizer.model.FileData;
import com.filescan.fileorganizer.model.FileType;
import com.filescan.fileorganizer.service.FileService;
import javafx.application.Platform;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TableView;

import java.nio.file.Files;
import java.nio.file.Path;

public class LargeScanController {
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

    public static int largeFileScan(String drive, TableView<FileData> table) {

        int value = FileService.scanPath(drive, true, false);

        System.out.println(FileService.categorizedFiles.size());
        for (Path i : FileService.largeFilesList) {
            Platform.runLater(() -> {
                try {
                    table.getItems().add(new FileData(i.getFileName().toString(), FileType.fromPath(i).toString(), formatSize(Files.size(i)), i.toAbsolutePath().toString()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        return value;
    }

    public static void showAllFiles(TableView<FileData> table) {
        table.getItems().removeAll();
        for (Path i : FileService.largeFilesList) {
            Platform.runLater(() -> {
                try {
                    table.getItems().add(new FileData(i.getFileName().toString(), FileType.fromPath(i).toString(), formatSize(Files.size(i)), i.toAbsolutePath().toString()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public static void showOnlyImages(TableView<FileData> table) {
        Platform.runLater(() -> {

            FilteredList<FileData> filtered = new FilteredList<>(table.getItems());

            filtered.setPredicate(file -> {
                try {
                    Path path = Path.of(file.nameProperty().get()); // ✅ correct usage
                    FileType type = FileType.fromPath(path);

                    return type == FileType.IMAGE;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            });

            table.setItems(filtered);
        });
    }
}
