package com.novaedit.plugin;

/**
 * ServiceLoader SPI for NovaEdit plugins.
 */
public interface Plugin {
    String getId();
    String getName();
    String getVersion();
    void onLoad(PluginContext context);
    default void onUnload() {}
}
