package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowToast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class MainActivitySpeechInputControllerTest {

    private Activity activity;
    private RecordingScheduler scheduler;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        scheduler = new RecordingScheduler();
        ShadowToast.reset();
        activity.getSharedPreferences("vibenavigator_poi_history", Activity.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void openDestinationSpeechInput_defersRecognizerLaunchForPressFeedback() {
        MainActivitySpeechInputController controller = new MainActivitySpeechInputController(activity, scheduler);
        PoiInputController inputController = createPoiController();

        controller.openDestinationSpeechInput(inputController);

        assertEquals(MainActivitySpeechInputController.SPEECH_INPUT_LAUNCH_DELAY_MS, scheduler.delayMs);
        assertNull(shadowOf(activity).getNextStartedActivityForResult());

        scheduler.runDelayed();

        ShadowActivity.IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();
        assertRecognizerIntent(
                started,
                activity.getString(R.string.prompt_speech_destination)
        );
        inputController.dispose();
    }

    @Test
    public void openStopSpeechInput_usesStopPrompt() {
        MainActivitySpeechInputController controller = new MainActivitySpeechInputController(activity, scheduler);
        PoiInputController inputController = createPoiController();

        controller.openStopSpeechInput(inputController);
        scheduler.runDelayed();

        ShadowActivity.IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();
        assertRecognizerIntent(started, activity.getString(R.string.prompt_speech_stop));
        inputController.dispose();
    }

    @Test
    public void handleActivityResult_appliesRecognizedTextAsEditableQuery() {
        MainActivitySpeechInputController controller = new MainActivitySpeechInputController(activity, scheduler);
        PoiInputController inputController = createPoiController();
        controller.openDestinationSpeechInput(inputController);
        scheduler.runDelayed();
        ShadowActivity.IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();
        Intent result = speechResult("  Cafe Central  ");

        assertTrue(controller.handleActivityResult(started.requestCode, Activity.RESULT_OK, result));

        assertEquals("Cafe Central", inputController.getRawText());
        assertNull(inputController.getSelectedPoi());
        inputController.dispose();
    }

    @Test
    public void handleActivityResult_showsMessageWhenRecognizerReturnsNoText() {
        MainActivitySpeechInputController controller = new MainActivitySpeechInputController(activity, scheduler);
        PoiInputController inputController = createPoiController();
        inputController.restoreText("Existing destination");
        controller.openDestinationSpeechInput(inputController);
        scheduler.runDelayed();
        ShadowActivity.IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();

        assertTrue(controller.handleActivityResult(started.requestCode, Activity.RESULT_OK, speechResult("   ")));

        assertEquals("Existing destination", inputController.getRawText());
        assertEquals(
                activity.getString(R.string.msg_speech_input_empty),
                ShadowToast.getTextOfLatestToast()
        );
        inputController.dispose();
    }

    @Test
    public void handleActivityResult_ignoresOtherRequestCodes() {
        MainActivitySpeechInputController controller = new MainActivitySpeechInputController(activity, scheduler);

        assertFalse(controller.handleActivityResult(9999, Activity.RESULT_OK, speechResult("Ignored")));
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
            @NonNull ShadowActivity.IntentForResult started,
            @NonNull String prompt
    ) {
        assertNotNull(started);
        assertEquals(RecognizerIntent.ACTION_RECOGNIZE_SPEECH, started.intent.getAction());
        assertEquals(
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                started.intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL)
        );
        assertEquals(prompt, started.intent.getStringExtra(RecognizerIntent.EXTRA_PROMPT));
        assertEquals(1, started.intent.getIntExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 0));
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
