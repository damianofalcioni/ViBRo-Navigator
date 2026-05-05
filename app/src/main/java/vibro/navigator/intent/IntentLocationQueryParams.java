package vibro.navigator.intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class IntentLocationQueryParams {

    private IntentLocationQueryParams() {
    }

    @Nullable
    static String firstValue(@Nullable String query, @NonNull String... keys) {
        if (query == null || query.trim().isEmpty()) {
            return null;
        }
        Map<String, String> params = parse(query);
        for (String key : keys) {
            String value = params.get(key.toLowerCase(Locale.US));
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    static boolean hasAnyKey(@Nullable String query, @NonNull String... keys) {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }
        Map<String, String> params = parse(query);
        for (String key : keys) {
            if (params.containsKey(key.toLowerCase(Locale.US))) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private static Map<String, String> parse(@NonNull String query) {
        Map<String, String> params = new LinkedHashMap<>();
        for (String part : query.split("&")) {
            if (part.isEmpty()) {
                continue;
            }
            int separator = part.indexOf('=');
            String key = separator >= 0 ? part.substring(0, separator) : part;
            String value = separator >= 0 ? part.substring(separator + 1) : "";
            params.put(IntentUriDecoder.decodeComponent(key).toLowerCase(Locale.US), value);
        }
        return params;
    }
}
