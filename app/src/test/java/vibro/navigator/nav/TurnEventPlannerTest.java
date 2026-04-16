package vibro.navigator.nav;

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
                5f,
                5f
        );

        assertEquals(1, progress.nextHintIdx);
        assertEquals(1, progress.signals.size());
        assertEquals(TurnEventPlanner.TurnSignal.Type.PASSED, progress.signals.get(0).type);
        assertFalse(progress.notified10);
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
                5f,
                5f
        );

        assertEquals(1, progress.signals.size());
        assertEquals(TurnEventPlanner.TurnSignal.Type.IMMINENT, progress.signals.get(0).type);
        assertTrue(progress.notified5);
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
    public void advance_suppressesImminentSignalInsideAccuracyRadius() {
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
                100.0,
                0,
                5f,
                20f
        );

        assertTrue(progress.signals.isEmpty());
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
