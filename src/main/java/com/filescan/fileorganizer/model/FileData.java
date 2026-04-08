package com.filescan.fileorganizer.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.nio.file.Path;

public class FileData {

    private final BooleanProperty selected;
    private final StringProperty name;
    private final StringProperty type;
    private final StringProperty size;
    private final StringProperty path;

    public FileData(String name, String type, String size, String path) {
        this.selected = new SimpleBooleanProperty(false);
        this.name = new SimpleStringProperty(name);
        this.type = new SimpleStringProperty(type);
        this.size = new SimpleStringProperty(size);
        this.path = new SimpleStringProperty(path);
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }
    public StringProperty nameProperty() { return name; }
    public StringProperty typeProperty() { return type; }
    public StringProperty sizeProperty() { return size; }
    public StringProperty pathProperty() { return path; }
}