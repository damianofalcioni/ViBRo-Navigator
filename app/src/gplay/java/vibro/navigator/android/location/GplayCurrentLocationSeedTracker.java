package vibro.navigator.android.location;

final class GplayCurrentLocationSeedTracker {
    private int activeRequestId;
    private boolean active;

    int begin() {
        active = true;
        return ++activeRequestId;
    }

    void cancel() {
        if (!active) {
            return;
        }
        active = false;
        activeRequestId++;
    }

    boolean completeIfActive(int requestId) {
        if (!active || requestId != activeRequestId) {
            return false;
        }
        active = false;
        return true;
    }
}
