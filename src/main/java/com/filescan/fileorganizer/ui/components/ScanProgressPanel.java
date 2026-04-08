package com.filescan.fileorganizer.ui.components;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class ScanProgressPanel {

    private final Label filesFoundLabel = new Label("0");
    private final Label foldersLabel = new Label("0");
    private final Label docsLabel = new Label("0");
    private final Label musicLabel = new Label("0");
    private final Label imagesLabel = new Label("0");
    private final Label videosLabel = new Label("0");
    private final Label currentPathLabel = new Label("Starting...");
    private final Label elapsedLabel = new Label("0s");
    private final Label progressPctLabel = new Label("0%");
    private final ProgressBar progressBar = new ProgressBar(0);

    public VBox build() {
        VBox panel = new VBox(10);
        panel.setPrefSize(350, 350);
        panel.setMaxSize(350, 350);
        panel.setMinSize(350, 350);
        panel.setPadding(new Insets(16));
        panel.setStyle("""
                -fx-background-color: #13131f;
                -fx-background-radius: 16;
                """);

        panel.getChildren().addAll(
                buildHeader(),
                buildMainCounters(),
                buildCategoryCounters(),
                buildProgressRow(),
                buildPathRow(),
                buildFooter()
        );

        return panel;
    }

    // ── Pulsing dot + "Scanning..." header ───────────────────
    private HBox buildHeader() {
        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill: #1a73e8; -fx-font-size: 12px;");

        FadeTransition pulse = new FadeTransition(Duration.millis(800), dot);
        pulse.setFromValue(1.0);
        pulse.setToValue(0.3);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();

        Label title = new Label("Scanning drives...");
        title.setStyle("""
                -fx-text-fill: white;
                -fx-font-size: 13px;
                -fx-font-weight: bold;
                """);

        HBox header = new HBox(8, dot, title);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    // ── Files found + Folders ────────────────────────────────
    private HBox buildMainCounters() {
        HBox row = new HBox(8);
        VBox filesCard = buildStatCard("FILES", filesFoundLabel, "#ffffff");
        VBox foldersCard = buildStatCard("FOLDERS", foldersLabel, "#ffffff");
        HBox.setHgrow(filesCard, Priority.ALWAYS);
        HBox.setHgrow(foldersCard, Priority.ALWAYS);
        row.getChildren().addAll(filesCard, foldersCard);
        return row;
    }

    // ── Docs / Music / Images / Videos ───────────────────────
    private HBox buildCategoryCounters() {
        HBox row = new HBox(6);
        VBox d = buildMiniCard("DOCS", docsLabel, "#378ADD");
        VBox m = buildMiniCard("MUSIC", musicLabel, "#D4537E");
        VBox i = buildMiniCard("IMAGES", imagesLabel, "#1D9E75");
        VBox v = buildMiniCard("VIDEOS", videosLabel, "#EF9F27");
        for (VBox card : new VBox[]{d, m, i, v})
            HBox.setHgrow(card, Priority.ALWAYS);
        row.getChildren().addAll(d, m, i, v);
        return row;
    }

    // ── Progress bar ─────────────────────────────────────────
    private VBox buildProgressRow() {
        progressBar.setPrefHeight(5);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("""
                -fx-accent: #1a73e8;
                -fx-background-color: #1a1a2e;
                -fx-background-radius: 4;
                """);

        Label driveLabel = new Label("D:\\ drive");
        driveLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");
        progressPctLabel.setStyle("-fx-text-fill: #1a73e8; -fx-font-size: 11px;");

        HBox labelRow = new HBox(driveLabel, progressPctLabel);
        HBox.setHgrow(driveLabel, Priority.ALWAYS);
        labelRow.setAlignment(Pos.CENTER_LEFT);

        return new VBox(4, labelRow, progressBar);
    }

    // ── Current path ─────────────────────────────────────────
    private VBox buildPathRow() {
        Label pathTitle = new Label("CURRENT PATH");
        pathTitle.setStyle("-fx-text-fill: #555555; -fx-font-size: 9px;");

        currentPathLabel.setStyle("""
                -fx-text-fill: #378ADD;
                -fx-font-size: 10px;
                -fx-font-family: monospace;
                """);
        currentPathLabel.setMaxWidth(310);
        currentPathLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

        VBox box = new VBox(3, pathTitle, currentPathLabel);
        box.setPadding(new Insets(8, 10, 8, 10));
        box.setStyle("""
                -fx-background-color: #0d0d1a;
                -fx-background-radius: 8;
                """);
        return box;
    }

    // ── Footer: elapsed + stop button ────────────────────────
    private HBox buildFooter() {
        Label elapsed = new Label("Elapsed:");
        elapsed.setStyle("-fx-text-fill: #555555; -fx-font-size: 11px;");
        elapsedLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");

        Button stopBtn = new Button("Stop");
        stopBtn.setStyle("""
                -fx-background-color: #2a1a0e;
                -fx-text-fill: #ff6f00;
                -fx-border-color: #ff6f00;
                -fx-border-radius: 6;
                -fx-background-radius: 6;
                -fx-border-width: 1;
                -fx-font-size: 11px;
                -fx-cursor: hand;
                -fx-padding: 4 12 4 12;
                """);

        HBox footer = new HBox(4, elapsed, elapsedLabel, stopBtn);
        footer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(elapsedLabel, Priority.ALWAYS);
        return footer;
    }

    // ── Card builders ─────────────────────────────────────────
    private VBox buildStatCard(String title, Label valueLabel, String color) {
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: #888888; -fx-font-size: 10px;");
        valueLabel.setStyle(
                "-fx-text-fill: " + color + ";" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;"
        );
        VBox card = new VBox(2, t, valueLabel);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 8;");
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private VBox buildMiniCard(String title, Label valueLabel, String color) {
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: #888888; -fx-font-size: 9px;");
        valueLabel.setStyle(
                "-fx-text-fill: " + color + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;"
        );
        VBox card = new VBox(2, t, valueLabel);
        card.setPadding(new Insets(8, 10, 8, 10));
        card.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 8;");
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    // ── Public update methods (call from virtual thread) ──────
    public void updateCounts(long files, long folders,
                             long docs, long music,
                             long images, long videos) {
        Platform.runLater(() -> {
            filesFoundLabel.setText(String.format("%,d", files));
            foldersLabel.setText(String.format("%,d", folders));
            docsLabel.setText(String.valueOf(docs));
            musicLabel.setText(String.valueOf(music));
            imagesLabel.setText(String.valueOf(images));
            videosLabel.setText(String.valueOf(videos));
        });
    }

    public void updatePath(String path) {
        Platform.runLater(() -> currentPathLabel.setText(path));
    }

    public void updateProgress(double progress, String driveName) {
        Platform.runLater(() -> {
            progressBar.setProgress(progress);
            progressPctLabel.setText(String.format("%.0f%%", progress * 100));
        });
    }

    public void updateElapsed(long seconds) {
        Platform.runLater(() -> elapsedLabel.setText(seconds + "s"));
    }
}
