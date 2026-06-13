package vibro.navigator.nav.location;

import androidx.annotation.NonNull;

public interface FusedLocationUpdateClient {
    boolean isAvailable();

    boolean requestUpdates(long minTimeMs, boolean fineGranted, boolean coarseGranted);

    void requestCurrentLocationSeed(boolean fineGranted, boolean coarseGranted);

    void cancelCurrentLocationSeed();

    void removeUpdates();

    @NonNull
    String describeAvailability();
}
