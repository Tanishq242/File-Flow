package com.filescan.fileorganizer.model;

import javafx.beans.property.*;

public class JunkCategory {

    private String name;
    private String size;
    private int fileCount;

    public JunkCategory(String name, String size, int fileCount) {
        this.name = name;
        this.size = size;
        this.fileCount = fileCount;
    }

    public String getName() { return name; }
    public String getSize() { return size; }
    public int getFileCount() { return fileCount; }
}
