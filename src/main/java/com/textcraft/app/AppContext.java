package com.textcraft.app;

import com.textcraft.auth.SessionManager;
import com.textcraft.dao.DocumentDAO;
import com.textcraft.dao.StudyDAO;
import com.textcraft.dao.UserDAO;
import com.textcraft.dao.VersionDAO;
import com.textcraft.db.DatabaseConnection;
import com.textcraft.db.SchemaInitializer;
import com.textcraft.exception.DatabaseException;
import com.textcraft.service.AIService;
import com.textcraft.auth.GoogleAuthService;
import com.textcraft.service.AuthService;
import com.textcraft.service.CodeRunService;
import com.textcraft.service.DocumentService;
import com.textcraft.service.VoiceService;
import com.textcraft.study.StudyService;
import com.textcraft.voice.SystemTtsService;
import com.textcraft.voice.TextToSpeechService;

/**
 * Creates and shares the services. The UI asks this class for a service and never builds DAOs or
 * touches SQL/HTTP itself. Everything heavy is created on first use (lazy loading for old PCs).
 */
public final class AppContext {
    private static final AppContext INSTANCE = new AppContext();

    private final DatabaseConnection db = DatabaseConnection.getInstance();
    private UserDAO users;
    private DocumentDAO documentDao;
    private VersionDAO versionDao;
    private StudyDAO studyDao;
    private AuthService auth;
    private DocumentService documents;
    private CodeRunService codeRunner;
    private VoiceService voice;
    private TextToSpeechService tts;

    private AppContext() { }

    public static AppContext get() {
        return INSTANCE;
    }

    /** Tries to connect and create the tables. Returns false (offline mode) when that is not possible. */
    public boolean initDatabase() {
        SessionManager s = SessionManager.getInstance();
        if (!db.isConfigured()) {
            s.setDatabaseAvailable(false);
            return false;
        }
        try {
            SchemaInitializer.ensureSchema(db);
            s.setDatabaseAvailable(true);
            return true;
        } catch (DatabaseException e) {
            java.util.logging.Logger.getLogger(AppContext.class.getName()).warning(e.getMessage());
            s.setDatabaseAvailable(false);
            return false;
        }
    }

    public synchronized UserDAO users() {
        if (users == null) users = new UserDAO(db);
        return users;
    }

    public synchronized StudyDAO studyDao() {
        if (studyDao == null) studyDao = new StudyDAO(db);
        return studyDao;
    }

    public synchronized AuthService auth() {
        if (auth == null) auth = new AuthService(users(), new GoogleAuthService());
        return auth;
    }

    public synchronized DocumentService documents() {
        if (documents == null) {
            documentDao = new DocumentDAO(db);
            versionDao = new VersionDAO(db);
            documents = new DocumentService(db, documentDao, versionDao);
        }
        return documents;
    }

    /** New each time so a changed model name in Settings is picked up. */
    public AIService ai() {
        return new AIService(AIService.providerFromConfig());
    }

    public StudyService study() {
        return new StudyService(AIService.providerFromConfig());
    }

    public synchronized CodeRunService codeRunner() {
        if (codeRunner == null) codeRunner = new CodeRunService();
        return codeRunner;
    }

    public synchronized VoiceService voice() {
        if (voice == null) voice = new VoiceService();
        return voice;
    }

    public synchronized TextToSpeechService tts() {
        if (tts == null) tts = new SystemTtsService();
        return tts;
    }
}
