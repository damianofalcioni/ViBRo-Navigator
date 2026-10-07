package vibro.navigator.nav.orientation;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NavigationRouteDisplayHeadingResolverTest {
    @Test
    public void smallRoadsideOffsetKeepsOverallStreetHeading() {
        GeoJsonRoute route = roadsideOffset(4, Collections.emptyList());
        PolylineIndex index = new PolylineIndex(route.track);
        NavigationRouteDisplayHeadingResolver resolver = new NavigationRouteDisplayHeadingResolver(route, index);
        for (LatLon location : Arrays.asList(point(-5, 0), point(0, 2), point(5, 4))) {
            assertEquals(90.0, resolver.resolve(index.match(location, -1)), 8.0);
        }
        // Guidance keeps its existing short forward bearing even though display spans the offset.
        PolylineIndex.Match approaching = index.match(point(-18, 0), -1);
        assertTrue(resolver.resolve(approaching) > NavigationExpectedBearingResolver.resolve(index, approaching));
    }

    @Test
    public void markedConnectorTurnsArePreservedEvenForSmallOffsets() {
        GeoJsonRoute route = roadsideOffset(4, Arrays.asList(
                new VoiceHint(1, 2, 0, 4, -90), new VoiceHint(2, 5, 0, 100, 90)));
        PolylineIndex index = new PolylineIndex(route.track);
        NavigationRouteDisplayHeadingResolver resolver = new NavigationRouteDisplayHeadingResolver(route, index);
        for (LatLon location : Arrays.asList(point(-2, 0), point(0, 2), point(2, 4))) {
            PolylineIndex.Match match = index.match(location, -1);
            assertEquals(NavigationExpectedBearingResolver.resolve(index, match), resolver.resolve(match), 0.01);
        }
    }

    @Test
    public void largeOffsetRetainsLocalForwardBearing() {
        GeoJsonRoute route = roadsideOffset(20, Collections.emptyList());
        assertLocalBearing(route, point(0, 10));
    }

    @Test
    public void unmarkedSharpTurnAndUturnRetainLocalBearing() {
        GeoJsonRoute corner = route(Arrays.asList(point(-100, 0), point(0, 0), point(0, 100)));
        assertLocalBearing(corner, point(-5, 0));
        GeoJsonRoute uturn = route(Arrays.asList(point(-100, 0), point(0, 0), point(0, 4), point(-100, 4)));
        assertLocalBearing(uturn, point(0, 2));
    }

    @Test
    public void markedTurnRetainsGradualLookaheadRotationBeforeItIsReached() {
        GeoJsonRoute corner = new GeoJsonRoute(Arrays.asList(point(-100, 0), point(0, 0), point(0, 100)),
                Collections.singletonList(new VoiceHint(1, 2, 0, 100, -90)), 100, 200);
        PolylineIndex index = new PolylineIndex(corner.track);
        NavigationRouteDisplayHeadingResolver resolver = new NavigationRouteDisplayHeadingResolver(corner, index);
        double previous = 90.0;
        for (int distance = 20; distance >= 0; distance -= 2) {
            PolylineIndex.Match match = index.match(point(-distance, 0), -1);
            double heading = resolver.resolve(match);
            assertEquals(NavigationExpectedBearingResolver.resolve(index, match), heading, 0.01);
            assertTrue(heading <= previous + 0.01);
            assertTrue(previous - heading < 15.0);
            previous = heading;
        }
        assertEquals(0.0, resolver.resolve(index.match(point(0, 0), -1)), 0.01);
        assertEquals(0.0, resolver.resolve(index.match(point(0, 5), -1)), 0.01);
    }

    @Test
    public void roundedMarkedCurveFollowsOriginalHeadingAcrossManeuverPoint() {
        GeoJsonRoute curve = new GeoJsonRoute(Arrays.asList(point(-100, 0), point(-30, 0),
                point(-18.5, 2.3), point(-8.8, 8.8), point(-2.3, 18.5), point(0, 30), point(0, 100)),
                Collections.singletonList(new VoiceHint(3, 2, 0, 100, -90)), 100, 200);
        PolylineIndex index = new PolylineIndex(curve.track);
        NavigationRouteDisplayHeadingResolver resolver = new NavigationRouteDisplayHeadingResolver(curve, index);
        double maneuver = index.distanceAtPointIndex(3);
        for (double along = maneuver - 30; along <= maneuver + 14; along += 2) {
            PolylineIndex.Match match = index.match(index.pointAtDistance(along), -1);
            assertEquals(NavigationExpectedBearingResolver.resolve(index, match), resolver.resolve(match), 0.01);
        }
    }

    @Test
    public void curveDoesNotBecomeAveragedStraightLine() {
        GeoJsonRoute curve = route(Arrays.asList(point(-40, 0), point(-20, 0), point(0, 8),
                point(15, 23), point(15, 60)));
        assertLocalBearing(curve, point(-10, 4));
    }

    @Test
    public void shortRouteAndRouteEndpointsKeepValidHeading() {
        GeoJsonRoute shortRoute = route(Arrays.asList(point(0, 0), point(2, 0)));
        PolylineIndex index = new PolylineIndex(shortRoute.track);
        NavigationRouteDisplayHeadingResolver resolver = new NavigationRouteDisplayHeadingResolver(shortRoute, index);
        assertEquals(90.0, resolver.resolve(index.match(point(2, 0), -1)), 0.01);
        GeoJsonRoute straight = route(Arrays.asList(point(0, 0), point(100, 0)));
        assertLocalBearing(straight, point(0, 0));
        assertLocalBearing(straight, point(100, 0));
    }

    @Test
    public void northCrossingIsSmoothedGeographicallyAndContinueHintsDoNotClipWindow() {
        GeoJsonRoute north = new GeoJsonRoute(Arrays.asList(point(0, -100), point(0, 0), point(4, 0), point(4, 100)),
                Collections.singletonList(new VoiceHint(1, 1, 0, 104, 0)), 100, 204);
        PolylineIndex index = new PolylineIndex(north.track);
        double heading = new NavigationRouteDisplayHeadingResolver(north, index).resolve(index.match(point(2, 0), -1));
        assertEquals(0.0, heading, 8.0);
    }

    private static void assertLocalBearing(GeoJsonRoute route, LatLon point) {
        PolylineIndex index = new PolylineIndex(route.track);
        PolylineIndex.Match match = index.match(point, -1);
        assertEquals(NavigationExpectedBearingResolver.resolve(index, match),
                new NavigationRouteDisplayHeadingResolver(route, index).resolve(match), 0.01);
    }

    private static GeoJsonRoute roadsideOffset(double offset, List<VoiceHint> hints) {
        return new GeoJsonRoute(Arrays.asList(point(-100, 0), point(0, 0), point(0, offset), point(100, offset)),
                hints, 100, 200 + offset);
    }

    private static GeoJsonRoute route(List<LatLon> track) {
        return new GeoJsonRoute(track, Collections.emptyList(), 100, 200);
    }

    private static LatLon point(double eastMeters, double northMeters) {
        return new LatLon(northMeters / 111_195.0, eastMeters / 111_195.0);
    }
}
