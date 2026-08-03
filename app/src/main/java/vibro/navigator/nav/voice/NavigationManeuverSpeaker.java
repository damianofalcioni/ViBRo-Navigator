package vibro.navigator.nav.voice;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.guidance.NavigationRerouteNotice;
import vibro.navigator.nav.orientation.StationaryOrientationAdvisor;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.settings.AppSettings;

public final class NavigationManeuverSpeaker implements NavigationAlertSpeaker {
    private static final String TAG = "NavManeuverSpeaker";

    @NonNull
    private final Context appContext;
    @NonNull
    private final NavigationSpeechAudioFocus audioFocus;
    @NonNull
    private final NavigationManeuverVoiceApplier voiceApplier;
    @Nullable
    private TextToSpeech tts;
    @Nullable
    private String pendingUtterance;
    private boolean initCallbackReceived;
    private boolean ready;
    private int initStatus = TextToSpeech.ERROR;
    private int utteranceSequence;

    public NavigationManeuverSpeaker(@NonNull Context context) {
        appContext = context.getApplicationContext();
        audioFocus = new NavigationSpeechAudioFocus(appContext);
        voiceApplier = new NavigationManeuverVoiceApplier(appContext);
        prepareEngineIfEnabled();
    }

    @Override
    public void speakTurn(@NonNull VoiceHint hint, double timeSeconds) {
        speakUtterance(NavigationManeuverSpeechFormatter.formatTurnSpeech(appContext, hint, timeSeconds));
    }

    @Override
    public void speakStationaryOrientation(@NonNull StationaryOrientationAdvisor.Decision decision) {
        speakUtterance(NavigationManeuverSpeechFormatter.formatStationaryOrientationSpeech(appContext, decision));
    }

    @Override
    public void speakOffRoute(@NonNull NavigationRerouteNotice rerouteNotice) {
        speakUtterance(NavigationManeuverSpeechFormatter.formatOffRouteSpeech(appContext, rerouteNotice));
    }

    @Override
    public void speakWrongDirection() {
        speakUtterance(NavigationManeuverSpeechFormatter.formatWrongDirectionSpeech(appContext));
    }

    private void speakUtterance(@NonNull String utterance) {
        if (!AppSettings.isManeuverSpeechEnabled(appContext)) {
            pendingUtterance = null;
            stop();
            releaseEngine();
            return;
        }
        if (utterance.isEmpty()) {
            return;
        }
        pendingUtterance = utterance;
        prepareEngineIfEnabled();
        speakPendingIfReady();
    }

    public void stop() {
        pendingUtterance = null;
        if (tts != null) {
            tts.stop();
        }
        audioFocus.abandonFocus();
    }

    public void shutdown() {
        stop();
        releaseEngine();
    }

    private void prepareEngineIfEnabled() {
        if (!AppSettings.isManeuverSpeechEnabled(appContext) || tts != null) {
            return;
        }
        initCallbackReceived = false;
        ready = false;
        initStatus = TextToSpeech.ERROR;
        try {
            TextToSpeech engine = new TextToSpeech(appContext, this::onTextToSpeechInit);
            tts = engine;
            handleTextToSpeechInitIfReady();
        } catch (RuntimeException e) {
            AppLogger.w(TAG, "Failed to initialize TextToSpeech", e);
        }
    }

    private void onTextToSpeechInit(int status) {
        initStatus = status;
        initCallbackReceived = true;
        handleTextToSpeechInitIfReady();
    }

    private void handleTextToSpeechInitIfReady() {
        if (!initCallbackReceived || tts == null) {
            return;
        }
        if (initStatus != TextToSpeech.SUCCESS) {
            AppLogger.w(TAG, "TextToSpeech unavailable status=" + initStatus);
            releaseEngine();
            return;
        }
        configureEngineAudio();
        ready = true;
        speakPendingIfReady();
    }

    private void speakPendingIfReady() {
        if (!ready || tts == null || pendingUtterance == null) {
            return;
        }
        if (!voiceApplier.applyConfiguredVoice(tts, this::restartEngine, this::clearPendingUtterance)) {
            return;
        }
        if (!audioFocus.requestTransientMayDuckFocus()) {
            pendingUtterance = null;
            AppLogger.w(TAG, "Audio focus unavailable for maneuver speech");
            return;
        }
        String utterance = pendingUtterance;
        pendingUtterance = null;
        int result = tts.speak(
                utterance,
                TextToSpeech.QUEUE_FLUSH,
                new Bundle(),
                "maneuver-" + (++utteranceSequence)
        );
        if (result == TextToSpeech.ERROR) {
            audioFocus.abandonFocus();
            AppLogger.w(TAG, "TextToSpeech failed to speak maneuver utterance");
        }
    }

    private void configureEngineAudio() {
        if (tts == null) {
            return;
        }
        int attributesResult = tts.setAudioAttributes(audioFocus.audioAttributes());
        if (attributesResult == TextToSpeech.ERROR) {
            AppLogger.w(TAG, "TextToSpeech rejected navigation audio attributes");
        }
        int listenerResult = tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
            }

            @Override
            public void onDone(String utteranceId) {
                audioFocus.abandonFocus();
            }

            @Override
            @SuppressWarnings("deprecation")
            public void onError(String utteranceId) {
                audioFocus.abandonFocus();
            }

            @Override
            public void onError(String utteranceId, int errorCode) {
                audioFocus.abandonFocus();
            }

            @Override
            public void onStop(String utteranceId, boolean interrupted) {
                audioFocus.abandonFocus();
            }
        });
        if (listenerResult == TextToSpeech.ERROR) {
            AppLogger.w(TAG, "TextToSpeech rejected maneuver progress listener");
        }
    }

    private void restartEngine() {
        releaseEngine();
        prepareEngineIfEnabled();
    }

    private void clearPendingUtterance() {
        pendingUtterance = null;
    }

    private void releaseEngine() {
        audioFocus.abandonFocus();
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
        ready = false;
        initCallbackReceived = false;
        initStatus = TextToSpeech.ERROR;
        voiceApplier.reset();
    }
}
