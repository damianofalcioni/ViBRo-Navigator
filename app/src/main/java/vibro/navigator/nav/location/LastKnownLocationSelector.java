package vibro.navigator.nav.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

import vibro.navigator.nav.startup.NavigationStartupLocationSelector;

public final class LastKnownLocationSelector {

    public interface ProviderReader {
        @Nullable
        NavigationLocation getLastKnownLocation(@NonNull String provider);
    }

    private LastKnownLocationSelector() {
    }

    @Nullable
    public static NavigationLocation findBest(@NonNull ProviderReader reader, @NonNull List<String> providers) {
        NavigationLocation best = null;
        for (String provider : providers) {
            NavigationLocation candidate = reader.getLastKnownLocation(provider);
            if (candidate != null && (best == null || isBetterLocation(candidate, best))) {
                best = candidate;
            }
        }
        return best == null ? null : new NavigationLocation(best);
    }

    @Nullable
    public static NavigationLocation findBestForMapPicker(@NonNull ProviderReader reader) {
        return findBest(reader, Arrays.asList(
                NavigationLocationProviders.GPS_PROVIDER,
                NavigationLocationProviders.NETWORK_PROVIDER,
                NavigationLocationProviders.PASSIVE_PROVIDER
        ));
    }

    @Nullable
    public static NavigationLocation findBestStartup(
            @NonNull ProviderReader reader,
            boolean fineGranted,
            boolean coarseGranted,
            long nowMs
    ) {
        NavigationLocation gps = fineGranted
                ? reader.getLastKnownLocation(NavigationLocationProviders.GPS_PROVIDER)
                : null;
        NavigationLocation network = fineGranted || coarseGranted
                ? reader.getLastKnownLocation(NavigationLocationProviders.NETWORK_PROVIDER)
                : null;
        return NavigationStartupLocationSelector.selectBest(gps, network, nowMs);
    }

    private static boolean isBetterLocation(@NonNull NavigationLocation candidate, @NonNull NavigationLocation best) {
        if (candidate.hasAccuracy() && best.hasAccuracy()) {
            float accuracyDelta = candidate.getAccuracy() - best.getAccuracy();
            if (accuracyDelta < -10f) {
                return true;
            }
            if (accuracyDelta > 10f) {
                return false;
            }
        }
        return candidate.getTime() > best.getTime();
    }
}
