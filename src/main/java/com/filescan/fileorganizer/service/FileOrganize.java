package com.filescan.fileorganizer.service;

import java.io.File;

import com.filescan.fileorganizer.model.FileType;
import org.apache.commons.io.FilenameUtils;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

public class FileOrganize {
    public enum MoveStatus {
        SUCCESS,
        SOURCE_NOT_FOUND,
        DESTINATION_DIR_NOT_FOUND,
        DESTINATION_ALREADY_EXISTS,
        PERMISSION_DENIED,
        FAILED
    }

    public static void organize(List<Path> src) {
        for (Path i : src) {

            if (Files.isDirectory(i)) {
                try (Stream<Path> files = Files.walk(i, 1)) { // ✅ depth 1 = only direct children, no recursion
                    files.filter(Files::isRegularFile)        // ✅ only files, skip folders
                            .forEach(file -> {
                                Path parentDir = file.getParent();
                                processFile(parentDir, file);
                            });
                } catch (IOException e) {
                    System.out.println("❌ Error scanning directory: " + i);
                }

            } else if (Files.isRegularFile(i)) {
                Path parentDir = i.getParent();
                processFile(parentDir, i);
            }
        }

        System.out.println("✅ FILE ORGANIZING IS FINISHED");
    }

    // ✅ Extracted helper to process a single file
    private static void processFile(Path path, Path file) {
        if (!categoryExist(path)) {
            createCategory(path, "DOCUMENT"); // create initial category
        }
        createDestination(path, file);
    }

    public static boolean categoryExist(Path uri) {
        // ✅ Dynamically check using enum names instead of hardcoded folder names
        for (FileType type : FileType.values()) {
            if (type == FileType.UNKNOWN) continue;

            if (Files.exists(Paths.get(uri + "\\" + type.name()))) {
                return true;
            }
        }
        return false;
    }

    public static boolean subCategoryExist(Path uri, String ext) {
        // ✅ Check against actual enum names instead of hardcoded strings
        for (FileType type : FileType.values()) {
            if (type == FileType.UNKNOWN) continue;

            String subCatPath = uri + "\\" + type.name() + "\\" + ext.toUpperCase();
            if (Files.exists(Paths.get(subCatPath))) {
                return true;
            }
        }
        return false;
    }

    public static void createCategory(Path uri, String dirName) {
        try {
            Files.createDirectories(Paths.get(uri + "\\" + dirName));
        } catch (Exception e) {
            throw new RuntimeException(e + " | Unable to create " + dirName + " Directory");
        }
    }

    public static void createSubCategory(Path uri, String ext) {
        String dirName = ext.toUpperCase();
        try {
            Files.createDirectories(Paths.get(uri + "\\" + dirName));
        } catch (Exception e) {
            throw new RuntimeException(e + " | Unable to create " + dirName + " Directory");
        }
    }

    public static void createDestination(Path path, Path i) {
        String ext = FilenameUtils.getExtension(i.getFileName().toString()); // e.g. "jpeg" (no dot)
        String extUpper = ext.toUpperCase();                                  // e.g. "JPEG"
        String fileName = i.getFileName().toString();                         // e.g. "photo.jpeg"

        // ✅ Use your FileType enum
        FileType fileType = FileType.fromPath(i);

        // ✅ Handle unknown/null type
        if (fileType == null || fileType == FileType.UNKNOWN) {
            System.out.println("⚠️ Skipping unknown file type: " + fileName);
            return;
        }

        // ✅ Use enum name as category folder (IMAGE → "IMAGE")
        String category = fileType.name(); // MUSIC, IMAGE, VIDEO, DOCUMENT, CODE

        // ✅ Full destination path including filename
        String destinationPath = path + "\\" + category + "\\" + extUpper + "\\" + fileName;

        System.out.println("📁 Category : " + category);
        System.out.println("📄 File     : " + fileName);
        System.out.println("📍 From     : " + i.toAbsolutePath());
        System.out.println("📍 To       : " + destinationPath);

        // ✅ Create subcategory folder if not exists
        if (!subCategoryExist(path, ext)) {
            createSubCategory( Paths.get(path + "\\" + category), ext);
        }

        MoveStatus status = moveFile(i.toAbsolutePath().toString(), destinationPath, false);
        System.out.println("Move status: " + status + " | File: " + fileName);
    }

    public static MoveStatus moveFile(String sourcePath, String destinationPath, boolean overwrite) {

        Path source = Paths.get(sourcePath);
        Path destination = Paths.get(destinationPath);

        // ✅ Check exists — works for both file and directory
        if (!Files.exists(source)) {
            System.out.println("❌ Source not found: " + sourcePath);
            return MoveStatus.SOURCE_NOT_FOUND;
        }

        // ✅ Check if it's a file OR directory
        boolean isFile = Files.isRegularFile(source);
        boolean isDirectory = Files.isDirectory(source);

        if (!isFile && !isDirectory) {
            System.out.println("❌ Source is neither a file nor a directory: " + sourcePath);
            return MoveStatus.SOURCE_NOT_FOUND;
        }

        // Check destination directory exists
        Path destinationDir = destination.getParent();
        if (destinationDir != null && !Files.exists(destinationDir)) {
            System.out.println("❌ Destination directory not found: " + destinationDir);
            return MoveStatus.DESTINATION_DIR_NOT_FOUND;
        }

        // Check destination already exists
        if (Files.exists(destination) && !overwrite) {
            System.out.println("⚠️ Destination already exists: " + destinationPath);
            return MoveStatus.DESTINATION_ALREADY_EXISTS;
        }

        // Move file or directory
        try {
            if (overwrite) {
                Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, destination);
            }
            System.out.println("✅ Moved successfully: " + sourcePath + " → " + destinationPath);
            return MoveStatus.SUCCESS;

        } catch (AccessDeniedException e) {
            System.out.println("❌ Permission denied: " + e.getMessage());
            return MoveStatus.PERMISSION_DENIED;

        } catch (IOException e) {
            System.out.println("❌ Failed to move: " + e.getMessage());
            return MoveStatus.FAILED;
        }
    }
}