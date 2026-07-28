package vibro.navigator.about;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.core.content.ContextCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.util.ReflectionHelpers;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.voice.NavigationManeuverVoiceLabelFormatter;
import vibro.navigator.nav.voice.NavigationTextToSpeechVoiceAvailability;
import vibro.navigator.nav.voice.NavigationVoiceOption;

@RunWith(RobolectricTestRunner.class)
public class AboutManeuverVoiceSettingsTest {
    private static final String OTHER_VOICE = "other-voice";
    private static final String SELECTED_VOICE = "selected-voice";

    @Test
    public void aboutPageDoesNotInitializeVoiceClientBeforeVoiceDialog() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        AboutSettingsControllers controllers = ReflectionHelpers.getField(activity, "settingsControllers");
        AboutManeuverVoiceSettings settings = ReflectionHelpers.getField(controllers, "maneuverVoiceSettings");
        AboutManeuverVoiceClientLoader loader = ReflectionHelpers.getField(settings, "voiceClientLoader");

        assertNull(ReflectionHelpers.getField(loader, "voiceClient"));
        assertNull(ReflectionHelpers.getField(settings, "voiceAdapter"));
    }

    @Test
    public void openingVoiceDialogDefersVoiceClientInitialization() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        ImageButton settingsButton = new ImageButton(activity);
        RecordingScheduler scheduler = new RecordingScheduler();

        new AboutManeuverVoiceSettings(activity, settingsButton, new Switch(activity), scheduler);

        settingsButton.performClick();
        shadowOf(Looper.getMainLooper()).idleFor(
                AboutDeferredDialogAction.OPEN_DELAY_MS + 50,
                TimeUnit.MILLISECONDS
        );

        assertNotNull(ShadowAlertDialog.getLatestAlertDialog());
        assertEquals(AboutManeuverVoiceClientLoader.INIT_DELAY_MS, scheduler.delayMs);
        assertNotNull(scheduler.delayedRunnable);
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
                Collections.emptySet()
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
                NavigationManeuverVoiceLabelFormatter.format(context, voice)
        );
    }

    @Test
    public void formatVoiceLabel_usesReadableMaleVariantForGoogleVoiceName() {
        Context context = ApplicationProvider.getApplicationContext();
        Voice voice = voice("en-us-x-sfg#male_2-local", Locale.US);

        assertEquals(
                voiceLabel(context, Locale.US, "Male 2"),
                NavigationManeuverVoiceLabelFormatter.format(context, voice)
        );
    }

    @Test
    public void formatVoiceLabel_usesCompactStableCodeForOpaqueGoogleVoiceName() {
        Context context = ApplicationProvider.getApplicationContext();
        Voice voice = voice("en-us-x-iol-local", Locale.US);

        assertEquals(
                voiceLabel(context, Locale.US, "Voice IOL"),
                NavigationManeuverVoiceLabelFormatter.format(context, voice)
        );
    }

    @Test
    public void formatVoiceLabel_stripsLocaleAndSourceFromPlainVoiceName() {
        Context context = ApplicationProvider.getApplicationContext();
        Voice voice = voice("en-us-default-local", Locale.US);

        assertEquals(
                voiceLabel(context, Locale.US, "Default"),
                NavigationManeuverVoiceLabelFormatter.format(context, voice)
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
                        new NavigationVoiceOption(OTHER_VOICE, "Other"),
                        new NavigationVoiceOption(SELECTED_VOICE, "Selected")
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

    private static final class RecordingScheduler implements TaskScheduler {
        private Runnable delayedRunnable;
        private long delayMs = -1L;

        @Override
        public void post(@NonNull Runnable runnable) {
            delayedRunnable = runnable;
            delayMs = 0L;
        }

        @Override
        public void postDelayed(@NonNull Runnable runnable, long delayMs) {
            delayedRunnable = runnable;
            this.delayMs = delayMs;
        }
    }
}
