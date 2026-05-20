package vibro.navigator.nav.guidance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class TurnEventPlannerTest {

    private final TurnEventPlanner planner = new TurnEventPlanner();

    @Test
    public void advance_marksPassedHintsAndResetsImminentFlags() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002)
                ),
                Arrays.asList(
                        new VoiceHint(1, 0, 0, 0.0, 0),
                        new VoiceHint(2, 0, 0, 0.0, 0)
                ),
                240.0,
                222.0
        );
        PolylineIndex index = new PolylineIndex(route.track);

        TurnEventPlanner.Progress progress = planner.advance(
                route,
                index,
                0,
                true,
                true,
                120.0,
                1,
                5f
        );

        assertEquals(1, progress.nextHintIdx);
        assertEquals(1, progress.signals.size());
        assertEquals(TurnEventPlanner.TurnSignal.Type.PASSED, progress.signals.get(0).type);
        assertFalse(progress.notified20);
        assertFalse(progress.notified5);
    }

    @Test
    public void advance_emitsOnlyMostUrgentSignalWhenAlreadyWithinFiveSeconds() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.singletonList(new VoiceHint(1, 0, 0, 0.0, 0)),
                30.0,
                111.0
        );
        PolylineIndex index = new PolylineIndex(route.track);

        TurnEventPlanner.Progress progress = planner.advance(
                route,
                index,
                0,
                false,
                false,
                90.0,
                0,
                5f
        );

        assertEquals(1, progress.signals.size());
        assertEquals(TurnEventPlanner.TurnSignal.Type.IMMINENT, progress.signals.get(0).type);
        assertTrue(progress.notified20);
        assertTrue(progress.notified5);
    }

    @Test
    public void advance_emitsPreparatorySignalAtTwentySeconds() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.singletonList(new VoiceHint(1, 0, 0, 0.0, 0)),
                0.0,
                111.0
        );
        PolylineIndex index = new PolylineIndex(route.track);

        TurnEventPlanner.Progress progress = planner.advance(
                route,
                index,
                0,
                false,
                false,
                index.totalLengthMeters() - 100.0,
                0,
                5f
        );

        assertEquals(1, progress.signals.size());
        assertEquals(TurnEventPlanner.TurnSignal.Type.IMMINENT, progress.signals.get(0).type);
        assertEquals(20.0, progress.signals.get(0).timeSeconds, 0.01);
        assertTrue(progress.notified20);
        assertFalse(progress.notified5);
    }

    @Test
    public void advance_waitsWhenMoreThanTwentySecondsRemain() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.singletonList(new VoiceHint(1, 0, 0, 0.0, 0)),
                0.0,
                111.0
        );
        PolylineIndex index = new PolylineIndex(route.track);

        TurnEventPlanner.Progress progress = planner.advance(
                route,
                index,
                0,
                false,
                false,
                index.totalLengthMeters() - 105.0,
                0,
                5f
        );

        assertTrue(progress.signals.isEmpty());
        assertFalse(progress.notified20);
        assertFalse(progress.notified5);
    }

    @Test
    public void buildInitialSignal_usesCurrentDistanceToNextHint() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.singletonList(new VoiceHint(1, 0, 0, 0.0, 0)),
                30.0,
                111.0
        );
        PolylineIndex index = new PolylineIndex(route.track);

        TurnEventPlanner.TurnSignal signal = planner.buildInitialSignal(
                route,
                index,
                0,
                false,
                55.0,
                0,
                5f,
                5f
        );

        assertNotNull(signal);
        assertEquals(TurnEventPlanner.TurnSignal.Type.INITIAL, signal.type);
        assertEquals(56.32, signal.distanceMeters, 0.5);
    }

    @Test
    public void buildInitialSignal_returnsNullWhenAlreadySent() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.singletonList(new VoiceHint(1, 0, 0, 0.0, 0)),
                60.0,
                111.0
        );
        PolylineIndex index = new PolylineIndex(route.track);

        TurnEventPlanner.TurnSignal signal = planner.buildInitialSignal(
                route,
                index,
                0,
                true,
                0.0,
                0,
                0f,
                0f
        );

        assertNull(signal);
    }

    @Test
    public void advance_emitsFiveSecondSignalAtWalkingSpeedNearTurn() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.singletonList(new VoiceHint(1, 0, 0, 0.0, 0)),
                0.0,
                111.0
        );
        PolylineIndex index = new PolylineIndex(route.track);

        TurnEventPlanner.Progress progress = planner.advance(
                route,
                index,
                0,
                false,
                false,
                index.totalLengthMeters() - 6.0,
                0,
                1.2f
        );

        assertEquals(1, progress.signals.size());
        assertEquals(TurnEventPlanner.TurnSignal.Type.IMMINENT, progress.signals.get(0).type);
        assertEquals(5.0, progress.signals.get(0).timeSeconds, 0.01);
        assertTrue(progress.notified20);
        assertTrue(progress.notified5);
    }

    @Test
    public void advance_doesNotEmitLateTwentySecondSignalAfterFiveSecondSignalFiresFirst() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.singletonList(new VoiceHint(1, 0, 0, 0.0, 0)),
                30.0,
                111.0
        );
        PolylineIndex index = new PolylineIndex(route.track);

        TurnEventPlanner.Progress firstProgress = planner.advance(
                route,
                index,
                0,
                false,
                false,
                90.0,
                0,
                5f
        );
        TurnEventPlanner.Progress secondProgress = planner.advance(
                route,
                index,
                firstProgress.nextHintIdx,
                firstProgress.notified20,
                firstProgress.notified5,
                92.0,
                0,
                5f
        );

        assertTrue(firstProgress.notified20);
        assertTrue(firstProgress.notified5);
        assertTrue(secondProgress.signals.isEmpty());
    }

    @Test
    public void advance_suppressesImminentSignalWhenAlreadyTooCloseToTurn() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.singletonList(new VoiceHint(1, 0, 0, 0.0, 0)),
                60.0,
                111.0
        );
        PolylineIndex index = new PolylineIndex(route.track);

        TurnEventPlanner.Progress progress = planner.advance(
                route,
                index,
                0,
                false,
                false,
                108.0,
                0,
                20f
        );

        assertTrue(progress.signals.isEmpty());
    }

    @Test
    public void advance_surfacesCloseFollowingTurnWhenNotifiedTurnIsTooLateToRepeat() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.001027),
                        new LatLon(0.0, 0.002)
                ),
                Arrays.asList(
                        new VoiceHint(1, 5, 0, 0.0, 0),
                        new VoiceHint(2, 2, 0, 0.0, 0)
                ),
                222.0,
                222.0
        );
        PolylineIndex index = new PolylineIndex(route.track);

        TurnEventPlanner.Progress progress = planner.advance(
                route,
                index,
                0,
                true,
                true,
                index.distanceAtPointIndex(1) - 5.5,
                0,
                3f
        );

        assertEquals(1, progress.nextHintIdx);
        assertEquals(1, progress.signals.size());
        assertEquals(TurnEventPlanner.TurnSignal.Type.IMMINENT, progress.signals.get(0).type);
        assertEquals(2, progress.signals.get(0).hint.indexInTrack);
        assertTrue(progress.notified20);
        assertTrue(progress.notified5);
    }

    @Test
    public void advance_emitsPreparatorySignalWhenEtaComesFromRouteTiming() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.singletonList(new VoiceHint(1, 6, 0, 0.0, 0)),
                88.0,
                111.0
        );
        PolylineIndex index = new PolylineIndex(route.track);

        TurnEventPlanner.Progress progress = planner.advance(
                route,
                index,
                0,
                false,
                false,
                index.totalLengthMeters() - 10.0,
                0,
                0f
        );

        assertEquals(1, progress.signals.size());
        assertEquals(TurnEventPlanner.TurnSignal.Type.IMMINENT, progress.signals.get(0).type);
        assertTrue(progress.notified20);
        assertFalse(progress.notified5);
    }

    @Test
    public void advance_emitsFiveSecondSignalAtSlowWalkingSpeed() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.singletonList(new VoiceHint(1, 0, 0, 0.0, 0)),
                0.0,
                111.0
        );
        PolylineIndex index = new PolylineIndex(route.track);

        TurnEventPlanner.Progress progress = planner.advance(
                route,
                index,
                0,
                false,
                false,
                index.totalLengthMeters() - 1.5,
                0,
                0.3f
        );

        assertEquals(1, progress.signals.size());
        assertEquals(TurnEventPlanner.TurnSignal.Type.IMMINENT, progress.signals.get(0).type);
        assertTrue(progress.notified5);
    }

    @Test
    public void advance_emitsFiveSecondSignalWhenRouteTimingIsSlow() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.singletonList(new VoiceHint(1, 0, 0, 0.0, 0)),
                Arrays.asList(0.0, 370.0),
                370.0,
                111.0
        );
        PolylineIndex index = new PolylineIndex(route.track);

        TurnEventPlanner.Progress progress = planner.advance(
                route,
                index,
                0,
                false,
                false,
                index.totalLengthMeters() - 1.5,
                0,
                0f
        );

        assertEquals(1, progress.signals.size());
        assertEquals(TurnEventPlanner.TurnSignal.Type.IMMINENT, progress.signals.get(0).type);
        assertTrue(progress.notified5);
    }

    @Test
    public void buildInitialSignal_returnsNullWhenTurnDistanceIsNotReliable() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.singletonList(new VoiceHint(1, 0, 0, 0.0, 0)),
                60.0,
                111.0
        );
        PolylineIndex index = new PolylineIndex(route.track);

        TurnEventPlanner.TurnSignal signal = planner.buildInitialSignal(
                route,
                index,
                0,
                false,
                100.0,
                0,
                5f,
                20f
        );

        assertNull(signal);
    }

    @Test
    public void buildInitialSignal_usesRouteTrackTimesWhenSameSegmentSpeedIsUnavailable() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.singletonList(new VoiceHint(1, 0, 0, 0.0, 0)),
                Arrays.asList(0.0, 42.0),
                42.0,
                111.0
        );
        PolylineIndex index = new PolylineIndex(route.track);

        TurnEventPlanner.TurnSignal signal = planner.buildInitialSignal(
                route,
                index,
                0,
                false,
                0.0,
                0,
                0f,
                5f
        );

        assertNotNull(signal);
        assertEquals(42.0, signal.timeSeconds, 0.0);
    }

    @Test
    public void buildInitialSignal_marksTimeUnavailableWhenSpeedAndTrackTimesAreUnavailable() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001)
                ),
                Collections.singletonList(new VoiceHint(1, 0, 0, 0.0, 0)),
                0.0,
                111.0
        );
        PolylineIndex index = new PolylineIndex(route.track);

        TurnEventPlanner.TurnSignal signal = planner.buildInitialSignal(
                route,
                index,
                0,
                false,
                0.0,
                0,
                0f,
                5f
        );

        assertNotNull(signal);
        assertTrue(Double.isNaN(signal.timeSeconds));
    }

    @Test
    public void buildInitialSignal_usesRouteTrackTimesWhenHintIsAfterCurrentSegment() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002)
                ),
                Collections.singletonList(new VoiceHint(2, 0, 0, 0.0, 0)),
                Arrays.asList(0.0, 20.0, 42.0),
                42.0,
                222.0
        );
        PolylineIndex index = new PolylineIndex(route.track);

        TurnEventPlanner.TurnSignal signal = planner.buildInitialSignal(
                route,
                index,
                0,
                false,
                0.0,
                0,
                0f,
                5f
        );

        assertNotNull(signal);
        assertEquals(42.0, signal.timeSeconds, 0.0);
    }
}
