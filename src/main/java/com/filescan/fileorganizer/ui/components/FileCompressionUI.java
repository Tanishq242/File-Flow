package com.filescan.fileorganizer.ui.components;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.zip.*;

public class FileCompressionUI{

    // ── Colour palette (matches your screenshot) ──────────────────────────────
    private static final String BG_MAIN        = "#000000";
    private static final String BG_PANEL       = "#0a0a0a";
    private static final String BG_CONTROL     = "#0d0d0d";
    private static final String BG_TABLE_EVEN  = "#080808";
    private static final String BG_TABLE_ODD   = "#0d0d0d";
    private static final String BLUE_BTN       = "#3d6ef5";
    private static final String BLUE_BTN_HOVER = "#2d5ee0";
    private static final String BLUE_LIGHT     = "#5b87f7";
    private static final String TEXT_WHITE      = "#ffffff";
    private static final String TEXT_GRAY       = "#888888";
    private static final String BORDER_DARK     = "#1a1a1a";
    private static final String GREEN           = "#39d353";
    private static final String ORANGE          = "#f0a500";
    private static final String RED             = "#e05252";

    private static final long MB         = 1024L * 1024;
    private static final long SCAN_LIMIT = 8_000;

    // ── State ─────────────────────────────────────────────────────────────────
    private final ObservableList<FileEntry> entries = FXCollections.observableArrayList();
    private volatile boolean scanCancelled     = false;
    private volatile boolean compressCancelled = false;
    private final AtomicLong    totalSaved      = new AtomicLong(0);
    private final AtomicInteger compressedCount = new AtomicInteger(0);

    // ── UI refs ───────────────────────────────────────────────────────────────
    private ComboBox<String>     driveCombo;
    private Button               startScanBtn;
    private Button               compressBtn;
    private Button               cancelBtn;
    private TableView<FileEntry> table;
    private Label                statusLabel;
    private Label                summaryLabel;
    private ProgressBar          progressBar;
    private Label                progressPctLabel;
    private Label                statScanned;
    private Label                statCompressed;
    private Label                statSaved;
    private Slider               minSizeSlider;
    private Label                minSizeLabel;

    // ══════════════════════════════════════════════════════════════════════════

    public Parent openUI() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG_MAIN + ";");
        root.setTop(buildTopBar());
        root.setCenter(buildMain());
        root.setBottom(buildBottomBar());
        return root;
    }

