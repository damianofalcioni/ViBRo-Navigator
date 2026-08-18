package vibro.navigator.nav.power;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import vibro.navigator.nav.time.ElapsedRealtimeClock;

public class NavigationBatterySnapshotCacheTest {

    @Test
    public void cachedSnapshotRemainsFreshUntilMaximumAge() {
        MutableClock clock = new MutableClock(1_000L);
        NavigationBatterySnapshotCache cache = new NavigationBatterySnapshotCache(clock, 300_000L);
        NavigationBatterySnapshot snapshot = NavigationBatterySnapshot.of(3_000_000, 80);

        assertTrue(cache.needsRefresh());
        cache.record(snapshot);
        clock.nowMs += 299_999L;

        assertFalse(cache.needsRefresh());
        assertSame(snapshot, cache.current());

        clock.nowMs += 1L;

        assertTrue(cache.needsRefresh());
    }

    @Test
    public void elapsedClockResetForcesRefresh() {
        MutableClock clock = new MutableClock(10_000L);
        NavigationBatterySnapshotCache cache = new NavigationBatterySnapshotCache(clock, 300_000L);
        cache.record(NavigationBatterySnapshot.of(3_000_000, 80));

        clock.nowMs = 500L;

        assertTrue(cache.needsRefresh());
    }

    private static final class MutableClock implements ElapsedRealtimeClock {
        private long nowMs;

        MutableClock(long nowMs) {
            this.nowMs = nowMs;
        }

        @Override
        public long elapsedRealtimeMs() {
            return nowMs;
        }
    }
}
