package vibro.navigator.android.speech;

import vibro.navigator.R;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.SpeechRecognizer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.speech.SpeechInputLauncher;

public final class AndroidSpeechInputLauncher implements SpeechInputLauncher {

    private static final int REQ_RECORD_AUDIO = 5002;
    private static final String TAG = "AndroidSpeechInput";

    @NonNull
    private final Activity activity;
    @NonNull
    private final AndroidSpeechRecognitionAvailability availability;

    @Nullable
    private AndroidSpeechServiceInputSession serviceSession;
    @Nullable
    private Intent pendingServiceIntent;
    @Nullable
    private Callback pendingServiceCallback;

    public AndroidSpeechInputLauncher(@NonNull Activity activity) {
        this(activity, new AndroidSpeechRecognitionAvailability(activity.getPackageManager()));
    }

    AndroidSpeechInputLauncher(
            @NonNull Activity activity,
            @NonNull AndroidSpeechRecognitionAvailability availability
    ) {
        this.activity = activity;
        this.availability = availability;
    }

    @NonNull
    @Override
    public StartMode start(
            @NonNull Intent recognizerIntent,
            int requestCode,
            @NonNull Callback callback
    ) {
        if (startRecognizerActivity(recognizerIntent, requestCode)) {
            return StartMode.ACTIVITY;
        }
        return startServiceFallback(recognizerIntent, callback);
    }

    @Override
    public boolean handleRequestPermissionsResult(int requestCode, @NonNull int[] grantResults) {
        if (requestCode != REQ_RECORD_AUDIO) {
            return false;
        }
        Callback callback = pendingServiceCallback;
        Intent intent = pendingServiceIntent;
        clearPendingServiceStart();
        if (callback == null || intent == null) {
            AppLogger.w(TAG, "Speech microphone permission result without pending recognition");
            return true;
        }
        if (!isPermissionGranted(grantResults)) {
            AppLogger.w(TAG, "Speech microphone permission denied");
            callback.onSpeechInputMessage(R.string.msg_speech_permission_required);
            return true;
        }
        StartMode startMode = startServiceFallback(intent, callback);
        if (startMode == StartMode.UNAVAILABLE) {
            callback.onSpeechInputMessage(R.string.msg_speech_input_unavailable);
        }
        return true;
    }

    @Override
    public void dispose() {
        clearPendingServiceStart();
        if (serviceSession != null) {
            serviceSession.dispose();
            serviceSession = null;
        }
    }

    private boolean startRecognizerActivity(@NonNull Intent recognizerIntent, int requestCode) {
        try {
            activity.startActivityForResult(recognizerIntent, requestCode);
            AppLogger.i(TAG, "Started speech recognizer activity");
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            AppLogger.w(
                    TAG,
                    "Speech recognizer activity unavailable, trying service fallback "
                            + availability.describe(),
                    e
            );
            return false;
        }
    }

    @NonNull
    private StartMode startServiceFallback(
            @NonNull Intent recognizerIntent,
            @NonNull Callback callback
    ) {
        ComponentName serviceComponent = availability.firstRecognitionService();
        if (serviceComponent == null && !SpeechRecognizer.isRecognitionAvailable(activity)) {
            AppLogger.w(TAG, "Speech recognizer service unavailable " + availability.describe());
            return StartMode.UNAVAILABLE;
        }
        if (!hasRecordAudioPermission()) {
            requestMicrophonePermission(recognizerIntent, callback);
            return StartMode.PENDING_PERMISSION;
        }
        startServiceRecognition(recognizerIntent, callback, serviceComponent);
        return StartMode.DIRECT;
    }

    private boolean hasRecordAudioPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestMicrophonePermission(
            @NonNull Intent recognizerIntent,
            @NonNull Callback callback
    ) {
        pendingServiceIntent = new Intent(recognizerIntent);
        pendingServiceCallback = callback;
        AppLogger.i(TAG, "Requesting speech microphone permission");
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQ_RECORD_AUDIO
        );
    }

    private void startServiceRecognition(
            @NonNull Intent recognizerIntent,
            @NonNull Callback callback,
            @Nullable ComponentName serviceComponent
    ) {
        if (serviceSession == null) {
            serviceSession = new AndroidSpeechServiceInputSession(activity);
        }
        serviceSession.start(recognizerIntent, callback, serviceComponent);
    }

    private void clearPendingServiceStart() {
        pendingServiceIntent = null;
        pendingServiceCallback = null;
    }

    private static boolean isPermissionGranted(@NonNull int[] grantResults) {
        return grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }
}
