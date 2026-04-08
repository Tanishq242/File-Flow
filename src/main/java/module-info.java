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


    opens com.filescan.fileorganizer to javafx.fxml;
    exports com.filescan.fileorganizer;
}