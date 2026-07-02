package vibro.navigator.nav.model;

import androidx.annotation.NonNull;

public final class NavGpsStatus {
    @NonNull
    public final String statusLine;
    public final long nextEvaluationDeadlineElapsedMs;
    @NonNull
    public final NavGpsTelemetry telemetry;

    public NavGpsStatus(
            @NonNull String statusLine,
            long nextEvaluationDeadlineElapsedMs
    ) {
        this(statusLine, nextEvaluationDeadlineElapsedMs, NavGpsTelemetry.unavailable(statusLine));
    }

    public NavGpsStatus(
            @NonNull String statusLine,
            long nextEvaluationDeadlineElapsedMs,
            @NonNull NavGpsTelemetry telemetry
    ) {
        this.statusLine = statusLine;
        this.nextEvaluationDeadlineElapsedMs = nextEvaluationDeadlineElapsedMs;
        this.telemetry = telemetry;
    }

    @NonNull
    public NavGpsStatus withStatusLine(@NonNull String statusLine) {
        return new NavGpsStatus(statusLine, nextEvaluationDeadlineElapsedMs, telemetry.withCompactLine(statusLine));
    }
}
