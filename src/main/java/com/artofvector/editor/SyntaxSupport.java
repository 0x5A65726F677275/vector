package com.artofvector.editor;

import java.nio.file.Path;
import java.util.Locale;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

public final class SyntaxSupport {

    private SyntaxSupport() {
    }

    public static String forFile(Path file) {
        if (file == null) {
            return SyntaxConstants.SYNTAX_STYLE_NONE;
        }
        return forName(file.getFileName().toString());
    }

    public static String forName(String fileName) {
        String name = fileName.toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 ? name.substring(dot + 1) : "";
        return switch (ext) {
            case "java" -> SyntaxConstants.SYNTAX_STYLE_JAVA;
            case "py" -> SyntaxConstants.SYNTAX_STYLE_PYTHON;
            case "asm", "s", "S", "nasm", "inc" -> SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86;
            case "c", "h" -> SyntaxConstants.SYNTAX_STYLE_C;
            case "cpp", "cc", "cxx", "hpp" -> SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS;
            case "json" -> SyntaxConstants.SYNTAX_STYLE_JSON;
            case "xml" -> SyntaxConstants.SYNTAX_STYLE_XML;
            case "md" -> SyntaxConstants.SYNTAX_STYLE_MARKDOWN;
            case "sh", "bash" -> SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL;
            case "js" -> SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT;
            default -> {
                if (name.endsWith(".s")) {
                    yield SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86;
                }
                yield SyntaxConstants.SYNTAX_STYLE_NONE;
            }
        };
    }
}
