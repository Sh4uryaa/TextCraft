package com.novaedit.plugin;

import com.novaedit.commands.Command;
import com.novaedit.util.EventBus;

import java.util.function.Consumer;

/**
 * Context provided to plugins for extending NovaEdit.
 */
public interface PluginContext {
    void registerCommand(Command command);
    EventBus getEventBus();
    void showToast(String message);
    String getActiveDocumentText();
}
