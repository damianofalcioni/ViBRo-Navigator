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
import vibro.navigator.settings.AppSettings;

@RunWith(RobolectricTestRunner.class)
public class AboutManeuverVoiceSettingsTest {
    private static final String CUSTOM_VOICE = "custom-voice";

    @Test
    public void shouldPersistSelectedVoice_ignoresTemporaryDisabledBeforeVoiceListLoads() {
        assertFalse(AboutManeuverVoiceSettings.shouldPersistSelectedVoice(
                false,
                CUSTOM_VOICE,
                AppSettings.MANEUVER_VOICE_DISABLED
        ));
    }

    @Test
    public void shouldPersistSelectedVoice_ignoresTemporarySystemDefaultBeforeVoiceListLoads() {
        assertFalse(AboutManeuverVoiceSettings.shouldPersistSelectedVoice(
                false,
                CUSTOM_VOICE,
                AppSettings.MANEUVER_VOICE_SYSTEM_DEFAULT
        ));
    }

    @Test
    public void shouldPersistSelectedVoice_allowsFallbackAfterVoiceListLoads() {
        assertTrue(AboutManeuverVoiceSettings.shouldPersistSelectedVoice(
                true,
                CUSTOM_VOICE,
                AppSettings.MANEUVER_VOICE_DISABLED
        ));
    }

    @Test
    public void shouldPersistSelectedVoice_allowsChangingBaseVoiceBeforeVoiceListLoads() {
        assertTrue(AboutManeuverVoiceSettings.shouldPersistSelectedVoice(
                false,
                AppSettings.MANEUVER_VOICE_DISABLED,
                AppSettings.MANEUVER_VOICE_SYSTEM_DEFAULT
        ));
    }

    @Test
    public void shouldPersistSelectedVoice_ignoresAlreadySavedSelection() {
        assertFalse(AboutManeuverVoiceSettings.shouldPersistSelectedVoice(
                true,
                CUSTOM_VOICE,
                CUSTOM_VOICE
        ));
    }

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
