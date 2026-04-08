package com.filescan.fileorganizer.service;

import java.sql.Connection;
import java.sql.DriverManager;

public class LocalDB {
    public static Connection connect() {
        try {
            String url = "jdbc:sqlite:fileorganizer.db";
            return DriverManager.getConnection(url);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void createTable() {}

    public static void insertFile() {}

    public static void deleteFile() {}

    public static void fetchFiles() {}
}
