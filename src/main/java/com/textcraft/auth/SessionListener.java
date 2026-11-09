package com.textcraft.auth;

import com.textcraft.model.User;

/** OOP: Observer - login/logout refreshes menus, avatar and settings. user is null after sign-out. */
public interface SessionListener {
    void onSessionChanged(User user);
}
