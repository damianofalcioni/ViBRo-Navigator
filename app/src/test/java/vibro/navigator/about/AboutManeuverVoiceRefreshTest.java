package vibro.navigator.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.widget.Spinner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowTextToSpeech;

import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.nav.voice.NavigationTextToSpeechSettingsLauncher;
import vibro.navigator.nav.voice.NavigationVoiceOption;
import vibro.navigator.settings.AppSettings;

@RunWith(RobolectricTestRunner.class)
public class AboutManeuverVoiceRefreshTest {
    private static final String SELECTED_VOICE = "selected-voice";
    private static final String INSTALLED_VOICE = "installed-voice";

    @Test
    public void returningFromAndroidTtsSettingsRefreshesOpenSpinnerAndKeepsSelection() {
        ActivityController<AboutActivity> controller = AboutActivityTestSupport.setupControllerWithSettings();
        AboutActivity activity = controller.get();
        ShadowTextToSpeech.addVoice(voice(SELECTED_VOICE));
        AppSettings.setManeuverSpeechEnabled(activity, true);
        AppSettings.setManeuverVoiceName(activity, SELECTED_VOICE);
        AlertDialog dialog = openVoiceDialog(activity);
        TextToSpeech originalEngine = finishVoiceInitialization();
        Spinner spinner = dialog.findViewById(R.id.aboutManeuverVoiceSpinner);
        assertEquals(2, spinner.getCount());

        dialog.findViewById(R.id.aboutTtsSettingsButton).performClick();
        assertEquals(NavigationTextToSpeechSettingsLauncher.ACTION_TTS_SETTINGS,
                shadowOf(activity).getNextStartedActivity().getAction());
        controller.pause().stop();
        ShadowTextToSpeech.addVoice(voice(INSTALLED_VOICE));
        controller.start().resume();
        idleVoiceLoading();
        assertNotSame(originalEngine, ShadowTextToSpeech.getLastTextToSpeechInstance());
        TextToSpeech refreshedEngine = finishVoiceInitialization();

        assertTrue(dialog.isShowing());
        assertSame(spinner, dialog.findViewById(R.id.aboutManeuverVoiceSpinner));
        assertEquals(3, spinner.getCount());
        assertEquals(SELECTED_VOICE, ((NavigationVoiceOption) spinner.getSelectedItem()).voiceName);
        assertEquals(SELECTED_VOICE, AppSettings.getManeuverVoiceName(activity));
        assertTrue(shadowOf(originalEngine).isShutdown());
        dialog.dismiss();
        controller.pause().stop().destroy();
        assertTrue(shadowOf(refreshedEngine).isShutdown());
    }

    @Test
    public void reopeningVoiceDialogLoadsVoicesInstalledWhileDialogWasClosed() {
        ActivityController<AboutActivity> controller = AboutActivityTestSupport.setupControllerWithSettings();
        AboutActivity activity = controller.get();
        AlertDialog originalDialog = openVoiceDialog(activity);
        TextToSpeech originalEngine = finishVoiceInitialization();
        originalDialog.dismiss();

        controller.pause().stop();
        ShadowTextToSpeech.addVoice(voice(INSTALLED_VOICE));
        controller.start().resume();
        idleVoiceLoading();
        assertSame(originalEngine, ShadowTextToSpeech.getLastTextToSpeechInstance());

        AlertDialog reopenedDialog = openVoiceDialog(activity);
        assertNotSame(originalEngine, ShadowTextToSpeech.getLastTextToSpeechInstance());
        TextToSpeech refreshedEngine = finishVoiceInitialization();
        Spinner spinner = reopenedDialog.findViewById(R.id.aboutManeuverVoiceSpinner);
        assertEquals(2, spinner.getCount());
        assertEquals(INSTALLED_VOICE, ((NavigationVoiceOption) spinner.getItemAtPosition(1)).voiceName);
        assertTrue(shadowOf(originalEngine).isShutdown());
        spinner.setSelection(1);
        shadowOf(Looper.getMainLooper()).idle();
        reopenedDialog.findViewById(R.id.aboutManeuverVoicePlayButton).performClick();
        assertEquals(INSTALLED_VOICE, shadowOf(refreshedEngine).getCurrentVoice().getName());
        assertEquals(INSTALLED_VOICE, AppSettings.getManeuverVoiceName(activity));
        reopenedDialog.dismiss();
        controller.pause().stop().destroy();
    }

    @Test
    public void resumingSettingsWithoutVoiceDialogDoesNotInitializeTts() {
        ActivityController<AboutActivity> controller = AboutActivityTestSupport.setupControllerWithSettings();

        controller.pause().stop().start().resume();
        idleVoiceLoading();

        assertNull(ShadowTextToSpeech.getLastTextToSpeechInstance());
        controller.pause().stop().destroy();
    }

    private static AlertDialog openVoiceDialog(AboutActivity activity) {
        activity.findViewById(R.id.aboutManeuverVoiceSettingsButton).performClick();
        idleVoiceLoading();
        return ShadowAlertDialog.getLatestAlertDialog();
    }

    private static void idleVoiceLoading() {
        shadowOf(Looper.getMainLooper()).idleFor(
                AboutDeferredDialogAction.OPEN_DELAY_MS + AboutManeuverVoiceClientLoader.INIT_DELAY_MS + 50,
                TimeUnit.MILLISECONDS
        );
    }

    private static TextToSpeech finishVoiceInitialization() {
        TextToSpeech engine = ShadowTextToSpeech.getLastTextToSpeechInstance();
        shadowOf(engine).getOnInitListener().onInit(TextToSpeech.SUCCESS);
        shadowOf(Looper.getMainLooper()).idle();
        return engine;
    }

    private static Voice voice(String name) {
        return new Voice(name, Locale.US, Voice.QUALITY_NORMAL, Voice.LATENCY_NORMAL,
                false, Collections.emptySet());
    }
}
