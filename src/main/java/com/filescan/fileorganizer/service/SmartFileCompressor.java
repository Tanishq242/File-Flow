package com.filescan.fileorganizer.service;

import net.jpountz.lz4.*;

import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SmartFileCompressor {

    // Thread pool
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    // ===== ENUM FOR FILE TYPES =====
    enum FileCategory {
        TEXT, IMAGE, VIDEO, ARCHIVE, OTHER
    }

    // ===== MAIN METHOD =====
    public static void main(String[] args) throws Exception {
        String inputFile = "E:\\Pictures\\Saved Pictures\\New folder\\dc.JPG";   // 👉 your file
        File file = new File(inputFile);

        processFile(file); // 🔥 process only this file

        executor.shutdown();
    }

    // ===== SCAN FOLDER =====
    public static void scanFolder(File folder) {
        for (File file : folder.listFiles()) {

            if (file.isDirectory()) {
                scanFolder(file);
            } else {
                executor.submit(() -> {
                    try {
                        processFile(file);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    // ===== PROCESS FILE =====
    public static void processFile(File file) throws Exception {

        // Skip already compressed
        if (file.getName().endsWith(".lz4")) return;

        FileCategory type = detectType(file.getName());

        if (!shouldCompress(type, file.length())) {
            System.out.println("[SKIPPED] " + file.getName());
            return;
        }

        String outputPath = file.getAbsolutePath() + ".lz4";

        compressFile(file.getAbsolutePath(), outputPath);

        System.out.println("[COMPRESSED] " + file.getName());
    }

    // ===== FILE TYPE DETECTION =====
    public static FileCategory detectType(String fileName) {
        String lower = fileName.toLowerCase();

        if (lower.endsWith(".txt") || lower.endsWith(".json") || lower.endsWith(".xml") || lower.endsWith(".java"))
            return FileCategory.TEXT;

        if (lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".jpeg"))
            return FileCategory.IMAGE;

        if (lower.endsWith(".mp4") || lower.endsWith(".mkv"))
            return FileCategory.VIDEO;

        if (lower.endsWith(".zip") || lower.endsWith(".rar") || lower.endsWith(".7z"))
            return FileCategory.ARCHIVE;

        return FileCategory.OTHER;
    }

    // ===== DECISION LOGIC =====
    public static boolean shouldCompress(FileCategory type, long size) {

        // Skip already compressed formats
        if (type == FileCategory.IMAGE || type == FileCategory.VIDEO || type == FileCategory.ARCHIVE)
            return false;

        // Skip small files
        if (size < 10 * 1024)
            return false;

        return true;
    }

    // ===== LZ4 COMPRESSION =====
    public static void compressFile(String inputPath, String outputPath) throws IOException {

        byte[] data = Files.readAllBytes(new File(inputPath).toPath());

        LZ4Factory factory = LZ4Factory.fastestInstance();
        LZ4Compressor compressor = factory.fastCompressor();

        int maxLen = compressor.maxCompressedLength(data.length);
        byte[] compressed = new byte[maxLen];

        int compressedSize = compressor.compress(data, 0, data.length, compressed, 0, maxLen);

        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(outputPath))) {
            dos.writeInt(data.length);          // original size
            dos.writeInt(compressedSize);       // compressed size
            dos.write(compressed, 0, compressedSize);
        }

        // Optional: delete original if compressed smaller
        if (compressedSize < data.length) {
            // new File(inputPath).delete();
        }
    }

    // ===== LZ4 DECOMPRESSION =====
    public static void decompressFile(String inputPath, String outputPath) throws IOException {

        try (DataInputStream dis = new DataInputStream(new FileInputStream(inputPath))) {

            int originalSize = dis.readInt();
            int compressedSize = dis.readInt();

            byte[] compressed = new byte[compressedSize];
            dis.readFully(compressed);

            LZ4Factory factory = LZ4Factory.fastestInstance();
            LZ4SafeDecompressor decompressor = factory.safeDecompressor();

            byte[] restored = new byte[originalSize];
            decompressor.decompress(compressed, 0, compressedSize, restored, 0);

            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                fos.write(restored);
            }
        }
    }
}