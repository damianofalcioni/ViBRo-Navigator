package vibro.navigator.android.time;

import android.os.SystemClock;

import vibro.navigator.nav.time.ElapsedRealtimeClock;

public enum AndroidElapsedRealtimeClock implements ElapsedRealtimeClock {
    INSTANCE;

    @Override
    public long elapsedRealtimeMs() {
        return SystemClock.elapsedRealtime();
    }
}
