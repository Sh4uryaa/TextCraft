package com.textcraft.voice;

import com.textcraft.exception.DatabaseException;
import com.textcraft.exception.VoiceException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/**
 * Protects the free Speech-to-Text quota (60 minutes a month): shows "x/60", warns at 50 minutes,
 * blocks recording above the configured cap.
 */
public class QuotaGuard {
    public static final int WARN_MINUTES = 50;

    private final UsageStore store;
    private final int capMinutes;
    private final Supplier<YearMonth> clock;

    public QuotaGuard(UsageStore store, int capMinutes) {
        this(store, capMinutes, YearMonth::now);
    }

    /** The clock can be replaced in tests. */
    public QuotaGuard(UsageStore store, int capMinutes, Supplier<YearMonth> clock) {
        this.store = store;
        this.capMinutes = capMinutes;
        this.clock = clock;
    }

    public int capMinutes() {
        return capMinutes;
    }

    private String month() {
        return clock.get().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    public int secondsUsed() throws DatabaseException {
        return store.getSeconds(month());
    }

    public double minutesUsed() throws DatabaseException {
        return secondsUsed() / 60.0;
    }

    /** Throws when the monthly cap is reached. */
    public void checkAllowed() throws VoiceException, DatabaseException {
        if (secondsUsed() >= capMinutes * 60) {
            throw new VoiceException("Voice typing limit reached (" + capMinutes + " minutes this month). It resets next month.");
        }
    }

    public boolean shouldWarn() throws DatabaseException {
        return minutesUsed() >= WARN_MINUTES;
    }

    public void record(int seconds) throws DatabaseException {
        if (seconds > 0) store.addSeconds(month(), seconds);
    }
}
