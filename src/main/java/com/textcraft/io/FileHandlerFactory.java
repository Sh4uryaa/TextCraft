package com.textcraft.io;

import com.textcraft.util.TextUtils;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * OOP: Factory - picks the right FileFormatHandler for a file extension.
 * The registry is a Map from extension to handler; unknown extensions fall back to plain text.
 */
public final class FileHandlerFactory {
    private static final Map<String, FileFormatHandler> REGISTRY = new LinkedHashMap<>();
    private static final TxtHandler PLAIN = new TxtHandler();

    /** Extensions that open in Code mode (RSyntaxTextArea) instead of the rich editor. */
    private static final Set<String> CODE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "py", "java", "c", "cpp", "h", "hpp", "js", "ts", "css", "sql", "sh", "bat", "json", "xml",
            "yaml", "yml", "ini", "properties", "cfg"));

    static {
        register(new HtmlHandler());
        register(new MdHandler());
        register(new RtfHandler());
        register(new DocxHandler());
        register(new PdfHandler());
        register(PLAIN);
    }

    private FileHandlerFactory() { }

    private static void register(AbstractFormatHandler h) {
        for (String ext : h.extensions()) REGISTRY.put(ext, h);
    }

    public static FileFormatHandler forExtension(String ext) {
        FileFormatHandler h = REGISTRY.get(ext == null ? "" : ext.toLowerCase());
        return h != null ? h : PLAIN; // unknown extension = plain text
    }

    public static FileFormatHandler forFile(File f) {
        return forExtension(TextUtils.extension(f.getName()));
    }

    public static boolean isKnown(String ext) {
        return REGISTRY.containsKey(ext.toLowerCase());
    }

    public static boolean isCode(String ext) {
        return CODE_EXTENSIONS.contains(ext.toLowerCase());
    }
}
