package com.artofvector.workspace;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * Persisted workbench preferences (font size, last workspace folder).
 */
public final class AppSettings {

    private static final Preferences PREFS = Preferences.userNodeForPackage(AppSettings.class);
    private static final String KEY_FONT_SIZE = "fontSize";
    private static final String KEY_FOLDER = "workspaceFolder";

    private AppSettings() {
    }

    public static int fontSize(int fallback) {
        return PREFS.getInt(KEY_FONT_SIZE, fallback);
    }

    public static void setFontSize(int size) {
        PREFS.putInt(KEY_FONT_SIZE, size);
    }

    public static Optional<Path> lastFolder() {
        String stored = PREFS.get(KEY_FOLDER, "");
        if (stored.isBlank()) {
            return Optional.empty();
        }
        Path folder = Path.of(stored);
        return Files.isDirectory(folder) ? Optional.of(folder) : Optional.empty();
    }

    public static void setLastFolder(Path folder) {
        if (folder != null) {
            PREFS.put(KEY_FOLDER, folder.toAbsolutePath().toString());
        }
    }
}
