package vibro.navigator.nav.orientation;

import static org.junit.Assert.assertEquals;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import vibro.navigator.nav.session.NavigationSessionRouteState;

public class RouteStartApproachStationaryOrientationTest {
    private static final String DESTINATION = "Destination";
    private static final String PROFILE = "shortest";

    @Test
    public void notifierUsesRouteStartApproachBearingUntilUserReachesRoute() {
        NavigationTextResources textResources = TestNavigationTextResources.metric();
        NavigationSessionRouteState routeState = new NavigationSessionRouteState();
        NavigationRequest request = new NavigationRequest(
                PROFILE,
                DESTINATION,
                new LatLon(0.001, 0.001),
                Collections.emptyList()
        );
        NavigationLocation requestedStart = location(0.0, 0.0, 1_000L);

        routeState.applyRouteResult(
                textResources,
                snapshot(request, new LatLon(0.0, 0.0)),
                routeStartingEastThenNorth(),
                requestedStart,
                0f,
                true,
                500L
        );
        RecordingSink sink = new RecordingSink();
        StationaryOrientationNotifier notifier =
                new StationaryOrientationNotifier(new StationaryOrientationAdvisor());

        notifier.maybeNotify(
                true,
                false,
                true,
                0f,
                routeState.currentRouteBearingDegrees(requestedStart),
                sample(0.0, 1_000L),
                1_000L,
                sink
        );
        notifier.maybeNotify(
                true,
                false,
                true,
                0f,
                routeState.currentRouteBearingDegrees(requestedStart),
                sample(0.0, 6_500L),
                6_500L,
                sink
        );

        assertEquals(1, sink.decisionCount);
        assertEquals(90.0, sink.lastDecision.absoluteTurnDegrees(), 1.0);
    }

    @NonNull
    private static GeoJsonRoute routeStartingEastThenNorth() {
        return new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.001),
                        new LatLon(0.001, 0.001),
                        new LatLon(0.001, 0.002)
                ),
                Collections.singletonList(new VoiceHint(1, 2, 0, 0.0, 0)),
                120.0,
                268.0
        );
    }

    @NonNull
    private static NavigationRouteRequestSnapshot snapshot(
            @NonNull NavigationRequest request,
            @NonNull LatLon start
    ) {
        return new NavigationRouteRequestSnapshot(
                1,
                1,
                request.routingMode,
                start,
                request.stops,
                request.destination,
                request.profile,
                request.customProfile,
                request.profileParameters,
                Collections.emptyList(),
                request.roundTripDistanceMeters,
                request.roundTripDirectionDegrees
        );
    }

    @NonNull
    private static NavigationLocation location(double lat, double lon, long timeMs) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setTime(timeMs);
        location.setAccuracy(5f);
        return location;
    }

    @NonNull
    private static GeomagneticOrientationMonitor.Sample sample(double headingDegrees, long elapsedRealtimeMs) {
        return new GeomagneticOrientationMonitor.Sample(
                headingDegrees,
                0.0,
                0.0,
                HeadingAccuracyStatus.HIGH,
                3.0,
                elapsedRealtimeMs
        );
    }

    private static final class RecordingSink implements StationaryOrientationNotifier.Sink {
        private int decisionCount;
        private StationaryOrientationAdvisor.Decision lastDecision;

        @Override
        public void sendStationaryOrientationNotification(
                @NonNull StationaryOrientationAdvisor.Decision decision
        ) {
            decisionCount++;
            lastDecision = decision;
        }
    }
}
