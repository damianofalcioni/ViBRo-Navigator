package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import vibro.navigator.R;

import android.app.Activity;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.widget.EditText;

import androidx.annotation.NonNull;

import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.poi.PoiHistoryStore;
import vibro.navigator.poi.search.PoiSearchClient;
import vibro.navigator.poi.ui.PoiInputController;
import vibro.navigator.settings.AppSpeechRecognitionSettings;
import vibro.navigator.speech.SpeechInputLauncher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowToast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class MainActivitySpeechInputControllerTest {

    private static final String CAFE_QUERY = "Cafe Central";
    private static final String EXISTING_DESTINATION = "Existing destination";

    private Activity activity;
    private RecordingScheduler scheduler;
    private RecordingSpeechInputLauncher speechInputLauncher;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        scheduler = new RecordingScheduler();
        speechInputLauncher = new RecordingSpeechInputLauncher();
        ShadowToast.reset();
        activity.getSharedPreferences("vibenavigator_poi_history", Activity.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
        AppSpeechRecognitionSettings.setEnabled(activity, true);
        AppSpeechRecognitionSettings.setLanguageTag(activity, AppSpeechRecognitionSettings.LANGUAGE_SYSTEM_DEFAULT);
    }

    @Test
    public void openDestinationSpeechInput_defersRecognizerLaunchForPressFeedback() {
        MainActivitySpeechInputController controller = createController();
        PoiInputController inputController = createPoiController();

        controller.openDestinationSpeechInput(inputController);

        assertEquals(MainActivitySpeechInputController.SPEECH_INPUT_LAUNCH_DELAY_MS, scheduler.delayMs);
        assertNull(speechInputLauncher.startedIntent);

        scheduler.runDelayed();

        assertRecognizerIntent(
                speechInputLauncher.startedIntent,
                activity.getString(R.string.prompt_speech_destination)
        );
        inputController.dispose();
    }

    @Test
    public void openStopSpeechInput_usesStopPrompt() {
        MainActivitySpeechInputController controller = createController();
        PoiInputController inputController = createPoiController();

        controller.openStopSpeechInput(inputController);
        scheduler.runDelayed();

        assertRecognizerIntent(speechInputLauncher.startedIntent, activity.getString(R.string.prompt_speech_stop));
        inputController.dispose();
    }

    @Test
    public void isSpeechInputVisible_returnsFalseWhenSettingIsDisabled() {
        AppSpeechRecognitionSettings.setLanguageTag(activity, "de-AT");
        AppSpeechRecognitionSettings.setEnabled(activity, false);
        MainActivitySpeechInputController controller = createController();

        assertFalse(controller.isSpeechInputVisible());
    }

    @Test
    public void openDestinationSpeechInput_usesConfiguredRecognitionLanguage() {
        AppSpeechRecognitionSettings.setLanguageTag(activity, "de_AT");
        MainActivitySpeechInputController controller = createController();
        PoiInputController inputController = createPoiController();

        controller.openDestinationSpeechInput(inputController);
        scheduler.runDelayed();

        assertRecognizerIntent(
                speechInputLauncher.startedIntent,
                activity.getString(R.string.prompt_speech_destination)
        );
        assertEquals("de-AT", speechInputLauncher.startedIntent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE));
        inputController.dispose();
    }

    @Test
    public void handleActivityResult_appliesRecognizedTextAsEditableQuery() {
        MainActivitySpeechInputController controller = createController();
        PoiInputController inputController = createPoiController();
        controller.openDestinationSpeechInput(inputController);
        scheduler.runDelayed();
        Intent result = speechResult("  " + CAFE_QUERY + "  ");

        assertTrue(controller.handleActivityResult(speechInputLauncher.requestCode, Activity.RESULT_OK, result));

        assertEquals(CAFE_QUERY, inputController.getRawText());
        assertNull(inputController.getSelectedPoi());
        inputController.dispose();
    }

    @Test
    public void handleActivityResult_showsMessageWhenRecognizerReturnsNoText() {
        MainActivitySpeechInputController controller = createController();
        PoiInputController inputController = createPoiController();
        inputController.restoreText(EXISTING_DESTINATION);
        controller.openDestinationSpeechInput(inputController);
        scheduler.runDelayed();

        assertTrue(controller.handleActivityResult(
                speechInputLauncher.requestCode,
                Activity.RESULT_OK,
                speechResult("   ")
        ));

        assertEquals(EXISTING_DESTINATION, inputController.getRawText());
        assertEquals(
                activity.getString(R.string.msg_speech_input_empty),
                ShadowToast.getTextOfLatestToast()
        );
        inputController.dispose();
    }

    @Test
    public void handleActivityResult_ignoresOtherRequestCodes() {
        MainActivitySpeechInputController controller = createController();

        assertFalse(controller.handleActivityResult(9999, Activity.RESULT_OK, speechResult("Ignored")));
    }

    @Test
    public void directSpeechInputCallback_appliesRecognizedTextAsEditableQuery() {
        speechInputLauncher.startMode = SpeechInputLauncher.StartMode.DIRECT;
        MainActivitySpeechInputController controller = createController();
        PoiInputController inputController = createPoiController();
        controller.openDestinationSpeechInput(inputController);
        scheduler.runDelayed();

        speechInputLauncher.callback.onSpeechInputResult(speechResult("  " + CAFE_QUERY + "  "));

        assertEquals(CAFE_QUERY, inputController.getRawText());
        assertNull(inputController.getSelectedPoi());
        inputController.dispose();
    }

    @Test
    public void directSpeechInput_ignoresStrayCancelledActivityResult() {
        speechInputLauncher.startMode = SpeechInputLauncher.StartMode.DIRECT;
        MainActivitySpeechInputController controller = createController();
        PoiInputController inputController = createPoiController();
        controller.openDestinationSpeechInput(inputController);
        scheduler.runDelayed();

        assertTrue(controller.handleActivityResult(speechInputLauncher.requestCode, Activity.RESULT_CANCELED, null));
        speechInputLauncher.callback.onSpeechInputResult(speechResult(CAFE_QUERY));

        assertEquals(CAFE_QUERY, inputController.getRawText());
        inputController.dispose();
    }

    @Test
    public void unavailableSpeechInput_showsMessageAndKeepsCurrentText() {
        speechInputLauncher.startMode = SpeechInputLauncher.StartMode.UNAVAILABLE;
        MainActivitySpeechInputController controller = createController();
        PoiInputController inputController = createPoiController();
        inputController.restoreText(EXISTING_DESTINATION);

        controller.openDestinationSpeechInput(inputController);
        scheduler.runDelayed();

        assertEquals(EXISTING_DESTINATION, inputController.getRawText());
        assertEquals(
                activity.getString(R.string.msg_speech_input_unavailable),
                ShadowToast.getTextOfLatestToast()
        );
        inputController.dispose();
    }

    @Test
    public void directSpeechInputMessage_showsMessageAndClearsPendingTarget() {
        speechInputLauncher.startMode = SpeechInputLauncher.StartMode.DIRECT;
        MainActivitySpeechInputController controller = createController();
        PoiInputController inputController = createPoiController();
        inputController.restoreText(EXISTING_DESTINATION);
        controller.openDestinationSpeechInput(inputController);
        scheduler.runDelayed();

        speechInputLauncher.callback.onSpeechInputMessage(R.string.msg_speech_permission_required);
        speechInputLauncher.callback.onSpeechInputResult(speechResult(CAFE_QUERY));

        assertEquals(EXISTING_DESTINATION, inputController.getRawText());
        assertEquals(
                activity.getString(R.string.msg_speech_permission_required),
                ShadowToast.getTextOfLatestToast()
        );
        inputController.dispose();
    }

    @Test
    public void handleRequestPermissionsResult_delegatesToSpeechLauncher() {
        speechInputLauncher.permissionResultHandled = true;
        MainActivitySpeechInputController controller = createController();
        int[] grantResults = {Activity.RESULT_OK};

        assertTrue(controller.handleRequestPermissionsResult(5002, grantResults));

        assertEquals(5002, speechInputLauncher.permissionRequestCode);
        assertEquals(grantResults, speechInputLauncher.permissionGrantResults);
    }

    @NonNull
    private MainActivitySpeechInputController createController() {
        return new MainActivitySpeechInputController(activity, scheduler, speechInputLauncher);
    }

    @NonNull
    private PoiInputController createPoiController() {
        return new PoiInputController(
                activity,
                new EditText(activity),
                new PoiHistoryStore(activity),
                emptySearchClient(),
                poi -> {
                }
        );
    }

    @NonNull
    private static Intent speechResult(@NonNull String text) {
        Intent intent = new Intent();
        intent.putStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS,
                new ArrayList<>(Arrays.asList(text))
        );
        return intent;
    }

    private static PoiSearchClient emptySearchClient() {
        return (query, limit) -> Collections.emptyList();
    }

    private static void assertRecognizerIntent(
            @NonNull Intent started,
            @NonNull String prompt
    ) {
        assertNotNull(started);
        assertEquals(RecognizerIntent.ACTION_RECOGNIZE_SPEECH, started.getAction());
        assertEquals(
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                started.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL)
        );
        assertEquals(prompt, started.getStringExtra(RecognizerIntent.EXTRA_PROMPT));
        assertEquals(1, started.getIntExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 0));
    }

    private static final class RecordingSpeechInputLauncher implements SpeechInputLauncher {
        private StartMode startMode = StartMode.ACTIVITY;
        private Intent startedIntent;
        private int requestCode = -1;
        private Callback callback;
        private boolean permissionResultHandled;
        private int permissionRequestCode = -1;
        private int[] permissionGrantResults;

        @NonNull
        @Override
        public StartMode start(
                @NonNull Intent recognizerIntent,
                int requestCode,
                @NonNull Callback callback
        ) {
            startedIntent = recognizerIntent;
            this.requestCode = requestCode;
            this.callback = callback;
            return startMode;
        }

        @Override
        public boolean handleRequestPermissionsResult(int requestCode, @NonNull int[] grantResults) {
            permissionRequestCode = requestCode;
            permissionGrantResults = grantResults;
            return permissionResultHandled;
        }

        @Override
        public void dispose() {
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

        private void runDelayed() {
            assertNotNull(delayedRunnable);
            Runnable runnable = delayedRunnable;
            delayedRunnable = null;
            runnable.run();
        }
    }
}
