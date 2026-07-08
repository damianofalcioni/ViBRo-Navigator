package vibro.navigator.nav.ui;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.presentation.NavStateComposer;
import vibro.navigator.nav.service.NavigationServiceBinder;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

final class NavigationDetailsDialogs {
    @NonNull
    private final Activity activity;
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
    String nextGpsEvaluationValue(@Nullable NavState state) {
        return gpsDetailsDialog.nextEvaluationValue(state);
    }

    @NonNull
    String gpsStatusLine(@Nullable NavState state) {
        return state == null ? NavStateComposer.waiting(activity).gpsStatus.statusLine : state.gpsStatus.statusLine;
    }

    void dismissAll() {
        gpsDetailsDialog.dismiss();
        directionsDetailsDialog.dismiss();
        tripStatsDialog.dismiss();
    }
}
