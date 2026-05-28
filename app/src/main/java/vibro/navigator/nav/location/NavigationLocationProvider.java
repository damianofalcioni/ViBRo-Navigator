package vibro.navigator.nav.location;

import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public interface NavigationLocationProvider {
    boolean hasFineLocationPermission();

    boolean hasCoarseLocationPermission();

    @NonNull
    List<String> enabledPermittedProviders(boolean fineGranted, boolean coarseGranted);

    @NonNull
    List<String> requestProviderUpdates(@NonNull List<String> providers, long minTimeMs);

    @Nullable
    NavigationLocation getLastKnownLocationQuietly(@NonNull String provider);

    void requestCurrentLocationSeeds(boolean fineGranted, boolean coarseGranted);

    void requestSeedForEnabledProvider(@NonNull String provider);

    void cancelPendingCurrentLocationRequests();

    void removeUpdates();

    @NonNull
    String describeAvailability();
}
