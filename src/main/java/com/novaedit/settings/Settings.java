package com.novaedit.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration options for NovaEdit.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Settings {
    private String theme = "dark";
    private int fontSize = 14;
    private String fontFamily = "JetBrains Mono";
    private int tabSize = 4;
    private boolean useSpaces = true;
    private boolean wordWrap = false;
    private boolean showLineNumbers = true;
    private boolean showMinimap = true;
    private boolean showWhitespace = false;
    private boolean toolbarLabelsVisible = true;
    private int autoSaveIntervalSeconds = 30;
    private String uiFontFamily = "Segoe UI";
    private int uiFontSize = 13;
    private Map<String, String> keybindings = new HashMap<>();

    public Settings() {}

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = Math.max(8, Math.min(48, fontSize));
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public int getTabSize() {
        return tabSize;
    }

    public void setTabSize(int tabSize) {
        this.tabSize = Math.max(1, Math.min(8, tabSize));
    }

    public boolean isUseSpaces() {
        return useSpaces;
    }

    public void setUseSpaces(boolean useSpaces) {
        this.useSpaces = useSpaces;
    }

    public boolean isWordWrap() {
        return wordWrap;
    }

    public void setWordWrap(boolean wordWrap) {
        this.wordWrap = wordWrap;
    }

    public boolean isShowLineNumbers() {
        return showLineNumbers;
    }

    public void setShowLineNumbers(boolean showLineNumbers) {
        this.showLineNumbers = showLineNumbers;
    }

    public boolean isShowMinimap() {
        return showMinimap;
    }

    public void setShowMinimap(boolean showMinimap) {
        this.showMinimap = showMinimap;
    }

    public boolean isShowWhitespace() {
        return showWhitespace;
    }

    public void setShowWhitespace(boolean showWhitespace) {
        this.showWhitespace = showWhitespace;
    }

    public boolean isToolbarLabelsVisible() {
        return toolbarLabelsVisible;
    }

    public void setToolbarLabelsVisible(boolean toolbarLabelsVisible) {
        this.toolbarLabelsVisible = toolbarLabelsVisible;
    }

    public int getAutoSaveIntervalSeconds() {
        return autoSaveIntervalSeconds;
    }

    public void setAutoSaveIntervalSeconds(int autoSaveIntervalSeconds) {
        this.autoSaveIntervalSeconds = autoSaveIntervalSeconds;
    }

    public String getUiFontFamily() {
        return uiFontFamily;
    }

    public void setUiFontFamily(String uiFontFamily) {
        this.uiFontFamily = uiFontFamily;
    }

    public int getUiFontSize() {
        return uiFontSize;
    }

    public void setUiFontSize(int uiFontSize) {
        this.uiFontSize = uiFontSize;
    }

    public Map<String, String> getKeybindings() {
        return keybindings;
    }

    public void setKeybindings(Map<String, String> keybindings) {
        this.keybindings = keybindings != null ? keybindings : new HashMap<>();
    }
}
