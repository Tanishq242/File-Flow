package com.filescan.fileorganizer.service;

import com.filescan.fileorganizer.model.FileType;
import javafx.application.Platform;
import javafx.scene.control.Label;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
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

    public static Map<FileType, List<Path>> categorizedFiles = new HashMap<>();
    public static Map<FileType, List<Path>> mediaFiles = new HashMap<>();
    public static Vector<Path> largeFilesList = new Vector<>();

    public static long countFiles(Path path, Set<String> extensions) throws IOException {
        try (Stream<Path> stream = Files.walk(path)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        try {
                            return !Files.isHidden(p);
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .filter(p -> {
                        String name = p.toString().toLowerCase();

                        // if no filter → count all files
                        if (extensions == null || extensions.isEmpty()) {
                            return true;
                        }

                        return extensions.stream()
                                .anyMatch(name::endsWith);
                    })
                    .count();
        }
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

    public static int scanPath(boolean checkLarge, boolean checkMedia) {

        Set<String> excluded = Set.of("C:", "F:");

        List<File> targetDrives = Arrays.stream(File.listRoots())
                .filter(d -> excluded.stream()
                        .noneMatch(ex -> d.getAbsolutePath().startsWith(ex)))
                .toList();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            List<Future<DriveSummary>> futures = targetDrives.stream()
                    .map(drive -> executor.submit(() -> scanDrive(drive, checkLarge, checkMedia)))
                    .toList();

            // ✅ VERY IMPORTANT → wait for all threads
            for (Future<DriveSummary> f : futures) {
                f.get();  // ensures scanning is finished
            }

            for (Path path : largeFilesList) {
                FileType type = FileType.fromPath(path);

                categorizedFiles
                        .computeIfAbsent(type, k -> new ArrayList<>())
                        .add(path);
            }

            // ✅ Debug (optional)
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
