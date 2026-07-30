package vibro.navigator.nav.voice;

import android.speech.tts.Voice;

import androidx.annotation.NonNull;

import java.util.Set;

public final class NavigationTextToSpeechVoiceAvailability {
    private static final String KEY_FEATURE_NOT_INSTALLED = "notInstalled";

    private NavigationTextToSpeechVoiceAvailability() {
    }

    public static boolean isOfflineVoiceAvailable(@NonNull Voice voice) {
        return isOfflineVoiceAvailable(NavigationTextToSpeechVoiceDescriptor.from(voice));
    }

    static boolean isOfflineVoiceAvailable(@NonNull NavigationTextToSpeechVoiceDescriptor voice) {
        Set<String> features = voice.features();
        return !voice.isNetworkConnectionRequired()
                && (features == null || !features.contains(KEY_FEATURE_NOT_INSTALLED));
    }
}
