package vibro.navigator.nav.voice;

import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;

import androidx.annotation.NonNull;

import java.util.Set;

public final class NavigationTextToSpeechVoiceAvailability {
    private NavigationTextToSpeechVoiceAvailability() {
    }

    public static boolean isOfflineVoiceAvailable(@NonNull Voice voice) {
        Set<String> features = voice.getFeatures();
        return !voice.isNetworkConnectionRequired()
                && (features == null || !features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED));
    }
}
