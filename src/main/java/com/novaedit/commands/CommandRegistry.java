package com.novaedit.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Central registry of all editor commands.
 * Powers the menu bar, toolbar, keymap, and fuzzy-search Command Palette.
 */
public class CommandRegistry {
    private static final Logger log = LoggerFactory.getLogger(CommandRegistry.class);
    private static final CommandRegistry INSTANCE = new CommandRegistry();

    public static CommandRegistry getInstance() {
        return INSTANCE;
    }

    private final Map<String, Command> commands = new LinkedHashMap<>();
    private final Map<String, String> customKeybindings = new ConcurrentHashMap<>();

    public synchronized void register(Command command) {
        if (command != null) {
            commands.put(command.id(), command);
        }
    }

    public synchronized Command get(String id) {
        return commands.get(id);
    }

    public synchronized Collection<Command> getAll() {
        return Collections.unmodifiableCollection(commands.values());
    }

    public synchronized void execute(String id) {
        Command cmd = commands.get(id);
        if (cmd != null) {
            try {
                cmd.execute();
            } catch (Exception e) {
                log.error("Error executing command {}", id, e);
            }
        } else {
            log.warn("Command not found: {}", id);
        }
    }

    /**
     * Fuzzy searches commands by query matching title, category, id, or shortcut.
     */
    public synchronized List<Command> search(String query) {
        if (query == null || query.isBlank()) {
            return new ArrayList<>(commands.values());
        }
        String q = query.trim().toLowerCase();
        return commands.values().stream()
                .filter(cmd -> cmd.title().toLowerCase().contains(q)
                        || cmd.category().toLowerCase().contains(q)
                        || (cmd.defaultShortcut() != null && cmd.defaultShortcut().toLowerCase().contains(q))
                        || cmd.id().toLowerCase().contains(q))
                .sorted(Comparator.comparing((Command cmd) -> !cmd.title().toLowerCase().startsWith(q))
                        .thenComparing(Command::title))
                .collect(Collectors.toList());
    }

    public synchronized void setCustomShortcut(String commandId, String shortcut) {
        if (shortcut == null || shortcut.isBlank()) {
            customKeybindings.remove(commandId);
        } else {
            customKeybindings.put(commandId, shortcut);
        }
    }

    public synchronized String getEffectiveShortcut(String commandId) {
        if (customKeybindings.containsKey(commandId)) {
            return customKeybindings.get(commandId);
        }
        Command cmd = commands.get(commandId);
        return cmd != null ? cmd.defaultShortcut() : "";
    }

    public synchronized Map<String, String> getCustomKeybindings() {
        return new HashMap<>(customKeybindings);
    }
}
