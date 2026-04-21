package com.filescan.fileorganizer.ui.components;

import javafx.beans.property.*;
import javafx.geometry.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.*;
import javafx.collections.*;
import javafx.scene.input.MouseEvent;

import java.io.File;

/**
 * SettingsUI — File Organizer Settings Screen
 * Matches the pure-black + blue (#3d6ef5) theme of the project.
 *
 * Settings sections covered:
 *  1. General          — startup, language, theme toggle
 *  2. File Organization — source/destination folders, auto-organize, rules
 *  3. File Types       — which extensions map to which category folders
 *  4. Duplicate Files  — detection method, action on duplicate
 *  5. Compression      — auto-compress threshold, format, schedule
 *  6. Scheduler        — run frequency, time, watch folders
 *  7. Notifications    — system tray, sound, log level
 *  8. Storage & Backup — recycle-bin vs permanent delete, backup path
 *  9. Advanced         — thread count, exclusion patterns, reset
 */
public class SettingUI {

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final String BG           = "#000000";
    private static final String BG_PANEL     = "#0a0a0a";
    private static final String BG_CONTROL   = "#0d0d0d";
    private static final String BG_INPUT     = "#111111";
    private static final String BLUE         = "#3d6ef5";
    private static final String BLUE_HOVER   = "#2d5ee0";
    private static final String BLUE_DIM     = "#1a2d6b";
    private static final String BLUE_LIGHT   = "#7da4ff";
    private static final String TEXT_WHITE   = "#ffffff";
    private static final String TEXT_GRAY    = "#888888";
    private static final String TEXT_LIGHT   = "#cccccc";
    private static final String BORDER       = "#1c1c1c";
    private static final String BORDER_BLUE  = "#2a3f8f";
    private static final String GREEN        = "#39d353";
    private static final String ORANGE       = "#f0a500";
    private static final String RED          = "#e05252";
    private static final String DIVIDER      = "#161616";

    // currently selected nav item
    private final StringProperty activeSection = new SimpleStringProperty("General");
    private final StackPane contentPane = new StackPane();

