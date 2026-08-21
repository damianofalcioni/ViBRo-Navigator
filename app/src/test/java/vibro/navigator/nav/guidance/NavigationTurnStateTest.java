package vibro.navigator.nav.guidance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class NavigationTurnStateTest {

    @Test
    public void onRouteApplied_buildsInitialTurnEventAndAdvanceMarksTurnPassed() {
        NavigationTurnState state = new NavigationTurnState();
        GeoJsonRoute route = routeWithHint();
        PolylineIndex polylineIndex = new PolylineIndex(route.track);

        state.reset();
        assertEquals(
                NavigationTurnEvent.Type.INITIAL,
                state.onRouteApplied(
                        route,
                        polylineIndex,
                        Collections.emptyList(),
                        location(0.0, 0.0),
                        5f,
                        5f
                ).get(0).type
        );

        NavigationTurnState.Progress progress = state.evaluate(
                route,
                polylineIndex,
                120.0,
                0,
                5f,
                1_000L,
                0L
        );

        assertEquals(1, progress.turnEvents.size());
        assertEquals(NavigationTurnEvent.Type.PASSED, progress.turnEvents.get(0).type);
        assertTrue(progress.suggestedUpdateIntervalMs >= NavigationUpdateScheduler.bounds().min);
    }

    @Test
    public void singleInstructionModeEmitsTenSecondAlertAfterRouteApplied() {
        NavigationTurnState state = new NavigationTurnState();
        GeoJsonRoute route = routeWithHint();
        PolylineIndex polylineIndex = new PolylineIndex(route.track);

        state.onRouteApplied(
                route,
                polylineIndex,
                Collections.emptyList(),
                location(0.0, 0.0),
                5f,
                5f
        );

        NavigationTurnState.Progress progress = state.evaluate(
                route,
                polylineIndex,
                polylineIndex.totalLengthMeters() - 50.0,
                0,
                5f,
                1_000L,
                0L,
                true
        );

        assertEquals(1, progress.turnEvents.size());
        assertEquals(NavigationTurnEvent.Type.IMMINENT, progress.turnEvents.get(0).type);
        assertEquals(10.0, progress.turnEvents.get(0).timeSeconds, 0.01);
    }

    @Test
    public void evaluate_emitsApproachingIntermediateArrivalBeforeLaterTurn() {
        NavigationTurnState state = new NavigationTurnState();
        GeoJsonRoute route = new GeoJsonRoute(
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
                100.0,
                444.0
        );
        PolylineIndex polylineIndex = new PolylineIndex(route.track);

        state.onRouteApplied(
                route,
                polylineIndex,
                Collections.singletonList(new LatLon(0.0, 0.002)),
                location(0.0, 0.0),
                20f,
                5f
        );

        NavigationTurnState.Progress progress = state.evaluate(
                route,
                polylineIndex,
                120.0,
                1,
                20f,
                1_000L,
                0L
        );

        assertEquals(NavigationTurnEvent.Type.PASSED, progress.turnEvents.get(0).type);
        assertEquals(101, progress.turnEvents.get(progress.turnEvents.size() - 1).hint.command);

        state.onIntermediateDestinationReached(2);
        NavigationTurnState.Progress afterReached = state.evaluate(
                route,
                polylineIndex,
                polylineIndex.distanceAtPointIndex(2),
                2,
                20f,
                2_000L,
                0L
        );

        for (NavigationTurnEvent event : afterReached.turnEvents) {
            assertTrue(event.hint.command != 101);
        }
    }

    @Test
    public void evaluate_schedulesSyntheticDestinationArrivalByEtaWhenNoRouteHintsRemain() {
        NavigationTurnState state = new NavigationTurnState();
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.emptyList(),
                60.0,
                111.0
        );
        PolylineIndex polylineIndex = new PolylineIndex(route.track);
        state.onRouteApplied(route, polylineIndex, Collections.emptyList(), location(0.0, 0.0), 0f, 5f);

        NavigationTurnState.Progress progress = state.evaluate(
                route,
                polylineIndex,
                0.0,
                0,
                0f,
                5_000L,
                0L
        );

        assertTrue(progress.turnEvents.isEmpty());
        assertEquals(12_000L, progress.suggestedUpdateIntervalMs);
    }

    @Test
    public void evaluate_rampsUpdateIntervalUpWhenStartupWarmupEnds() {
        NavigationTurnState state = new NavigationTurnState();
        GeoJsonRoute route = routeWithDistantHint();
        PolylineIndex polylineIndex = new PolylineIndex(route.track);
        state.onRouteApplied(route, polylineIndex, Collections.emptyList(), location(0.0, 0.0), 1f, 5f);

        NavigationTurnState.Progress duringWarmup = state.evaluate(
                route,
                polylineIndex,
                0.0,
                0,
                1f,
                1_000L,
                10_000L
        );
        NavigationTurnState.Progress firstAfterWarmup = state.evaluate(
                route,
                polylineIndex,
                0.0,
                0,
                1f,
                10_001L,
                10_000L
        );
        NavigationTurnState.Progress firstRampStep = state.evaluate(
                route,
                polylineIndex,
                0.0,
                0,
                1f,
                13_001L,
                10_000L
        );

        assertEquals(3_000L, duringWarmup.suggestedUpdateIntervalMs);
        assertEquals(3_000L, firstAfterWarmup.suggestedUpdateIntervalMs);
        assertEquals(5_000L, firstRampStep.suggestedUpdateIntervalMs);
    }

    @Test
    public void evaluate_rampsUpdateIntervalUpAfterPassedTurn() {
        NavigationTurnState state = new NavigationTurnState();
        GeoJsonRoute route = routeWithTwoTurnHints();
        PolylineIndex polylineIndex = new PolylineIndex(route.track);
        state.onRouteApplied(route, polylineIndex, Collections.emptyList(), location(0.0, 0.0), 5f, 5f);

        double afterFirstTurnMeters = polylineIndex.distanceAtPointIndex(1) + 6.0;
        NavigationTurnState.Progress passedTurn = state.evaluate(
                route,
                polylineIndex,
                afterFirstTurnMeters,
                1,
                0f,
                1_000L,
                0L
        );
        NavigationTurnState.Progress earlyAfterPassedTurn = state.evaluate(
                route,
                polylineIndex,
                afterFirstTurnMeters,
                1,
                0f,
                3_999L,
                0L
        );
        NavigationTurnState.Progress firstRampStep = state.evaluate(
                route,
                polylineIndex,
                afterFirstTurnMeters,
                1,
                0f,
                4_000L,
                0L
        );
        NavigationTurnState.Progress earlyAfterFirstRampStep = state.evaluate(
                route,
                polylineIndex,
                afterFirstTurnMeters,
                1,
                0f,
                8_999L,
                0L
        );
        NavigationTurnState.Progress secondRampStep = state.evaluate(
                route,
                polylineIndex,
                afterFirstTurnMeters,
                1,
                0f,
                9_000L,
                0L
        );

        assertEquals(1, passedTurn.turnEvents.size());
        assertEquals(NavigationTurnEvent.Type.PASSED, passedTurn.turnEvents.get(0).type);
        assertEquals(3_000L, passedTurn.suggestedUpdateIntervalMs);
        assertTrue(earlyAfterPassedTurn.turnEvents.isEmpty());
        assertEquals(3_000L, earlyAfterPassedTurn.suggestedUpdateIntervalMs);
        assertTrue(firstRampStep.turnEvents.isEmpty());
        assertEquals(5_000L, firstRampStep.suggestedUpdateIntervalMs);
        assertTrue(earlyAfterFirstRampStep.turnEvents.isEmpty());
        assertEquals(5_000L, earlyAfterFirstRampStep.suggestedUpdateIntervalMs);
        assertEquals(8_000L, secondRampStep.suggestedUpdateIntervalMs);
    }

    @Test
    public void evaluate_rampsUpdateIntervalUpAfterRetiredTurnWithoutPassedNotification() {
        NavigationTurnState state = new NavigationTurnState();
        GeoJsonRoute route = routeWithTwoTurnHints();
        PolylineIndex polylineIndex = new PolylineIndex(route.track);
        state.onRouteApplied(route, polylineIndex, Collections.emptyList(), location(0.0, 0.0), 1f, 5f);

        double firstTurnMeters = polylineIndex.distanceAtPointIndex(1);
        NavigationTurnState.Progress imminentTurn = state.evaluate(
                route,
                polylineIndex,
                firstTurnMeters - 3.0,
                0,
                1f,
                1_000L,
                0L
        );
        NavigationTurnState.Progress retiredTurn = state.evaluate(
                route,
                polylineIndex,
                firstTurnMeters + 1.0,
                1,
                0f,
                4_000L,
                0L
        );
        NavigationTurnState.Progress firstRampStep = state.evaluate(
                route,
                polylineIndex,
                firstTurnMeters + 1.0,
                1,
                0f,
                7_000L,
                0L
        );

        assertEquals(1, imminentTurn.turnEvents.size());
        assertEquals(NavigationTurnEvent.Type.IMMINENT, imminentTurn.turnEvents.get(0).type);
        assertTrue(retiredTurn.turnEvents.isEmpty());
        assertEquals(3_000L, retiredTurn.suggestedUpdateIntervalMs);
        assertEquals(5_000L, firstRampStep.suggestedUpdateIntervalMs);
    }

    @Test
    public void evaluate_activatesTurnManeuverCueForFiveSecondSignal() {
        NavigationTurnState state = new NavigationTurnState();
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.singletonList(new VoiceHint(1, 5, 0, 0.0, 90)),
                0.0,
                111.0
        );
        PolylineIndex polylineIndex = new PolylineIndex(route.track);
        state.onRouteApplied(route, polylineIndex, Collections.emptyList(), location(0.0, 0.0), 2f, 5f);

        NavigationTurnState.Progress progress = state.evaluate(
                route,
                polylineIndex,
                polylineIndex.totalLengthMeters() - 6.0,
                0,
                2f,
                1_000L,
                0L
        );

        assertEquals(1, progress.turnEvents.size());
        assertEquals(NavigationTurnEvent.Type.IMMINENT, progress.turnEvents.get(0).type);
        assertEquals(Integer.valueOf(90), state.getActiveTurnManeuverDegrees());
        assertEquals(Integer.valueOf(1), state.getActiveTurnManeuverTrackIndex());
    }

    private static GeoJsonRoute routeWithDistantHint() {
        return new GeoJsonRoute(
                Arrays.asList(new LatLon(0.0, 0.0), new LatLon(0.0, 0.01)),
                Collections.singletonList(new VoiceHint(1, 2, 0, 0.0, 0)),
                600.0,
                1_110.0
        );
    }

    private static GeoJsonRoute routeWithHint() {
        return new GeoJsonRoute(
                Arrays.asList(new LatLon(0.0, 0.0), new LatLon(0.0, 0.001)),
                Collections.singletonList(new VoiceHint(1, 2, 0, 0.0, 0)),
                60.0,
                111.0
        );
    }

    private static GeoJsonRoute routeWithTwoTurnHints() {
        return new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002),
                        new LatLon(0.0, 0.003),
                        new LatLon(0.0, 0.004)
                ),
                Arrays.asList(
                        new VoiceHint(1, 2, 0, 0.0, 0),
                        new VoiceHint(4, 5, 0, 0.0, 0)
                ),
                400.0,
                444.0
        );
    }

    private static LatLon location(double lat, double lon) {
        return new LatLon(lat, lon);
    }
}
