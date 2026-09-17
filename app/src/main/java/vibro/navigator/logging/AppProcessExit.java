package vibro.navigator.logging;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class AppProcessExit {
    public final long timestampMs;
    public final int pid;
    public final int status;
    public final int importance;
    public final boolean anomalous;
    @NonNull public final String reason;
    @Nullable public final String description;

    public AppProcessExit(
            long timestampMs,
            int pid,
            int status,
            int importance,
            boolean anomalous,
            @NonNull String reason,
            @Nullable String description
    ) {
        this.timestampMs = timestampMs;
        this.pid = pid;
        this.status = status;
        this.importance = importance;
        this.anomalous = anomalous;
        this.reason = reason;
        this.description = description;
    }
}
