package vibro.navigator.nav.ui;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.nav.format.NavigationTripStatsFormatter;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavTripStatus;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

final class NavigationTripStatsDialog {
    @NonNull
    private final Activity activity;
    @NonNull
    private final ElapsedRealtimeClock elapsedRealtimeClock;
    @NonNull
    private final NavigationDetailsDialog detailsDialog;

    NavigationTripStatsDialog(
            @NonNull Activity activity,
            @NonNull ElapsedRealtimeClock elapsedRealtimeClock
    ) {
        this.activity = activity;
        this.elapsedRealtimeClock = elapsedRealtimeClock;
        detailsDialog = new NavigationDetailsDialog(activity, R.string.title_nav_trip_stats);
    }

    void show(@Nullable NavState state) {
        detailsDialog.show();
        update(state);
    }

    void update(@Nullable NavState state) {
        NavTripStatus tripStatus = state == null ? NavTripStatus.unavailable() : state.tripStatus;
        detailsDialog.update(NavigationTripStatsFormatter.formatDetails(
                activity,
                tripStatus,
                elapsedRealtimeClock.elapsedRealtimeMs()
        ));
    }

    void dismiss() {
        detailsDialog.dismiss();
    }
}
