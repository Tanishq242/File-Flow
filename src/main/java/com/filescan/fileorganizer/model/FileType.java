package com.filescan.fileorganizer.model;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public enum FileType {
    MUSIC   (Set.of(".mp3", ".wav", ".flac", ".aac", ".ogg")),
    IMAGE   (Set.of(".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp")),
    VIDEO   (Set.of(".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv", ".webm", ".m4v", ".mpeg")),
    DOCUMENT(Set.of(".pdf", ".docx", ".doc", ".txt", ".xlsx", ".pptx")),
    CODE    (Set.of(".java", ".py", ".c", ".cpp", ".html", ".css", ".js", ".ts", ".go", ".rs",
            ".kt", ".cs", ".php", ".rb"));

    private final Set<String> extensions;

    // Built once at class-load time — shared across all threads, immutable
    private static final Map<String, FileType> EXTENSION_MAP = new HashMap<>();

    static {
        for (FileType type : values()) {
            for (String ext : type.extensions) {
                EXTENSION_MAP.put(ext, type);   // ".mp3" → MUSIC, etc.
            }
        }
    }

    FileType(Set<String> extensions) {
        this.extensions = extensions;
    }

    public Set<String> getExtensions() {
        return extensions;
    }

    public static boolean isImage(Path path) {
        return FileType.fromPath(path) == FileType.IMAGE;
    }

    public static boolean isMusic(Path path) {
        return FileType.fromPath(path) == FileType.MUSIC;
    }

    public static boolean isDocument(Path path) {
        return FileType.fromPath(path) == FileType.DOCUMENT;
    }

    public static boolean isVideo(Path path) {
        return FileType.fromPath(path) == FileType.VIDEO;
    }

    /**
     * O(1) lookup. Returns null if extension is unrecognized.
     * Input: any path or filename — extraction handled here.
     */
    public static FileType fromPath(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot == -1) return null;                          // no extension
        return EXTENSION_MAP.get(name.substring(dot));       // ".mp3" → MUSIC
    }
}