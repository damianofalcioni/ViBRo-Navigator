package vibro.navigator.nav.presentation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.compass.NavCompassHeadingRefresh;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.model.NavRouteStatus;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.orientation.NavigationHeadingSource;

/** Composes selected heading and its source together for structural and heading-only updates. */
public final class NavStateCompassHeadingComposer {
    private NavStateCompassHeadingComposer() {
    }

    @NonNull
    public static NavState withHeading(@NonNull NavState base, @Nullable Double heading,
            @Nullable Float accuracy) {
        NavCompassState compass = base.routeStatus.compassState;
        return compass == null ? base : withHeading(base, heading, accuracy, compass.displayMode.headingSource);
    }

    @NonNull
    public static NavState withHeading(@NonNull NavState base, @Nullable Double heading,
            @Nullable Float accuracy, @NonNull NavigationHeadingSource source) {
        NavCompassState compass = base.routeStatus.compassState;
        if (compass == null) {
            return base;
        }
        NavCompassState updated = NavCompassHeadingRefresh.apply(compass, heading, accuracy,
                base.pauseStatus.paused ? NavigationHeadingSource.UNKNOWN : source);
        if (updated == compass) {
            return base;
        }
        return new NavState(new NavRouteStatus(base.routeStatus.guidance, base.routeStatus.progress,
                updated, base.routeStatus.speedLimit, base.routeStatus.blockedRoadActionAvailable),
                base.gpsStatus, base.pauseStatus, base.tripStatus);
    }
}
