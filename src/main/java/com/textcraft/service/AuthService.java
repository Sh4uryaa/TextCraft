package com.textcraft.service;

import com.textcraft.auth.AuthException;
import com.textcraft.auth.GoogleAuthService;
import com.textcraft.auth.SessionManager;
import com.textcraft.dao.UserDAO;
import com.textcraft.exception.DatabaseException;
import com.textcraft.model.User;
import com.textcraft.util.PasswordUtil;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * OOP: Facade - email/password accounts and Google accounts behind one class.
 * Per-user data starts here: the signed-in User goes into SessionManager.
 */
public class AuthService {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserDAO users;
    private final GoogleAuthService google;
    private final SessionManager session = SessionManager.getInstance();

    public AuthService(UserDAO users, GoogleAuthService google) {
        this.users = users;
        this.google = google;
    }

    public GoogleAuthService google() {
        return google;
    }

    public User register(String email, String password, String displayName) throws AuthException, DatabaseException {
        email = email == null ? "" : email.trim().toLowerCase();
        if (!EMAIL.matcher(email).matches()) throw new AuthException("Please enter a valid email address.");
        if (password == null || password.length() < 8) throw new AuthException("Password must be at least 8 characters.");
        if (users.findByEmail(email).isPresent()) throw new AuthException("This email is already registered. Please sign in.");
        User u = new User();
        u.setEmail(email);
        u.setDisplayName(displayName == null || displayName.trim().isEmpty() ? email : displayName.trim());
        u.setSalt(PasswordUtil.newSalt());
        u.setPasswordHash(PasswordUtil.hash(password, u.getSalt()));
        users.save(u);
        session.setUser(u);
        return u;
    }

    public User login(String email, String password) throws AuthException, DatabaseException {
        email = email == null ? "" : email.trim().toLowerCase();
        Optional<User> found = users.findByEmail(email);
        // Same message for unknown email and wrong password: do not reveal which accounts exist.
        if (!found.isPresent() || !PasswordUtil.verify(password, found.get().getSalt(), found.get().getPasswordHash())) {
            throw new AuthException("Wrong email or password.");
        }
        session.setUser(found.get());
        return found.get();
    }

    /** Opens the browser, waits for the user, then creates/finds the account. Call from a worker thread. */
    public User signInWithGoogle() throws AuthException, DatabaseException {
        return finishGoogle(google.signIn());
    }

    /** Auto-login at startup. Empty if there is no stored token. */
    public Optional<User> trySilentGoogle() throws DatabaseException {
        Optional<GoogleAuthService.Profile> p = google.silentSignIn();
        return p.isPresent() ? Optional.of(finishGoogle(p.get())) : Optional.empty();
    }

    private User finishGoogle(GoogleAuthService.Profile p) throws DatabaseException {
        Optional<User> existing = users.findByGoogleId(p.sub);
        if (!existing.isPresent()) existing = users.findByEmail(p.email.toLowerCase());
        User u = existing.orElseGet(User::new);
        u.setGoogleId(p.sub);
        u.setEmail(p.email.toLowerCase());
        u.setDisplayName(p.name.isEmpty() ? p.email : p.name);
        u.setAvatarUrl(p.picture);
        users.save(u);
        session.setUser(u);
        return u;
    }

    public void signOut() {
        google.signOut();
        session.setUser(null);
    }
}
