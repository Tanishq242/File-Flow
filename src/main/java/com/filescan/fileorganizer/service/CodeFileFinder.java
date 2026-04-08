package com.filescan.fileorganizer.service;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

public class CodeFileFinder {

    private static final Set<String> CODE_EXTENSIONS = Set.of(
            ".java", ".py", ".c", ".cpp", ".html", ".css", ".js", ".ts", ".go", ".rs",
            ".kt", ".cs", ".php", ".rb");

    // Groups related files by their name (without extension)
    // e.g., Main.java + Main.html + Main.css are "related"
    public static Map<String, List<Path>> findAllCodeFiles() {

        Map<String, List<Path>> allFiles = new HashMap<>(); // name → list of paths
        List<File> drives = getNonCDrives();

        if (drives.isEmpty()) {
            System.out.println("No drives found except C:");
            return allFiles;
        }

        for (File drive : drives) {
            System.out.println("Scanning drive: " + drive.getAbsolutePath());
            scanDrive(drive, allFiles);
        }

        return allFiles;
    }

    // ── Get all drives except C: ─────────────────────────────────────────────
    private static List<File> getNonCDrives() {
        return Arrays.stream(File.listRoots())
                .filter(drive -> !drive.getAbsolutePath().toUpperCase().startsWith("C")
                        && !drive.getAbsolutePath().toUpperCase().startsWith("F"))
                .filter(File::exists)
                .filter(File::canRead)
                .collect(Collectors.toList());
    }

    // ── Scan entire drive recursively ────────────────────────────────────────
    private static void scanDrive(File drive, Map<String, List<Path>> allFiles) {
        try {
            Files.walkFileTree(drive.toPath(), new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String fileName = file.getFileName().toString();
                    String extension = getExtension(fileName);

                    if (CODE_EXTENSIONS.contains(extension)) {
                        // Key = filename without extension (to group related files)
                        String baseName = getBaseName(fileName);
                        allFiles.computeIfAbsent(baseName, k -> new ArrayList<>()).add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    // Silently skip inaccessible files/folders
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // Skip system/hidden directories to speed up scan
                    String dirName = dir.getFileName() != null ? dir.getFileName().toString() : "";
                    if (isSkippableDirectory(dirName)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("Error scanning drive: " + drive + " → " + e.getMessage());
        }
    }

    // ── Print results clearly ────────────────────────────────────────────────
    // ── Print results grouped by extension ──────────────────────────────────────
    public static void printResults(Map<String, List<Path>> allFiles) {
        if (allFiles.isEmpty()) {
            System.out.println("No code files found.");
            return;
        }

        // Flatten all paths and group by extension
        Map<String, List<Path>> byExtension = new TreeMap<>(); // TreeMap = sorted A-Z

        allFiles.values().stream()
                .flatMap(List::stream)
                .forEach(path -> {
                    String ext = getExtension(path.getFileName().toString());
                    byExtension.computeIfAbsent(ext, k -> new ArrayList<>()).add(path);
                });

        // Extension icons for better readability
        Map<String, String> extIcons = Map.ofEntries(
                Map.entry(".java", "☕"),
                Map.entry(".py", "🐍"),
                Map.entry(".c", "🔵"),
                Map.entry(".cpp", "🔷"),
                Map.entry(".html", "🌐"),
                Map.entry(".css", "🎨"),
                Map.entry(".js", "💛"),
                Map.entry(".ts", "🔹"),
                Map.entry(".go", "🐹"),
                Map.entry(".rs", "🦀"),
                Map.entry(".kt", "🟣"),
                Map.entry(".cs", "🟩"),
                Map.entry(".php", "🐘"),
                Map.entry(".rb", "💎"));

        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║      CODE FILES BY EXTENSION         ║");
        System.out.println("╚══════════════════════════════════════╝");

        byExtension.forEach((ext, paths) -> {
            String icon = extIcons.getOrDefault(ext, "📄");

            // Extension header
            System.out.printf("%n  %s  %-10s  (%d files)%n", icon, ext.toUpperCase(), paths.size());
            System.out.println("  " + "─".repeat(50));

            // Sort files by path for clean output
            paths.stream()
                    .sorted()
                    .forEach(p -> System.out.println("      ├── " + p));
        });

        // ── Related files section ────────────────────────────────────────────────
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║         RELATED CODE FILES           ║");
        System.out.println("╚══════════════════════════════════════╝");

        Map<String, List<Path>> relatedFiles = allFiles.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (relatedFiles.isEmpty()) {
            System.out.println("\n  No related files found.");
        } else {
            relatedFiles.forEach((baseName, paths) -> {
                System.out.println("\n  📁 " + baseName);
                paths.stream()
                        .sorted()
                        .forEach(p -> System.out.println("      ├── " + p));
            });
        }

        // ── Summary ──────────────────────────────────────────────────────────────
        long totalFiles = allFiles.values().stream().mapToLong(List::size).sum();

        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║             SUMMARY                  ║");
        System.out.println("╚══════════════════════════════════════╝");

        // Per extension count in summary
        byExtension.forEach((ext, paths) -> {
            String icon = extIcons.getOrDefault(ext, "📄");
            System.out.printf("  %s  %-10s → %d files%n", icon, ext, paths.size());
        });

        System.out.println("  " + "─".repeat(38));
        System.out.printf("  📦 Total files found   : %d%n", totalFiles);
        System.out.printf("  🔗 Related file groups : %d%n", relatedFiles.size());
        System.out.printf("  🗂  Extensions found    : %d%n", byExtension.size());
        System.out.println("  " + "─".repeat(38));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private static String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot != -1) ? fileName.substring(dot).toLowerCase() : "";
    }

    private static String getBaseName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot != -1) ? fileName.substring(0, dot) : fileName;
    }

    private static boolean isSkippableDirectory(String dirName) {
        Set<String> skipDirs = Set.of(
                "$Recycle.Bin", "System Volume Information",
                "node_modules", ".git", "build", "dist", "target", ".idea");
        return dirName.startsWith("$") || skipDirs.contains(dirName);
    }

    // ── Entry Point ──────────────────────────────────────────────────────────
    public static void startScan() {
        System.out.println("Starting scan on all drives except C: ...\n");
        long start = System.currentTimeMillis();

        Map<String, List<Path>> result = findAllCodeFiles();
        printResults(result);

        long duration = System.currentTimeMillis() - start;
        System.out.printf("%nScan completed in: %d min %d sec%n",
                duration / 60000, (duration % 60000) / 1000);
    }
}
