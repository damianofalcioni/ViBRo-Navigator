package vibro.navigator.about;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

final class AboutManeuverVoiceNameParser {
    private static final String HASH_SEPARATOR = "#";
    private static final String GOOGLE_EXTENSION_MARKER = "-x-";
    private static final String LOCAL_SUFFIX = "-local";
    private static final String NETWORK_SUFFIX = "-network";
    private static final String FEMALE_TOKEN = "female";
    private static final String MALE_TOKEN = "male";

    private AboutManeuverVoiceNameParser() {
    }

    @NonNull
    static VoiceVariant extractVariant(@NonNull String voiceName) {
        String normalized = stripSourceSuffix(voiceName.trim());
        String hashToken = tokenAfterHash(normalized);
        if (!hashToken.equals(normalized)) {
            return new VoiceVariant(hashToken, false);
        }
        String extensionToken = tokenAfterGoogleExtension(normalized);
        if (!extensionToken.equals(normalized)) {
            return new VoiceVariant(extensionToken, !hasReadableVoiceWord(extensionToken));
        }
        return new VoiceVariant(tokenAfterLocalePrefix(normalized), false);
    }

    @NonNull
    private static String stripSourceSuffix(@NonNull String voiceName) {
        String lowerName = voiceName.toLowerCase(Locale.US);
        if (lowerName.endsWith(LOCAL_SUFFIX)) {
            return voiceName.substring(0, voiceName.length() - LOCAL_SUFFIX.length());
        }
        if (lowerName.endsWith(NETWORK_SUFFIX)) {
            return voiceName.substring(0, voiceName.length() - NETWORK_SUFFIX.length());
        }
        return voiceName;
    }

    @NonNull
    private static String tokenAfterHash(@NonNull String voiceName) {
        int hashIndex = voiceName.indexOf(HASH_SEPARATOR);
        if (hashIndex < 0 || hashIndex + HASH_SEPARATOR.length() >= voiceName.length()) {
            return voiceName;
        }
        return voiceName.substring(hashIndex + HASH_SEPARATOR.length());
    }

    @NonNull
    private static String tokenAfterGoogleExtension(@NonNull String voiceName) {
        String normalized = voiceName.replace('_', '-').toLowerCase(Locale.US);
        int markerIndex = normalized.indexOf(GOOGLE_EXTENSION_MARKER);
        if (markerIndex < 0) {
            return voiceName;
        }
        return voiceName.substring(markerIndex + GOOGLE_EXTENSION_MARKER.length());
    }

    @NonNull
    private static String tokenAfterLocalePrefix(@NonNull String voiceName) {
        String normalized = voiceName.replace('_', '-').toLowerCase(Locale.US);
        int firstSeparator = normalized.indexOf('-');
        if (firstSeparator < 0 || !looksLikeLanguageCode(normalized.substring(0, firstSeparator))) {
            return voiceName;
        }
        int secondSeparator = normalized.indexOf('-', firstSeparator + 1);
        if (secondSeparator < 0) {
            return voiceName;
        }
        return voiceName.substring(secondSeparator + 1);
    }

    private static boolean looksLikeLanguageCode(@NonNull String token) {
        return token.length() >= 2
                && token.length() <= 3
                && containsOnlyLetters(token);
    }

    private static boolean containsOnlyLetters(@NonNull String token) {
        for (int i = 0; i < token.length(); i++) {
            if (!Character.isLetter(token.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasReadableVoiceWord(@NonNull String token) {
        String searchable = ' ' + token.replace('-', ' ').replace('_', ' ').toLowerCase(Locale.US) + ' ';
        return searchable.contains(' ' + FEMALE_TOKEN + ' ')
                || searchable.contains(' ' + MALE_TOKEN + ' ');
    }

    static final class VoiceVariant {
        @NonNull
        final String token;
        final boolean displayAsCode;

        VoiceVariant(@Nullable String token, boolean displayAsCode) {
            this.token = token == null ? "" : token.trim();
            this.displayAsCode = displayAsCode;
        }
    }
}
