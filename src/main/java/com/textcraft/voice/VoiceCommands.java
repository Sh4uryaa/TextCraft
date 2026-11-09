package com.textcraft.voice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Spoken commands inside the recognised text: new line, new paragraph, full stop, comma, question mark. */
public final class VoiceCommands {
    private static final Map<String, String> COMMANDS = new LinkedHashMap<>();

    static {
        COMMANDS.put("new paragraph", "\n\n"); // before "new line" so the longer phrase wins
        COMMANDS.put("new line", "\n");
        COMMANDS.put("full stop", ".");
        COMMANDS.put("question mark", "?");
        COMMANDS.put("comma", ",");
    }

    private VoiceCommands() { }

    public static String apply(String transcript) {
        String s = transcript == null ? "" : transcript;
        for (Map.Entry<String, String> e : COMMANDS.entrySet()) {
            // swallow the punctuation the recogniser may have added next to the spoken command
            Pattern p = Pattern.compile("[,.]?\\s*\\b" + Pattern.quote(e.getKey()) + "\\b[,.]?", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(s);
            s = m.replaceAll(Matcher.quoteReplacement(e.getValue()));
        }
        return s.replaceAll("[ \\t]+\\n", "\n").replaceAll("\\n[ \\t]+", "\n");
    }
}
