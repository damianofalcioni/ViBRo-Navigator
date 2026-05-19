package vibro.navigator.about;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.Locale;

import vibro.navigator.nav.voice.NavigationTextToSpeechVoiceAvailability;

@RunWith(RobolectricTestRunner.class)
public class AboutManeuverVoiceSettingsTest {

    @Test
    public void isOfflineVoiceAvailable_acceptsInstalledEmbeddedVoice() {
        Voice voice = new Voice(
                "offline",
                Locale.US,
                Voice.QUALITY_NORMAL,
                Voice.LATENCY_NORMAL,
                false,
                Collections.emptySet()
        );

        assertTrue(NavigationTextToSpeechVoiceAvailability.isOfflineVoiceAvailable(voice));
    }

    @Test
    public void isOfflineVoiceAvailable_rejectsNetworkVoice() {
        Voice voice = new Voice(
                "network",
                Locale.US,
                Voice.QUALITY_NORMAL,
                Voice.LATENCY_NORMAL,
                true,
                Collections.singleton(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS)
        );

        assertFalse(NavigationTextToSpeechVoiceAvailability.isOfflineVoiceAvailable(voice));
    }

    @Test
    public void isOfflineVoiceAvailable_rejectsNotInstalledVoice() {
        Voice voice = new Voice(
                "missing",
                Locale.US,
                Voice.QUALITY_NORMAL,
                Voice.LATENCY_NORMAL,
                false,
                Collections.singleton(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
        );

        assertFalse(NavigationTextToSpeechVoiceAvailability.isOfflineVoiceAvailable(voice));
    }
}
