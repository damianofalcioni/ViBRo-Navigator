package vibro.navigator.nav.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import vibro.navigator.nav.location.NavigationLocation;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavTarget;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

public class NavIntermediateArrivalGuidanceTest {
    private static final String DESTINATION = "Destination";
    private static final String STOP_1 = "Stop 1";

    private final NavigationTextResources context = TestNavigationTextResources.metric();

    @Test
    public void from_ordersIntermediateArrivalBetweenSurroundingManeuvers() {
        GeoJsonRoute route = routeWithIntermediateBetweenTurns();
        PolylineIndex index = new PolylineIndex(route.track);
        List<NavTarget> targets = Arrays.asList(
                new NavTarget(STOP_1, index.distanceAtPointIndex(2), 2),
                new NavTarget(DESTINATION, index.totalLengthMeters(), 4)
        );

        NavState start = stateAt(route, index, 0.0, 0, 0, -1, targets, locationAt(0.0, 0.0));

        assertTrue(start.routeStatus.guidance.nextLine.contains(context.getString(R.string.direction_turn_right)));
        assertTrue(start.routeStatus.guidance.afterNextLine.contains(
                context.getString(R.string.direction_intermediate_arrive)
        ));

        NavState atIntermediate = stateAt(
                route,
                index,
                index.distanceAtPointIndex(2),
                1,
                2,
                2,
                targets,
                locationAt(0.0, 0.002)
        );

        assertTrue(atIntermediate.routeStatus.guidance.nextLine.contains(
                context.getString(R.string.direction_turn_right)
        ));
        assertTrue(atIntermediate.routeStatus.guidance.afterNextLine.contains(
                context.getString(R.string.direction_arrive)
        ));
        assertFalse(atIntermediate.routeStatus.guidance.nextLine.contains(
                context.getString(R.string.direction_intermediate_arrive)
        ));

        NavState afterSecondTurn = stateAt(
                route,
                index,
                index.distanceAtPointIndex(3),
                2,
                3,
                -1,
                targets,
                locationAt(0.0, 0.003)
        );

        assertTrue(afterSecondTurn.routeStatus.guidance.nextLine.contains(
                context.getString(R.string.direction_arrive)
        ));
        assertEquals("", afterSecondTurn.routeStatus.guidance.afterNextLine);
    }

    @NonNull
    private NavState stateAt(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int nextHintIndex,
            int currentSegmentIndex,
            int intermediateReachedTrackIndex,
            @NonNull List<NavTarget> targets,
            @NonNull NavigationLocation location
    ) {
        return NavStateComposer.from(NavStateBuildInput
                .builder(context, route, index, location)
                .routeProgress(alongTrackMeters, nextHintIndex, currentSegmentIndex)
                .motion(0f, 0f, false, 5f, 5f)
                .intermediateDestinationReachedTrackIndex(intermediateReachedTrackIndex)
                .targets(targets)
                .build());
    }

    @NonNull
    private static GeoJsonRoute routeWithIntermediateBetweenTurns() {
        return new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002),
                        new LatLon(0.0, 0.003),
                        new LatLon(0.0, 0.004)
                ),
                Arrays.asList(
                        new VoiceHint(1, 5, 0, 0.0, 0),
                        new VoiceHint(3, 5, 0, 0.0, 0)
                ),
                Arrays.asList(0.0, 20.0, 45.0, 70.0, 100.0),
                100.0,
                444.0
        );
    }

    @NonNull
    private static NavigationLocation locationAt(double lat, double lon) {
        NavigationLocation location = new NavigationLocation("test");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setTime(1L);
        return location;
    }
}
