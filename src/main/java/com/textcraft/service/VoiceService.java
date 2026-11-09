package com.textcraft.service;

import com.textcraft.app.AppConfig;
import com.textcraft.auth.SessionManager;
import com.textcraft.dao.VoiceUsageDAO;
import com.textcraft.db.DatabaseConnection;
import com.textcraft.exception.DatabaseException;
import com.textcraft.exception.VoiceException;
import com.textcraft.model.User;
import com.textcraft.voice.DaoUsageStore;
import com.textcraft.voice.FileUsageStore;
import com.textcraft.voice.GoogleSpeechRecognizer;
import com.textcraft.voice.MicRecorder;
import com.textcraft.voice.QuotaGuard;
import com.textcraft.voice.SpeechRecognizer;
import com.textcraft.voice.VoiceCommands;

/**
 * OOP: Facade - quota check, record, recognise, count seconds. Push-to-talk:
 * startRecording() blocks (worker thread), stopRecording() is called from the UI thread.
 */
public class VoiceService {
    private final SpeechRecognizer recognizer;
    private final MicRecorder recorder = new MicRecorder();

    public VoiceService() {
        this(new GoogleSpeechRecognizer(AppConfig.get().get("speech.apiKey", "")));
    }

    public VoiceService(SpeechRecognizer recognizer) {
        this.recognizer = recognizer;
    }

    /** Guard for the current user (database) or this PC (local file) when signed out / offline. */
    public QuotaGuard quota() {
        User u = SessionManager.getInstance().getUser();
        int cap = AppConfig.get().getInt("voice.monthlyCapMinutes", 60);
        if (u != null && SessionManager.getInstance().isDatabaseAvailable()) {
            return new QuotaGuard(new DaoUsageStore(new VoiceUsageDAO(DatabaseConnection.getInstance()), u.getId()), cap);
        }
        return new QuotaGuard(new FileUsageStore(), cap);
    }

    /** Blocks until stopRecording() or 60 s. Returns the text to insert ("" if nothing heard). */
    public String listen(String langCode) throws VoiceException, DatabaseException {
        QuotaGuard guard = quota();
        guard.checkAllowed();
        byte[] pcm = recorder.record();
        int seconds = MicRecorder.seconds(pcm);
        if (seconds < 1) return "";
        String text = recognizer.recognize(pcm, langCode);
        guard.record(seconds);   // counted only after a successful call
        return VoiceCommands.apply(text);
    }

    public void stopRecording() {
        recorder.stop();
    }
}
