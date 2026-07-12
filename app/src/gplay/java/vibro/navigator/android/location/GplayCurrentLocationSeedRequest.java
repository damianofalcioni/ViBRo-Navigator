package vibro.navigator.android.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.CancellationTokenSource;

import vibro.navigator.logging.AppLogger;

final class GplayCurrentLocationSeedRequest {
    private static final String TAG = "FusedLocation";
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

    void handleFailure(int requestId, @NonNull Exception error) {
        if (!completeIfActive(requestId)) {
            AppLogger.d(TAG, "Ignoring stale fused current location seed failure");
            return;
        }
        AppLogger.w(TAG, "Fused current location seed failed", error);
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
