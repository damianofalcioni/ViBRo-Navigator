package vibro.navigator.main;

import vibro.navigator.R;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.poi.ui.PoiInputController;

import java.util.ArrayList;

final class MainActivitySpeechInputController {

    static final long SPEECH_INPUT_LAUNCH_DELAY_MS = 100L;

    private static final int REQ_SPEECH_INPUT = 5001;
    private static final int MAX_SPEECH_RESULTS = 1;
    private static final String TAG = "MainSpeechInput";

    @NonNull
    private final Activity activity;
    @NonNull
    private final TaskScheduler scheduler;

    @Nullable
    private PoiInputController pendingController;

    MainActivitySpeechInputController(@NonNull Activity activity) {
        this(activity, AndroidTaskScheduler.main());
    }

    MainActivitySpeechInputController(
            @NonNull Activity activity,
            @NonNull TaskScheduler scheduler
    ) {
        this.activity = activity;
        this.scheduler = scheduler;
    }

    void openDestinationSpeechInput(@NonNull PoiInputController controller) {
        openSpeechInput(controller, activity.getString(R.string.prompt_speech_destination));
    }

    void openStopSpeechInput(@NonNull PoiInputController controller) {
        openSpeechInput(controller, activity.getString(R.string.prompt_speech_stop));
    }

    private void openSpeechInput(
            @NonNull PoiInputController controller,
            @NonNull String prompt
    ) {
        AppLogger.i(TAG, "Speech input requested");
        scheduler.postDelayed(
                () -> startSpeechInput(controller, prompt),
                SPEECH_INPUT_LAUNCH_DELAY_MS
        );
    }

    private void startSpeechInput(
            @NonNull PoiInputController controller,
            @NonNull String prompt
    ) {
        if (!canStartSpeechInput()) {
            AppLogger.w(TAG, "Speech input ignored because activity is finishing");
            return;
        }
        pendingController = controller;
        try {
            activity.startActivityForResult(createSpeechIntent(prompt), REQ_SPEECH_INPUT);
        } catch (ActivityNotFoundException e) {
            pendingController = null;
            AppLogger.w(TAG, "Speech recognizer activity unavailable", e);
            Toast.makeText(activity, R.string.msg_speech_input_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean canStartSpeechInput() {
        return !activity.isFinishing() && !activity.isDestroyed();
    }

    private static Intent createSpeechIntent(@NonNull String prompt) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, MAX_SPEECH_RESULTS);
        return intent;
    }

    boolean handleActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data
    ) {
        if (requestCode != REQ_SPEECH_INPUT) {
            return false;
        }
        PoiInputController controller = pendingController;
        pendingController = null;
        if (resultCode != Activity.RESULT_OK || controller == null) {
            AppLogger.i(TAG, "Speech input cancelled or missing target");
            return true;
        }
        applySpeechResult(controller, data);
        return true;
    }

    private void applySpeechResult(
            @NonNull PoiInputController controller,
            @Nullable Intent data
    ) {
        String spokenText = firstSpeechResult(data);
        if (spokenText == null) {
            AppLogger.w(TAG, "Speech input returned without recognized text");
            Toast.makeText(activity, R.string.msg_speech_input_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        controller.getEditText().requestFocus();
        controller.setText(spokenText);
        AppLogger.i(TAG, "Applied speech input text length=" + spokenText.length());
    }

    @Nullable
    private static String firstSpeechResult(@Nullable Intent data) {
        if (data == null) {
            return null;
        }
        ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (results == null || results.isEmpty()) {
            return null;
        }
        String first = results.get(0);
        if (first == null) {
            return null;
        }
        String trimmed = first.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
