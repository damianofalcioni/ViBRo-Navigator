package vibro.navigator.about;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import vibro.navigator.R;
import vibro.navigator.settings.AppSpeechRecognitionSettings;

final class AboutSpeechRecognitionLanguageOptions {
    private static final String[] COMMON_LANGUAGE_TAGS = {
            "en",
            "en-US",
            "en-GB",
            "de",
            "de-AT",
            "de-DE",
            "fr",
            "fr-FR",
            "it",
            "it-IT",
            "es",
            "es-ES",
            "pt",
            "pt-PT",
            "pt-BR",
            "nl",
            "nl-NL",
            "pl",
            "pl-PL",
            "cs",
            "cs-CZ",
            "sk",
            "sk-SK",
            "hu",
            "hu-HU",
            "ro",
            "ro-RO",
            "sl",
            "sl-SI",
            "hr",
            "hr-HR",
            "da",
            "da-DK",
            "sv",
            "sv-SE",
            "fi",
            "fi-FI",
            "nb",
            "nb-NO",
            "tr",
            "tr-TR",
            "el",
            "el-GR",
            "he",
            "he-IL",
            "ru",
            "ru-RU",
            "uk",
            "uk-UA",
            "ar",
            "ar-SA",
            "hi",
            "hi-IN",
            "id",
            "id-ID",
            "ja",
            "ja-JP",
            "ko",
            "ko-KR",
            "th",
            "th-TH",
            "vi",
            "vi-VN",
            "zh",
            "zh-CN",
            "zh-TW"
    };

    private AboutSpeechRecognitionLanguageOptions() {
    }

    @NonNull
    static List<AboutSpeechRecognitionLanguageOption> defaultOptions(@NonNull Context context) {
        List<AboutSpeechRecognitionLanguageOption> options = new ArrayList<>();
        options.add(new AboutSpeechRecognitionLanguageOption(
                AppSpeechRecognitionSettings.LANGUAGE_SYSTEM_DEFAULT,
                context.getString(R.string.label_speech_recognition_language_system_default)
        ));
        Set<String> languageTags = new LinkedHashSet<>();
        addLanguageTag(languageTags, Locale.getDefault().toLanguageTag());
        for (String languageTag : COMMON_LANGUAGE_TAGS) {
            addLanguageTag(languageTags, languageTag);
        }
        for (String languageTag : languageTags) {
            options.add(new AboutSpeechRecognitionLanguageOption(
                    languageTag,
                    languageLabel(languageTag)
            ));
        }
        return options;
    }

    private static void addLanguageTag(
            @NonNull Set<String> languageTags,
            @NonNull String languageTag
    ) {
        String normalized = normalizeLanguageTag(languageTag);
        if (!normalized.isEmpty()) {
            languageTags.add(normalized);
        }
    }

    @NonNull
    private static String normalizeLanguageTag(@NonNull String languageTag) {
        Locale locale = Locale.forLanguageTag(languageTag.trim().replace('_', '-'));
        if (locale.getLanguage().isEmpty()) {
            return "";
        }
        return locale.toLanguageTag();
    }

    @NonNull
    private static String languageLabel(@NonNull String languageTag) {
        Locale locale = Locale.forLanguageTag(languageTag);
        String label = locale.getDisplayName(locale);
        if (label == null || label.trim().isEmpty()) {
            return languageTag;
        }
        return label.substring(0, 1).toUpperCase(locale) + label.substring(1);
    }
}
