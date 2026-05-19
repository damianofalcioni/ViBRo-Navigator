package vibro.navigator.nav.voice;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.settings.AppSettings;

public final class NavigationManeuverSpeaker {
    private static final String TAG = "NavManeuverSpeaker";

    @NonNull
    private final Context appContext;
    @Nullable
    private TextToSpeech tts;
    @Nullable
    private String pendingUtterance;
    @Nullable
    private String appliedVoiceName;
    private boolean initCallbackReceived;
    private boolean ready;
    private int initStatus = TextToSpeech.ERROR;
    private int utteranceSequence;

    public NavigationManeuverSpeaker(@NonNull Context context) {
        appContext = context.getApplicationContext();
        prepareEngineIfEnabled();
    }

    public void speakTurn(@NonNull VoiceHint hint, double timeSeconds) {
        if (!AppSettings.isManeuverSpeechEnabled(appContext)) {
            pendingUtterance = null;
            stop();
            releaseEngine();
            return;
        }
        String utterance = NavigationManeuverSpeechFormatter.formatTurnSpeech(appContext, hint, timeSeconds);
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
        ready = true;
        speakPendingIfReady();
    }

    private void speakPendingIfReady() {
        if (!ready || tts == null || pendingUtterance == null) {
            return;
        }
        if (!applyConfiguredVoice()) {
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
            AppLogger.w(TAG, "TextToSpeech failed to speak maneuver utterance");
        }
    }

    private boolean applyConfiguredVoice() {
        if (tts == null) {
            return false;
        }
        String voiceName = AppSettings.getManeuverVoiceName(appContext);
        if (AppSettings.isSystemDefaultManeuverVoice(voiceName)) {
            return applySystemDefaultVoice(voiceName);
        }
        if (voiceName.equals(appliedVoiceName)) {
            return true;
        }
        Voice voice = findVoice(voiceName);
        if (voice == null || !NavigationTextToSpeechVoiceAvailability.isOfflineVoiceAvailable(voice)) {
            AppLogger.w(TAG, "Configured TextToSpeech voice unavailable: " + voiceName);
            pendingUtterance = null;
            appliedVoiceName = voiceName;
            return false;
        }
        int result = tts.setVoice(voice);
        if (result == TextToSpeech.ERROR) {
            AppLogger.w(TAG, "TextToSpeech rejected configured voice: " + voiceName);
            pendingUtterance = null;
            return false;
        }
        appliedVoiceName = voiceName;
        return true;
    }

    private boolean applySystemDefaultVoice(@NonNull String voiceName) {
        if (appliedVoiceName == null || AppSettings.isSystemDefaultManeuverVoice(appliedVoiceName)) {
            appliedVoiceName = voiceName;
            return true;
        }
        releaseEngine();
        prepareEngineIfEnabled();
        return false;
    }

    @Nullable
    private Voice findVoice(@NonNull String voiceName) {
        if (tts == null) {
            return null;
        }
        Set<Voice> voices = tts.getVoices();
        if (voices == null) {
            return null;
        }
        for (Voice voice : voices) {
            if (voiceName.equals(voice.getName())) {
                return voice;
            }
        }
        return null;
    }

    private void releaseEngine() {
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
        ready = false;
        initCallbackReceived = false;
        initStatus = TextToSpeech.ERROR;
        appliedVoiceName = null;
    }
}
