package vibro.navigator.intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IntentLocationParser {

    private static final String ACTION_SEND = "android.intent.action.SEND";
    private static final String ACTION_SEND_MULTIPLE = "android.intent.action.SEND_MULTIPLE";
    private static final String TRAILING_URL_PUNCTUATION = ".,;:!?)]}\"'";
    private static final Pattern MAP_URL_IN_TEXT = Pattern.compile("((?:https?://|geo:|google\\.navigation:)[^\\s]+)", Pattern.CASE_INSENSITIVE);

    private IntentLocationParser() {
    }

    @Nullable
    public static String parseToQuery(@Nullable String action, @Nullable String dataString, @Nullable String sharedText) {
        String parsedData = IntentLocationUriParser.parse(dataString);
        if (parsedData != null) {
            return parsedData;
        }

        if (ACTION_SEND.equals(action) || ACTION_SEND_MULTIPLE.equals(action) || sharedText != null) {
            return parseSharedText(sharedText);
        }
        return null;
    }

    @Nullable
    private static String parseSharedText(@Nullable String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        Matcher urlMatcher = MAP_URL_IN_TEXT.matcher(trimmed);
        while (urlMatcher.find()) {
            String parsedUrl = parseSharedUrlCandidate(urlMatcher.group(1));
            if (parsedUrl != null) {
                return parsedUrl;
            }
        }

        String coords = IntentLocationCoordinates.extract(trimmed);
        if (coords != null) {
            return coords;
        }
        return trimmed;
    }

    @Nullable
    private static String parseSharedUrlCandidate(@NonNull String raw) {
        String candidate = raw;
        while (!candidate.isEmpty()) {
            String parsed = IntentLocationUriParser.parse(candidate);
            if (parsed != null) {
                return parsed;
            }
            String trimmed = trimTrailingUrlPunctuation(candidate);
            if (trimmed.length() == candidate.length()) {
                return null;
            }
            candidate = trimmed;
        }
        return null;
    }

    @NonNull
    private static String trimTrailingUrlPunctuation(@NonNull String value) {
        int end = value.length();
        while (end > 0 && isTrailingUrlPunctuation(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }

    private static boolean isTrailingUrlPunctuation(char value) {
        return TRAILING_URL_PUNCTUATION.indexOf(value) >= 0;
    }

}
