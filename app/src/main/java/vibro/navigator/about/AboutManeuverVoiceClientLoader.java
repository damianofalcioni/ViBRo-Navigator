package vibro.navigator.about;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.voice.NavigationTextToSpeechSettingsClient;
import vibro.navigator.nav.voice.NavigationVoiceOption;

final class AboutManeuverVoiceClientLoader {
    interface VoiceOptionsCallback {
        void onLoaded(@NonNull List<NavigationVoiceOption> options);
    }

    private static final String TAG = "AboutManeuverVoice";
    static final long INIT_DELAY_MS = 50L;

    @NonNull
    private final Activity activity;
    @NonNull
    private final TaskScheduler scheduler;
    @NonNull
    private final VoiceOptionsCallback callback;
    @NonNull
    private final Runnable deferredInit = this::initializeIfNeeded;

    @Nullable
    private NavigationTextToSpeechSettingsClient voiceClient;
    @Nullable
    private String pendingPreviewVoiceName;
    private boolean dialogOpen;
    private boolean initScheduled;
    private boolean shutdown;

    AboutManeuverVoiceClientLoader(
            @NonNull Activity activity,
            @NonNull TaskScheduler scheduler,
            @NonNull VoiceOptionsCallback callback
    ) {
        this.activity = activity;
        this.scheduler = scheduler;
        this.callback = callback;
    }

    void onDialogShown() {
        dialogOpen = true;
        scheduleInitialization();
    }

    void onDialogDismissed() {
        dialogOpen = false;
        pendingPreviewVoiceName = null;
    }

    void requestPreview(@NonNull String voiceName) {
        if (voiceClient != null) {
            voiceClient.speakPreview(voiceName);
            return;
        }
        pendingPreviewVoiceName = voiceName;
        scheduleInitialization();
    }

    void shutdown() {
        shutdown = true;
        dialogOpen = false;
        pendingPreviewVoiceName = null;
        initScheduled = false;
        scheduler.removeCallbacks(deferredInit);
        if (voiceClient != null) {
            voiceClient.shutdown();
            voiceClient = null;
        }
    }

    private void scheduleInitialization() {
        if (voiceClient != null || initScheduled) {
            return;
        }
        initScheduled = true;
        scheduler.postDelayed(deferredInit, INIT_DELAY_MS);
    }

    private void initializeIfNeeded() {
        initScheduled = false;
        if (voiceClient != null || shutdown || !isClientNeeded()) {
            return;
        }
        initialize();
    }

    private boolean isClientNeeded() {
        return dialogOpen || pendingPreviewVoiceName != null;
    }

    private void initialize() {
        try {
            NavigationTextToSpeechSettingsClient client = new NavigationTextToSpeechSettingsClient(
                    activity.getApplicationContext(),
                    this::onAvailableVoicesLoaded
            );
            voiceClient = client;
            client.initialize();
        } catch (RuntimeException e) {
            pendingPreviewVoiceName = null;
            AppLogger.w(TAG, "Failed to initialize maneuver voice settings client", e);
        }
    }

    private void onAvailableVoicesLoaded(@NonNull List<NavigationVoiceOption> options) {
        activity.runOnUiThread(() -> {
            if (shutdown) {
                return;
            }
            callback.onLoaded(options);
            speakPendingPreview();
        });
    }

    private void speakPendingPreview() {
        if (pendingPreviewVoiceName == null || voiceClient == null) {
            return;
        }
        String voiceName = pendingPreviewVoiceName;
        pendingPreviewVoiceName = null;
        voiceClient.speakPreview(voiceName);
    }
}
