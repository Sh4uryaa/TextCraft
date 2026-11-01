package com.novaedit.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Discovers and initializes plugins via java.util.ServiceLoader.
 */
public class PluginLoader {
    private static final Logger log = LoggerFactory.getLogger(PluginLoader.class);
    private final List<Plugin> activePlugins = new ArrayList<>();

    public void loadPlugins(PluginContext context) {
        ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class);
        for (Plugin plugin : loader) {
            try {
                plugin.onLoad(context);
                activePlugins.add(plugin);
                log.info("Loaded plugin: {} v{}", plugin.getName(), plugin.getVersion());
            } catch (Exception e) {
                log.error("Failed to load plugin {}", plugin.getId(), e);
            }
        }
    }

    public void unloadPlugins() {
        for (Plugin plugin : activePlugins) {
            try {
                plugin.onUnload();
            } catch (Exception e) {
                log.error("Error unloading plugin {}", plugin.getId(), e);
            }
        }
        activePlugins.clear();
    }

    public List<Plugin> getActivePlugins() {
        return activePlugins;
    }
}
