package vibro.navigator.nav.service;

import androidx.annotation.NonNull;

import vibro.navigator.nav.location.NavigationLocationRecoveryAlarm;

final class NavigationLocationRecoveryScheduler {
    private static final long NO_DEADLINE = Long.MIN_VALUE;

    @NonNull
    private final NavigationLocationRecoveryAlarm alarm;
    private long scheduledDeadlineMs = NO_DEADLINE;

    NavigationLocationRecoveryScheduler(@NonNull NavigationLocationRecoveryAlarm alarm) {
        this.alarm = alarm;
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

    void cancel() {
        alarm.cancel();
        scheduledDeadlineMs = NO_DEADLINE;
    }
}
