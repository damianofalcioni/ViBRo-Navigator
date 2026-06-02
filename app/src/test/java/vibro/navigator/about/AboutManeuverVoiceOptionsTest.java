package vibro.navigator.about;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import vibro.navigator.settings.AppSettings;

public class AboutManeuverVoiceOptionsTest {
    private static final String CUSTOM_VOICE = "custom-voice";

    @Test
    public void shouldPersistSelectedVoice_ignoresTemporaryDisabledBeforeVoiceListLoads() {
        assertFalse(AboutManeuverVoiceOptions.shouldPersistSelectedVoice(
                false,
                CUSTOM_VOICE,
                AppSettings.MANEUVER_VOICE_DISABLED
        ));
    }

    @Test
    public void shouldPersistSelectedVoice_ignoresTemporarySystemDefaultBeforeVoiceListLoads() {
        assertFalse(AboutManeuverVoiceOptions.shouldPersistSelectedVoice(
                false,
                CUSTOM_VOICE,
                AppSettings.MANEUVER_VOICE_SYSTEM_DEFAULT
        ));
    }

    @Test
    public void shouldPersistSelectedVoice_allowsFallbackAfterVoiceListLoads() {
        assertTrue(AboutManeuverVoiceOptions.shouldPersistSelectedVoice(
                true,
                CUSTOM_VOICE,
                AppSettings.MANEUVER_VOICE_DISABLED
        ));
    }

    @Test
    public void shouldPersistSelectedVoice_allowsChangingBaseVoiceBeforeVoiceListLoads() {
        assertTrue(AboutManeuverVoiceOptions.shouldPersistSelectedVoice(
                false,
                AppSettings.MANEUVER_VOICE_DISABLED,
                AppSettings.MANEUVER_VOICE_SYSTEM_DEFAULT
        ));
    }

    @Test
    public void shouldPersistSelectedVoice_ignoresAlreadySavedSelection() {
        assertFalse(AboutManeuverVoiceOptions.shouldPersistSelectedVoice(
                true,
                CUSTOM_VOICE,
                CUSTOM_VOICE
        ));
    }
}
