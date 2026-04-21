package com.filescan.fileorganizer.service;

import com.filescan.fileorganizer.model.FileType;
import javafx.application.Platform;
import javafx.scene.control.Label;
import org.apache.commons.io.FilenameUtils;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public class FileService {
    static final int docsMaxSize = 10485760;
    static final int imagesMaxSize = 5242880;
    static final int videosMaxSize = 524288000;
    static final int audioMaxSize = 10485760;

    private static final Set<String> DOCS = Set.of(
            ".pdf", ".doc", ".docx", ".txt", ".xlsx", ".xls", ".pptx", ".csv", ".odt", ".rtf");

    private static final Set<String> MUSIC = Set.of(
            ".mp3", ".wav", ".flac", ".aac", ".ogg", ".m4a", ".wma", ".opus");

    private static final Set<String> PICS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".tiff", ".heic", ".svg", ".raw");

    private static final Set<String> VIDS = Set.of(
            ".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv", ".webm", ".m4v", ".mpeg");

    private static final Set<String> codeExtensions = Set.of(
            ".java", ".py", ".c", ".cpp", ".html", ".css", ".js", ".ts", ".go", ".rs",
            ".kt", ".cs", ".php", ".rb");

    private static final Set<String> SKIP_FOLDERS = Set.of(
            "node_modules", ".npm", ".yarn", ".pnpm-store",
            ".m2", ".gradle", "build", "target",
            ".venv", "venv", "env", "__pycache__", ".tox", "dist", "eggs", ".eggs", "site-packages",
            "cmake-build-debug", "cmake-build-release",
            ".idea", ".vscode", ".eclipse", ".settings",
            ".android", "captures",
            ".dart_tool", ".pub-cache", ".flutter",
            ".bundle", "vendor",
            "pkg",
            ".docker",
            ".git", ".svn", ".hg",
            ".Trash", "$RECYCLE.BIN", "System Volume Information",
            ".cache", "logs", "tmp", "temp"
    );

    private static final Set<String> GAME_EXTENSIONS = Set.of(
            ".exe", ".dll", ".pak",
            ".unity3d", ".unitypackage",
            ".uasset", ".umap", ".upk",
            ".rpgmvp", ".rpgsave",
            ".vpk", ".bsp",
            ".wad", ".pk3", ".pk4",
            ".gcf", ".ncf",
            ".bnk",
            ".sav", ".save", ".dat",
            ".iso", ".bin", ".cue", ".img",
            ".cache", ".shadercache"
    );

    public static Map<FileType, List<Path>> categorizedFiles = new HashMap<>();
    public static Map<FileType, List<Path>> mediaFiles = new HashMap<>();
    public static Vector<Path> largeFilesList = new Vector<>();
    public static Map<FileType, Long> size = new HashMap<>();

    private static boolean isGameFolder(Path dir) {
        // Game folders usually contain these marker files
        Set<String> gameMarkers = Set.of(
                "steam_api.dll",        // Steam game
                "steam_api64.dll",      // Steam 64-bit
                "eossdk-win64-shipping.dll", // Epic Games
                "uplay_r1_loader.dll",  // Ubisoft
                "bsapi.dll",            // Battle.net
                "unins000.exe",         // Inno Setup installer (most games)
                "engine.ini",           // Unreal Engine
                "boot.cfg"              // Various games
        );

        try (Stream<Path> files = Files.list(dir)) {
            return files
                    .map(p -> p.getFileName().toString().toLowerCase())
                    .anyMatch(gameMarkers::contains);

        } catch (IOException e) {
            return false;
        }
    }

    public static Map<FileType, Long> calculateCategorySizes(
            Map<FileType, List<Path>> categorizedFiles) {

        Map<FileType, Long> sizeMap = new EnumMap<>(FileType.class);

        for (Map.Entry<FileType, List<Path>> entry : categorizedFiles.entrySet()) {

            FileType type = entry.getKey();
            List<Path> files = entry.getValue();

            long totalSize = 0;

            for (Path file : files) {
                try {
                    if (Files.exists(file) && Files.isRegularFile(file)) {
                        totalSize += Files.size(file);
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ Error reading size: " + file);
                }
            }

            sizeMap.put(type, totalSize);
        }

        return sizeMap;
    }

    private static void countInPath(Path start, Map<FileType, Long> counts) {

        try {
            Files.walkFileTree(start, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {

                    String name = dir.getFileName() != null
                            ? dir.getFileName().toString().toLowerCase()
                            : "";

                    // 🚫 Skip protected/system folders early
                    if (SKIP_FOLDERS.contains(name)
                            || name.equals("system volume information")
                            || name.equals("$recycle.bin")) {

                        System.out.println("⏭️ Skipping folder: " + dir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    // 🎮 Skip game folders
                    if (isGameFolder(dir)) {
                        System.out.println("🎮 Skipping game folder: " + dir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

                    try {
                        if (!Files.isReadable(file)) {
                            return FileVisitResult.CONTINUE;
                        }

                        String ext = "." + FilenameUtils.getExtension(file.toString()).toLowerCase();

                        if (GAME_EXTENSIONS.contains(ext)) {
                            return FileVisitResult.CONTINUE;
                        }

                        FileType type = FileType.fromPath(file);

                        if (type != null && type != FileType.UNKNOWN) {

                            // 📊 COUNT
                            counts.merge(type, 1L, Long::sum);

                            // 📦 SIZE
                            long fileSize = attrs.size();
                            size.merge(type, fileSize, Long::sum);

                            // 📁 CATEGORY (ADD THIS 👇)
                            categorizedFiles
                                    .computeIfAbsent(type, k -> new ArrayList<>())
                                    .add(file);
                        }

                    } catch (Exception e) {
                        System.out.println("⚠️ Skipping file: " + file);
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {

                    System.out.println("🚫 Access denied: " + file);
                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (IOException e) {
            System.out.println("❌ Error walking: " + start + " → " + e.getMessage());
        }
    }

    public static Map<FileType, Long> countFilesByType(Path path) {
        System.out.println("count file method");
        size.clear();

        Map<FileType, Long> counts = new EnumMap<>(FileType.class);

        // Initialize all types to 0
        for (FileType type : FileType.values()) {
            counts.put(type, 0L);
            size.put(type, 0L);
        }

        try {
            // 👉 If specific path is given
            if (path != null) {
                System.out.println("📁 Scanning provided path: " + path);
                countInPath(path, counts);
                return counts;
            }

            // 👉 If path is null → scan all drives except C:
            System.out.println("🌐 Scanning all drives except C:");

            for (Path root : FileSystems.getDefault().getRootDirectories()) {
                String drive = root.toString().toUpperCase();

                System.out.println("Found root: " + drive);

                // Skip C drive
                if (drive.startsWith("C")) {
                    System.out.println("⏭️ Skipping system drive: " + drive);
                    continue;
                }

                System.out.println("🔍 Scanning drive: " + drive);
                countInPath(root, counts);
            }

        } catch (Exception e) {
            System.out.println("❌ Error in countFilesByType: " + e.getMessage());
            e.printStackTrace();
        }

        return counts;
    }

    // ── holds counts for one drive ────────────────────────────
    record DriveSummary(String drive, int dirs, int docs, int music, int pics, int vids, int codes) {
    }

    private static DriveSummary scanDrive(File drive, boolean isLargeScan, boolean isMedia) throws IOException {

        // local counters — no static state, no thread safety issues
        int[] dirs = {0};
        int[] docs = {0};
        int[] music = {0};
        int[] pics = {0};
        int[] vids = {0};
        int[] codes = {0};

        StringWriter buffer = new StringWriter();
        BufferedWriter local = new BufferedWriter(buffer);

        local.write(drive.getAbsolutePath() + " Drive");
        local.newLine();

        Files.walkFileTree(Path.of(drive.getAbsolutePath()), new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                try {
                    local.write(dir.toString());
                    local.newLine();
                } catch (IOException ex) {
                    System.err.println("Write error: " + ex.getMessage());
                }
                dirs[0]++;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String name = file.getFileName().toString().toLowerCase();
                int dot = name.lastIndexOf('.');
                if (dot < 0)
                    return FileVisitResult.CONTINUE;

                String ext = name.substring(dot);
                if (DOCS.contains(ext)) {
                    docs[0]++;
                    if (isLargeScan) largeFileCount(file, "D");
                } else if (MUSIC.contains(ext)) {
                    music[0]++;
                    if (isLargeScan) largeFileCount(file, "A");
                    if (isMedia) mediaFiles.computeIfAbsent(FileType.MUSIC, k -> new ArrayList<>()).add(file);
                } else if (PICS.contains(ext)) {
                    pics[0]++;
                    if (isLargeScan) largeFileCount(file, "I");
                    if (isMedia) mediaFiles.computeIfAbsent(FileType.IMAGE, k -> new ArrayList<>()).add(file);
                } else if (VIDS.contains(ext)) {
                    vids[0]++;
                    if (isLargeScan) largeFileCount(file, "V");
                    if (isMedia) mediaFiles.computeIfAbsent(FileType.VIDEO, k -> new ArrayList<>()).add(file);
                } else if (codeExtensions.contains(ext)) {
                    codes[0]++;
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException e) {
                return FileVisitResult.CONTINUE;
            }
        });

        local.flush();

        return new DriveSummary(
                drive.getAbsolutePath(),
                dirs[0], docs[0], music[0], pics[0], vids[0], codes[0]);
    }

    public static int scanPath(boolean checkLarge, boolean checkMedia, File uri) {

        Set<String> excluded = Set.of("C:", "F:");

        // ✅ Use uri if provided, otherwise fall back to filtered roots
        List<File> targetDrives;
        if (uri != null) {
            targetDrives = List.of(uri);  // scan only the given path
        } else {
            targetDrives = Arrays.stream(File.listRoots())
                    .filter(d -> excluded.stream()
                            .noneMatch(ex -> d.getAbsolutePath().startsWith(ex)))
                    .toList();
        }

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            List<Future<DriveSummary>> futures = targetDrives.stream()
                    .map(drive -> executor.submit(() -> scanDrive(drive, checkLarge, checkMedia)))
                    .toList();

            for (Future<DriveSummary> f : futures) {
                f.get();
            }

            for (Path path : largeFilesList) {
                FileType type = FileType.fromPath(path);
                categorizedFiles
                        .computeIfAbsent(type, k -> new ArrayList<>())
                        .add(path);
            }

            System.out.println("Large files: " + largeFilesList.size());
            categorizedFiles.forEach((k, v) ->
                    System.out.println(k + " -> " + v.size())
            );

            return 0;

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    public static int scanPath(String driveLetter, boolean checkLarge, boolean checkMedia) {
        // Normalize input → "D" or "D:" or "d:" all become "D:\"
        String normalizedDrive = driveLetter
                .toUpperCase()
                .replace(":", "")
                .concat(":\\");

        File targetDrive = new File(normalizedDrive);

        // Validate the drive
        if (!targetDrive.exists() || !targetDrive.isDirectory()) {
            System.err.println("Drive not found or not accessible: " + normalizedDrive);
            return 1;
        }

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            Future<DriveSummary> future = executor.submit(() ->
                    scanDrive(targetDrive, checkLarge, checkMedia));

            // Wait for scan to finish
            future.get();

            // Categorize scanned files
            categorizedFiles.clear();

            for (Path path : largeFilesList) {
                FileType type = FileType.fromPath(path);

                categorizedFiles
                        .computeIfAbsent(type, k -> new ArrayList<>())
                        .add(path);
            }

            return 0;

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }


    public static void largeFileCount(Path file, String fileType) {
        try {
            long fileSize = Files.size(file);
            if (fileType.equals("D") && fileSize > docsMaxSize) {
                largeFilesList.add(file);
//                System.out.println(file.getFileName());
            } else if (fileType.equals("I") && fileSize > imagesMaxSize) {
                largeFilesList.add(file);
//                System.out.println(file.getFileName());
            } else if (fileType.equals("V") && fileSize > videosMaxSize) {
                largeFilesList.add(file);
//                System.out.println(file.getFileName());
            } else if (fileType.equals("A") && fileSize > audioMaxSize) {
                largeFilesList.add(file);
//                System.out.println(file.getFileName());
            }
        } catch (IOException e) {
            System.out.println(e);
        }

    }
}
