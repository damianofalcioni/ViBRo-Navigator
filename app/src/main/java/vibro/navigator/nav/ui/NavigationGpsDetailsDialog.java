package vibro.navigator.nav.ui;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.nav.location.NavigationGpsTelemetryFormatter;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.presentation.NavStateComposer;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

final class NavigationGpsDetailsDialog {
    @NonNull
    private final Activity activity;
    @NonNull
    private final ElapsedRealtimeClock elapsedRealtimeClock;
    @NonNull
    private final NavigationDetailsDialog detailsDialog;

    NavigationGpsDetailsDialog(
            @NonNull Activity activity,
            @NonNull ElapsedRealtimeClock elapsedRealtimeClock
    ) {
        this.activity = activity;
        this.elapsedRealtimeClock = elapsedRealtimeClock;
        detailsDialog = new NavigationDetailsDialog(activity, R.string.title_nav_gps_details);
    }

    void show(@Nullable NavState state) {
        detailsDialog.show();
        update(state);
    }

    void update(@Nullable NavState state) {
        NavState detailsState = state == null ? NavStateComposer.waiting(activity) : state;
        detailsDialog.update(NavigationGpsTelemetryFormatter.formatDetails(
                activity,
                detailsState.gpsStatus.telemetry,
                nextEvaluationValue(state)
        ));
    }

    void dismiss() {
        detailsDialog.dismiss();
    }

    @NonNull
    String nextEvaluationValue(@Nullable NavState state) {
        if (state == null) {
            return activity.getString(R.string.nav_status_unavailable);
        }
        long deadlineElapsedMs = state.gpsStatus.nextEvaluationDeadlineElapsedMs;
        long remainingMs = Math.max(0L, deadlineElapsedMs - elapsedRealtimeClock.elapsedRealtimeMs());
        if (deadlineElapsedMs == NavState.NO_DEADLINE || remainingMs <= 0L) {
            return activity.getString(R.string.nav_status_unavailable);
        }
        long remainingSeconds = (long) Math.ceil(remainingMs / 1000.0);
        return activity.getString(R.string.format_nav_next_position_check_value, remainingSeconds);
    }

}
