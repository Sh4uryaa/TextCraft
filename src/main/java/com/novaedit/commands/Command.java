package com.novaedit.commands;

/**
 * Encapsulates an executable command with metadata, category, icon, and default shortcut.
 */
public record Command(
        String id,
        String title,
        String category,
        String defaultShortcut,
        String iconLiteral,
        Runnable action
) {
    public void execute() {
        if (action != null) {
            action.run();
        }
    }
}
