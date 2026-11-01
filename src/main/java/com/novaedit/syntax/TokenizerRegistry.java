package com.novaedit.syntax;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry of available tokenizers with auto-detection by file extension and language ID.
 */
public class TokenizerRegistry {
    private static final Map<String, Tokenizer> TOKENIZERS_BY_ID = new LinkedHashMap<>();
    private static final Map<String, Tokenizer> TOKENIZERS_BY_EXT = new LinkedHashMap<>();
    private static final Tokenizer DEFAULT_TOKENIZER = new PlainLexer();

    static {
        JavaLexer java = new JavaLexer();
        JsonLexer json = new JsonLexer();
        XmlLexer xml = new XmlLexer();
        MarkdownLexer md = new MarkdownLexer();
        PlainLexer plain = new PlainLexer();

        register(java, "java");
        register(json, "json");
        register(xml, "xml", "html", "htm", "svg", "fxml", "xhtml");
        register(md, "md", "markdown");
        register(plain, "txt", "log", "csv", "tsv");
    }

    public static synchronized void register(Tokenizer tokenizer, String... extensions) {
        TOKENIZERS_BY_ID.put(tokenizer.getLanguageId().toLowerCase(), tokenizer);
        for (String ext : extensions) {
            TOKENIZERS_BY_EXT.put(ext.toLowerCase(), tokenizer);
        }
    }

    public static Tokenizer getById(String id) {
        if (id == null) return DEFAULT_TOKENIZER;
        return TOKENIZERS_BY_ID.getOrDefault(id.toLowerCase(), DEFAULT_TOKENIZER);
    }

    public static Tokenizer detect(Path path) {
        if (path == null || path.getFileName() == null) {
            return DEFAULT_TOKENIZER;
        }
        String fileName = path.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            String ext = fileName.substring(lastDot + 1).toLowerCase();
            return TOKENIZERS_BY_EXT.getOrDefault(ext, DEFAULT_TOKENIZER);
        }
        return DEFAULT_TOKENIZER;
    }

    public static Map<String, Tokenizer> getAll() {
        return Collections.unmodifiableMap(TOKENIZERS_BY_ID);
    }
}