//    @Override
//    public void start(Stage stage) {
//        stage.setTitle("File Compression");
//        stage.setMinWidth(920);
//        stage.setMinHeight(640);
//
//        BorderPane root = new BorderPane();
//        root.setStyle("-fx-background-color: " + BG_MAIN + ";");
//        root.setTop(buildTopBar());
//        root.setCenter(buildMain());
//        root.setBottom(buildBottomBar());
//
//        Scene scene = new Scene(root, 1050, 680);
//        stage.setScene(scene);
//        stage.show();
//    }

    // ─── Top bar ──────────────────────────────────────────────────────────────
    private VBox buildTopBar() {
        VBox top = new VBox(14);
        top.setPadding(new Insets(28, 30, 16, 30));
        top.setStyle("-fx-background-color: " + BG_MAIN + ";");

        Label title = new Label("File Compression");
        title.setStyle(
                "-fx-font-size: 22px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + TEXT_WHITE + "; -fx-font-family: 'Segoe UI', Arial;"
        );

        // Drive selector row
        HBox driveRow = new HBox(14);
        driveRow.setAlignment(Pos.CENTER_LEFT);

        Label driveLabel = new Label("Select the drive");
        driveLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TEXT_WHITE + "; -fx-font-family: 'Segoe UI', Arial;");

        driveCombo = new ComboBox<>();
        driveCombo.setPrefWidth(460);
        driveCombo.setPrefHeight(40);
        driveCombo.setStyle(
                "-fx-background-color: " + BG_CONTROL + ";" +
                        "-fx-border-color: " + BORDER_DARK + ";" +
                        "-fx-border-radius: 6; -fx-background-radius: 6;" +
                        "-fx-text-fill: " + TEXT_WHITE + ";" +
                        "-fx-font-family: 'Segoe UI', Arial; -fx-font-size: 13px;"
        );
        populateDrives();

        Label sizeLabel = new Label("Min size:");
        sizeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + TEXT_GRAY + "; -fx-font-family: 'Segoe UI', Arial;");
        minSizeSlider = new Slider(1, 200, 10);
        minSizeSlider.setPrefWidth(130);
        minSizeLabel = new Label("10 MB");
        minSizeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + BLUE_LIGHT + "; -fx-font-family: 'Segoe UI', Arial; -fx-min-width: 50;");
        minSizeSlider.valueProperty().addListener((ob, o, n) -> minSizeLabel.setText(n.intValue() + " MB"));

        driveRow.getChildren().addAll(driveLabel, driveCombo, sizeLabel, minSizeSlider, minSizeLabel);

        // Buttons row
        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        startScanBtn   = blueButton("Start Scan");
        compressBtn    = blueButton("Compress Selected");
        cancelBtn      = grayButton("Cancel");
        Button selAll  = grayButton("Select All");
        Button deselAll= grayButton("Deselect All");

        compressBtn.setDisable(true);
        cancelBtn.setDisable(true);

        startScanBtn.setOnAction(e    -> startScan());
        compressBtn.setOnAction(e     -> startCompression());
        cancelBtn.setOnAction(e       -> { scanCancelled = true; compressCancelled = true; });
        selAll.setOnAction(e          -> entries.forEach(fe -> fe.selectedProperty().set(true)));
        deselAll.setOnAction(e        -> entries.forEach(fe -> fe.selectedProperty().set(false)));

        btnRow.getChildren().addAll(startScanBtn, compressBtn, cancelBtn, selAll, deselAll);
        top.getChildren().addAll(title, driveRow, btnRow);
        return top;
    }

    // ─── Main ────────────────────────────────────────────────────────────────
    private HBox buildMain() {
        HBox main = new HBox(0);

        VBox tableBox = new VBox(8);
        tableBox.setPadding(new Insets(0, 0, 0, 30));
        HBox.setHgrow(tableBox, Priority.ALWAYS);

        summaryLabel = new Label("No scan performed yet.");
        summaryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + TEXT_GRAY + "; -fx-font-family: 'Segoe UI', Arial;");

        table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        tableBox.getChildren().addAll(summaryLabel, table);

        main.getChildren().addAll(tableBox, buildStatsPanel());
        return main;
    }

    // ─── Stats panel ─────────────────────────────────────────────────────────
    private VBox buildStatsPanel() {
        VBox panel = new VBox(18);
        panel.setPrefWidth(195);
        panel.setPadding(new Insets(4, 20, 20, 16));
        panel.setStyle("-fx-background-color: " + BG_MAIN + ";");

        Label header = new Label("Session Stats");
        header.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_WHITE + "; -fx-font-family: 'Segoe UI', Arial;");

        statScanned    = grayLabel("Scanned:  0 files");
        statCompressed = grayLabel("Compressed:  0 files");
        statSaved      = grayLabel("Saved:  0 B");

        Label pLabel = grayLabel("Progress");
        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(10);
        progressBar.setStyle(
                "-fx-accent: " + BLUE_BTN + ";" +
                        "-fx-background-color: " + BORDER_DARK + ";" +
                        "-fx-background-radius: 4; -fx-border-radius: 4;"
        );
        progressPctLabel = grayLabel("0%");

        Region filler = new Region();
        VBox.setVgrow(filler, Priority.ALWAYS);
        panel.getChildren().addAll(header, statScanned, statCompressed, statSaved, filler, pLabel, progressBar, progressPctLabel);
        return panel;
    }

    // ─── Bottom bar ──────────────────────────────────────────────────────────
    private HBox buildBottomBar() {
        HBox bar = new HBox();
        bar.setPadding(new Insets(8, 30, 10, 30));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: " + BG_MAIN + "; -fx-border-color: " + BORDER_DARK + "; -fx-border-width: 1 0 0 0;");
        statusLabel = grayLabel("Ready — select a drive and click Start Scan.");
        bar.getChildren().add(statusLabel);
        return bar;
    }

    // ─── Table ───────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private TableView<FileEntry> buildTable() {
        TableView<FileEntry> tv = new TableView<>(entries);
        tv.setEditable(true);
        tv.setStyle(
                "-fx-background-color: " + BG_PANEL + ";" +
                        "-fx-table-cell-border-color: " + BORDER_DARK + ";" +
                        "-fx-border-color: " + BORDER_DARK + ";" +
                        "-fx-border-radius: 6; -fx-background-radius: 6;"
        );
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        Label ph = new Label("No files found. Select a drive and click Start Scan.");
        ph.setStyle("-fx-text-fill: " + TEXT_GRAY + "; -fx-font-family: 'Segoe UI', Arial;");
        tv.setPlaceholder(ph);

        // Checkbox
        TableColumn<FileEntry, Boolean> selCol = new TableColumn<>("");
        selCol.setMinWidth(36); selCol.setMaxWidth(36);
        selCol.setCellValueFactory(c -> c.getValue().selectedProperty());
        selCol.setCellFactory(CheckBoxTableCell.forTableColumn(selCol));
        selCol.setEditable(true);

        TableColumn<FileEntry, String> nameCol = textCol("File Name", "name", TEXT_WHITE, true);
        TableColumn<FileEntry, String> pathCol = textCol("Path", "path", TEXT_GRAY, false);

        TableColumn<FileEntry, String> sizeCol = textCol("Size", "sizeDisplay", ORANGE, false);
        sizeCol.setMinWidth(80); sizeCol.setMaxWidth(100);

        // Priority (colour coded)
        TableColumn<FileEntry, String> prioCol = new TableColumn<>("Priority");
        prioCol.setMinWidth(80); prioCol.setMaxWidth(90);
        prioCol.setCellValueFactory(c -> c.getValue().priorityProperty());
        prioCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label l = new Label(item);
                String color = switch (item) {
                    case "HIGH"   -> RED;
                    case "MEDIUM" -> ORANGE;
                    default       -> GREEN;
                };
                l.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + color + "; -fx-font-family: 'Segoe UI', Arial;");
                setGraphic(l); setText(null); setAlignment(Pos.CENTER);
            }
        });

        TableColumn<FileEntry, String> savCol = textCol("Est. Saving", "estimatedSaving", GREEN, false);
        savCol.setMinWidth(90); savCol.setMaxWidth(110);

        // Status (colour coded)
        TableColumn<FileEntry, String> statCol = new TableColumn<>("Status");
        statCol.setMinWidth(110); statCol.setMaxWidth(130);
        statCol.setCellValueFactory(c -> c.getValue().statusProperty());
        statCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label l = new Label(item);
                String color = switch (item) {
                    case "Compressed"  -> GREEN;
                    case "Compressing" -> BLUE_LIGHT;
                    case "Failed"      -> RED;
                    case "Skipped"     -> ORANGE;
                    default            -> TEXT_GRAY;
                };
                l.setStyle("-fx-font-size: 11px; -fx-text-fill: " + color + "; -fx-font-family: 'Segoe UI', Arial;");
                setGraphic(l); setText(null);
            }
        });

        tv.getColumns().addAll(selCol, nameCol, pathCol, sizeCol, prioCol, savCol, statCol);
        tv.setRowFactory(t -> new TableRow<>() {
            @Override protected void updateItem(FileEntry item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color: " + ((getIndex() % 2 == 0) ? BG_TABLE_EVEN : BG_TABLE_ODD) + ";");
            }
        });
        return tv;
    }

    // ─── Scan ────────────────────────────────────────────────────────────────
    private void startScan() {
        String sel = driveCombo.getValue();
        if (sel == null || sel.isBlank()) { showAlert("No Drive Selected", "Please select a drive."); return; }
        String rootStr = sel.split("\\s+")[0];
        Path root = Paths.get(rootStr);
        if (!Files.isDirectory(root)) { showAlert("Invalid Path", "Cannot access: " + rootStr); return; }

        long minBytes = (long)(minSizeSlider.getValue() * MB);
        entries.clear();
        totalSaved.set(0); compressedCount.set(0);
        updateStats(0, 0, 0);
        scanCancelled = false;
        setScanUI(true);
        setStatus("Scanning " + rootStr + " …", TEXT_GRAY);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        progressPctLabel.setText("…");

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                AtomicLong count = new AtomicLong(0);
                Files.walkFileTree(root, new SimpleFileVisitor<>() {
                    @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (scanCancelled) return FileVisitResult.TERMINATE;
                        if (count.incrementAndGet() > SCAN_LIMIT) return FileVisitResult.TERMINATE;
                        long size = attrs.size();
                        if (size >= minBytes && isCompressible(file)) {
                            FileEntry fe = new FileEntry(file, size);
                            Platform.runLater(() -> {
                                entries.add(fe);
                                updateStats(entries.size(), compressedCount.get(), totalSaved.get());
                                summaryLabel.setText("Found " + entries.size() + " file(s) — scanning…");
                            });
                        }
                        return FileVisitResult.CONTINUE;
                    }
                    @Override public FileVisitResult visitFileFailed(Path file, IOException exc) { return FileVisitResult.CONTINUE; }
                });
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            setScanUI(false);
            progressBar.setProgress(0); progressPctLabel.setText("0%");
            int n = entries.size();
            long totalSize = entries.stream().mapToLong(FileEntry::getSize).sum();
            long high = entries.stream().filter(f -> "HIGH".equals(f.getPriority())).count();
            summaryLabel.setText(n + " files found  |  Total: " + humanSize(totalSize) + "  |  High priority: " + high);
            if (scanCancelled) setStatus("Scan cancelled. " + n + " file(s) found.", ORANGE);
            else               setStatus("Scan complete — " + n + " compressible file(s) found.", GREEN);
            compressBtn.setDisable(n == 0);
        });
        task.setOnFailed(e -> { setScanUI(false); progressBar.setProgress(0); setStatus("Scan error: " + task.getException().getMessage(), RED); });
        new Thread(task, "scan") {{ setDaemon(true); start(); }};
    }

    // ─── Compress ────────────────────────────────────────────────────────────
    private void startCompression() {
        List<FileEntry> sel = entries.stream()
                .filter(fe -> fe.selectedProperty().get() && "Pending".equals(fe.statusProperty().get()))
                .toList();
        if (sel.isEmpty()) { showAlert("Nothing Selected", "Check at least one Pending file."); return; }

        compressCancelled = false;
        compressBtn.setDisable(true); cancelBtn.setDisable(false);
        progressBar.setProgress(0);
        setStatus("Compressing " + sel.size() + " file(s)…", BLUE_LIGHT);

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                int total = sel.size();
                for (int i = 0; i < total; i++) {
                    if (compressCancelled) break;
                    FileEntry fe = sel.get(i);
                    final int idx = i;
                    Platform.runLater(() -> {
                        fe.statusProperty().set("Compressing");
                        progressBar.setProgress((double) idx / total);
                        progressPctLabel.setText(pct(idx, total) + "%");
                        setStatus("Compressing: " + fe.getName() + "  [" + (idx + 1) + "/" + total + "]", BLUE_LIGHT);
                        table.refresh();
                    });
                    long saved = compressFile(fe);
                    Platform.runLater(() -> {
                        if (saved > 0) {
                            fe.statusProperty().set("Compressed");
                            fe.setEstimatedSaving("▼ " + humanSize(saved));
                            updateStats(entries.size(), compressedCount.incrementAndGet(), totalSaved.addAndGet(saved));
                        } else {
                            fe.statusProperty().set(saved == 0 ? "Skipped" : "Failed");
                        }
                        table.refresh();
                    });
                    Thread.sleep(80);
                }
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            progressBar.setProgress(1.0); progressPctLabel.setText("100%");
            compressBtn.setDisable(false); cancelBtn.setDisable(true);
            if (compressCancelled) setStatus("Compression cancelled.", ORANGE);
            else setStatus("Done — " + compressedCount.get() + " file(s) compressed, " + humanSize(totalSaved.get()) + " saved.", GREEN);
        });
        task.setOnFailed(e -> { compressBtn.setDisable(false); cancelBtn.setDisable(true); setStatus("Error: " + task.getException().getMessage(), RED); });
        new Thread(task, "compress") {{ setDaemon(true); start(); }};
    }

    private long compressFile(FileEntry fe) {
        Path src = Paths.get(fe.getPath());
        Path dst = src.resolveSibling(src.getFileName() + ".drivepress.zip");
        try {
            try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(dst)));
                 InputStream in     = new BufferedInputStream(Files.newInputStream(src))) {
                zos.setLevel(Deflater.BEST_COMPRESSION);
                zos.putNextEntry(new ZipEntry(src.getFileName().toString()));
                byte[] buf = new byte[8192]; int n;
                while ((n = in.read(buf)) != -1) zos.write(buf, 0, n);
                zos.closeEntry();
            }
            long saved = fe.getSize() - Files.size(dst);
            if (saved > 0) return saved;
            Files.deleteIfExists(dst); return 0;
        } catch (IOException ex) {
            try { Files.deleteIfExists(dst); } catch (IOException ignored) {}
            return -1;
        }
    }

    // ─── Utilities ───────────────────────────────────────────────────────────
    private void populateDrives() {
        File[] roots = File.listRoots();
        if (roots == null) return;
        for (File r : roots) {
            String label = r.getAbsolutePath();
            try { FileStore fs = Files.getFileStore(r.toPath()); label += "  " + fs.name() + "  (" + humanSize(fs.getTotalSpace()) + ")"; } catch (IOException ignored) {}
            driveCombo.getItems().add(label);
        }
        if (!driveCombo.getItems().isEmpty()) driveCombo.getSelectionModel().selectFirst();
    }

    private boolean isCompressible(Path p) {
        String n = p.getFileName().toString().toLowerCase();
        return !n.endsWith(".zip") && !n.endsWith(".gz") && !n.endsWith(".7z") && !n.endsWith(".rar")
                && !n.endsWith(".mp4") && !n.endsWith(".mkv") && !n.endsWith(".mov")
                && !n.endsWith(".jpg") && !n.endsWith(".jpeg") && !n.endsWith(".png")
                && !n.endsWith(".mp3") && !n.endsWith(".bz2");
    }

    private void setScanUI(boolean scanning) {
        startScanBtn.setDisable(scanning);
        driveCombo.setDisable(scanning);
        minSizeSlider.setDisable(scanning);
        cancelBtn.setDisable(!scanning);
        if (!scanning) compressBtn.setDisable(entries.isEmpty());
    }

    private void setStatus(String msg, String color) {
        Platform.runLater(() -> {
            statusLabel.setText(msg);
            statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + color + "; -fx-font-family: 'Segoe UI', Arial;");
        });
    }

    private void updateStats(int scanned, int compressed, long saved) {
        statScanned.setText("Scanned:  " + scanned + " files");
        statCompressed.setText("Compressed:  " + compressed + " files");
        statSaved.setText("Saved:  " + humanSize(saved));
    }

    private static String humanSize(long b) {
        if (b >= 1024L * 1024 * 1024) return String.format("%.2f GB", b / (1024.0 * 1024 * 1024));
        if (b >= MB)   return String.format("%.1f MB", b / (double) MB);
        if (b >= 1024) return String.format("%.0f KB", b / 1024.0);
        return b + " B";
    }

    private static int pct(int i, int total) { return total == 0 ? 0 : (int)(100.0 * i / total); }

    private static Button blueButton(String text) {
        Button b = new Button(text);
        b.setStyle(
                "-fx-background-color: " + BLUE_BTN + "; -fx-text-fill: " + TEXT_WHITE + ";" +
                        "-fx-font-family: 'Segoe UI', Arial; -fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 6; -fx-padding: 8 20 8 20; -fx-cursor: hand;"
        );
        b.setOnMouseEntered(e -> b.setStyle(b.getStyle().replace(BLUE_BTN, BLUE_BTN_HOVER)));
        b.setOnMouseExited(e  -> b.setStyle(b.getStyle().replace(BLUE_BTN_HOVER, BLUE_BTN)));
        return b;
    }

    private static Button grayButton(String text) {
        Button b = new Button(text);
        b.setStyle(
                "-fx-background-color: #1e1e1e; -fx-text-fill: " + TEXT_GRAY + ";" +
                        "-fx-font-family: 'Segoe UI', Arial; -fx-font-size: 12px;" +
                        "-fx-background-radius: 6; -fx-padding: 8 16 8 16; -fx-cursor: hand;" +
                        "-fx-border-color: #2a2a2a; -fx-border-radius: 6;"
        );
        return b;
    }

    private static Label grayLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: " + TEXT_GRAY + "; -fx-font-family: 'Segoe UI', Arial;");
        return l;
    }

    private static <S> TableColumn<S, String> textCol(String title, String prop, String color, boolean bold) {
        TableColumn<S, String> col = new TableColumn<>(title);
        col.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>(prop));
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item);
                setStyle("-fx-font-family: 'Segoe UI', Arial; -fx-font-size: 12px; -fx-text-fill: " + color + ";" + (bold ? "-fx-font-weight: bold;" : ""));
            }
        });
        return col;
    }

    private static void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    // ─── FileEntry model ──────────────────────────────────────────────────────
    public static class FileEntry {
        private final StringProperty  name            = new SimpleStringProperty();
        private final StringProperty  path            = new SimpleStringProperty();
        private final long            size;
        private final StringProperty  sizeDisplay     = new SimpleStringProperty();
        private final StringProperty  priority        = new SimpleStringProperty();
        private final StringProperty  estimatedSaving = new SimpleStringProperty();
        private final StringProperty  status          = new SimpleStringProperty("Pending");
        private final BooleanProperty selected        = new SimpleBooleanProperty(true);

        public FileEntry(Path p, long bytes) {
            this.size = bytes;
            name.set(p.getFileName().toString());
            path.set(p.toAbsolutePath().toString());
            sizeDisplay.set(humanSize(bytes));
            priority.set(bytes > 500 * MB ? "HIGH" : bytes > 50 * MB ? "MEDIUM" : "LOW");
            estimatedSaving.set("~" + humanSize((long)(bytes * 0.45)));
        }

        public String  getName()            { return name.get(); }
        public String  getPath()            { return path.get(); }
        public long    getSize()            { return size; }
        public String  getSizeDisplay()     { return sizeDisplay.get(); }
        public String  getPriority()        { return priority.get(); }
        public String  getEstimatedSaving() { return estimatedSaving.get(); }
        public String  getStatus()          { return status.get(); }
        public void    setEstimatedSaving(String v) { estimatedSaving.set(v); }

        public StringProperty  nameProperty()            { return name; }
        public StringProperty  pathProperty()            { return path; }
        public StringProperty  sizeDisplayProperty()     { return sizeDisplay; }
        public StringProperty  priorityProperty()        { return priority; }
        public StringProperty  estimatedSavingProperty() { return estimatedSaving; }
        public StringProperty  statusProperty()          { return status; }
        public BooleanProperty selectedProperty()        { return selected; }
    }
}
