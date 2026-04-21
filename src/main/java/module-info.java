module com.filescan.fileorganizer {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires jdk.compiler;
    requires com.github.oshi;
    requires java.sql;
    requires org.slf4j;
    requires org.apache.pdfbox;
    requires javafx.swing;
    requires mp3agic;
    requires org.apache.commons.io;
    requires org.lz4.java;


    opens com.filescan.fileorganizer to javafx.fxml;
    exports com.filescan.fileorganizer;

    // ── THE CRITICAL FIX: open your UI packages to JavaFX ───
    opens com.filescan.fileorganizer.ui.components to javafx.graphics, javafx.fxml;

    // ── Export if other modules need access ─────────────────
    exports com.filescan.fileorganizer.ui.components;

    exports com.filescan.fileorganizer.testProgram;
}