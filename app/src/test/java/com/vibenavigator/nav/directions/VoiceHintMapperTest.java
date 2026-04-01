package com.vibenavigator.nav.directions;

import com.vibenavigator.R;
import com.vibenavigator.nav.route.VoiceHint;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VoiceHintMapperTest {

    @Test
    public void mapsTurnLeft() {
        VoiceHint vh = new VoiceHint(0, 2, 0, 0, 0);
        DirectionInfo di = VoiceHintMapper.toDirection(vh);
        assertEquals(DirectionKind.LEFT, di.kind);
        assertEquals(R.string.direction_turn_left, di.labelRes);
    }

    @Test
    public void mapsRoundaboutExit() {
        VoiceHint vh = new VoiceHint(0, 13, 3, 0, 0);
        DirectionInfo di = VoiceHintMapper.toDirection(vh);
        assertEquals(R.string.direction_roundabout_exit, di.labelRes);
        assertEquals(3, di.exitNumber);
    }
}

