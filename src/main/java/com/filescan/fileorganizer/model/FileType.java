package com.filescan.fileorganizer.model;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public enum FileType {
    UNKNOWN(Set.of()),
    MUSIC   (Set.of(".mp3", ".wav", ".flac", ".aac", ".ogg")),
    IMAGE   (Set.of(".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp", ".svg")),
    VIDEO   (Set.of(".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv", ".webm", ".m4v", ".mpeg")),
    DOCUMENT(Set.of(".pdf", ".docx", ".doc", ".txt", ".xlsx", ".pptx", ".json")),
    CODE    (Set.of(".java", ".py", ".c", ".cpp", ".html", ".css", ".js", ".ts", ".go", ".rs",
            ".kt", ".cs", ".php", ".rb", ".xml")),
    ARCHIVE (Set.of(".zip", ".rar", ".7z", ".tar", ".gz", ".bz2", ".xz", ".tgz", ".tar.gz", ".tar.bz2", ".tar.xz", ".iso", ".cab", ".arj", ".lz", ".lzma"));

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

    public static Map<FileType, Set<String>> getAllTypeSets() {
        return Arrays.stream(FileType.values())
                .collect(Collectors.toMap(
                        type -> type,
                        FileType::getExtensions
                ));
    }

    public static Set<String> getAllExtensions() {
        return EXTENSION_MAP.keySet();
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