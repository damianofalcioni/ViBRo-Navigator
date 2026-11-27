package vibro.navigator.nav.ui;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.location.NavigationGpsStatusFormatter;
import vibro.navigator.nav.location.NavigationGpsTelemetryFormatter;
import vibro.navigator.nav.model.NavGpsStatus;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.presentation.NavStateResourceComposer;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

final class NavigationGpsDetailsDialog {
    @NonNull
    private final NavigationTextResources resources;
    @NonNull
    private final ElapsedRealtimeClock elapsedRealtimeClock;
    @NonNull
    private final NavigationDetailsDialog detailsDialog;

    NavigationGpsDetailsDialog(
            @NonNull Activity activity,
            @NonNull ElapsedRealtimeClock elapsedRealtimeClock
    ) {
        resources = new AndroidNavigationTextResources(activity);
        this.elapsedRealtimeClock = elapsedRealtimeClock;
        detailsDialog = new NavigationDetailsDialog(activity, R.string.title_nav_gps_details);
    }

    void show(@Nullable NavState state) {
        detailsDialog.show();
        update(state);
    }

    void update(@Nullable NavState state) {
        if (!detailsDialog.isShowing()) { return; }
        NavGpsStatus status = gpsStatus(state);
        long nowMs = elapsedRealtimeClock.elapsedRealtimeMs();
        detailsDialog.update(NavigationGpsTelemetryFormatter.formatDetails(
                resources,
                status.telemetry,
                NavigationGpsStatusFormatter.nextEvaluationValue(resources, status, nowMs),
                nowMs
        ));
    }

    void dismiss() {
        detailsDialog.dismiss();
    }

    @NonNull
    String statusLine(@Nullable NavState state) {
        return NavigationGpsStatusFormatter.statusLine(resources, gpsStatus(state), elapsedRealtimeClock.elapsedRealtimeMs());
    }

    private NavGpsStatus gpsStatus(@Nullable NavState state) {
        return state == null ? NavStateResourceComposer.waiting(resources).gpsStatus : state.gpsStatus;
    }
}
