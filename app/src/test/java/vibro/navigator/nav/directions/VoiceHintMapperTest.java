package vibro.navigator.nav.directions;

import vibro.navigator.R;
import vibro.navigator.nav.route.VoiceHint;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VoiceHintMapperTest {

    @Test
    public void mapsTurnLeft() {
        VoiceHint vh = new VoiceHint(0, 2, 0, 0, 0);
        DirectionInfo di = VoiceHintMapper.toDirection(vh);
        assertEquals(DirectionKind.LEFT, di.kind);
        assertEquals(R.string.direction_turn_left, di.labelRes);
        assertEquals("←", di.emoji);
    }

    @Test
    public void mapsRoundaboutExit() {
        VoiceHint vh = new VoiceHint(0, 13, 3, 0, 0);
        DirectionInfo di = VoiceHintMapper.toDirection(vh);
        assertEquals(R.string.direction_roundabout_exit, di.labelRes);
        assertEquals(3, di.exitNumber);
        assertEquals("○>", di.emoji);
    }

    @Test
    public void mapsAllMode9JsonCodes() {
        assertDirection(1, 0, DirectionKind.STRAIGHT, R.string.direction_continue, 0, "⬆");
        assertDirection(2, 0, DirectionKind.LEFT, R.string.direction_turn_left, 0, "←");
        assertDirection(3, 0, DirectionKind.LEFT, R.string.direction_slight_left, 0, "↖");
        assertDirection(4, 0, DirectionKind.LEFT, R.string.direction_sharp_left, 0, "<←");
        assertDirection(5, 0, DirectionKind.RIGHT, R.string.direction_turn_right, 0, "→");
        assertDirection(6, 0, DirectionKind.RIGHT, R.string.direction_slight_right, 0, "↗");
        assertDirection(7, 0, DirectionKind.RIGHT, R.string.direction_sharp_right, 0, "→>");
        assertDirection(8, 0, DirectionKind.LEFT, R.string.direction_keep_left, 0, "↖");
        assertDirection(9, 0, DirectionKind.RIGHT, R.string.direction_keep_right, 0, "↗");
        assertDirection(10, 0, DirectionKind.LEFT, R.string.direction_uturn_left, 0, "U↙");
        assertDirection(11, 0, DirectionKind.RIGHT, R.string.direction_uturn_right, 0, "U↘");
        assertDirection(12, 0, DirectionKind.OTHER, R.string.direction_offroute, 0, "!!×");
        assertDirection(13, 3, DirectionKind.RIGHT, R.string.direction_roundabout_exit, 3, "○>");
        assertDirection(14, -2, DirectionKind.LEFT, R.string.direction_roundabout_exit_left, 2, "<○");
        assertDirection(15, 0, DirectionKind.OTHER, R.string.direction_uturn, 0, "U↓");
        assertDirection(16, 0, DirectionKind.OTHER, R.string.direction_beeline, 0, "◉");
        assertDirection(17, 0, DirectionKind.LEFT, R.string.direction_exit_left, 0, "←");
        assertDirection(18, 0, DirectionKind.RIGHT, R.string.direction_exit_right, 0, "→");
    }

    @Test
    public void mapsUnknownCommandToNeutralFallback() {
        assertDirection(999, 0, DirectionKind.OTHER, R.string.direction_unknown, 0, "?");
    }

    private static void assertDirection(int command,
                                        int exitNumber,
                                        DirectionKind expectedKind,
                                        int expectedLabelRes,
                                        int expectedExitNumber,
                                        String expectedEmoji) {
        VoiceHint vh = new VoiceHint(0, command, exitNumber, 0, 0);
        DirectionInfo di = VoiceHintMapper.toDirection(vh);
        assertEquals(expectedKind, di.kind);
        assertEquals(expectedLabelRes, di.labelRes);
        assertEquals(expectedExitNumber, di.exitNumber);
        assertEquals(expectedEmoji, di.emoji);
    }
}
