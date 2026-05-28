package vibro.navigator.nav.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public final class NavigationLocationProviders {
    public static final String GPS_PROVIDER = "gps";
    public static final String NETWORK_PROVIDER = "network";
    public static final String PASSIVE_PROVIDER = "passive";

    private NavigationLocationProviders() {
    }

    public static boolean hasAnyLocationPermission(boolean fineGranted, boolean coarseGranted) {
        return fineGranted || coarseGranted;
    }

    public static boolean canUseProvider(@NonNull String provider, boolean fineGranted, boolean coarseGranted) {
        if (GPS_PROVIDER.equals(provider)) {
            return fineGranted;
        }
        if (NETWORK_PROVIDER.equals(provider) || PASSIVE_PROVIDER.equals(provider)) {
            return hasAnyLocationPermission(fineGranted, coarseGranted);
        }
        return hasAnyLocationPermission(fineGranted, coarseGranted);
    }

    @Nullable
    public static String join(@NonNull List<String> providers) {
        if (providers.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < providers.size(); i++) {
            if (i > 0) {
                sb.append("+");
            }
            sb.append(providers.get(i));
        }
        return sb.toString();
    }

    public static boolean shouldReuseActiveLocationRequest(
            long minTimeMs,
            @Nullable String providerSummary,
            long lastRequestedLocationMinTimeMs,
            @Nullable String lastRequestedProvider
    ) {
        return providerSummary != null
                && minTimeMs == lastRequestedLocationMinTimeMs
                && providerSummary.equals(lastRequestedProvider);
    }
}
