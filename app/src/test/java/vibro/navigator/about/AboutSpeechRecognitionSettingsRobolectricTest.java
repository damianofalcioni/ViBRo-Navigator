package vibro.navigator.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;
import static vibro.navigator.about.AboutDialogButtonStyleAssertions.assertSecondaryButtonBackground;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Intent;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.util.ReflectionHelpers;

import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.android.speech.AndroidSpeechRecognitionSettingsLauncher;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppSpeechRecognitionSettings;

@RunWith(RobolectricTestRunner.class)
public class AboutSpeechRecognitionSettingsRobolectricTest {
    @Before
    public void setUp() {
        Application context = ApplicationProvider.getApplicationContext();
        AppLogger.init(context);
        AppSpeechRecognitionSettings.setEnabled(context, true);
        AppSpeechRecognitionSettings.setLanguageTag(context, AppSpeechRecognitionSettings.LANGUAGE_SYSTEM_DEFAULT);
    }

    @Test
    public void aboutPageShowsSpeechRecognitionSettingWithSwitch() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        TextView label = activity.findViewById(R.id.aboutSpeechRecognitionLabel);
        ImageButton settingsButton = activity.findViewById(R.id.aboutSpeechRecognitionSettingsButton);
        Switch enabledSwitch = activity.findViewById(R.id.aboutSpeechRecognitionSwitch);

        assertEquals(activity.getString(R.string.label_speech_recognition), label.getText().toString());
        assertEquals(
                activity.getString(R.string.action_open_speech_recognition_settings),
                settingsButton.getContentDescription().toString()
        );
        assertEquals(
                activity.getString(R.string.action_enable_speech_recognition),
                enabledSwitch.getContentDescription().toString()
        );
    }

    @Test
    public void speechRecognitionSwitchDefaultsOnWhenProviderIsAvailable() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        Switch enabledSwitch = new Switch(activity);

        new AboutSpeechRecognitionSettings(
                activity,
                new Button(activity),
                enabledSwitch,
                () -> true
        );

        assertTrue(enabledSwitch.isEnabled());
        assertTrue(enabledSwitch.isChecked());

        enabledSwitch.performClick();

        assertFalse(AppSpeechRecognitionSettings.isEnabled(activity));
    }

    @Test
    public void speechRecognitionSwitchIsDisabledWhenProviderIsMissing() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        Switch enabledSwitch = new Switch(activity);

        new AboutSpeechRecognitionSettings(
                activity,
                new Button(activity),
                enabledSwitch,
                () -> false
        );

        assertFalse(enabledSwitch.isEnabled());
        assertFalse(enabledSwitch.isChecked());
        assertTrue(AppSpeechRecognitionSettings.isEnabled(activity));
    }

    @Test
    public void speechRecognitionProviderAvailabilityCanBeDeferred() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        Switch enabledSwitch = new Switch(activity);
        RecordingProviderAvailability providerAvailability = new RecordingProviderAvailability(true);
        RecordingScheduler scheduler = new RecordingScheduler();

        new AboutSpeechRecognitionSettings(
                activity,
                new Button(activity),
                enabledSwitch,
                providerAvailability,
                scheduler
        );

        assertEquals(0, providerAvailability.callCount);
        assertFalse(enabledSwitch.isEnabled());
        assertFalse(enabledSwitch.isChecked());
        assertEquals(AboutSpeechRecognitionSettings.PROVIDER_AVAILABILITY_RENDER_DELAY_MS, scheduler.delayMs);

        scheduler.runDelayed();

        assertEquals(1, providerAvailability.callCount);
        assertTrue(enabledSwitch.isEnabled());
        assertTrue(enabledSwitch.isChecked());
    }

    @Test
    public void speechRecognitionLanguageOptionsAreLoadedOnlyForDialog() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();
        AboutSpeechRecognitionSettings settings = new AboutSpeechRecognitionSettings(
                activity,
                new Button(activity),
                new Switch(activity),
                () -> true,
                new RecordingScheduler()
        );

        assertNull(ReflectionHelpers.getField(settings, "languageAdapter"));
    }

    @Test
    public void speechRecognitionDialogPersistsSelectedLanguage() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();

        openSpeechRecognitionDialog(activity);

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        TextView languageLabel = dialog.findViewById(R.id.aboutSpeechRecognitionLanguageLabel);
        Spinner spinner = dialog.findViewById(R.id.aboutSpeechRecognitionLanguageSpinner);

        assertEquals(activity.getString(R.string.label_speech_recognition_language), languageLabel.getText().toString());
        assertEquals(
                activity.getString(R.string.label_speech_recognition_language_system_default),
                spinner.getItemAtPosition(0).toString()
        );

        spinner.setSelection(languagePosition(spinner, "de-AT"));
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("de-AT", AppSpeechRecognitionSettings.getLanguageTag(activity));
    }

    @Test
    public void androidSttSettingsButtonOpensVoiceInputSettings() {
        AboutActivity activity = AboutActivityTestSupport.setupWithSettings();

        openSpeechRecognitionDialog(activity);

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        Button sttSettingsButton = dialog.findViewById(R.id.aboutSpeechRecognitionAndroidSettingsButton);

        assertSecondaryButtonBackground(sttSettingsButton);
        assertEquals(
                activity.getString(R.string.action_android_stt_settings_short),
                sttSettingsButton.getText().toString()
        );
        sttSettingsButton.performClick();

        Intent startedIntent = shadowOf(activity).getNextStartedActivity();
        assertEquals(AndroidSpeechRecognitionSettingsLauncher.ACTION_VOICE_INPUT_SETTINGS, startedIntent.getAction());
    }

    private static void openSpeechRecognitionDialog(AboutActivity activity) {
        ImageButton settingsButton = activity.findViewById(R.id.aboutSpeechRecognitionSettingsButton);
        settingsButton.performClick();
        shadowOf(Looper.getMainLooper()).idleFor(
                AboutDeferredDialogAction.OPEN_DELAY_MS + 50,
                TimeUnit.MILLISECONDS
        );
    }

    private static int languagePosition(Spinner spinner, String languageTag) {
        for (int i = 0; i < spinner.getCount(); i++) {
            Object item = spinner.getItemAtPosition(i);
            if (item instanceof AboutSpeechRecognitionLanguageOption
                    && languageTag.equals(((AboutSpeechRecognitionLanguageOption) item).languageTag)) {
                return i;
            }
        }
        throw new AssertionError("Missing speech recognition language option " + languageTag);
    }

    private static final class RecordingProviderAvailability
            implements AboutSpeechRecognitionSettings.ProviderAvailability {
        private final boolean available;
        private int callCount;

        RecordingProviderAvailability(boolean available) {
            this.available = available;
        }

        @Override
        public boolean isAvailable() {
            callCount++;
            return available;
        }
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

        void runDelayed() {
            delayedRunnable.run();
        }
    }
}