    public Parent openSetting() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG + ";");
        root.setTop(buildHeader());
        root.setLeft(buildSidebar());
        root.setCenter(buildContent());
        root.setBottom(buildFooter());

        return root;
    }

    // ─── Header ───────────────────────────────────────────────────────────────
    private HBox buildHeader() {
        HBox h = new HBox();
        h.setPadding(new Insets(18, 28, 18, 28));
        h.setAlignment(Pos.CENTER_LEFT);
        h.setStyle("-fx-background-color: " + BG + "; -fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0;");

        Label title = new Label("Settings");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_WHITE + "; -fx-font-family: 'Segoe UI', Arial;");

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        Label version = new Label("File Organizer  v2.1.0");
        version.setStyle("-fx-font-size: 11px; -fx-text-fill: " + TEXT_GRAY + "; -fx-font-family: 'Segoe UI', Arial;");

        h.getChildren().addAll(title, sp, version);
        return h;
    }

    // ─── Sidebar navigation ───────────────────────────────────────────────────
    private VBox buildSidebar() {
        VBox sb = new VBox(2);
        sb.setPrefWidth(200);
        sb.setPadding(new Insets(16, 10, 16, 10));
        sb.setStyle("-fx-background-color: " + BG + "; -fx-border-color: " + BORDER + "; -fx-border-width: 0 1 0 0;");

        String[][] navItems = {
                {"⚙", "General"},
                {"📁", "File Organization"},
                {"🗂", "File Types"},
                {"👥", "Duplicate Files"},
                {"🗜", "Compression"},
                {"🕐", "Scheduler"},
                {"🔔", "Notifications"},
                {"💾", "Storage & Backup"},
                {"🔧", "Advanced"},
        };

        for (String[] item : navItems) {
            HBox row = buildNavItem(item[0], item[1]);
            sb.getChildren().add(row);
        }

        return sb;
    }

    private HBox buildNavItem(String icon, String label) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(9, 12, 9, 12));
        row.setStyle("-fx-background-radius: 6; -fx-cursor: hand;");

        Label ic = new Label(icon);
        ic.setStyle("-fx-font-size: 14px;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 13px; -fx-font-family: 'Segoe UI', Arial; -fx-text-fill: " + TEXT_GRAY + ";");

        row.getChildren().addAll(ic, lbl);

        // Highlight active
        Runnable applyStyle = () -> {
            boolean active = activeSection.get().equals(label);
            row.setStyle("-fx-background-color: " + (active ? BLUE_DIM : "transparent") + "; -fx-background-radius: 6; -fx-cursor: hand;");
            lbl.setStyle("-fx-font-size: 13px; -fx-font-family: 'Segoe UI', Arial; -fx-text-fill: " + (active ? TEXT_WHITE : TEXT_GRAY) + ";");
        };
        applyStyle.run();

        activeSection.addListener((ob, o, n) -> applyStyle.run());

        row.setOnMouseClicked((MouseEvent e) -> {
            activeSection.set(label);
            showSection(label);
        });
        row.setOnMouseEntered(e -> {
            if (!activeSection.get().equals(label))
                row.setStyle("-fx-background-color: #111111; -fx-background-radius: 6; -fx-cursor: hand;");
        });
        row.setOnMouseExited(e -> applyStyle.run());

        return row;
    }

    // ─── Content area ─────────────────────────────────────────────────────────
    private ScrollPane buildContent() {
        ScrollPane sp = new ScrollPane(contentPane);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.setStyle("-fx-background: " + BG + "; -fx-background-color: " + BG + "; -fx-border-color: transparent;");
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        showSection("General");
        return sp;
    }

    private void showSection(String name) {
        contentPane.getChildren().clear();
        VBox page = switch (name) {
            case "General"          -> buildGeneral();
            case "File Organization"-> buildFileOrganization();
            case "File Types"       -> buildFileTypes();
            case "Duplicate Files"  -> buildDuplicateFiles();
            case "Compression"      -> buildCompression();
            case "Scheduler"        -> buildScheduler();
            case "Notifications"    -> buildNotifications();
            case "Storage & Backup" -> buildStorage();
            case "Advanced"         -> buildAdvanced();
            default                 -> buildGeneral();
        };
        contentPane.getChildren().add(page);
        StackPane.setAlignment(page, Pos.TOP_LEFT);
    }

    // ─── Footer ───────────────────────────────────────────────────────────────
    private HBox buildFooter() {
        HBox f = new HBox(10);
        f.setPadding(new Insets(12, 28, 12, 28));
        f.setAlignment(Pos.CENTER_RIGHT);
        f.setStyle("-fx-background-color: " + BG + "; -fx-border-color: " + BORDER + "; -fx-border-width: 1 0 0 0;");

        Button reset  = grayBtn("Reset to Defaults");
        Button cancel = grayBtn("Cancel");
        Button save   = blueBtn("Save Settings");

        reset.setOnAction(e -> showConfirm("Reset Settings", "Reset all settings to factory defaults?", () -> {}));
        f.getChildren().addAll(reset, new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }}, cancel, save);
        return f;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SECTION PAGES
    // ══════════════════════════════════════════════════════════════════════════

    // 1. GENERAL ───────────────────────────────────────────────────────────────
    private VBox buildGeneral() {
        VBox page = page("General Settings");

        // Startup
        VBox startupCard = card("Startup");
        startupCard.getChildren().addAll(
                toggleRow("Launch on system startup",        "Automatically start File Organizer when Windows boots.", true),
                divider(),
                toggleRow("Start minimized to system tray", "Open in background without showing the main window.", false),
                divider(),
                toggleRow("Show splash screen on launch",   "Display the welcome splash for 2 seconds on startup.", true)
        );

        // Appearance
        VBox appearCard = card("Appearance");
        HBox langRow = labeledRow("Language", comboInput(new String[]{"English", "Hindi", "French", "German", "Spanish", "Japanese"}, "English"));
        HBox themeRow = labeledRow("Theme",   comboInput(new String[]{"Dark (Black)", "Dark (Midnight)", "Light"}, "Dark (Black)"));
        HBox fontRow  = labeledRow("Font Size", comboInput(new String[]{"Small (11px)", "Medium (13px)", "Large (15px)"}, "Medium (13px)"));
        appearCard.getChildren().addAll(langRow, divider(), themeRow, divider(), fontRow);

        // Updates
        VBox updateCard = card("Updates");
        updateCard.getChildren().addAll(
                toggleRow("Auto-check for updates",  "Check for new versions on startup.", true),
                divider(),
                labeledRow("Update channel", comboInput(new String[]{"Stable", "Beta", "Nightly"}, "Stable"))
        );

        page.getChildren().addAll(startupCard, vgap(12), appearCard, vgap(12), updateCard);
        return page;
    }

    // 2. FILE ORGANIZATION ────────────────────────────────────────────────────
    private VBox buildFileOrganization() {
        VBox page = page("File Organization");

        VBox foldersCard = card("Folders");
        foldersCard.getChildren().addAll(
                labeledRow("Watch folder (source)",       folderPickerRow("C:\\Users\\You\\Downloads")),
                divider(),
                labeledRow("Destination root folder",     folderPickerRow("C:\\Users\\You\\Organized")),
                divider(),
                labeledRow("Unknown files folder",        folderPickerRow("C:\\Users\\You\\Organized\\Misc"))
        );

        VBox behaviorCard = card("Organization Behavior");
        behaviorCard.getChildren().addAll(
                toggleRow("Auto-organize on file change",   "Organize files as soon as they appear in the watch folder.", true),
                divider(),
                toggleRow("Preserve original folder structure", "Mirror sub-folder layout in the destination.", false),
                divider(),
                toggleRow("Rename duplicates automatically",    "Append (1), (2)… instead of overwriting.", true),
                divider(),
                labeledRow("Date folder format", comboInput(new String[]{"YYYY/MM/DD", "YYYY-MM", "YYYY", "None"}, "YYYY/MM")),
                divider(),
                labeledRow("Conflict resolution", comboInput(new String[]{"Rename new", "Skip", "Overwrite", "Ask me"}, "Rename new"))
        );

        VBox rulesCard = card("Custom Rules");
        rulesCard.getChildren().addAll(
                infoRow("💡", "Rules are applied in order. Drag to reorder."),
                vgap(8),
                ruleRow("*.pdf",   "→  Documents/PDF",   GREEN),
                ruleRow("*.mp4",   "→  Videos/MP4",      BLUE_LIGHT),
                ruleRow("*.zip",   "→  Archives",         ORANGE),
                ruleRow("Invoice*","→  Finance/Invoices", GREEN),
                vgap(8),
                blueOutlineBtn("+ Add Rule")
        );

        page.getChildren().addAll(foldersCard, vgap(12), behaviorCard, vgap(12), rulesCard);
        return page;
    }

    // 3. FILE TYPES ───────────────────────────────────────────────────────────
    private VBox buildFileTypes() {
        VBox page = page("File Types & Categories");

        VBox mapCard = card("Extension → Category Mapping");
        mapCard.getChildren().add(infoRow("💡", "Define which file extensions belong to which category folder."));
        mapCard.getChildren().add(vgap(8));

        String[][] types = {
                {"Documents",  "pdf, docx, doc, xlsx, xls, pptx, txt, odt, csv",   "#7da4ff"},
                {"Images",     "jpg, jpeg, png, gif, webp, svg, bmp, tiff, raw",    GREEN},
                {"Videos",     "mp4, mkv, mov, avi, wmv, flv, m4v, webm",           ORANGE},
                {"Audio",      "mp3, wav, flac, aac, ogg, m4a, wma",                "#c77dff"},
                {"Archives",   "zip, rar, 7z, gz, tar, bz2, iso",                   RED},
                {"Code",       "java, py, js, ts, html, css, json, xml, sh",        "#39d353"},
                {"Executables","exe, msi, apk, dmg, deb, AppImage",                 "#f0a500"},
        };

        for (String[] row : types) {
            mapCard.getChildren().add(typeRow(row[0], row[1], row[2]));
            mapCard.getChildren().add(divider());
        }

        VBox unhandledCard = card("Unrecognized Extensions");
        unhandledCard.getChildren().addAll(
                labeledRow("Place in folder", comboInput(new String[]{"Misc", "Root (unsorted)", "Skip / ignore"}, "Misc")),
                divider(),
                toggleRow("Log unrecognized extensions", "Write unknown types to the activity log.", true)
        );

        page.getChildren().addAll(mapCard, vgap(12), unhandledCard);
        return page;
    }

    // 4. DUPLICATE FILES ──────────────────────────────────────────────────────
    private VBox buildDuplicateFiles() {
        VBox page = page("Duplicate Files");

        VBox detectionCard = card("Detection Method");
        detectionCard.getChildren().addAll(
                toggleRow("Detect by content hash (MD5/SHA-256)", "Most accurate. Slower on large drives.", true),
                divider(),
                toggleRow("Detect by file name + size",           "Fast. May miss renamed duplicates.", false),
                divider(),
                toggleRow("Detect by visual similarity (images)", "Uses perceptual hashing for near-identical images.", false),
                divider(),
                labeledRow("Hash algorithm", comboInput(new String[]{"MD5 (fast)", "SHA-256 (secure)", "xxHash (fastest)"}, "MD5 (fast)"))
        );

        VBox actionCard = card("Action on Duplicate Found");
        actionCard.getChildren().addAll(
                labeledRow("Default action",   comboInput(new String[]{"Ask me each time", "Move to Duplicates folder", "Delete (Recycle Bin)", "Delete permanently", "Keep newest", "Keep largest"}, "Ask me each time")),
                divider(),
                labeledRow("Duplicates folder",folderPickerRow("C:\\Users\\You\\Duplicates")),
                divider(),
                toggleRow("Protect original (keep at least one)", "Never delete the last copy of a file.", true)
        );

        VBox scopeCard = card("Scan Scope");
        scopeCard.getChildren().addAll(
                toggleRow("Include hidden files",     "Also scan files/folders starting with '.'", false),
                divider(),
                toggleRow("Include system files",     "Scan Windows/system directories (use with care).", false),
                divider(),
                labeledRow("Minimum file size to check", comboInput(new String[]{"Any size", "> 1 KB", "> 100 KB", "> 1 MB", "> 10 MB"}, "> 1 KB"))
        );

        page.getChildren().addAll(detectionCard, vgap(12), actionCard, vgap(12), scopeCard);
        return page;
    }

    // 5. COMPRESSION ──────────────────────────────────────────────────────────
    private VBox buildCompression() {
        VBox page = page("Compression");

        VBox autoCard = card("Auto-Compression");
        autoCard.getChildren().addAll(
                toggleRow("Enable auto-compression",        "Automatically compress files that exceed the size threshold.", false),
                divider(),
                labeledRow("Compress files larger than",   comboInput(new String[]{"10 MB", "50 MB", "100 MB", "500 MB", "1 GB"}, "100 MB")),
                divider(),
                labeledRow("Compression format",           comboInput(new String[]{"ZIP", "GZ", "7Z (best ratio)", "BZ2"}, "ZIP")),
                divider(),
                labeledRow("Compression level",            comboInput(new String[]{"Fast (low ratio)", "Balanced", "Best ratio (slow)"}, "Balanced")),
                divider(),
                toggleRow("Delete original after compression", "Remove the source file once the archive is created.", false)
        );

        VBox skipCard = card("Skip These File Types");
        skipCard.getChildren().addAll(
                infoRow("💡", "Already-compressed formats are always skipped automatically."),
                vgap(6),
                toggleRow("Skip images  (jpg, png, webp…)",  "Images are already compressed; re-compressing saves nothing.", true),
                divider(),
                toggleRow("Skip videos  (mp4, mkv…)",        "Video containers use internal compression.", true),
                divider(),
                toggleRow("Skip audio   (mp3, aac…)",        "Lossy audio formats gain nothing from ZIP compression.", true),
                divider(),
                toggleRow("Skip archives (zip, rar, 7z…)",   "Don't compress already-compressed archives.", true)
        );

        VBox schedCard = card("Compression Schedule");
        schedCard.getChildren().addAll(
                toggleRow("Run compression on a schedule", "Compress files automatically at the chosen time.", false),
                divider(),
                labeledRow("Frequency",  comboInput(new String[]{"Daily", "Weekly", "Monthly", "On idle"}, "Weekly")),
                divider(),
                labeledRow("Run at",     timePickerRow("02:00 AM"))
        );

        page.getChildren().addAll(autoCard, vgap(12), skipCard, vgap(12), schedCard);
        return page;
    }

    // 6. SCHEDULER ────────────────────────────────────────────────────────────
    private VBox buildScheduler() {
        VBox page = page("Scheduler");

        VBox orgCard = card("File Organization Schedule");
        orgCard.getChildren().addAll(
                toggleRow("Enable scheduled organization",  "Run the organizer automatically on a timed basis.", true),
                divider(),
                labeledRow("Frequency",  comboInput(new String[]{"Every hour", "Every 6 hours", "Daily", "Weekly"}, "Daily")),
                divider(),
                labeledRow("Run at",     timePickerRow("03:00 AM")),
                divider(),
                toggleRow("Run only when computer is idle", "Wait until no keyboard/mouse input for 5 minutes.", true)
        );

        VBox scanCard = card("Scan & Analysis Schedule");
        scanCard.getChildren().addAll(
                toggleRow("Enable scheduled duplicate scan", "Automatically search for duplicates.", false),
                divider(),
                labeledRow("Frequency", comboInput(new String[]{"Daily", "Weekly", "Monthly"}, "Weekly")),
                divider(),
                labeledRow("Run at",    timePickerRow("04:00 AM"))
        );

        VBox watchCard = card("Real-Time Watching");
        watchCard.getChildren().addAll(
                toggleRow("Watch folders for new files",       "Instantly react to new files appearing in watched paths.", true),
                divider(),
                toggleRow("Watch for file renames",            "Trigger rules when a file is renamed.", true),
                divider(),
                labeledRow("Debounce delay", comboInput(new String[]{"500 ms", "1 sec", "3 sec", "5 sec"}, "1 sec"))
        );

        page.getChildren().addAll(orgCard, vgap(12), scanCard, vgap(12), watchCard);
        return page;
    }

    // 7. NOTIFICATIONS ────────────────────────────────────────────────────────
    private VBox buildNotifications() {
        VBox page = page("Notifications");

        VBox toastCard = card("System Notifications");
        toastCard.getChildren().addAll(
                toggleRow("Show system tray notifications",    "Pop-up toasts when tasks complete.", true),
                divider(),
                toggleRow("Notify on organization complete",   "Alert when a scheduled organize run finishes.", true),
                divider(),
                toggleRow("Notify on duplicate found",         "Alert whenever duplicates are detected.", false),
                divider(),
                toggleRow("Notify on compression complete",    "Alert when compression finishes.", true),
                divider(),
                toggleRow("Notify on errors",                  "Always show a notification if any error occurs.", true)
        );

        VBox soundCard = card("Sound");
        soundCard.getChildren().addAll(
                toggleRow("Play sound on task complete", "Soft chime when a background task finishes.", false),
                divider(),
                labeledRow("Sound theme", comboInput(new String[]{"None", "Chime", "Click", "Windows Default"}, "None"))
        );

        VBox logCard = card("Activity Log");
        logCard.getChildren().addAll(
                labeledRow("Log level",        comboInput(new String[]{"Errors only", "Warnings + Errors", "Info", "Verbose (Debug)"}, "Info")),
                divider(),
                labeledRow("Log file location",folderPickerRow("C:\\Users\\You\\AppData\\Local\\FileOrganizer\\logs")),
                divider(),
                labeledRow("Keep logs for",    comboInput(new String[]{"7 days", "30 days", "90 days", "Forever"}, "30 days")),
                divider(),
                blueOutlineBtn("Open Log Folder")
        );

        page.getChildren().addAll(toastCard, vgap(12), soundCard, vgap(12), logCard);
        return page;
    }

    // 8. STORAGE & BACKUP ────────────────────────────────────────────────────
    private VBox buildStorage() {
        VBox page = page("Storage & Backup");

        VBox deleteCard = card("File Deletion Behavior");
        deleteCard.getChildren().addAll(
                infoRow("⚠", "Choose what happens when the organizer removes files."),
                vgap(6),
                labeledRow("Delete mode", comboInput(new String[]{"Move to Recycle Bin (safe)", "Permanent delete (fast)", "Move to Trash folder first"}, "Move to Recycle Bin (safe)")),
                divider(),
                labeledRow("Trash folder",folderPickerRow("C:\\Users\\You\\.fileorganizer_trash")),
                divider(),
                labeledRow("Auto-empty trash after", comboInput(new String[]{"Never", "7 days", "30 days", "On exit"}, "30 days"))
        );

        VBox backupCard = card("Backup Before Moving");
        backupCard.getChildren().addAll(
                toggleRow("Backup files before organizing", "Keep a snapshot before any file is moved or renamed.", false),
                divider(),
                labeledRow("Backup folder",   folderPickerRow("C:\\Users\\You\\FileOrganizerBackups")),
                divider(),
                labeledRow("Max backup size", comboInput(new String[]{"500 MB", "1 GB", "5 GB", "10 GB", "Unlimited"}, "1 GB")),
                divider(),
                toggleRow("Compress backup archives", "ZIP backups to save disk space.", true)
        );

        VBox diskCard = card("Disk Usage Monitor");
        diskCard.getChildren().addAll(
                toggleRow("Warn when disk space is low",   "Show a notification if free space drops below threshold.", true),
                divider(),
                labeledRow("Warn threshold", comboInput(new String[]{"500 MB free", "1 GB free", "5 GB free", "10 GB free"}, "5 GB free"))
        );

        page.getChildren().addAll(deleteCard, vgap(12), backupCard, vgap(12), diskCard);
        return page;
    }

    // 9. ADVANCED ─────────────────────────────────────────────────────────────
    private VBox buildAdvanced() {
        VBox page = page("Advanced");

        VBox perfCard = card("Performance");
        perfCard.getChildren().addAll(
                labeledRow("Worker threads",    comboInput(new String[]{"1 (safe)", "2", "4 (default)", "8", "Auto"}, "4 (default)")),
                divider(),
                labeledRow("Scan batch size",   comboInput(new String[]{"500 files", "1,000 files", "5,000 files", "Unlimited"}, "1,000 files")),
                divider(),
                toggleRow("Use low-priority CPU mode", "Runs in background without affecting other apps.", true)
        );

        VBox exclusionCard = card("Exclusions");
        exclusionCard.getChildren().addAll(
                infoRow("💡", "Paths and patterns here are completely ignored by all operations."),
                vgap(8),
                exclusionRow("C:\\Windows"),
                exclusionRow("C:\\Program Files"),
                exclusionRow("*.tmp"),
                exclusionRow("~$*"),
                vgap(8),
                blueOutlineBtn("+ Add Exclusion")
        );

        VBox dataCard = card("Application Data");
        dataCard.getChildren().addAll(
                labeledRow("Config file location", readOnlyInput("C:\\Users\\You\\AppData\\Local\\FileOrganizer\\config.json")),
                divider(),
                labeledRow("Database location",    readOnlyInput("C:\\Users\\You\\AppData\\Local\\FileOrganizer\\index.db")),
                divider(),
                blueOutlineBtn("Export Settings"),
                vgap(6),
                blueOutlineBtn("Import Settings")
        );

        VBox dangerCard = card("⚠  Danger Zone");
        dangerCard.setStyle(dangerCard.getStyle() + "-fx-border-color: " + RED + "; -fx-border-width: 1; -fx-border-radius: 8;");
        Button resetBtn = new Button("Reset All Settings to Defaults");
        resetBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: " + RED + ";" +
                        "-fx-font-family: 'Segoe UI', Arial; -fx-font-size: 13px;" +
                        "-fx-border-color: " + RED + "; -fx-border-radius: 6; -fx-background-radius: 6;" +
                        "-fx-padding: 8 18 8 18; -fx-cursor: hand;"
        );
        Button clearDbBtn = new Button("Clear File Index Database");
        clearDbBtn.setStyle(resetBtn.getStyle());
        dangerCard.getChildren().addAll(
                infoRow("⚠", "These actions are irreversible. Make sure you have a backup."),
                vgap(8),
                resetBtn, vgap(6), clearDbBtn
        );

        page.getChildren().addAll(perfCard, vgap(12), exclusionCard, vgap(12), dataCard, vgap(12), dangerCard);
        return page;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  COMPONENT BUILDERS
    // ══════════════════════════════════════════════════════════════════════════

    private VBox page(String sectionTitle) {
        VBox v = new VBox(0);
        v.setPadding(new Insets(24, 28, 28, 28));
        v.setStyle("-fx-background-color: " + BG + ";");

        Label lbl = new Label(sectionTitle);
        lbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_WHITE + "; -fx-font-family: 'Segoe UI', Arial;");
        v.getChildren().addAll(lbl, vgap(14));
        return v;
    }

    private VBox card(String title) {
        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color: " + BG_PANEL + ";" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 8; -fx-background-radius: 8;"
        );

        // Card header
        Label lbl = new Label(title);
        lbl.setPadding(new Insets(12, 16, 10, 16));
        lbl.setStyle(
                "-fx-font-size: 12px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + BLUE_LIGHT + "; -fx-font-family: 'Segoe UI', Arial;" +
                        "-fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0;"
        );
        lbl.setMaxWidth(Double.MAX_VALUE);
        card.getChildren().add(lbl);
        return card;
    }

    // Toggle row
    private HBox toggleRow(String label, String desc, boolean defaultOn) {
        HBox row = new HBox();
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        VBox text = new VBox(2);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TEXT_WHITE + "; -fx-font-family: 'Segoe UI', Arial;");
        Label d = new Label(desc);
        d.setStyle("-fx-font-size: 11px; -fx-text-fill: " + TEXT_GRAY + "; -fx-font-family: 'Segoe UI', Arial;");
        text.getChildren().addAll(lbl, d);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        // Custom toggle switch
        StackPane toggle = buildToggle(defaultOn);

        row.getChildren().addAll(text, sp, toggle);
        return row;
    }

    private StackPane buildToggle(boolean on) {
        BooleanProperty state = new SimpleBooleanProperty(on);

        Rectangle track = new Rectangle(40, 20);
        track.setArcWidth(20); track.setArcHeight(20);
        track.setFill(Color.web(on ? BLUE : "#2a2a2a"));

        Circle thumb = new Circle(8);
        thumb.setFill(Color.WHITE);
        thumb.setTranslateX(on ? 10 : -10);

        StackPane sp = new StackPane(track, thumb);
        sp.setStyle("-fx-cursor: hand;");

        sp.setOnMouseClicked(e -> {
            state.set(!state.get());
            track.setFill(Color.web(state.get() ? BLUE : "#2a2a2a"));
            thumb.setTranslateX(state.get() ? 10 : -10);
        });
        return sp;
    }

    // Labeled control row
    private HBox labeledRow(String label, javafx.scene.Node control) {
        HBox row = new HBox(16);
        row.setPadding(new Insets(10, 16, 10, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label);
        lbl.setMinWidth(180);
        lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TEXT_LIGHT + "; -fx-font-family: 'Segoe UI', Arial;");

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        row.getChildren().addAll(lbl, sp, control);
        return row;
    }

    private ComboBox<String> comboInput(String[] options, String selected) {
        ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList(options));
        cb.setValue(selected);
        cb.setPrefWidth(220);
        cb.setPrefHeight(34);
        cb.setStyle(
                "-fx-background-color: " + BG_INPUT + ";" +
                        "-fx-border-color: " + BORDER + "; -fx-border-radius: 5; -fx-background-radius: 5;" +
                        "-fx-text-fill: " + TEXT_WHITE + "; -fx-font-family: 'Segoe UI', Arial; -fx-font-size: 12px;"
        );
        return cb;
    }

    private HBox folderPickerRow(String defaultPath) {
        TextField tf = new TextField(defaultPath);
        tf.setPrefWidth(260);
        tf.setPrefHeight(34);
        tf.setStyle(
                "-fx-background-color: " + BG_INPUT + "; -fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 5; -fx-background-radius: 5;" +
                        "-fx-text-fill: " + TEXT_LIGHT + "; -fx-font-family: 'Segoe UI', Arial; -fx-font-size: 11px;"
        );
        Button browse = new Button("Browse");
        browse.setPrefHeight(34);
        browse.setStyle(
                "-fx-background-color: " + BG_CONTROL + "; -fx-text-fill: " + TEXT_GRAY + ";" +
                        "-fx-font-family: 'Segoe UI', Arial; -fx-font-size: 12px;" +
                        "-fx-border-color: " + BORDER + "; -fx-border-radius: 5; -fx-background-radius: 5;" +
                        "-fx-padding: 0 12 0 12; -fx-cursor: hand;"
        );
        browse.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = dc.showDialog(null);
            if (f != null) tf.setText(f.getAbsolutePath());
        });
        HBox row = new HBox(6, tf, browse);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private TextField readOnlyInput(String val) {
        TextField tf = new TextField(val);
        tf.setEditable(false);
        tf.setPrefWidth(320);
        tf.setPrefHeight(32);
        tf.setStyle(
                "-fx-background-color: " + BG_CONTROL + "; -fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 5; -fx-background-radius: 5;" +
                        "-fx-text-fill: " + TEXT_GRAY + "; -fx-font-family: 'Segoe UI', Arial; -fx-font-size: 11px;"
        );
        return tf;
    }

    private HBox timePickerRow(String defaultTime) {
        TextField tf = new TextField(defaultTime);
        tf.setPrefWidth(120); tf.setPrefHeight(34);
        tf.setStyle(
                "-fx-background-color: " + BG_INPUT + "; -fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 5; -fx-background-radius: 5;" +
                        "-fx-text-fill: " + TEXT_WHITE + "; -fx-font-family: 'Segoe UI', Arial; -fx-font-size: 12px;"
        );
        HBox row = new HBox(tf);
        return row;
    }

    private HBox typeRow(String category, String extensions, String color) {
        HBox row = new HBox(12);
        row.setPadding(new Insets(10, 16, 10, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        Label cat = new Label(category);
        cat.setMinWidth(100);
        cat.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + color + "; -fx-font-family: 'Segoe UI', Arial;");

        TextField extField = new TextField(extensions);
        extField.setPrefHeight(30);
        HBox.setHgrow(extField, Priority.ALWAYS);
        extField.setStyle(
                "-fx-background-color: " + BG_INPUT + "; -fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 5; -fx-background-radius: 5;" +
                        "-fx-text-fill: " + TEXT_LIGHT + "; -fx-font-family: 'Segoe UI', Arial; -fx-font-size: 11px;"
        );

        row.getChildren().addAll(cat, extField);
        return row;
    }

    private HBox ruleRow(String pattern, String action, String color) {
        HBox row = new HBox(12);
        row.setPadding(new Insets(7, 16, 7, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        Label p = new Label(pattern);
        p.setMinWidth(100);
        p.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_WHITE + "; -fx-font-family: 'Courier New', monospace;");

        Label a = new Label(action);
        a.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + "; -fx-font-family: 'Segoe UI', Arial;");

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        Button del = new Button("✕");
        del.setStyle("-fx-background-color: transparent; -fx-text-fill: " + RED + "; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 2 6 2 6;");

        row.getChildren().addAll(p, a, sp, del);
        return row;
    }

    private HBox exclusionRow(String path) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(6, 16, 6, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(path);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + TEXT_LIGHT + "; -fx-font-family: 'Courier New', monospace;");
        HBox.setHgrow(lbl, Priority.ALWAYS);

        Button del = new Button("✕");
        del.setStyle("-fx-background-color: transparent; -fx-text-fill: " + RED + "; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 2 6 2 6;");

        row.getChildren().addAll(lbl, del);
        return row;
    }

    private HBox infoRow(String icon, String msg) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(8, 16, 4, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        Label ic = new Label(icon);
        ic.setStyle("-fx-font-size: 13px;");
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + TEXT_GRAY + "; -fx-font-family: 'Segoe UI', Arial;");
        lbl.setWrapText(true);

        row.getChildren().addAll(ic, lbl);
        return row;
    }

    private Region divider() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setMaxWidth(Double.MAX_VALUE);
        r.setStyle("-fx-background-color: " + DIVIDER + ";");
        return r;
    }

    private Region vgap(double h) {
        Region r = new Region();
        r.setPrefHeight(h);
        return r;
    }

    private Button blueBtn(String text) {
        Button b = new Button(text);
        b.setStyle(
                "-fx-background-color: " + BLUE + "; -fx-text-fill: " + TEXT_WHITE + ";" +
                        "-fx-font-family: 'Segoe UI', Arial; -fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 6; -fx-padding: 8 22 8 22; -fx-cursor: hand;"
        );
        b.setOnMouseEntered(e -> b.setStyle(b.getStyle().replace(BLUE + ";", BLUE_HOVER + ";")));
        b.setOnMouseExited(e  -> b.setStyle(b.getStyle().replace(BLUE_HOVER + ";", BLUE + ";")));
        return b;
    }

    private static Button grayBtn(String text) {
        Button b = new Button(text);
        b.setStyle(
                "-fx-background-color: #1a1a1a; -fx-text-fill: " + TEXT_GRAY + ";" +
                        "-fx-font-family: 'Segoe UI', Arial; -fx-font-size: 13px;" +
                        "-fx-background-radius: 6; -fx-padding: 8 18 8 18; -fx-cursor: hand;" +
                        "-fx-border-color: #2a2a2a; -fx-border-radius: 6;"
        );
        return b;
    }

    private Button blueOutlineBtn(String text) {
        Button b = new Button(text);
        b.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: " + BLUE_LIGHT + ";" +
                        "-fx-font-family: 'Segoe UI', Arial; -fx-font-size: 12px;" +
                        "-fx-border-color: " + BORDER_BLUE + "; -fx-border-radius: 6; -fx-background-radius: 6;" +
                        "-fx-padding: 6 16 6 16; -fx-cursor: hand;"
        );
        VBox.setMargin(b, new Insets(0, 16, 8, 16));
        return b;
    }

    private void showConfirm(String title, String msg, Runnable onConfirm) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait().ifPresent(r -> { if (r == ButtonType.OK) onConfirm.run(); });
    }
}

