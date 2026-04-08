package com.filescan.fileorganizer.ui.components;

import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class GradientButton {
    private static boolean isOrange = false;

    // Blue gradient stops
    private static final Stop[] blueStops = {
            new Stop(0, Color.web("#1a73e8")),
            new Stop(1, Color.web("#0d47a1"))
    };

    // Orange gradient stops
    private static final Stop[] orangeStops = {
            new Stop(0, Color.web("#ff6f00")),
            new Stop(1, Color.web("#e65100"))
    };

    public static StackPane ScanButton() {
        Button btn = new Button("Start Scan");
        btn.setPrefSize(160, 50);
        btn.setStyle("""
                -fx-background-color: transparent;
                -fx-text-fill: white;
                -fx-font-size: 16px;
                -fx-font-weight: bold;
                -fx-cursor: hand;
                -fx-background-radius: 25;
                """);

        // ── Gradient rectangle as button background ───────────
        Rectangle bg = new Rectangle(160, 50);
        bg.setArcWidth(25);
        bg.setArcHeight(25);
        bg.setFill(new LinearGradient(0, 0, 1, 0, true,
                CycleMethod.NO_CYCLE, blueStops));

        // ── Stack them: rect behind, button on top ────────────
        StackPane btnPane = new StackPane(bg, btn);
        btnPane.setMaxSize(160, 50);

        // ── Animation: interpolate gradient color ─────────────
        btn.setOnAction(e -> {
            animateGradient(bg);
            if (btn.getText().contains("Stop")) btn.setText("Start Scan");
            btn.setText("Stop Scanning");
        });

        // ── Hover: slight scale effect ────────────────────────
        btnPane.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), btnPane);
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();
        });
        btnPane.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), btnPane);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        return btnPane;
    }

    private static void animateGradient(Rectangle bg) {
        Stop[] fromStops = isOrange ? orangeStops : blueStops;
        Stop[] toStops = isOrange ? blueStops : orangeStops;
        isOrange = !isOrange;

        // Animate over 60 frames
        int frames = 60;
        Timeline timeline = new Timeline();

        for (int i = 0; i <= frames; i++) {
            double t = (double) i / frames;  // 0.0 → 1.0
            int frameIndex = i;

            KeyFrame kf = new KeyFrame(Duration.millis(i * (500.0 / frames)), ev -> {
                double progress = (double) frameIndex / frames;

                // Interpolate each stop color
                Color start0 = interpolate(
                        (Color) fromStops[0].getColor(),
                        (Color) toStops[0].getColor(), progress);
                Color start1 = interpolate(
                        (Color) fromStops[1].getColor(),
                        (Color) toStops[1].getColor(), progress);

                bg.setFill(new LinearGradient(0, 0, 1, 0, true,
                        CycleMethod.NO_CYCLE,
                        new Stop(0, start0),
                        new Stop(1, start1)));
            });

            timeline.getKeyFrames().add(kf);
        }

        // Pulse effect at the end
        timeline.setOnFinished(e -> pulse(bg));
        timeline.play();
    }

    // Quick scale pulse when animation finishes
    private static void pulse(Rectangle bg) {
        ScaleTransition st = new ScaleTransition(Duration.millis(100), bg);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(1.08);
        st.setToY(1.08);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }

    // Linearly interpolate between two colors
    private static Color interpolate(Color from, Color to, double t) {
        return new Color(
                from.getRed() + (to.getRed() - from.getRed()) * t,
                from.getGreen() + (to.getGreen() - from.getGreen()) * t,
                from.getBlue() + (to.getBlue() - from.getBlue()) * t,
                1.0
        );
    }
}
