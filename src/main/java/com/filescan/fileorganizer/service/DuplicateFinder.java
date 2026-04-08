package com.filescan.fileorganizer.service;

import com.filescan.fileorganizer.model.DuplicateFile;
import com.filescan.fileorganizer.model.FileType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.*;

public class DuplicateFinder {
    public static Map<FileType, List<List<Path>>> filesByType = new EnumMap<>(FileType.class);
    // ── Phase 1: collect all files grouped by size ────────────
    public static Map<Long, List<Path>> groupBySize(Path root) throws IOException {
        Map<Long, List<Path>> sizeMap = new HashMap<>();

        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                long size = attrs.size();
                if (size == 0) return FileVisitResult.CONTINUE; // skip empty files

                sizeMap.computeIfAbsent(size, k -> new ArrayList<>()).add(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException e) {
                return FileVisitResult.CONTINUE;
            }
        });

        // Keep only sizes that have more than one file
        sizeMap.entrySet().removeIf(e -> e.getValue().size() < 2);
        return sizeMap;
    }

    // ── Phase 2: hash first 4KB to quickly eliminate non-duplicates ──
    public static Map<String, List<Path>> groupByHeader(List<Path> candidates) {
        Map<String, List<Path>> headerMap = new HashMap<>();

        for (Path file : candidates) {
            String hash = hashHead(file);
            if (hash != null) {
                headerMap.computeIfAbsent(hash, k -> new ArrayList<>()).add(file);
            }
        }

        headerMap.entrySet().removeIf(e -> e.getValue().size() < 2);
        return headerMap;
    }

    // ── Phase 3: full SHA-256 hash — only called on survivors ────
    public static Map<String, List<Path>> groupByFullHash(List<Path> candidates) {
        Map<String, List<Path>> hashMap = new HashMap<>();

        for (Path file : candidates) {
            String hash = hashFull(file);
            if (hash != null) {
                hashMap.computeIfAbsent(hash, k -> new ArrayList<>()).add(file);
            }
        }

        hashMap.entrySet().removeIf(e -> e.getValue().size() < 2);
        return hashMap;
    }

    // ── Main logic: run all 3 phases ─────────────────────────
    public static List<List<Path>> findDuplicates(Path root) throws IOException {
        List<List<Path>> duplicateGroups = new ArrayList<>();

        // Phase 1 — free, no I/O
        Map<Long, List<Path>> bySize = groupBySize(root);
        System.out.println("Size matches: " + bySize.values().stream().mapToInt(List::size).sum() + " files");

        for (List<Path> sameSize : bySize.values()) {

            // 🔥 Detect file type
            FileType type = FileType.fromPath(sameSize.get(0));

            Map<String, List<Path>> byHeader;

            // 🔥 Apply header check ONLY for images
            if (type == FileType.IMAGE) {
                byHeader = groupByHeader(sameSize);
            } else {
                // Skip header phase for music, video, documents
                byHeader = new HashMap<>();
                byHeader.put("all", sameSize);
            }

            for (List<Path> sameHeader : byHeader.values()) {

                // Phase 3 — full hash
                Map<String, List<Path>> byHash = groupByFullHash(sameHeader);

                duplicateGroups.addAll(byHash.values());
            }
        }

        return duplicateGroups;
    }

    // ── Hash helpers ─────────────────────────────────────────

    // Reads only the first 4KB — fast pre-filter
    private static String hashHead(Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            byte[] buf = new byte[4096];
            int read = is.read(buf);
            if (read < 0) return null;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(buf, 0, read);
            return toHex(md.digest());
        } catch (Exception e) {
            return null;
        }
    }

    // Reads the entire file
    private static String hashFull(Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[65_536]; // 64KB chunks
            int n;
            while ((n = is.read(buf)) != -1) md.update(buf, 0, n);
            return toHex(md.digest());
        } catch (Exception e) {
            return null;
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // ── Print results ─────────────────────────────────────────

    public static void showDuplicate(String uri) throws IOException {

//        Map<FileType, List<List<Path>>> filesByType = new EnumMap<>(FileType.class);

        Path root = Path.of(uri);
        List<List<Path>> duplicates = findDuplicates(root);

        for (List<Path> group : duplicates) {
            if (group.isEmpty()) continue;

            FileType type = FileType.fromPath(group.getFirst());

            if (type != null) {
                filesByType
                        .computeIfAbsent(type, t -> new ArrayList<>())
                        .add(group);
            }
        }

        System.out.println("Final Map: " + filesByType.keySet());
    }
}
