package com.filescan.fileorganizer.controller;

import com.filescan.fileorganizer.service.FileService;
import javafx.scene.control.CheckBox;
import java.util.List;

public class ScanCenterController {
    public static int applyScanButton(List<CheckBox> checkBoxList) {
        boolean checkDuplicateFiles = checkBoxList.getFirst().isSelected();
        boolean checkLargeFiles = checkBoxList.get(1).isSelected();
        boolean checkJunkFiles = checkBoxList.get(2).isSelected();
        boolean checkMediaFiles = checkBoxList.getLast().isSelected();

        System.out.println(checkDuplicateFiles + " " + checkLargeFiles + " " + checkJunkFiles + " " + checkMediaFiles);

        return 1;
    }
}
