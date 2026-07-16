package vibro.navigator.main;

import vibro.navigator.R;

import android.app.Activity;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.android.speech.AndroidSpeechInputLauncher;
import vibro.navigator.android.speech.AndroidSpeechRecognitionSupport;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.poi.ui.PoiInputController;
import vibro.navigator.settings.AppSpeechRecognitionSettings;
import vibro.navigator.speech.SpeechInputLauncher;

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
    @NonNull
    private final SpeechInputLauncher speechInputLauncher;

    @Nullable
    private PoiInputController pendingController;
    private boolean waitingForActivityResult;

    MainActivitySpeechInputController(@NonNull Activity activity) {
        this(activity, AndroidTaskScheduler.main());
    }

    MainActivitySpeechInputController(
            @NonNull Activity activity,
            @NonNull TaskScheduler scheduler
    ) {
        this(activity, scheduler, new AndroidSpeechInputLauncher(activity));
    }

    MainActivitySpeechInputController(
            @NonNull Activity activity,
            @NonNull TaskScheduler scheduler,
            @NonNull SpeechInputLauncher speechInputLauncher
    ) {
        this.activity = activity;
        this.scheduler = scheduler;
        this.speechInputLauncher = speechInputLauncher;
    }

    void openDestinationSpeechInput(@NonNull PoiInputController controller) {
        openSpeechInput(controller, activity.getString(R.string.prompt_speech_destination));
    }

    void openStopSpeechInput(@NonNull PoiInputController controller) {
        openSpeechInput(controller, activity.getString(R.string.prompt_speech_stop));
    }

    boolean isSpeechInputVisible() {
        return AppSpeechRecognitionSettings.isEnabled(activity)
                && AndroidSpeechRecognitionSupport.isAvailable(activity);
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
        waitingForActivityResult = false;
        SpeechInputLauncher.StartMode startMode = speechInputLauncher.start(
                createSpeechIntent(prompt, AppSpeechRecognitionSettings.getLanguageTag(activity)),
                REQ_SPEECH_INPUT,
                speechCallback()
        );
        if (startMode == SpeechInputLauncher.StartMode.ACTIVITY) {
            waitingForActivityResult = true;
        } else if (startMode == SpeechInputLauncher.StartMode.UNAVAILABLE) {
            clearPendingSpeechInput();
            Toast.makeText(activity, R.string.msg_speech_input_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean canStartSpeechInput() {
        return !activity.isFinishing() && !activity.isDestroyed();
    }

    private static Intent createSpeechIntent(
            @NonNull String prompt,
            @NonNull String languageTag
    ) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, MAX_SPEECH_RESULTS);
        if (!languageTag.isEmpty()) {
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag);
        }
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
        if (!waitingForActivityResult) {
            AppLogger.i(TAG, "Ignoring speech activity result without pending activity launch");
            return true;
        }
        waitingForActivityResult = false;
        PoiInputController controller = pendingController;
        pendingController = null;
        if (resultCode != Activity.RESULT_OK || controller == null) {
            AppLogger.i(TAG, "Speech input cancelled or missing target");
            return true;
        }
        applySpeechResult(controller, data);
        return true;
    }

    boolean handleRequestPermissionsResult(int requestCode, @NonNull int[] grantResults) {
        return speechInputLauncher.handleRequestPermissionsResult(requestCode, grantResults);
    }

    void dispose() {
        clearPendingSpeechInput();
        speechInputLauncher.dispose();
    }

    @NonNull
    private SpeechInputLauncher.Callback speechCallback() {
        return new SpeechInputLauncher.Callback() {
            @Override
            public void onSpeechInputResult(@NonNull Intent data) {
                PoiInputController controller = pendingController;
                clearPendingSpeechInput();
                if (controller == null) {
                    AppLogger.w(TAG, "Speech input returned without a target field");
                    return;
                }
                applySpeechResult(controller, data);
            }

            @Override
            public void onSpeechInputMessage(@StringRes int messageResId) {
                clearPendingSpeechInput();
                Toast.makeText(activity, messageResId, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSpeechInputCancelled() {
                clearPendingSpeechInput();
                AppLogger.i(TAG, "Speech input cancelled or missing target");
            }
        };
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

    private void clearPendingSpeechInput() {
        pendingController = null;
        waitingForActivityResult = false;
    }
}
