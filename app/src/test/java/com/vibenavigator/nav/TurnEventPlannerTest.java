package com.vibenavigator.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.vibenavigator.geo.LatLon;
import com.vibenavigator.nav.route.PolylineIndex;
import com.vibenavigator.nav.route.VoiceHint;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class TurnEventPlannerTest {

    private final TurnEventPlanner planner = new TurnEventPlanner();

    @Test
    public void advance_marksPassedHintsAndResetsImminentFlags() {
        PolylineIndex index = new PolylineIndex(Arrays.asList(
                new LatLon(0.0, 0.0),
                new LatLon(0.0, 0.001),
                new LatLon(0.0, 0.002)
        ));
        VoiceHint first = new VoiceHint(1, 0, 0, 0.0, 0);
        VoiceHint second = new VoiceHint(2, 0, 0, 0.0, 0);

        TurnEventPlanner.Progress progress = planner.advance(
                Arrays.asList(first, second),
                index,
                0,
                true,
                true,
                120.0,
                5f
        );

        assertEquals(1, progress.nextHintIdx);
        assertEquals(1, progress.signals.size());
        assertEquals(TurnEventPlanner.TurnSignal.Type.PASSED, progress.signals.get(0).type);
        assertFalse(progress.notified10);
        assertFalse(progress.notified5);
    }

    @Test
    public void advance_emitsTenAndFiveSecondSignalsWhenNeeded() {
        PolylineIndex index = new PolylineIndex(Arrays.asList(
                new LatLon(0.0, 0.0),
                new LatLon(0.0, 0.001)
        ));
        VoiceHint hint = new VoiceHint(1, 0, 0, 0.0, 0);

        TurnEventPlanner.Progress progress = planner.advance(
                Collections.singletonList(hint),
                index,
                0,
                false,
                false,
                90.0,
                5f
        );

        assertEquals(2, progress.signals.size());
        assertEquals(TurnEventPlanner.TurnSignal.Type.IMMINENT, progress.signals.get(0).type);
        assertEquals(TurnEventPlanner.TurnSignal.Type.IMMINENT, progress.signals.get(1).type);
        assertTrue(progress.notified10);
        assertTrue(progress.notified5);
    }

    @Test
    public void buildInitialSignal_usesCurrentDistanceToNextHint() {
        PolylineIndex index = new PolylineIndex(Arrays.asList(
                new LatLon(0.0, 0.0),
                new LatLon(0.0, 0.001)
        ));
        VoiceHint hint = new VoiceHint(1, 0, 0, 0.0, 0);

        TurnEventPlanner.TurnSignal signal = planner.buildInitialSignal(
                Collections.singletonList(hint),
                index,
                0,
                false,
                55.0,
                5f
        );

        assertNotNull(signal);
        assertEquals(TurnEventPlanner.TurnSignal.Type.INITIAL, signal.type);
        assertEquals(56.32, signal.distanceMeters, 0.5);
    }

    @Test
    public void buildInitialSignal_returnsNullWhenAlreadySent() {
        PolylineIndex index = new PolylineIndex(Arrays.asList(
                new LatLon(0.0, 0.0),
                new LatLon(0.0, 0.001)
        ));

        TurnEventPlanner.TurnSignal signal = planner.buildInitialSignal(
                Collections.singletonList(new VoiceHint(1, 0, 0, 0.0, 0)),
                index,
                0,
                true,
                0.0,
                0f
        );

        assertNull(signal);
    }
}
