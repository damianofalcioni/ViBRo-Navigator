package vibro.navigator.android.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.CancellationTokenSource;

final class GplayCurrentLocationSeedRequest {
    @NonNull
    private final GplayCurrentLocationSeedTracker tracker = new GplayCurrentLocationSeedTracker();
    @Nullable
    private CancellationTokenSource cancellation;

    @NonNull
    ActiveRequest begin() {
        cancellation = new CancellationTokenSource();
        return new ActiveRequest(tracker.begin(), cancellation);
    }

    void cancel() {
        if (cancellation != null) {
            cancellation.cancel();
            cancellation = null;
        }
        tracker.cancel();
    }

    boolean completeIfActive(int requestId) {
        if (!tracker.completeIfActive(requestId)) {
            return false;
        }
        cancellation = null;
        return true;
    }

    static final class ActiveRequest {
        final int id;
        @NonNull
        final CancellationTokenSource cancellation;

        ActiveRequest(int id, @NonNull CancellationTokenSource cancellation) {
            this.id = id;
            this.cancellation = cancellation;
        }
    }
}
