package com.filescan.fileorganizer.controller;

import com.filescan.fileorganizer.model.FileType;
import com.filescan.fileorganizer.service.FileService;
import javafx.scene.control.Label;

import java.nio.file.Path;
import java.nio.file.Paths;

public class DashBoardController {
    public static void fileCountShow(Label label) {
        try {
            Path path = Paths.get("F:\\Music\\American Truck Simulator\\music");
//            return FileService.countFiles(path, FileType.MUSIC.getExtensions());
//            label.setText(String.valueOf(count));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
