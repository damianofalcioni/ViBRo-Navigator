package vibro.navigator.nav.model;

import androidx.annotation.NonNull;

public final class NavGpsStatus {
    @NonNull
    public final String statusLine;
    public final long nextEvaluationDeadlineElapsedMs;

    public NavGpsStatus(
            @NonNull String statusLine,
            long nextEvaluationDeadlineElapsedMs
    ) {
        this.statusLine = statusLine;
        this.nextEvaluationDeadlineElapsedMs = nextEvaluationDeadlineElapsedMs;
    }

    @NonNull
    public NavGpsStatus withStatusLine(@NonNull String statusLine) {
        return new NavGpsStatus(statusLine, nextEvaluationDeadlineElapsedMs);
    }
}
