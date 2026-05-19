package vibro.navigator.nav.voice;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppSettings;

final class NavigationManeuverVoiceApplier {
    private static final String TAG = "NavManeuverVoice";

    @NonNull
    private final Context appContext;
    @Nullable
    private String appliedVoiceName;

    NavigationManeuverVoiceApplier(@NonNull Context context) {
        appContext = context.getApplicationContext();
    }

    boolean applyConfiguredVoice(
            @Nullable TextToSpeech tts,
            @NonNull Runnable restartEngine,
            @NonNull Runnable clearPendingUtterance
    ) {
        if (tts == null) {
            return false;
        }
        String voiceName = AppSettings.getManeuverVoiceName(appContext);
        if (AppSettings.isSystemDefaultManeuverVoice(voiceName)) {
            return applySystemDefaultVoice(voiceName, restartEngine);
        }
        if (voiceName.equals(appliedVoiceName)) {
            return true;
        }
        Voice voice = findVoice(tts, voiceName);
        if (voice == null || !NavigationTextToSpeechVoiceAvailability.isOfflineVoiceAvailable(voice)) {
            AppLogger.w(TAG, "Configured TextToSpeech voice unavailable: " + voiceName);
            clearPendingUtterance.run();
            appliedVoiceName = voiceName;
            return false;
        }
        return setVoice(tts, voice, voiceName, clearPendingUtterance);
    }

    void reset() {
        appliedVoiceName = null;
    }

    private boolean setVoice(
            @NonNull TextToSpeech tts,
            @NonNull Voice voice,
            @NonNull String voiceName,
            @NonNull Runnable clearPendingUtterance
    ) {
        int result = tts.setVoice(voice);
        if (result == TextToSpeech.ERROR) {
            AppLogger.w(TAG, "TextToSpeech rejected configured voice: " + voiceName);
            clearPendingUtterance.run();
            return false;
        }
        appliedVoiceName = voiceName;
        return true;
    }

    private boolean applySystemDefaultVoice(
            @NonNull String voiceName,
            @NonNull Runnable restartEngine
    ) {
        if (appliedVoiceName == null || AppSettings.isSystemDefaultManeuverVoice(appliedVoiceName)) {
            appliedVoiceName = voiceName;
            return true;
        }
        restartEngine.run();
        return false;
    }

    @Nullable
    private Voice findVoice(@NonNull TextToSpeech tts, @NonNull String voiceName) {
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
}
