package vibro.navigator.about;

import androidx.annotation.NonNull;

final class AboutSpeechRecognitionLanguageOption {
    @NonNull
    final String languageTag;
    @NonNull
    private final String label;

    AboutSpeechRecognitionLanguageOption(
            @NonNull String languageTag,
            @NonNull String label
    ) {
        this.languageTag = languageTag;
        this.label = label;
    }

    @NonNull
    @Override
    public String toString() {
        return label;
    }
}
