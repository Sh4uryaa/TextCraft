package com.textcraft.auth;

import com.textcraft.model.User;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.SwingUtilities;

/** OOP: Singleton + Observer - who is signed in, and whether the database is reachable. */
public final class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();

    private final List<SessionListener> listeners = new CopyOnWriteArrayList<>();
    private volatile User user;
    private volatile boolean databaseAvailable;

    private SessionManager() { }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void addListener(SessionListener l) {
        listeners.add(l);
    }

    public User getUser() {
        return user;
    }

    public boolean isSignedIn() {
        return user != null;
    }

    public boolean isDatabaseAvailable() {
        return databaseAvailable;
    }

    public void setDatabaseAvailable(boolean available) {
        this.databaseAvailable = available;
    }

    /** Cloud features need both a signed-in user and a reachable database. */
    public boolean canUseCloud() {
        return user != null && databaseAvailable;
    }

    public void setUser(User u) {
        this.user = u;
        SwingUtilities.invokeLater(() -> listeners.forEach(l -> l.onSessionChanged(u)));
    }
}
