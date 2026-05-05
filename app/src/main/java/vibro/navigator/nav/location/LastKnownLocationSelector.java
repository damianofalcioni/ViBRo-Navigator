package vibro.navigator.nav.location;

import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

import vibro.navigator.nav.startup.NavigationStartupLocationSelector;

public final class LastKnownLocationSelector {

    public interface ProviderReader {
        @Nullable
        Location getLastKnownLocation(@NonNull String provider);
    }

    private LastKnownLocationSelector() {
    }

    @Nullable
    public static Location findBest(@NonNull ProviderReader reader, @NonNull List<String> providers) {
        Location best = null;
        for (String provider : providers) {
            Location candidate = reader.getLastKnownLocation(provider);
            if (candidate != null && (best == null || isBetterLocation(candidate, best))) {
                best = candidate;
            }
        }
        return best == null ? null : new Location(best);
    }

    @Nullable
    public static Location findBestForMapPicker(@NonNull ProviderReader reader) {
        return findBest(reader, Arrays.asList(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
        ));
    }

    @Nullable
    public static Location findBestStartup(
            @NonNull ProviderReader reader,
            boolean fineGranted,
            boolean coarseGranted,
            long nowMs
    ) {
        Location gps = fineGranted ? reader.getLastKnownLocation(LocationManager.GPS_PROVIDER) : null;
        Location network = fineGranted || coarseGranted
                ? reader.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                : null;
        return NavigationStartupLocationSelector.selectBest(gps, network, nowMs);
    }

    private static boolean isBetterLocation(@NonNull Location candidate, @NonNull Location best) {
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
