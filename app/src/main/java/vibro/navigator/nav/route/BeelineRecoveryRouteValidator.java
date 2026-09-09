package vibro.navigator.nav.route;

import java.util.List;
import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.guidance.BeelineRecoveryTracker;
import vibro.navigator.nav.guidance.RouteDeviationPolicy;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;

/** A successful router response must offer road access before any still-required stop. */
public final class BeelineRecoveryRouteValidator {
    private static final double TARGET_TOLERANCE_METERS = 1;

    private BeelineRecoveryRouteValidator() {
    }

    public static boolean isUsable(GeoJsonRoute route, NavigationRouteRequestSnapshot request,
            NavigationLocation latest) {
        if (!BeelineRecoveryTracker.hasUsableAccuracy(latest) || route.track.size() < 2) {
            return false;
        }
        if (!samePoint(route.track.get(route.track.size() - 1), request.destination)
                || RouteStartApproach.plan(route, request.start, latest.getAccuracy()).active) {
            return false;
        }
        int firstStop = firstRequiredStopIndex(route.track, request.intermediates);
        if (firstStop < 0) {
            return false;
        }
        PolylineIndex index = new PolylineIndex(route.track);
        PolylineIndex.Match match = index.match(new LatLon(latest.getLatitude(), latest.getLongitude()), -1);
        return isRoadMatch(route, match) && match.alongTrackMeters <= index.distanceAtPointIndex(firstStop)
                && match.distanceToTrackMeters <= RouteDeviationPolicy.resolveOffTrackThresholdMeters(latest.getAccuracy());
    }

    private static boolean isRoadMatch(GeoJsonRoute route, PolylineIndex.Match match) {
        if (match == null) {
            return false;
        }
        for (VoiceHint hint : route.voiceHints) {
            if (hint.command == RouteStartApproach.BEELINE_COMMAND && hint.indexInTrack == match.segmentIndex) {
                return false;
            }
        }
        return true;
    }

    private static int firstRequiredStopIndex(List<LatLon> track, List<LatLon> stops) {
        int next = 0;
        int first = track.size() - 1;
        for (int i = 0; i < stops.size(); i++) {
            next = findPoint(track, stops.get(i), next);
            if (next < 0) {
                return -1;
            }
            if (i == 0) {
                first = next;
            }
            next++;
        }
        return first;
    }

    private static int findPoint(List<LatLon> track, LatLon stop, int from) {
        for (int i = from; i < track.size(); i++) {
            if (samePoint(track.get(i), stop)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean samePoint(LatLon first, LatLon second) {
        return second != null && GeoMath.distanceMeters(first.lat, first.lon, second.lat, second.lon)
                <= TARGET_TOLERANCE_METERS;
    }
}
