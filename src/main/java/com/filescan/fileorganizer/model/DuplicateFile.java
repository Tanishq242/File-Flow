package com.filescan.fileorganizer.model;

public class DuplicateFile {
    private String fileName;
    private String path;
    private long size;
    private boolean selected;

    public DuplicateFile(String fileName, String path, long size) {
        this.fileName = fileName;
        this.path = path;
        this.size = size;
        this.selected = true;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
