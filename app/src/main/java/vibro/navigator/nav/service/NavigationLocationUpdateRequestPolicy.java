package vibro.navigator.nav.service;

import android.content.Context;

import androidx.annotation.NonNull;

import vibro.navigator.nav.location.NavigationLocationController;
import vibro.navigator.settings.AppLocationSettings;

final class NavigationLocationUpdateRequestPolicy {

    private NavigationLocationUpdateRequestPolicy() {
    }

    static void requestLocationUpdates(
            @NonNull Context context,
            @NonNull NavigationLocationController locationController,
            long suggestedUpdateIntervalMs
    ) {
        long intervalMs = effectiveIntervalMs(context, suggestedUpdateIntervalMs);
        if (intervalMs == NavigationLocationController.STARTUP_UPDATE_INTERVAL_MS) {
            locationController.requestFastLocationUpdates();
            return;
        }
        locationController.requestLocationUpdates(intervalMs);
    }

    static void requestProviderEnabledUpdates(
            @NonNull Context context,
            @NonNull NavigationLocationController locationController,
            @NonNull String provider,
            boolean requestCurrentLocationSeed
    ) {
        if (AppLocationSettings.isDynamicGpsFixIntervalEnabled(context)) {
            locationController.onProviderEnabled(
                    provider,
                    NavigationLocationController.DEFAULT_UPDATE_INTERVAL_MS,
                    requestCurrentLocationSeed
            );
            return;
        }
        locationController.onProviderEnabledFast(provider, requestCurrentLocationSeed);
    }

    static long effectiveIntervalMs(@NonNull Context context, long suggestedUpdateIntervalMs) {
        return effectiveIntervalMs(
                AppLocationSettings.isDynamicGpsFixIntervalEnabled(context),
                suggestedUpdateIntervalMs
        );
    }

    static long effectiveIntervalMs(boolean dynamicIntervalEnabled, long suggestedUpdateIntervalMs) {
        return dynamicIntervalEnabled
                ? suggestedUpdateIntervalMs
                : NavigationLocationController.STARTUP_UPDATE_INTERVAL_MS;
    }
}
