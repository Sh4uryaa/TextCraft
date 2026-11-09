package com.textcraft.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PasswordUtilTest {
    @Test
    void correctPasswordVerifies() {
        String salt = PasswordUtil.newSalt();
        String hash = PasswordUtil.hash("secret123", salt);
        assertTrue(PasswordUtil.verify("secret123", salt, hash));
    }

    @Test
    void wrongPasswordFails() {
        String salt = PasswordUtil.newSalt();
        assertFalse(PasswordUtil.verify("other", salt, PasswordUtil.hash("secret123", salt)));
    }

    @Test
    void saltsAreRandomAndChangeTheHash() {
        String s1 = PasswordUtil.newSalt();
        String s2 = PasswordUtil.newSalt();
        assertNotEquals(s1, s2);
        assertNotEquals(PasswordUtil.hash("same", s1), PasswordUtil.hash("same", s2));
    }

    @Test
    void nullInputsDoNotVerify() {
        assertFalse(PasswordUtil.verify(null, "x", "y"));
    }
}
