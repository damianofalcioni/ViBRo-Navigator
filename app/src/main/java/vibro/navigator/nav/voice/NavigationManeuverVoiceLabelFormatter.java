package vibro.navigator.nav.voice;

import android.content.Context;
import android.speech.tts.Voice;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import vibro.navigator.R;

public final class NavigationManeuverVoiceLabelFormatter {
    private static final String FEMALE_TOKEN = "female";
    private static final String MALE_TOKEN = "male";

    private NavigationManeuverVoiceLabelFormatter() {
    }

    @NonNull
    public static String format(@NonNull Context context, @NonNull Voice voice) {
        String variantLabel = formatVariantLabel(context, voice.getName());
        Locale locale = voice.getLocale();
        if (locale == null) {
            return variantLabel;
        }
        return context.getString(
                R.string.format_maneuver_voice_option,
                locale.getDisplayName(),
                variantLabel
        );
    }

    @NonNull
    private static String formatVariantLabel(@NonNull Context context, @NonNull String voiceName) {
        NavigationManeuverVoiceNameParser.VoiceVariant variant =
                NavigationManeuverVoiceNameParser.extractVariant(voiceName);
        if (variant.token.isEmpty()) {
            return voiceName.trim();
        }
        if (variant.displayAsCode) {
            return context.getString(
                    R.string.format_maneuver_voice_variant_code,
                    formatVoiceCode(variant.token)
            );
        }
        return humanizeVariant(context, variant.token);
    }

    @NonNull
    private static String humanizeVariant(@NonNull Context context, @NonNull String token) {
        List<String> formattedWords = new ArrayList<>();
        for (String word : splitWords(token)) {
            formattedWords.add(formatWord(context, word));
        }
        return joinWords(formattedWords);
    }

    @NonNull
    private static List<String> splitWords(@NonNull String token) {
        String[] rawWords = token.replace('-', ' ').replace('_', ' ').trim().split("\\s+");
        List<String> words = new ArrayList<>();
        for (String rawWord : rawWords) {
            if (!rawWord.isEmpty()) {
                words.add(rawWord);
            }
        }
        return words;
    }

    @NonNull
    private static String formatWord(@NonNull Context context, @NonNull String word) {
        String lowerWord = word.toLowerCase(Locale.US);
        if (FEMALE_TOKEN.equals(lowerWord)) {
            return context.getString(R.string.label_maneuver_voice_gender_female);
        }
        if (MALE_TOKEN.equals(lowerWord)) {
            return context.getString(R.string.label_maneuver_voice_gender_male);
        }
        if (isNumeric(word)) {
            return word;
        }
        return Character.toUpperCase(lowerWord.charAt(0)) + lowerWord.substring(1);
    }

    private static boolean isNumeric(@NonNull String word) {
        for (int i = 0; i < word.length(); i++) {
            if (!Character.isDigit(word.charAt(i))) {
                return false;
            }
        }
        return !word.isEmpty();
    }

    @NonNull
    private static String formatVoiceCode(@NonNull String token) {
        return joinWords(splitWords(token)).toUpperCase(Locale.US);
    }

    @NonNull
    private static String joinWords(@NonNull List<String> words) {
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(word);
        }
        return result.toString();
    }
}
