package vibro.navigator.about;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

import vibro.navigator.R;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppSettings;

final class AboutManeuverVoicePreview {
    private static final String TAG = "AboutVoicePreview";

    private AboutManeuverVoicePreview() {
    }

    static void speak(
            @NonNull Context context,
            @Nullable TextToSpeech tts,
            @NonNull String voiceName
    ) {
        if (tts == null) {
            AppLogger.w(TAG, "TextToSpeech preview requested before engine initialization");
            return;
        }
        if (!applyVoice(tts, voiceName)) {
            return;
        }
        int result = tts.speak(
                sampleText(context),
                TextToSpeech.QUEUE_FLUSH,
                new Bundle(),
                "maneuver-preview"
        );
        if (result == TextToSpeech.ERROR) {
            AppLogger.w(TAG, "TextToSpeech failed to speak preview");
        }
    }

    private static boolean applyVoice(@NonNull TextToSpeech tts, @NonNull String voiceName) {
        if (AppSettings.isSystemDefaultManeuverVoice(voiceName)) {
            return applySystemDefaultVoice(tts);
        }
        Voice voice = findVoice(tts, voiceName);
        if (voice == null) {
            AppLogger.w(TAG, "Configured TextToSpeech preview voice unavailable: " + voiceName);
            return false;
        }
        int result = tts.setVoice(voice);
        if (result == TextToSpeech.ERROR) {
            AppLogger.w(TAG, "TextToSpeech rejected preview voice: " + voiceName);
            return false;
        }
        return true;
    }

    private static boolean applySystemDefaultVoice(@NonNull TextToSpeech tts) {
        Voice voice = tts.getDefaultVoice();
        if (voice == null) {
            return true;
        }
        int result = tts.setVoice(voice);
        if (result == TextToSpeech.ERROR) {
            AppLogger.w(TAG, "TextToSpeech rejected default preview voice");
            return false;
        }
        return true;
    }

    @Nullable
    private static Voice findVoice(@NonNull TextToSpeech tts, @NonNull String voiceName) {
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

    @NonNull
    private static String sampleText(@NonNull Context context) {
        return context.getString(
                R.string.format_turn_speech,
                context.getString(R.string.format_time_speech_seconds, 20),
                context.getString(R.string.direction_turn_left)
        );
    }
}
