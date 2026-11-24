package vibro.navigator.nav.ui;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.location.NavigationGpsStatusFormatter;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.presentation.NavStateComposer;
import vibro.navigator.nav.service.NavigationServiceBinder;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

final class NavigationDetailsDialogs {
    @NonNull
    private final Activity activity;
    private final ElapsedRealtimeClock elapsedRealtimeClock;
    @NonNull
    private final NavigationGpsDetailsDialog gpsDetailsDialog;
    @NonNull
    private final NavigationDirectionsDetailsDialog directionsDetailsDialog;
    @NonNull
    private final NavigationTripStatsDialog tripStatsDialog;

    NavigationDetailsDialogs(
            @NonNull Activity activity,
            @NonNull ElapsedRealtimeClock elapsedRealtimeClock
    ) {
        this.activity = activity;
        this.elapsedRealtimeClock = elapsedRealtimeClock;
        gpsDetailsDialog = new NavigationGpsDetailsDialog(activity, elapsedRealtimeClock);
        directionsDetailsDialog = new NavigationDirectionsDetailsDialog(activity);
        tripStatsDialog = new NavigationTripStatsDialog(activity, elapsedRealtimeClock);
    }

    void showGps(@Nullable NavState state) {
        gpsDetailsDialog.show(state);
    }

    void showDirections(@Nullable NavigationServiceBinder navBinder) {
        directionsDetailsDialog.show(directionDetailsProvider(navBinder));
    }

    void showTripStats(@Nullable NavState state) {
        tripStatsDialog.show(state);
    }

    void updateLiveDetails(@Nullable NavState state, @Nullable NavigationServiceBinder navBinder) {
        gpsDetailsDialog.update(state);
        directionsDetailsDialog.update(directionDetailsProvider(navBinder));
        tripStatsDialog.update(state);
    }

    @Nullable
    private static NavigationDirectionsDetailsDialog.DirectionDetailsProvider directionDetailsProvider(
            @Nullable NavigationServiceBinder navBinder
    ) {
        return navBinder == null ? null : navBinder::buildCurrentDirectionDetails;
    }

    @NonNull
    String gpsStatusLine(@Nullable NavState state) {
        var display = state == null ? NavStateComposer.waiting(activity) : state;
        return NavigationGpsStatusFormatter.statusLine(new AndroidNavigationTextResources(activity),
                display.gpsStatus, elapsedRealtimeClock.elapsedRealtimeMs());
    }

    void dismissAll() {
        gpsDetailsDialog.dismiss();
        directionsDetailsDialog.dismiss();
        tripStatsDialog.dismiss();
    }
}
