package com.novaedit.plugin.sample;

import com.novaedit.commands.Command;
import com.novaedit.plugin.Plugin;
import com.novaedit.plugin.PluginContext;

/**
 * Sample plugin demonstrating the NovaEdit Plugin SPI.
 * Adds a "Count Words & Characters" command.
 */
public class WordCountPlugin implements Plugin {

    @Override
    public String getId() {
        return "com.novaedit.wordcount";
    }

    @Override
    public String getName() {
        return "Word Counter";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public void onLoad(PluginContext context) {
        Command countCommand = new Command(
                "plugin.wordcount",
                "Count Words and Characters",
                "Tools",
                null,
                "feather-hash",
                () -> {
                    String text = context.getActiveDocumentText();
                    if (text == null || text.isEmpty()) {
                        context.showToast("Document is empty (0 words, 0 chars)");
                        return;
                    }
                    int chars = text.length();
                    String[] words = text.trim().split("\\s+");
                    int wordCount = text.trim().isEmpty() ? 0 : words.length;
                    context.showToast("Stats: " + wordCount + " words | " + chars + " characters");
                }
        );
        context.registerCommand(countCommand);
    }
}
