package com.textcraft.voice;

import com.textcraft.exception.DatabaseException;

/** Where voice seconds are stored (database for signed-in users, a local file otherwise, memory in tests). */
public interface UsageStore {
    int getSeconds(String month) throws DatabaseException;

    void addSeconds(String month, int seconds) throws DatabaseException;
}
