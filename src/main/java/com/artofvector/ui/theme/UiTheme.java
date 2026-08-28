package com.artofvector.ui.theme;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.border.Border;

/**
 * Dark workbench palette shared by Swing chrome and custom Java2D views.
 */
public final class UiTheme {

    public static final Color BG_ROOT = new Color(0x0F1419);
    public static final Color BG_PANEL = new Color(0x151B22);
    public static final Color BG_ELEVATED = new Color(0x1C242D);
    public static final Color BG_INPUT = new Color(0x12181F);
    public static final Color BG_HOVER = new Color(0x24303A);
    public static final Color BG_SELECTED = new Color(0x1A3A3A);

    public static final Color ACCENT = new Color(0x3DCCC7);
    public static final Color ACCENT_DIM = new Color(0x2A9D98);
    public static final Color ACCENT_SOFT = new Color(61, 204, 199, 48);

    public static final Color TEXT = new Color(0xE6EDF3);
    public static final Color TEXT_MUTED = new Color(0x8B9BAB);
    public static final Color TEXT_DIM = new Color(0x5C6B78);

    public static final Color BORDER = new Color(0x2B3640);
    public static final Color GRID = new Color(0x1E2730);

    public static final Color DANGER = new Color(0xF85149);
    public static final Color SUCCESS = new Color(0x3FB950);
    public static final Color WARNING = new Color(0xD29922);
    public static final Color CURRENT = new Color(0x58A6FF);

    public static final Color BREAKPOINT = DANGER;
    public static final Color RIP_HIGHLIGHT = new Color(0x1A3F4A);

    public static final Font UI_FONT = new Font(pickUiFamily(), Font.PLAIN, 13);
    public static final Font UI_FONT_BOLD = UI_FONT.deriveFont(Font.BOLD);
    public static final Font MONO_FONT = new Font(pickMonoFamily(), Font.PLAIN, 13);
    public static final Font MONO_SMALL = MONO_FONT.deriveFont(12f);

    private UiTheme() {
    }

    public static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // Keep the platform default if Metal cannot be loaded.
        }

        UIManager.put("control", BG_PANEL);
        UIManager.put("info", BG_ELEVATED);
        UIManager.put("nimbusBase", BG_ROOT);
        UIManager.put("nimbusFocus", ACCENT);

        UIManager.put("Panel.background", BG_PANEL);
        UIManager.put("Panel.foreground", TEXT);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Label.background", BG_PANEL);
        UIManager.put("Label.font", UI_FONT);

        UIManager.put("Button.background", BG_ELEVATED);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("Button.font", UI_FONT);
        UIManager.put("Button.focus", ACCENT);
        UIManager.put("Button.select", ACCENT_DIM);

        UIManager.put("ToggleButton.background", BG_ELEVATED);
        UIManager.put("ToggleButton.foreground", TEXT);

        UIManager.put("TextField.background", BG_INPUT);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", TEXT);
        UIManager.put("TextField.selectionBackground", ACCENT_DIM);
        UIManager.put("TextField.selectionForeground", Color.WHITE);
        UIManager.put("TextField.border", BorderFactory.createLineBorder(BORDER));

        UIManager.put("TextArea.background", BG_INPUT);
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("TextArea.caretForeground", TEXT);
        UIManager.put("TextArea.font", MONO_FONT);

        UIManager.put("ComboBox.background", BG_ELEVATED);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ComboBox.selectionBackground", ACCENT_DIM);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);

        UIManager.put("List.background", BG_PANEL);
        UIManager.put("List.foreground", TEXT);
        UIManager.put("List.selectionBackground", BG_SELECTED);
        UIManager.put("List.selectionForeground", TEXT);
        UIManager.put("List.font", UI_FONT);

        UIManager.put("Table.background", BG_PANEL);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.selectionBackground", BG_SELECTED);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("Table.font", MONO_SMALL);
        UIManager.put("TableHeader.background", BG_ELEVATED);
        UIManager.put("TableHeader.foreground", TEXT_MUTED);
        UIManager.put("TableHeader.font", UI_FONT);

        UIManager.put("Tree.background", BG_PANEL);
        UIManager.put("Tree.foreground", TEXT);
        UIManager.put("Tree.textBackground", BG_PANEL);
        UIManager.put("Tree.textForeground", TEXT);
        UIManager.put("Tree.selectionBackground", BG_SELECTED);
        UIManager.put("Tree.selectionForeground", TEXT);
        UIManager.put("Tree.hash", BORDER);
        UIManager.put("Tree.line", BORDER);
        UIManager.put("Tree.font", UI_FONT);

        UIManager.put("TabbedPane.background", BG_ROOT);
        UIManager.put("TabbedPane.foreground", TEXT_MUTED);
        UIManager.put("TabbedPane.selected", BG_PANEL);
        UIManager.put("TabbedPane.contentAreaColor", BG_PANEL);
        UIManager.put("TabbedPane.font", UI_FONT);

        UIManager.put("SplitPane.background", BG_ROOT);
        UIManager.put("SplitPane.dividerSize", 6);
        UIManager.put("SplitPaneDivider.draggingColor", ACCENT_DIM);

        UIManager.put("ScrollBar.background", BG_PANEL);
        UIManager.put("ScrollBar.thumb", BG_HOVER);
        UIManager.put("ScrollBar.track", BG_ROOT);

        UIManager.put("MenuBar.background", BG_ELEVATED);
        UIManager.put("MenuBar.foreground", TEXT);
        UIManager.put("Menu.background", BG_ELEVATED);
        UIManager.put("Menu.foreground", TEXT);
        UIManager.put("MenuItem.background", BG_ELEVATED);
        UIManager.put("MenuItem.foreground", TEXT);
        UIManager.put("MenuItem.selectionBackground", BG_SELECTED);
        UIManager.put("MenuItem.selectionForeground", TEXT);
        UIManager.put("MenuItem.font", UI_FONT);
        UIManager.put("Menu.font", UI_FONT);

        UIManager.put("PopupMenu.background", BG_ELEVATED);
        UIManager.put("PopupMenu.foreground", TEXT);
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(BORDER));

        UIManager.put("OptionPane.background", BG_PANEL);
        UIManager.put("OptionPane.foreground", TEXT);
        UIManager.put("OptionPane.messageForeground", TEXT);

        UIManager.put("ToolBar.background", BG_ELEVATED);
        UIManager.put("ToolBar.foreground", TEXT);
        UIManager.put("ToolBar.border", BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));

        UIManager.put("Separator.foreground", BORDER);
        UIManager.put("Separator.background", BORDER);
    }

    public static Border panelBorder() {
        return BorderFactory.createLineBorder(BORDER);
    }

    public static Border empty(int pad) {
        return BorderFactory.createEmptyBorder(pad, pad, pad, pad);
    }

    private static String pickUiFamily() {
        String[] candidates = {"Segoe UI", "Inter", "Noto Sans", "SansSerif"};
        return firstInstalled(candidates, "SansSerif");
    }

    private static String pickMonoFamily() {
        String[] candidates = {"JetBrains Mono", "Cascadia Mono", "Consolas", "Monospaced"};
        return firstInstalled(candidates, "Monospaced");
    }

    private static String firstInstalled(String[] families, String fallback) {
        String[] installed = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String wanted : families) {
            for (String have : installed) {
                if (have.equalsIgnoreCase(wanted)) {
                    return have;
                }
            }
        }
        return fallback;
    }
}
