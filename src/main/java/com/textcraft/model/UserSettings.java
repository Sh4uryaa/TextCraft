package com.textcraft.model;

/** Per-user settings (also kept in a local file so they work offline). */
public class UserSettings {
    private Long userId;
    private String theme = "LIGHT";
    private String defaultFont = "Arial";
    private int defaultFontSize = 12;
    private String geminiModel = "gemini-2.0-flash";
    private String voiceLanguage = "en-US";
    private double voiceSpeed = 1.0;
    private boolean liteMode;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public String getDefaultFont() { return defaultFont; }
    public void setDefaultFont(String defaultFont) { this.defaultFont = defaultFont; }
    public int getDefaultFontSize() { return defaultFontSize; }
    public void setDefaultFontSize(int defaultFontSize) { this.defaultFontSize = defaultFontSize; }
    public String getGeminiModel() { return geminiModel; }
    public void setGeminiModel(String geminiModel) { this.geminiModel = geminiModel; }
    public String getVoiceLanguage() { return voiceLanguage; }
    public void setVoiceLanguage(String voiceLanguage) { this.voiceLanguage = voiceLanguage; }
    public double getVoiceSpeed() { return voiceSpeed; }
    public void setVoiceSpeed(double voiceSpeed) { this.voiceSpeed = voiceSpeed; }
    public boolean isLiteMode() { return liteMode; }
    public void setLiteMode(boolean liteMode) { this.liteMode = liteMode; }
}
