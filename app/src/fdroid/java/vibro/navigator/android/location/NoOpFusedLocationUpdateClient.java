package vibro.navigator.android.location;

import androidx.annotation.NonNull;

import vibro.navigator.nav.location.FusedLocationUpdateClient;

public final class NoOpFusedLocationUpdateClient implements FusedLocationUpdateClient {
    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public boolean requestUpdates(long minTimeMs, boolean fineGranted, boolean coarseGranted) {
        return false;
    }

    @Override
    public void requestCurrentLocationSeed(boolean fineGranted, boolean coarseGranted) {
    }

    @Override
    public void removeUpdates() {
    }

    @NonNull
    @Override
    public String describeAvailability() {
        return "fusedLocation=false";
    }
}
