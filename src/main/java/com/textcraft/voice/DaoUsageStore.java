package com.textcraft.voice;

import com.textcraft.dao.VoiceUsageDAO;
import com.textcraft.exception.DatabaseException;

/** Voice usage in the voice_usage table for one user. */
public class DaoUsageStore implements UsageStore {
    private final VoiceUsageDAO dao;
    private final long userId;

    public DaoUsageStore(VoiceUsageDAO dao, long userId) {
        this.dao = dao;
        this.userId = userId;
    }

    @Override
    public int getSeconds(String month) throws DatabaseException {
        return dao.getSeconds(userId, month);
    }

    @Override
    public void addSeconds(String month, int seconds) throws DatabaseException {
        dao.addSeconds(userId, month, seconds);
    }
}
