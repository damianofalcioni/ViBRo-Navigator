package vibro.navigator.nav.voice;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.nav.route.VoiceHint;

@RunWith(RobolectricTestRunner.class)
public class NavigationManeuverSpeechFormatterTest {
    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void formatTurnSpeech_readsCountdownBeforeDirection() {
        String message = NavigationManeuverSpeechFormatter.formatTurnSpeech(
                context,
                new VoiceHint(0, 2, 0, 0.0, 0),
                20.0
        );

        assertEquals("20s turn left", message);
    }

    @Test
    public void formatTurnSpeech_includesRoundaboutExit() {
        String message = NavigationManeuverSpeechFormatter.formatTurnSpeech(
                context,
                new VoiceHint(0, 13, 3, 0.0, 0),
                65.0
        );

        assertEquals("1min roundabout, exit 3", message);
    }

    @Test
    public void formatTurnSpeech_formatsReachedArrivalWithoutCountdown() {
        String message = NavigationManeuverSpeechFormatter.formatTurnSpeech(
                context,
                new VoiceHint(0, 100, 0, 0.0, 0),
                0.0
        );

        assertEquals("destination reached", message);
    }
}
