package com.textcraft.voice;

import static org.junit.jupiter.api.Assertions.*;

import com.textcraft.exception.VoiceException;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class QuotaGuardTest {
    /** In-memory usage store. */
    private static class MemStore implements UsageStore {
        final Map<String, Integer> map = new HashMap<>();
        public int getSeconds(String m) { return map.getOrDefault(m, 0); }
        public void addSeconds(String m, int s) { map.merge(m, s, Integer::sum); }
    }

    @Test
    void countsMinutesAndWarnsAtFifty() throws Exception {
        MemStore store = new MemStore();
        QuotaGuard g = new QuotaGuard(store, 60, () -> YearMonth.of(2026, 5));
        assertFalse(g.shouldWarn());
        g.record(49 * 60);
        assertFalse(g.shouldWarn());
        g.record(60);
        assertTrue(g.shouldWarn());
        assertEquals(50.0, g.minutesUsed(), 0.001);
    }

    @Test
    void blocksAtTheCap() throws Exception {
        MemStore store = new MemStore();
        QuotaGuard g = new QuotaGuard(store, 2, () -> YearMonth.of(2026, 5));
        g.checkAllowed();
        g.record(120);
        assertThrows(VoiceException.class, g::checkAllowed);
    }

    @Test
    void usageResetsInANewMonth() throws Exception {
        MemStore store = new MemStore();
        YearMonth[] now = {YearMonth.of(2026, 5)};
        QuotaGuard g = new QuotaGuard(store, 1, () -> now[0]);
        g.record(60);
        assertThrows(VoiceException.class, g::checkAllowed);
        now[0] = YearMonth.of(2026, 6);
        g.checkAllowed(); // no exception
        assertEquals(0, g.secondsUsed());
    }
}
