package vibro.navigator.nav.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public interface NavigationLocationProvider {
    boolean hasFineLocationPermission();

    boolean hasCoarseLocationPermission();

    @NonNull
    List<String> enabledPermittedProviders(boolean fineGranted, boolean coarseGranted);

    @NonNull
    List<String> requestProviderUpdates(@NonNull List<String> providers, long minTimeMs);

    /** Reconcile subscriptions without interrupting unchanged providers. */
    @NonNull
    default List<String> requestProviderUpdates(@NonNull List<String> providers,
            long minTimeMs, long networkMinTimeMs) {
        if (minTimeMs == networkMinTimeMs) {
            return requestProviderUpdates(providers, minTimeMs);
        }
        List<String> requested = new ArrayList<>();
        for (String provider : providers) {
            long interval = NavigationLocationProviders.NETWORK_PROVIDER.equals(provider) ? networkMinTimeMs : minTimeMs;
            requested.addAll(requestProviderUpdates(Collections.singletonList(provider), interval));
        }
        return requested;
    }

    @Nullable
    NavigationLocation getLastKnownLocationQuietly(@NonNull String provider);

    void requestCurrentLocationSeeds(boolean fineGranted, boolean coarseGranted);

    void requestSeedForEnabledProvider(@NonNull String provider);

    void cancelPendingCurrentLocationRequests();

    void removeUpdates();

    @NonNull
    String describeAvailability();
}
