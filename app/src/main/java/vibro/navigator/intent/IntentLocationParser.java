package vibro.navigator.intent;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IntentLocationParser {

    private static final Pattern MAP_URL_IN_TEXT = Pattern.compile("((?:https?://|geo:|google\\.navigation:)[^\\s]+)", Pattern.CASE_INSENSITIVE);

    private IntentLocationParser() {
    }

    @Nullable
    public static String parseToQuery(@NonNull Intent intent) {
        return parseToQuery(intent.getAction(), intent.getDataString(), intent.getStringExtra(Intent.EXTRA_TEXT));
    }

    @Nullable
    static String parseToQuery(@Nullable String action, @Nullable String dataString, @Nullable String sharedText) {
        String parsedData = IntentLocationUriParser.parse(dataString);
        if (parsedData != null) {
            return parsedData;
        }

        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action) || sharedText != null) {
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
        if (urlMatcher.find()) {
            String parsedUrl = IntentLocationUriParser.parse(urlMatcher.group(1));
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

}
