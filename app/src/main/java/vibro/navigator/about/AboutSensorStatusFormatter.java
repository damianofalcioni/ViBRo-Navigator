package vibro.navigator.about;

import android.content.Context;

import androidx.annotation.NonNull;

import vibro.navigator.R;
import vibro.navigator.android.location.AndroidLocationDiagnostics;
import vibro.navigator.android.sensor.AndroidHeadingSensorDiagnostics;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationProviders;

final class AboutSensorStatusFormatter {

    @NonNull
    private final AndroidLocationDiagnostics locationDiagnostics;
    @NonNull
    private final AndroidHeadingSensorDiagnostics headingSensorDiagnostics;
    @NonNull
    private final AboutFusedLocationDiagnostic fusedLocationDiagnostic;

    private boolean started;

    AboutSensorStatusFormatter(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        locationDiagnostics = new AndroidLocationDiagnostics(appContext);
        headingSensorDiagnostics = new AndroidHeadingSensorDiagnostics(appContext);
        fusedLocationDiagnostic = new AboutFusedLocationDiagnostic(appContext);
    }

    void start() {
        if (started) {
            return;
        }
        boolean sensorStarted = headingSensorDiagnostics.start();
        boolean gnssStarted = locationDiagnostics.startFixedSatelliteTracking();
        started = sensorStarted || gnssStarted;
    }

    void stop() {
        headingSensorDiagnostics.stop();
        locationDiagnostics.stopFixedSatelliteTracking();
        started = false;
    }

    @NonNull
    String build(@NonNull Context context) {
        StringBuilder sb = new StringBuilder();
        if (fusedLocationDiagnostic.shouldDisplay()) {
            appendLine(
                    context,
                    sb,
                    R.string.label_sensor_fused_provider,
                    fusedLocationDiagnostic.statusResId(),
                    fusedLocationDiagnostic.value()
            );
        }
        appendLine(
                context,
                sb,
                R.string.label_sensor_gps_provider,
                locationDiagnostics.providerStatusResId(NavigationLocationProviders.GPS_PROVIDER),
                describeLocationValue(NavigationLocationProviders.GPS_PROVIDER)
        );
        appendLine(
                context,
                sb,
                R.string.label_sensor_network_provider,
                locationDiagnostics.providerStatusResId(NavigationLocationProviders.NETWORK_PROVIDER),
                describeLocationValue(NavigationLocationProviders.NETWORK_PROVIDER)
        );
        for (AndroidHeadingSensorDiagnostics.Snapshot snapshot : headingSensorDiagnostics.snapshots()) {
            appendLine(
                    context,
                    sb,
                    snapshot.labelResId,
                    snapshot.available ? R.string.sensor_status_available : R.string.sensor_status_unavailable,
                    snapshot.value
            );
        }
        return sb.toString();
    }

    private void appendLine(
            @NonNull Context context,
            @NonNull StringBuilder sb,
            int labelResId,
            int statusResId,
            @NonNull String value
    ) {
        if (sb.length() > 0) {
            sb.append('\n');
        }
        sb.append(context.getString(
                R.string.format_about_sensor_status_detail,
                context.getString(labelResId),
                context.getString(statusResId),
                value
        ));
    }

    @NonNull
    private String describeLocationValue(@NonNull String provider) {
        final NavigationLocation location;
        try {
            location = locationDiagnostics.lastKnownLocation(provider);
        } catch (AndroidLocationDiagnostics.LocationDiagnosticException e) {
            return e.error == AndroidLocationDiagnostics.LocationDiagnosticError.PERMISSION_DENIED
                    ? "value=permission denied"
                    : "value=unavailable";
        }
        if (location == null) {
            return "value=none";
        }
        return AboutSensorValueFormatter.describeLocationValue(location, locationDiagnostics.fixedSatelliteCount());
    }
}
