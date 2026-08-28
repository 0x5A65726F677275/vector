package com.artofvector.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.artofvector.log.AppLog;
import com.artofvector.ui.theme.UiControls;
import com.artofvector.ui.theme.UiIcons;
import com.artofvector.ui.theme.UiTheme;
import com.artofvector.workspace.Workspace;
import com.sun.jna.Platform;

/**
 * Interactive shell in the workspace folder. Line-oriented: type a command, Enter.
 */
public final class TerminalPanel extends JPanel {

    private static final int MAX_CHARS = 200_000;

    private final Workspace workspace;
    private final JTextArea output = new JTextArea();
    private final JTextField input = new JTextField();
    private final JLabel folderLabel = new JLabel();
    private final List<String> history = new ArrayList<>();
    private int historyIndex;
    private final AtomicBoolean running = new AtomicBoolean();

    private Process process;
    private OutputStream stdin;
    private Path cwd;

    public TerminalPanel(Workspace workspace) {
        super(new BorderLayout());
        this.workspace = workspace;
        setBackground(UiTheme.BG_INPUT);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER),
                BorderFactory.createEmptyBorder()));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UiTheme.BG_ELEVATED);
        header.setBorder(new EmptyBorder(4, 8, 4, 8));

        JLabel title = new JLabel("Terminal", UiIcons.of(UiIcons.Glyph.CONSOLE, 14), JLabel.LEFT);
        title.setIconTextGap(8);
        title.setForeground(UiTheme.TEXT_MUTED);
        title.setFont(UiTheme.UI_FONT_BOLD);

        folderLabel.setForeground(UiTheme.TEXT_DIM);
        folderLabel.setFont(UiTheme.MONO_SMALL);
        folderLabel.setBorder(new EmptyBorder(0, 12, 0, 0));

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(title, BorderLayout.WEST);
        left.add(folderLabel, BorderLayout.CENTER);

        JPanel tools = UiControls.toolbar();
        tools.setBorder(null);
        tools.setBackground(UiTheme.BG_ELEVATED);
        tools.add(UiControls.toolButton("Kill", UiIcons.Glyph.STOP, this::interrupt));
        tools.add(UiControls.toolButton("Restart", UiIcons.Glyph.RESET, this::restart));

        header.add(left, BorderLayout.CENTER);
        header.add(tools, BorderLayout.EAST);

        output.setEditable(false);
        output.setLineWrap(true);
        output.setWrapStyleWord(true);
        output.setBackground(UiTheme.BG_INPUT);
        output.setForeground(UiTheme.TEXT);
        output.setCaretColor(UiTheme.TEXT);
        output.setFont(UiTheme.MONO_FONT);
        output.setMargin(new Insets(8, 10, 8, 10));

        JScrollPane scroll = new JScrollPane(output);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UiTheme.BG_INPUT);

        JLabel prompt = new JLabel(Platform.isWindows() ? ">" : "$");
        prompt.setForeground(UiTheme.ACCENT);
        prompt.setFont(UiTheme.MONO_FONT);
        prompt.setBorder(new EmptyBorder(0, 10, 0, 8));

        input.setFont(UiTheme.MONO_FONT);
        input.setBackground(UiTheme.BG_INPUT);
        input.setForeground(UiTheme.TEXT);
        input.setCaretColor(UiTheme.TEXT);
        input.setBorder(new EmptyBorder(8, 0, 8, 10));
        input.addActionListener(e -> submit());
        input.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    history(-1);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    history(1);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_C && e.isControlDown()) {
                    interrupt();
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_L && e.isControlDown()) {
                    output.setText("");
                    e.consume();
                }
            }
        });

        JPanel promptRow = new JPanel(new BorderLayout());
        promptRow.setBackground(UiTheme.BG_INPUT);
        promptRow.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER));
        promptRow.add(prompt, BorderLayout.WEST);
        promptRow.add(input, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(promptRow, BorderLayout.SOUTH);

        workspace.addFolderListener(this::onFolderChanged);
        cwd = workspace.rootFolder();
        refreshFolderLabel();
        startShell();
    }

    public void focusInput() {
        SwingUtilities.invokeLater(input::requestFocusInWindow);
    }

    public void applyFont() {
        output.setFont(UiTheme.MONO_FONT);
        input.setFont(UiTheme.MONO_FONT);
        folderLabel.setFont(UiTheme.MONO_SMALL);
        revalidate();
        repaint();
    }

    public void shutdown() {
        running.set(false);
        Process current = process;
        if (current != null) {
            current.destroyForcibly();
        }
    }

    public void restart() {
        shutdown();
        startShell();
        focusInput();
    }

    private void interrupt() {
        OutputStream out = stdin;
        if (out != null) {
            try {
                out.write(3);
                out.flush();
                return;
            } catch (IOException ignored) {
                // Fall through to kill.
            }
        }
        restart();
    }

    private void onFolderChanged(Path folder) {
        cwd = folder;
        refreshFolderLabel();
        if (running.get() && folder != null && Files.isDirectory(folder)) {
            sendRaw(cdCommand(folder));
        } else {
            restart();
        }
    }

    private void submit() {
        String line = input.getText();
        input.setText("");
        if (!line.isEmpty()) {
            history.add(line);
            historyIndex = history.size();
        }
        append(line + "\n");
        sendRaw(line);
    }

    private void sendRaw(String line) {
        OutputStream out = stdin;
        if (out == null) {
            append("[no shell — Restart]\n");
            return;
        }
        try {
            Charset charset = charset();
            out.write((line + System.lineSeparator()).getBytes(charset));
            out.flush();
        } catch (IOException e) {
            append("[send failed] " + e.getMessage() + "\n");
            restart();
        }
    }

    private void history(int delta) {
        if (history.isEmpty()) {
            return;
        }
        historyIndex = Math.max(0, Math.min(history.size(), historyIndex + delta));
        if (historyIndex >= history.size()) {
            input.setText("");
            return;
        }
        input.setText(history.get(historyIndex));
        input.setCaretPosition(input.getText().length());
    }

    private synchronized void startShell() {
        running.set(false);
        Process previous = process;
        if (previous != null) {
            previous.destroyForcibly();
        }
        cwd = workspace.rootFolder();
        refreshFolderLabel();
        try {
            ProcessBuilder builder = new ProcessBuilder(shellArgv());
            builder.redirectErrorStream(true);
            if (cwd != null && Files.isDirectory(cwd)) {
                builder.directory(cwd.toFile());
            }
            builder.environment().put("TERM", "dumb");
            builder.environment().put("PS1", "\\w $ ");
            process = builder.start();
            stdin = process.getOutputStream();
            running.set(true);
            Thread reader = new Thread(() -> drain(process), "terminal-stdout");
            reader.setDaemon(true);
            reader.start();
            append(banner());
        } catch (IOException e) {
            stdin = null;
            process = null;
            append("[failed to start shell] " + e.getMessage() + "\n");
            AppLog.error("Terminal failed to start", e);
        }
    }

    private void drain(Process target) {
        Charset charset = charset();
        try (InputStream in = target.getInputStream()) {
            byte[] chunk = new byte[4096];
            int n;
            while ((n = in.read(chunk)) >= 0) {
                append(new String(chunk, 0, n, charset));
            }
        } catch (IOException ignored) {
            // Process closed.
        } finally {
            if (process == target) {
                running.set(false);
                stdin = null;
                append("\n[shell exited]\n");
            }
        }
    }

    private void append(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Runnable write = () -> {
            output.append(text);
            int extra = output.getDocument().getLength() - MAX_CHARS;
            if (extra > 0) {
                output.replaceRange("", 0, extra);
            }
            output.setCaretPosition(output.getDocument().getLength());
        };
        if (SwingUtilities.isEventDispatchThread()) {
            write.run();
        } else {
            SwingUtilities.invokeLater(write);
        }
    }

    private void refreshFolderLabel() {
        folderLabel.setText(cwd == null ? "no folder" : cwd.toAbsolutePath().toString());
    }

    private String banner() {
        String shell = Platform.isWindows() ? "cmd.exe" : "bash";
        String dir = cwd == null ? System.getProperty("user.dir") : cwd.toAbsolutePath().toString();
        return shell + "  " + dir + "\n";
    }

    private static List<String> shellArgv() {
        if (Platform.isWindows()) {
            return List.of("cmd.exe");
        }
        return List.of("bash", "--noprofile", "--norc");
    }

    private static String cdCommand(Path folder) {
        String path = folder.toAbsolutePath().toString();
        if (Platform.isWindows()) {
            return "cd /d \"" + path.replace("\"", "") + "\"";
        }
        return "cd \"" + path.replace("\"", "\\\"") + "\"";
    }

    private static Charset charset() {
        return Platform.isWindows() ? Charset.defaultCharset() : StandardCharsets.UTF_8;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(800, 180);
    }

    public Font terminalFont() {
        return output.getFont();
    }
}
