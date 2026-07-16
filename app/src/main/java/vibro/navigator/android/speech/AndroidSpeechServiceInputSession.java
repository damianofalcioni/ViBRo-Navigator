package vibro.navigator.android.speech;

import vibro.navigator.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.speech.SpeechInputLauncher;

import java.util.ArrayList;

final class AndroidSpeechServiceInputSession {

    static final long SERVICE_READY_TIMEOUT_MS = 6_000L;

    private static final String TAG = "SpeechServiceInput";

    @NonNull
    private final Activity activity;
    @NonNull
    private final TaskScheduler scheduler;

    @Nullable
    private SpeechRecognizer speechRecognizer;
    @Nullable
    private AlertDialog listeningDialog;
    @Nullable
    private SpeechInputLauncher.Callback activeCallback;
    private boolean programmaticDialogClose;
    private boolean serviceReady;
    private int sessionId;

    AndroidSpeechServiceInputSession(@NonNull Activity activity) {
        this(activity, AndroidTaskScheduler.main());
    }

    AndroidSpeechServiceInputSession(
            @NonNull Activity activity,
            @NonNull TaskScheduler scheduler
    ) {
        this.activity = activity;
        this.scheduler = scheduler;
    }

    void start(
            @NonNull Intent recognizerIntent,
            @NonNull SpeechInputLauncher.Callback callback,
            @Nullable ComponentName serviceComponent
    ) {
        dispose();
        activeCallback = callback;
        serviceReady = false;
        int startedSessionId = ++sessionId;
        try {
            speechRecognizer = createSpeechRecognizer(serviceComponent);
            speechRecognizer.setRecognitionListener(createRecognitionListener());
            showListeningDialog(recognizerIntent);
            speechRecognizer.startListening(recognizerIntent);
            scheduleReadyTimeout(startedSessionId);
            AppLogger.i(TAG, "Started speech recognizer service fallback service="
                    + describeServiceComponent(serviceComponent));
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Speech recognizer service missing microphone permission", e);
            completeFailure(R.string.msg_speech_permission_required);
        } catch (RuntimeException e) {
            AppLogger.w(TAG, "Speech recognizer service failed to start", e);
            completeFailure(R.string.msg_speech_input_unavailable);
        }
    }

    void dispose() {
        activeCallback = null;
        serviceReady = false;
        sessionId++;
        dismissListeningDialog();
        destroySpeechRecognizer();
    }

    private RecognitionListener createRecognitionListener() {
        return new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                serviceReady = true;
                AppLogger.i(TAG, "Speech recognizer service ready");
            }

            @Override
            public void onBeginningOfSpeech() {
                AppLogger.i(TAG, "Speech recognizer service heard speech");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
                AppLogger.i(TAG, "Speech recognizer service end of speech");
            }

            @Override
            public void onError(int error) {
                AppLogger.w(TAG, "Speech recognizer service error=" + error);
                completeFailure(messageResIdForError(error));
            }

            @Override
            public void onResults(Bundle results) {
                completeResult(bundleResultsToIntent(results));
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        };
    }

    private void scheduleReadyTimeout(int startedSessionId) {
        scheduler.postDelayed(
                () -> failIfServiceDidNotBecomeReady(startedSessionId),
                SERVICE_READY_TIMEOUT_MS
        );
    }

    private void failIfServiceDidNotBecomeReady(int startedSessionId) {
        if (startedSessionId != sessionId || serviceReady || activeCallback == null) {
            return;
        }
        AppLogger.w(TAG, "Speech recognizer service did not become ready");
        completeFailure(R.string.msg_speech_input_unresponsive);
    }

    @NonNull
    private SpeechRecognizer createSpeechRecognizer(@Nullable ComponentName serviceComponent) {
        if (serviceComponent == null) {
            return SpeechRecognizer.createSpeechRecognizer(activity);
        }
        return SpeechRecognizer.createSpeechRecognizer(activity, serviceComponent);
    }

    private void showListeningDialog(@NonNull Intent recognizerIntent) {
        String prompt = recognizerIntent.getStringExtra(RecognizerIntent.EXTRA_PROMPT);
        listeningDialog = new AlertDialog.Builder(activity)
                .setTitle(prompt)
                .setMessage(R.string.msg_speech_input_listening)
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> completeCancelled())
                .create();
        listeningDialog.setOnCancelListener(dialog -> {
            if (!programmaticDialogClose) {
                completeCancelled();
            }
        });
        listeningDialog.show();
    }

    private void completeResult(@NonNull Intent data) {
        SpeechInputLauncher.Callback callback = activeCallback;
        finishServiceRecognition();
        if (callback != null) {
            callback.onSpeechInputResult(data);
        }
    }

    private void completeFailure(@StringRes int messageResId) {
        SpeechInputLauncher.Callback callback = activeCallback;
        finishServiceRecognition();
        if (callback != null) {
            callback.onSpeechInputMessage(messageResId);
        }
    }

    private void completeCancelled() {
        SpeechInputLauncher.Callback callback = activeCallback;
        finishServiceRecognition();
        if (callback != null) {
            AppLogger.i(TAG, "Speech recognizer service cancelled");
            callback.onSpeechInputCancelled();
        }
    }

    private void finishServiceRecognition() {
        activeCallback = null;
        serviceReady = false;
        sessionId++;
        dismissListeningDialog();
        destroySpeechRecognizer();
    }

    private void dismissListeningDialog() {
        if (listeningDialog == null) {
            return;
        }
        AlertDialog dialog = listeningDialog;
        listeningDialog = null;
        programmaticDialogClose = true;
        try {
            dialog.dismiss();
        } finally {
            programmaticDialogClose = false;
        }
    }

    private void destroySpeechRecognizer() {
        if (speechRecognizer == null) {
            return;
        }
        SpeechRecognizer recognizer = speechRecognizer;
        speechRecognizer = null;
        recognizer.cancel();
        recognizer.destroy();
    }

    @StringRes
    private static int messageResIdForError(int error) {
        if (isEmptyResultError(error)) {
            return R.string.msg_speech_input_empty;
        }
        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            return R.string.msg_speech_permission_required;
        }
        return R.string.msg_speech_input_failed;
    }

    private static boolean isEmptyResultError(int error) {
        return error == SpeechRecognizer.ERROR_NO_MATCH
                || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT;
    }

    @NonNull
    private static String describeServiceComponent(@Nullable ComponentName serviceComponent) {
        return serviceComponent == null ? "default" : serviceComponent.flattenToShortString();
    }

    @NonNull
    private static Intent bundleResultsToIntent(@NonNull Bundle results) {
        Intent data = new Intent();
        ArrayList<String> recognized = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (recognized != null) {
            data.putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, recognized);
        }
        return data;
    }
}
