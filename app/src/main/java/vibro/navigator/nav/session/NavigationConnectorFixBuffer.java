package vibro.navigator.nav.session;

import java.util.ArrayList;
import java.util.List;
import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.route.RouteSection;

/** Delays one small reversal so a follow-up fix can distinguish jitter from backtracking. */
final class NavigationConnectorFixBuffer {
    private static final double MAX_JITTER_METERS = 30;
    private static final long MAX_REVERSAL_INTERVAL_MS = 5_000L;
    private static final double REVERSAL_ANGLE_DEGREES = 120;
    private final List<NavigationLocation> fixes = new ArrayList<>();
    private NavigationLocation pending;

    void clear() {
        fixes.clear();
        pending = null;
    }

    boolean isEmpty() {
        return fixes.isEmpty();
    }

    void add(NavigationLocation fix) {
        if (pending != null) {
            if (isReversal(fix)) {
                fixes.add(pending);
            }
            pending = null;
            fixes.add(new NavigationLocation(fix));
        } else if (isSmallReversal(fix)) {
            pending = new NavigationLocation(fix);
        } else {
            fixes.add(new NavigationLocation(fix));
        }
    }

    List<LatLon> points(boolean includeUnresolved) {
        List<LatLon> result = new ArrayList<>();
        for (NavigationLocation fix : fixes) {
            RouteSection.appendDistinct(result, point(fix));
        }
        if (includeUnresolved && pending != null) {
            RouteSection.appendDistinct(result, point(pending));
        }
        return result;
    }

    private boolean isSmallReversal(NavigationLocation fix) {
        if (!isReversal(fix)) {
            return false;
        }
        NavigationLocation last = fixes.get(fixes.size() - 1);
        double uncertainty = Math.min(MAX_JITTER_METERS, accuracy(last) + accuracy(fix));
        long deltaMs = fix.getTime() - last.getTime();
        return deltaMs > 0 && deltaMs <= MAX_REVERSAL_INTERVAL_MS
                && GeoMath.distanceMeters(last.getLatitude(), last.getLongitude(),
                        fix.getLatitude(), fix.getLongitude()) <= uncertainty;
    }

    private boolean isReversal(NavigationLocation fix) {
        if (fixes.size() < 2) {
            return false;
        }
        NavigationLocation before = fixes.get(fixes.size() - 2);
        NavigationLocation last = fixes.get(fixes.size() - 1);
        double incoming = GeoMath.bearingDegrees(before.getLatitude(), before.getLongitude(),
                last.getLatitude(), last.getLongitude());
        double outgoing = GeoMath.bearingDegrees(last.getLatitude(), last.getLongitude(),
                fix.getLatitude(), fix.getLongitude());
        return GeoMath.angularDiffDegrees(incoming, outgoing) > REVERSAL_ANGLE_DEGREES;
    }

    private static double accuracy(NavigationLocation fix) {
        return fix.hasAccuracy() ? Math.max(0, fix.getAccuracy()) : 0;
    }

    private static LatLon point(NavigationLocation fix) {
        return new LatLon(fix.getLatitude(), fix.getLongitude());
    }
}
