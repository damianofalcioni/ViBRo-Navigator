package vibro.navigator.nav.location;

import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.model.NavGpsStatus;
import vibro.navigator.nav.model.NavState;

public final class NavigationGpsStatusFormatter {
    private NavigationGpsStatusFormatter() { }

    public static String statusLine(NavigationTextResources resources, NavGpsStatus status, long nowElapsedMs) {
        return resources.getString(R.string.format_nav_gps_status_with_countdown, status.statusLine,
                nextEvaluationValue(resources, status, nowElapsedMs));
    }

    public static String nextEvaluationValue(NavigationTextResources resources,
            @Nullable NavGpsStatus status, long nowElapsedMs) {
        long deadlineMs = status == null ? NavState.NO_DEADLINE : status.nextEvaluationDeadlineElapsedMs;
        if (deadlineMs == NavState.NO_DEADLINE || deadlineMs <= nowElapsedMs) {
            return resources.getString(R.string.nav_status_unavailable);
        }
        long remainingSeconds = (long) Math.ceil((deadlineMs - nowElapsedMs) / 1_000.0);
        return resources.getString(R.string.format_nav_next_position_check_value, remainingSeconds);
    }
}
