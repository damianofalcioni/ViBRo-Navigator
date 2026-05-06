package vibro.navigator.nav.location;

import androidx.annotation.NonNull;

public interface FusedLocationUpdateClient {
    boolean isAvailable();

    boolean requestUpdates(long minTimeMs, boolean fineGranted, boolean coarseGranted);

    void removeUpdates();

    @NonNull
    String describeAvailability();
}
