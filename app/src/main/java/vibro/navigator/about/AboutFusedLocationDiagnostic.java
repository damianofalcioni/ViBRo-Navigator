package vibro.navigator.about;

import android.content.Context;
import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.android.location.AndroidLocationPermissions;
import vibro.navigator.distribution.DistributionServices;
import vibro.navigator.nav.location.FusedLocationDiagnosticClient;
import vibro.navigator.settings.AppSettings;

final class AboutFusedLocationDiagnostic {
    private static final String VALUE_NONE = "value=none";
    private static final String VALUE_PERMISSION_DENIED = "value=permission denied";
    private static final String VALUE_UNAVAILABLE = "value=unavailable";
    private static final String VALUE_WAITING_FOR_SAMPLE = "value=waiting for sample";

    @NonNull
    private final Context appContext;
    @NonNull
    private final FusedLocationDiagnosticClient client;

    @Nullable
    private NavigationLocation latestLocation;
    @Nullable
    private String valueOverride;
    private boolean refreshInFlight;

    AboutFusedLocationDiagnostic(@NonNull Context context) {
        appContext = context.getApplicationContext();
        client = DistributionServices.createFusedLocationDiagnosticClient(appContext);
    }

    boolean shouldDisplay() {
        return DistributionServices.supportsFusedLocation();
    }

    int statusResId() {
        if (!shouldDisplay() || !client.isAvailable()) {
            return R.string.sensor_status_unavailable;
        }
        return AppSettings.isFusedLocationEnabled(appContext)
                ? R.string.sensor_status_enabled
                : R.string.sensor_status_disabled;
    }

    @NonNull
    String value() {
        refresh();
        if (!shouldDisplay() || !client.isAvailable()) {
            return VALUE_UNAVAILABLE;
        }
        if (!AndroidLocationPermissions.hasAnyLocationPermission(appContext)) {
            return VALUE_PERMISSION_DENIED;
        }
        if (latestLocation != null) {
            return AboutSensorValueFormatter.describeLocationValue(latestLocation, null);
        }
        if (valueOverride != null) {
            return valueOverride;
        }
        return refreshInFlight ? VALUE_WAITING_FOR_SAMPLE : VALUE_NONE;
    }

    private void refresh() {
        if (!canRefresh()) {
            return;
        }
        refreshInFlight = true;
        client.requestLastKnownLocation(new FusedLocationDiagnosticClient.Callback() {
            @Override
            public void onLocation(@Nullable NavigationLocation location) {
                latestLocation = location;
                valueOverride = location == null ? VALUE_NONE : null;
                refreshInFlight = false;
            }

            @Override
            public void onFailure(@NonNull String reason) {
                latestLocation = null;
                valueOverride = "value=" + reason;
                refreshInFlight = false;
            }
        });
    }

    private boolean canRefresh() {
        return shouldDisplay()
                && client.isAvailable()
                && AndroidLocationPermissions.hasAnyLocationPermission(appContext)
                && !refreshInFlight;
    }
}
