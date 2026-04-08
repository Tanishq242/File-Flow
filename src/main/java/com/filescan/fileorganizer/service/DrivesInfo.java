package com.filescan.fileorganizer.service;

import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class DrivesInfo {
    static int count = 0;
    public static void listDrive() {
        SystemInfo si = new SystemInfo();

        for (HWDiskStore disk : si.getHardware().getDiskStores()) {
            System.out.println("Model: " + disk.getModel());
            System.out.println("Size: " + disk.getSize() / (1024 * 1024 * 1024));
            System.out.println("Partitions: " + disk.getPartitions());
            System.out.println("------------------");
        }
    }

    public static void pathCount() {
        try {
            // 4. Best way — never crashes on permission denied
            File[] drives = File.listRoots();

            for (File drive : drives) {
                if (drive.getAbsolutePath().contains("C")) continue;
                Files.walkFileTree(Path.of(drive.getAbsolutePath()), new SimpleFileVisitor<>() {

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        count++;
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException e) {
                        // silently skip folders we can't access
                        return FileVisitResult.CONTINUE;
                    }
                });

                System.out.println("Directory Count in drive " + drive.getAbsolutePath() + " is " + count);
                count = 0;
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
