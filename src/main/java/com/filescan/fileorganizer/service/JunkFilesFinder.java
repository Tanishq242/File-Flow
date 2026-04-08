package com.filescan.fileorganizer.service;

import java.io.File;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JunkFilesFinder {
    public static Map<File, List<File>> junkFiles = new HashMap<>();
    private static final String[] JUNK_PATHS = {
            "C:\\Windows\\Temp",
            System.getProperty("user.home") + "\\AppData\\Local\\Temp",
            System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\Windows\\INetCache",
            System.getProperty("user.home") + "\\AppData\\Local\\CrashDumps",
            "C:\\Windows\\Prefetch",
            "C:\\Windows\\SoftwareDistribution\\Download",
            "C:\\Windows\\Logs",
            "C:\\ProgramData\\Microsoft\\Windows\\WER\\ReportArchive",
            "C:\\ProgramData\\Microsoft\\Windows\\WER\\ReportQueue",
            "C:\\$Recycle.Bin"
    };

    public static long totalSize = 0;
    public static int totalFiles = 0;
    public static String junkFileSize = null;

    // ── Recursive directory scanner ─────────────────────────────────────────
    private static long scanDirectory(File dir, List<File> collected) {
        long size = 0;
        File[] entries = dir.listFiles();
        if (entries == null) return size;          // permission denied

        for (File entry : entries) {
            collected.add(entry);
            if (entry.isDirectory()) {
                size += scanDirectory(entry, collected);
            } else {
                size += entry.length();
            }
        }
        return size;
    }

    // ── Human-readable file size ────────────────────────────────────────────
    private static String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int idx = (int) (Math.log10(bytes) / Math.log10(1024));
        idx = Math.min(idx, units.length - 1);
        double value = bytes / Math.pow(1024, idx);
        return new DecimalFormat("#,##0.##").format(value) + " " + units[idx];
    }

    // ── Truncate long paths for display ────────────────────────────────────
    private static String truncate(String s, int max) {
        return s.length() <= max ? s : "..." + s.substring(s.length() - (max - 3));
    }

    public static void startScan() {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║          JUNK FILE SCANNER - C: Drive            ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println();

        for (String path : JUNK_PATHS) {
            File dir = new File(path);
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("📁 Location : " + path);

            if (!dir.exists()) {
                System.out.println("   [SKIPPED] Directory does not exist.");
                System.out.println();
                continue;
            }
            if (!dir.canRead()) {
                System.out.println("   [SKIPPED] Access denied (run as Administrator).");
                System.out.println();
                continue;
            }

            List<File> files = new ArrayList<>();
            long dirSize = scanDirectory(dir, files);
            junkFiles.put(dir, files);

            System.out.printf("   Files found : %,d%n", files.size());
            System.out.printf("   Total size  : %s%n", formatSize(dirSize));
            System.out.println();

            // Print each file
            for (File f : files) {
                String type = f.isDirectory() ? "[DIR] " : "[FILE]";
                System.out.printf("   %s %-60s %10s%n",
                        type,
                        truncate(f.getAbsolutePath(), 60),
                        formatSize(f.length()));
            }

            totalFiles += files.size();
            totalSize += dirSize;
            System.out.println();
        }

        junkFileSize = formatSize(totalSize);
        // ── Summary ──────────────────────────────────────────────────────────
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("SUMMARY");
        System.out.printf("  Total junk files found : %,d%n", totalFiles);
        System.out.printf("  Total space consumed   : %s%n", junkFileSize);
        System.out.println();
        System.out.println("Tip: Run this program as Administrator to scan all");
        System.out.println("     protected locations (e.g. Prefetch, $Recycle.Bin).");

        for (File i : junkFiles.keySet()) {
            System.out.println(i);
        }
    }
}