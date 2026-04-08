package com.filescan.fileorganizer.ui.components;

import com.filescan.fileorganizer.service.CodeFileFinder;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.util.Duration;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class CodeFileUI {

    // ── Theme Colors ─────────────────────────────────────────────────────────
    private static final String BG_DARK = "#0d1117";
    private static final String BG_CARD = "#161b22";
    private static final String BG_HOVER = "#1f2937";
    private static final String ACCENT_BLUE = "#58a6ff";
    private static final String ACCENT_GREEN = "#3fb950";
    private static final String ACCENT_PURPLE = "#bc8cff";
    private static final String ACCENT_ORANGE = "#f0883e";
    private static final String TEXT_PRIMARY = "#e6edf3";
    private static final String TEXT_MUTED = "#8b949e";
    private static final String BORDER_COLOR = "#30363d";

    // ── Extension Config ─────────────────────────────────────────────────────
    private static final Map<String, String[]> EXT_CONFIG = new LinkedHashMap<>();

    static {
        EXT_CONFIG.put(".java", new String[]{"/lang_icon/java.png", "#f89820", "Java"});
        EXT_CONFIG.put(".py", new String[]{"/lang_icon/python.png", "#3776ab", "Python"});
        EXT_CONFIG.put(".js", new String[]{"/lang_icon/js.png", "#f7df1e", "JavaScript"});
        EXT_CONFIG.put(".ts", new String[]{"/lang_icon/typescript.png", "#3178c6", "TypeScript"});
        EXT_CONFIG.put(".html", new String[]{"/lang_icon/html-5.png", "#e34c26", "HTML"});
        EXT_CONFIG.put(".css", new String[]{"/lang_icon/css-3.png", "#264de4", "CSS"});
        EXT_CONFIG.put(".cpp", new String[]{"/lang_icon/c-.png", "#00599c", "C++"});
        EXT_CONFIG.put(".c", new String[]{"/lang_icon/letter-c.png", "#555555", "C"});
        EXT_CONFIG.put(".go", new String[]{"/lang_icon/go.png", "#00add8", "Go"});
        EXT_CONFIG.put(".rs", new String[]{"/lang_icon/rust.png", "#ce422b", "Rust"});
        EXT_CONFIG.put(".kt", new String[]{"/lang_icon/kotlin.png", "#7f52ff", "Kotlin"});
        EXT_CONFIG.put(".cs", new String[]{"/lang_icon/c-sharp.png", "#239120", "C#"});
        EXT_CONFIG.put(".php", new String[]{"/lang_icon/php.png", "#8892be", "PHP"});
        EXT_CONFIG.put(".rb", new String[]{"/lang_icon/ruby.png", "#cc342d", "Ruby"});
    }

    // ── Main UI Builder ──────────────────────────────────────────────────────
    public static Parent buildCodeFinderUI() {

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG_DARK + ";");

        // Header
        root.setTop(buildHeader());

        // Body — scan button + results
        VBox body = new VBox(20);
        body.setPrefHeight(700);
        body.setPadding(new Insets(24));
        body.setStyle("-fx-background-color: " + BG_DARK + ";");

        // Stats bar (hidden until scan)
        HBox statsBar = buildStatsBar(0, 0, 0);
        statsBar.setVisible(false);

        // Scan button
        Button scanBtn = buildScanButton();

        // Progress bar (hidden until scanning)
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setStyle(
                "-fx-accent: " + ACCENT_BLUE + ";" +
                        "-fx-background-color: " + BORDER_COLOR + ";" +
                        "-fx-background-radius: 4;"
        );
        progressBar.setVisible(false);

        Label progressLabel = new Label("Scanning drives...");
        progressLabel.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 12px;");
        progressLabel.setVisible(false);

        // Tab pane for By Extension / Related Files
        TabPane tabPane = new TabPane();
        tabPane.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-background: transparent;" +
                        "-fx-tab-min-width: 140px;"
        );
        tabPane.setVisible(false);

        Tab extTab = new Tab("📦  By Extension");
        Tab relatedTab = new Tab("🔗  Related Files");
        Tab summaryTab = new Tab("📊  Summary");
        extTab.setClosable(false);
        relatedTab.setClosable(false);
        summaryTab.setClosable(false);
        tabPane.getTabs().addAll(extTab, relatedTab, summaryTab);

        // Search bar
        HBox searchBar = buildSearchBar();
        searchBar.setVisible(false);

        // Wire scan button
        scanBtn.setOnAction(e -> {
            scanBtn.setDisable(true);
            scanBtn.setText("  ⏳  Scanning...");
            progressBar.setVisible(true);
            progressLabel.setVisible(true);
            statsBar.setVisible(false);
            tabPane.setVisible(false);
            searchBar.setVisible(false);

            // Animate progress
            Timeline fakeProgress = new Timeline(
                    new KeyFrame(Duration.seconds(0), new KeyValue(progressBar.progressProperty(), 0)),
                    new KeyFrame(Duration.seconds(1.5), new KeyValue(progressBar.progressProperty(), 0.6))
            );
            fakeProgress.play();

            // Run scan in background
            Thread scanThread = new Thread(() -> {
                Map<String, List<Path>> results = CodeFileFinder.findAllCodeFiles();

                Platform.runLater(() -> {
                    progressBar.setProgress(1.0);

                    // Count stats
                    long total = results.values().stream().mapToLong(List::size).sum();
                    long related = results.values().stream().filter(l -> l.size() > 1).count();
                    long extCount = results.values().stream()
                            .flatMap(List::stream)
                            .map(p -> getExtension(p.getFileName().toString()))
                            .distinct().count();

                    // Populate tabs
                    extTab.setContent(buildExtensionTab(results));
                    relatedTab.setContent(buildRelatedTab(results));
                    summaryTab.setContent(buildSummaryTab(results));

                    // Update stats bar
                    HBox newStats = buildStatsBar(total, related, extCount);
                    body.getChildren().set(body.getChildren().indexOf(statsBar), newStats);

                    // Show UI
                    newStats.setVisible(true);
                    tabPane.setVisible(true);
                    searchBar.setVisible(true);
                    progressBar.setVisible(false);
                    progressLabel.setVisible(false);
                    scanBtn.setDisable(false);
                    scanBtn.setText("  🔍  Scan Again");

                    // Animate tab pane in
                    FadeTransition ft = new FadeTransition(Duration.millis(400), tabPane);
                    ft.setFromValue(0);
                    ft.setToValue(1);
                    ft.play();
                });
            });
            scanThread.setDaemon(true);
            scanThread.start();
        });

        body.getChildren().addAll(scanBtn, progressBar, progressLabel, statsBar, searchBar, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        root.setCenter(body);
        return root;
    }

    // ── Header ───────────────────────────────────────────────────────────────
    private static HBox buildHeader() {
        HBox header = new HBox(12);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color: " + BG_CARD + ";" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-width: 0 0 1 0;"
        );

        // Icon circle
        StackPane iconBox = new StackPane();
        Circle circle = new Circle(22);
        circle.setFill(Color.web(ACCENT_BLUE, 0.15));
        circle.setStroke(Color.web(ACCENT_BLUE, 0.4));
        circle.setStrokeWidth(1.5);
        Label iconLabel = new Label("</>");
        iconLabel.setStyle("-fx-text-fill: " + ACCENT_BLUE + "; -fx-font-size: 14px; -fx-font-weight: bold;");
        iconBox.getChildren().addAll(circle, iconLabel);

        VBox titleBox = new VBox(2);
        Label title = new Label("Code File Explorer");
        title.setStyle("-fx-text-fill: " + TEXT_PRIMARY + "; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label subtitle = new Label("Scan all drives (except C:) for source code files");
        subtitle.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 12px;");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Drive badges
        HBox drives = new HBox(6);
        drives.setAlignment(Pos.CENTER);
        Arrays.stream(java.io.File.listRoots())
                .filter(d -> !d.getAbsolutePath().toUpperCase().startsWith("C"))
                .forEach(d -> drives.getChildren().add(buildBadge(d.getAbsolutePath(), ACCENT_GREEN)));

        header.getChildren().addAll(iconBox, titleBox, spacer, drives);
        return header;
    }

    // ── Scan Button ──────────────────────────────────────────────────────────
    private static Button buildScanButton() {
        Button btn = new Button("  🔍  Scan All Drives");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(48);
        btn.setStyle(
                "-fx-background-color: " + ACCENT_BLUE + ";" +
                        "-fx-text-fill: #0d1117;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #79c0ff;" +
                        "-fx-text-fill: #0d1117;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: " + ACCENT_BLUE + ";" +
                        "-fx-text-fill: #0d1117;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        ));
        return btn;
    }

    // ── Stats Bar ────────────────────────────────────────────────────────────
    private static HBox buildStatsBar(long total, long related, long extCount) {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER);
        bar.getChildren().addAll(
                buildStatCard("📄", String.valueOf(total), "Total Files", ACCENT_BLUE),
                buildStatCard("🔗", String.valueOf(related), "Related Groups", ACCENT_PURPLE),
                buildStatCard("🗂", String.valueOf(extCount), "Extensions Found", ACCENT_GREEN)
        );
        return bar;
    }

    private static VBox buildStatCard(String icon, String value, String label, String color) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14, 28, 14, 28));
        card.setStyle(
                "-fx-background-color: " + BG_CARD + ";" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: " + color + "33;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-width: 1;"
        );
        HBox.setHgrow(card, Priority.ALWAYS);

        Label iconLabel = new Label(icon + "  " + value);
        iconLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label labelLabel = new Label(label);
        labelLabel.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 11px;");

        card.getChildren().addAll(iconLabel, labelLabel);
        return card;
    }

    // ── Search Bar ───────────────────────────────────────────────────────────
    private static HBox buildSearchBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("🔎  Filter files by name or path...");
        searchField.setPrefHeight(38);
        searchField.setStyle(
                "-fx-background-color: " + BG_CARD + ";" +
                        "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                        "-fx-prompt-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-border-color: " + BORDER_COLOR + ";" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-size: 13px;" +
                        "-fx-padding: 0 12 0 12;"
        );
        HBox.setHgrow(searchField, Priority.ALWAYS);
        bar.getChildren().add(searchField);
        return bar;
    }

    // ── By Extension Tab ─────────────────────────────────────────────────────
    private static ScrollPane buildExtensionTab(Map<String, List<Path>> allFiles) {
        VBox container = new VBox(12);
        container.setPadding(new Insets(16));
        container.setStyle("-fx-background-color: " + BG_DARK + ";");

        // Group all paths by extension
        Map<String, List<Path>> byExt = new TreeMap<>();
        allFiles.values().stream()
                .flatMap(List::stream)
                .forEach(p -> byExt
                        .computeIfAbsent(getExtension(p.getFileName().toString()), k -> new ArrayList<>())
                        .add(p));

        byExt.forEach((ext, paths) -> {
            String[] cfg = EXT_CONFIG.getOrDefault(ext, new String[]{"📄", ACCENT_BLUE, ext});
            ImageView icon = new ImageView(new Image(CodeFileUI.class.getResourceAsStream(cfg[0])));
            icon.setFitWidth(19);
            icon.setFitHeight(19);
            String color = cfg[1];
            String name = cfg[2];

            // Collapsible section per extension
            TitledPane section = buildExtensionSection(icon, name, ext, color, paths);
            container.getChildren().add(section);
        });

        ScrollPane scroll = new ScrollPane(container);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: " + BG_DARK + "; -fx-background: " + BG_DARK + ";");
        return scroll;
    }

    private static TitledPane buildExtensionSection(ImageView icon, String name, String ext,
                                                    String color, List<Path> paths) {
        VBox content = new VBox(4);
        content.setPadding(new Insets(8, 8, 8, 8));
        content.setStyle("-fx-background-color: " + BG_DARK + ";");

        paths.stream().sorted().forEach(p -> {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 10, 6, 10));
            row.setStyle(
                    "-fx-background-color: " + BG_CARD + ";" +
                            "-fx-background-radius: 6;" +
                            "-fx-cursor: hand;"
            );

            Label fileIcon = new Label("📄");
            Label path = new Label(p.toString());
            path.setStyle("-fx-text-fill: " + TEXT_PRIMARY + "; -fx-font-size: 12px;");
            path.setFont(Font.font("Monospace", 12));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label extBadge = buildBadge(ext, color);

            row.getChildren().addAll(fileIcon, path, spacer, extBadge);

            // Hover effect
            row.setOnMouseEntered(e -> row.setStyle(
                    "-fx-background-color: " + BG_HOVER + ";" +
                            "-fx-background-radius: 6;" +
                            "-fx-cursor: hand;"
            ));
            row.setOnMouseExited(e -> row.setStyle(
                    "-fx-background-color: " + BG_CARD + ";" +
                            "-fx-background-radius: 6;" +
                            "-fx-cursor: hand;"
            ));

            content.getChildren().add(row);
        });

        TitledPane pane = new TitledPane();
        pane.getStyleClass().add("dropDownBox");
        pane.setExpanded(false);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label iconL = new Label("", icon);
        iconL.setStyle("-fx-font-size: 16px;");
        Label nameL = new Label(name);
        nameL.setStyle("-fx-text-fill: " + TEXT_PRIMARY + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label countL = new Label(paths.size() + " files");
        countL.setStyle(
                "-fx-background-color: " + color + "33;" +
                        "-fx-text-fill: " + "white" + ";" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 2 8 2 8;" +
                        "-fx-font-size: 11px;"
        );

        header.getChildren().addAll(iconL, nameL, countL);
        pane.setGraphic(header);
        pane.setText("");
        pane.setContent(content);
        return pane;
    }

    // ── Related Files Tab ─────────────────────────────────────────────────────
    private static ScrollPane buildRelatedTab(Map<String, List<Path>> allFiles) {
        VBox container = new VBox(10);
        container.setPadding(new Insets(16));
        container.setStyle("-fx-background-color: " + BG_DARK + ";");

        Map<String, List<Path>> related = allFiles.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (related.isEmpty()) {
            Label empty = new Label("No related files found.");
            empty.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 14px;");
            container.getChildren().add(empty);
        } else {
            related.forEach((baseName, paths) -> {
                VBox card = new VBox(6);
                card.setPadding(new Insets(12));
                card.setStyle(
                        "-fx-background-color: " + BG_CARD + ";" +
                                "-fx-background-radius: 10;" +
                                "-fx-border-color: " + ACCENT_PURPLE + "33;" +
                                "-fx-border-radius: 10;" +
                                "-fx-border-width: 1;"
                );

                Label nameLabel = new Label("📁  " + baseName);
                nameLabel.setStyle(
                        "-fx-text-fill: " + ACCENT_PURPLE + ";" +
                                "-fx-font-size: 13px;" +
                                "-fx-font-weight: bold;"
                );
                card.getChildren().add(nameLabel);

                // Extension chips row
                HBox chips = new HBox(6);
                paths.stream().sorted().forEach(p -> {
                    String ext = getExtension(p.getFileName().toString());
                    String[] cfg = EXT_CONFIG.getOrDefault(ext, new String[]{"📄", ACCENT_BLUE, ext});
                    chips.getChildren().add(buildBadge(cfg[0] + " " + ext, cfg[1]));
                });
                card.getChildren().add(chips);

                // File paths
                paths.stream().sorted().forEach(p -> {
                    Label pathLabel = new Label("  ├──  " + p);
                    pathLabel.setStyle(
                            "-fx-text-fill: " + TEXT_MUTED + ";" +
                                    "-fx-font-size: 11px;"
                    );
                    pathLabel.setFont(Font.font("Monospace", 11));
                    card.getChildren().add(pathLabel);
                });

                container.getChildren().add(card);
            });
        }

        ScrollPane scroll = new ScrollPane(container);
        scroll.setPrefHeight(200);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: " + BG_DARK + "; -fx-background: " + BG_DARK + ";");
        return scroll;
    }

    // ── Summary Tab ──────────────────────────────────────────────────────────
    private static ScrollPane buildSummaryTab(Map<String, List<Path>> allFiles) {
        VBox container = new VBox(10);
        container.setPadding(new Insets(16));
        container.setStyle("-fx-background-color: " + BG_DARK + ";");

        // Group by extension for bar chart
        Map<String, Long> extCount = new TreeMap<>();
        allFiles.values().stream().flatMap(List::stream).forEach(p -> {
            String ext = getExtension(p.getFileName().toString());
            extCount.merge(ext, 1L, Long::sum);
        });

        long maxCount = extCount.values().stream().mapToLong(Long::longValue).max().orElse(1);

        Label chartTitle = new Label("Files per Language");
        chartTitle.setStyle(
                "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;"
        );
        container.getChildren().add(chartTitle);

        extCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    String ext = entry.getKey();
                    long count = entry.getValue();
                    String[] cfg = EXT_CONFIG.getOrDefault(ext, new String[]{"📄", ACCENT_BLUE, ext});

                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(4, 0, 4, 0));

                    // Language label
                    ImageView icon = new ImageView(new Image(CodeFileUI.class.getResourceAsStream(cfg[0])));
                    icon.setFitWidth(19);
                    icon.setFitHeight(19);
                    Label langLabel = new Label(cfg[2], icon);
                    langLabel.setMinWidth(120);
                    langLabel.setStyle("-fx-text-fill: " + TEXT_PRIMARY + "; -fx-font-size: 12px;");

                    // Bar
                    double ratio = (double) count / maxCount;
                    StackPane barContainer = new StackPane();
                    barContainer.setAlignment(Pos.CENTER_LEFT);
                    HBox.setHgrow(barContainer, Priority.ALWAYS);

                    Rectangle bgBar = new Rectangle(0, 20);
                    bgBar.setFill(Color.web(BORDER_COLOR));
                    bgBar.setArcWidth(6);
                    bgBar.setArcHeight(6);
                    bgBar.widthProperty().bind(barContainer.widthProperty());

                    Rectangle fgBar = new Rectangle(0, 20);
                    fgBar.setFill(Color.web(cfg[1]));
                    fgBar.setArcWidth(6);
                    fgBar.setArcHeight(6);

                    // Animate bar width
                    barContainer.widthProperty().addListener((obs, old, newW) -> {
                        Timeline tl = new Timeline(
                                new KeyFrame(Duration.ZERO, new KeyValue(fgBar.widthProperty(), 0)),
                                new KeyFrame(Duration.millis(600), new KeyValue(fgBar.widthProperty(), newW.doubleValue() * ratio))
                        );
                        tl.play();
                    });

                    barContainer.getChildren().addAll(bgBar, fgBar);

                    Label countLabel = new Label(String.valueOf(count));
                    countLabel.setMinWidth(40);
                    countLabel.setStyle("-fx-text-fill: " + cfg[1] + "; -fx-font-size: 12px; -fx-font-weight: bold;");

                    row.getChildren().addAll(langLabel, barContainer, countLabel);
                    container.getChildren().add(row);
                });

        ScrollPane scroll = new ScrollPane(container);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: " + BG_DARK + "; -fx-background: " + BG_DARK + ";");
        return scroll;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private static Label buildBadge(String text, String color) {
        Label badge = new Label(text);
        badge.setStyle(
                "-fx-background-color: " + color + "22;" +
                        "-fx-text-fill: " + color + ";" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: " + color + "55;" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 2 8 2 8;" +
                        "-fx-font-size: 11px;"
        );
        return badge;
    }

    private static String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot != -1) ? fileName.substring(dot).toLowerCase() : "";
    }
}