package vibro.navigator.about;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.core.content.ContextCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import vibro.navigator.R;
import vibro.navigator.nav.voice.NavigationTextToSpeechVoiceAvailability;
import vibro.navigator.settings.AppSettings;

@RunWith(RobolectricTestRunner.class)
@SuppressWarnings("deprecation")
public class AboutManeuverVoiceSettingsTest {
    private static final String CUSTOM_VOICE = "custom-voice";
    private static final String OTHER_VOICE = "other-voice";
    private static final String SELECTED_VOICE = "selected-voice";

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

    @Test
    public void formatVoiceLabel_usesReadableGenderVariantForGoogleVoiceName() {
        Context context = ApplicationProvider.getApplicationContext();
        Voice voice = voice("en-us-x-sfg#female_1-local", Locale.US);

        assertEquals(
                voiceLabel(context, Locale.US, "Female 1"),
                AboutManeuverVoiceLabelFormatter.format(context, voice)
        );
    }

    @Test
    public void formatVoiceLabel_usesReadableMaleVariantForGoogleVoiceName() {
        Context context = ApplicationProvider.getApplicationContext();
        Voice voice = voice("en-us-x-sfg#male_2-local", Locale.US);

        assertEquals(
                voiceLabel(context, Locale.US, "Male 2"),
                AboutManeuverVoiceLabelFormatter.format(context, voice)
        );
    }

    @Test
    public void formatVoiceLabel_usesCompactStableCodeForOpaqueGoogleVoiceName() {
        Context context = ApplicationProvider.getApplicationContext();
        Voice voice = voice("en-us-x-iol-local", Locale.US);

        assertEquals(
                voiceLabel(context, Locale.US, "Voice IOL"),
                AboutManeuverVoiceLabelFormatter.format(context, voice)
        );
    }

    @Test
    public void formatVoiceLabel_stripsLocaleAndSourceFromPlainVoiceName() {
        Context context = ApplicationProvider.getApplicationContext();
        Voice voice = voice("en-us-default-local", Locale.US);

        assertEquals(
                voiceLabel(context, Locale.US, "Default"),
                AboutManeuverVoiceLabelFormatter.format(context, voice)
        );
    }

    @Test
    public void getDropDownView_highlightsSelectedVoice() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        AboutManeuverVoiceOptionAdapter adapter = voiceOptionAdapter(activity);
        adapter.setSelectedVoiceName(SELECTED_VOICE);

        View selectedView = adapter.getDropDownView(1, null, new Spinner(activity));
        TextView selectedText = (TextView) selectedView;

        assertTrue(selectedView.isSelected());
        assertTrue(selectedView.isActivated());
        assertEquals(
                ContextCompat.getColor(activity, R.color.success),
                selectedText.getCurrentTextColor()
        );
    }

    @Test
    public void getDropDownView_clearsHighlightForOtherVoices() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        AboutManeuverVoiceOptionAdapter adapter = voiceOptionAdapter(activity);
        adapter.setSelectedVoiceName(SELECTED_VOICE);

        View otherView = adapter.getDropDownView(0, null, new Spinner(activity));
        TextView otherText = (TextView) otherView;

        assertFalse(otherView.isSelected());
        assertFalse(otherView.isActivated());
        assertEquals(
                ContextCompat.getColor(activity, R.color.white),
                otherText.getCurrentTextColor()
        );
    }

    private static AboutManeuverVoiceOptionAdapter voiceOptionAdapter(Activity activity) {
        return new AboutManeuverVoiceOptionAdapter(
                activity,
                Arrays.asList(
                        new AboutManeuverVoiceSettings.VoiceOption(OTHER_VOICE, "Other"),
                        new AboutManeuverVoiceSettings.VoiceOption(SELECTED_VOICE, "Selected")
                )
        );
    }

    private static Voice voice(String name, Locale locale) {
        return new Voice(
                name,
                locale,
                Voice.QUALITY_NORMAL,
                Voice.LATENCY_NORMAL,
                false,
                Collections.emptySet()
        );
    }

    private static String voiceLabel(Context context, Locale locale, String variant) {
        return context.getString(
                R.string.format_maneuver_voice_option,
                locale.getDisplayName(),
                variant
        );
    }
}
