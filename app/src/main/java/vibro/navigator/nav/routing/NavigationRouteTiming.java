package vibro.navigator.nav.routing;

import androidx.annotation.Nullable;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

/** Request-scoped diagnostics only; never used to decide route validity or guidance. */
public final class NavigationRouteTiming {
    @Nullable private ElapsedRealtimeClock clock;
    private long calculationStartMs;
    private long calculationEndMs;
    private long receivedMs;
    private long confirmedMs;
    private long applicationStartMs;
    private boolean received;
    private boolean confirmed;

    public void start(ElapsedRealtimeClock clock) {
        this.clock = clock;
        calculationStartMs = clock.elapsedRealtimeMs();
    }

    public long calculationStartedAtMs() {
        return calculationStartMs;
    }

    public String calculationCompleted() {
        calculationEndMs = nowMs();
        return "calculationMs=" + duration(calculationStartMs, calculationEndMs);
    }

    public void resultReceived() {
        receivedMs = nowMs();
        received = true;
    }

    public void confirmed() {
        if (!confirmed) {
            confirmedMs = nowMs();
            confirmed = true;
        }
    }

    public void applicationStarted() {
        applicationStartMs = nowMs();
    }

    public String applicationCompleted() {
        long endMs = nowMs();
        long confirmationWaitMs = received && confirmed ? Math.max(0, confirmedMs - receivedMs) : 0;
        return "calculationMs=" + duration(calculationStartMs, calculationEndMs)
                + " callbackWaitMs=" + duration(calculationEndMs, receivedMs)
                + " confirmationWaitMs=" + (clock == null ? "unobserved" : confirmationWaitMs)
                + " applicationMs=" + duration(applicationStartMs, endMs)
                + " totalMs=" + duration(calculationStartMs, endMs);
    }

    private long nowMs() {
        return clock == null ? 0 : clock.elapsedRealtimeMs();
    }

    private String duration(long fromMs, long toMs) {
        return clock == null ? "unobserved" : Long.toString(Math.max(0, toMs - fromMs));
    }
}
