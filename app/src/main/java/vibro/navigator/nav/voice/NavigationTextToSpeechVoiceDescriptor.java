package vibro.navigator.nav.voice;

import android.speech.tts.Voice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

final class NavigationTextToSpeechVoiceDescriptor {
    @NonNull
    private final String name;
    @Nullable
    private final Locale locale;
    private final boolean networkConnectionRequired;
    @Nullable
    private final Set<String> features;

    NavigationTextToSpeechVoiceDescriptor(
            @NonNull String name,
            @Nullable Locale locale,
            boolean networkConnectionRequired,
            @Nullable Set<String> features
    ) {
        this.name = name;
        this.locale = locale;
        this.networkConnectionRequired = networkConnectionRequired;
        this.features = copyFeatures(features);
    }

    @NonNull
    static NavigationTextToSpeechVoiceDescriptor from(@NonNull Voice voice) {
        return new NavigationTextToSpeechVoiceDescriptor(
                voice.getName(),
                voice.getLocale(),
                voice.isNetworkConnectionRequired(),
                voice.getFeatures()
        );
    }

    @NonNull
    String name() {
        return name;
    }

    @Nullable
    Locale locale() {
        return locale;
    }

    boolean isNetworkConnectionRequired() {
        return networkConnectionRequired;
    }

    @Nullable
    Set<String> features() {
        return features;
    }

    @Nullable
    private static Set<String> copyFeatures(@Nullable Set<String> features) {
        return features == null ? null : Collections.unmodifiableSet(new HashSet<>(features));
    }
}
