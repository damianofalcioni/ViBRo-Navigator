package vibro.navigator.nav.service;

import androidx.annotation.NonNull;

import vibro.navigator.nav.location.NavigationLocationRecoveryAlarm;
import vibro.navigator.logging.AppLogger;

final class NavigationLocationRecoveryScheduler {
    interface Diagnostics {
        void record(String message);
    }
    private static final long NO_DEADLINE = Long.MIN_VALUE;

    @NonNull
    private final NavigationLocationRecoveryAlarm alarm;
    private final Diagnostics diagnostics;
    private long scheduledDeadlineMs = NO_DEADLINE;

    NavigationLocationRecoveryScheduler(@NonNull NavigationLocationRecoveryAlarm alarm) {
        this(alarm, message -> AppLogger.d("NavLocationRecovery", message));
    }

    NavigationLocationRecoveryScheduler(NavigationLocationRecoveryAlarm alarm, Diagnostics diagnostics) {
        this.alarm = alarm;
        this.diagnostics = diagnostics;
    }

    boolean schedule(long deadlineMs) {
        if (deadlineMs < 0L) {
            return false;
        }
        if (deadlineMs == scheduledDeadlineMs) {
            return true;
        }
        if (alarm.schedule(deadlineMs)) {
            scheduledDeadlineMs = deadlineMs;
            return true;
        }
        return false;
    }

    void onAlarmTriggered() {
        scheduledDeadlineMs = NO_DEADLINE;
    }

    void onCheckDispatched(String source, long nowMs, long uptimeMs) {
        if (scheduledDeadlineMs != NO_DEADLINE) {
            diagnostics.record("Recovery check dispatched source=" + source
                    + " requestedDeadlineMs=" + scheduledDeadlineMs
                    + " actualElapsedMs=" + nowMs + " uptimeMs=" + uptimeMs
                    + " latenessMs=" + Math.max(0, nowMs - scheduledDeadlineMs));
        }
        onAlarmTriggered();
    }

    void cancel() {
        alarm.cancel();
        scheduledDeadlineMs = NO_DEADLINE;
    }
}
