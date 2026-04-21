package com.filescan.fileorganizer.model;

import java.util.HashMap;
import java.util.Map;

public class FileCount {
    static Map<String, Integer> fileCount = new HashMap<>();
    public FileCount() {
        fileCount.put("IMAGE", 0);
        fileCount.put("MUSIC", 0);
        fileCount.put("VIDEO", 0);
        fileCount.put("DOCUMENT", 0);
        fileCount.put("CODE", 0);
        fileCount.put("UNKNOWN", 0);
    }

    public static void changeValue(String type, int value) {
        fileCount.replace(type, value);
    }
}
