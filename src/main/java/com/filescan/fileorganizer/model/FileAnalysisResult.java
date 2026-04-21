package com.filescan.fileorganizer.model;

import java.util.Map;

public class FileAnalysisResult {
    Map<FileType, Long> counts;
    Map<FileType, Long> sizes;

    public FileAnalysisResult(Map<FileType, Long> counts, Map<FileType, Long> sizes) {
        this.counts = counts;
        this.sizes = sizes;
    }

    public Map<FileType, Long> getCounts() {
        return counts;
    }

    public Map<FileType, Long> getSizes() {
        return sizes;
    }
}
