package com.artofvector.workflow.engine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.artofvector.log.AppLog;
import com.artofvector.workflow.model.NodeContext;
import com.sun.jna.Platform;

/**
 * Runs a user-authored command line and streams output to the console.
 * Placeholders {@code {in}}, {@code {out}}, {@code {stdout}} expand to the previous node's stdout.
 */
public final class CommandLine {

    public static final int TIMEOUT_MINUTES = 10;

    private CommandLine() {
    }

    public record Result(int exitCode, String stdout, String stderr, String command) {
    }

    public static String pipelineText(NodeContext context) {
        if (context == null) {
            return "";
        }
        for (String key : List.of("in", "out", "stdout")) {
            Object value = context.input(key);
            if (value != null) {
                String text = String.valueOf(value);
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return "";
    }

    public static String expand(String command, NodeContext context) {
        String incoming = pipelineText(context);
        String expanded = command == null ? "" : command;
        expanded = expanded.replace("{in}", incoming);
        expanded = expanded.replace("{stdout}", incoming);
        expanded = expanded.replace("{out}", incoming);
        return expanded.trim();
    }

    public static Result run(String command, NodeContext context) throws IOException, InterruptedException {
        String expanded = expand(command, context);
        if (expanded.isBlank()) {
            throw new IOException("Command is empty — double-click the node and write a command");
        }
        AppLog.info("$ " + expanded);

        List<String> argv = Platform.isWindows()
                ? List.of("cmd.exe", "/c", expanded)
                : List.of("sh", "-c", expanded);

        ProcessBuilder builder = new ProcessBuilder(argv);
        builder.redirectErrorStream(false);
        Process process = builder.start();

        StreamCollector stdout = new StreamCollector(process.getInputStream(), false);
        StreamCollector stderr = new StreamCollector(process.getErrorStream(), true);
        stdout.start();
        stderr.start();

        boolean finished = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Command timed out after " + TIMEOUT_MINUTES + " minutes: " + expanded);
        }
        stdout.join();
        stderr.join();

        int code = process.exitValue();
        AppLog.info("exit " + code + (code == 0 ? "" : " (failed)"));
        return new Result(code, stdout.text(), stderr.text(), expanded);
    }

    private static final class StreamCollector extends Thread {
        private final InputStream in;
        private final boolean error;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        private StreamCollector(InputStream in, boolean error) {
            this.in = in;
            this.error = error;
            setDaemon(true);
            setName(error ? "cmd-stderr" : "cmd-stdout");
        }

        @Override
        public void run() {
            Charset charset = Platform.isWindows() ? Charset.defaultCharset() : StandardCharsets.UTF_8;
            try {
                byte[] chunk = new byte[4096];
                int n;
                StringBuilder line = new StringBuilder();
                while ((n = in.read(chunk)) >= 0) {
                    buffer.write(chunk, 0, n);
                    String piece = new String(chunk, 0, n, charset);
                    for (int i = 0; i < piece.length(); i++) {
                        char c = piece.charAt(i);
                        if (c == '\n') {
                            emit(line.toString());
                            line.setLength(0);
                        } else if (c != '\r') {
                            line.append(c);
                        }
                    }
                }
                if (line.length() > 0) {
                    emit(line.toString());
                }
            } catch (IOException ignored) {
                // Process closed the stream.
            }
        }

        private void emit(String line) {
            if (error) {
                AppLog.warn(line);
            } else {
                AppLog.info(line);
            }
        }

        private String text() {
            Charset charset = Platform.isWindows() ? Charset.defaultCharset() : StandardCharsets.UTF_8;
            return buffer.toString(charset).replace("\r\n", "\n").trim();
        }
    }
}
