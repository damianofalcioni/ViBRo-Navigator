package vibro.navigator.nav.session;

import vibro.navigator.nav.location.NavigationLocation;

/** Startup may use live compass without declaring uncertain motion to be a stop. */
final class NavigationUnconfirmedHeadingPolicy {
    private NavigationUnconfirmedHeadingPolicy() { }

    static boolean useCompass(boolean atStartup, NavigationSessionLocationState state, NavigationLocation location, long nowMs) {
        return state.getLastFilteredLocation() != null && !state.isMotionKnown(nowMs) && (poorUnconfirmedPosition(location)
                || (atStartup && state.preferredLocationHeading(location, false) == null));
    }

    private static boolean poorUnconfirmedPosition(NavigationLocation location) {
        return location.hasAccuracy() && location.getProviderAccuracy() > 25 && !location.hasSpeedAccuracy();
    }
}
