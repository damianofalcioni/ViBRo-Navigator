package vibro.navigator.intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
        Map<String, List<String>> params = parse(query);
        for (String key : keys) {
            String value = firstNonEmptyValue(params.get(key.toLowerCase(Locale.US)));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    static boolean hasAnyKey(@Nullable String query, @NonNull String... keys) {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }
        Map<String, List<String>> params = parse(query);
        for (String key : keys) {
            if (params.containsKey(key.toLowerCase(Locale.US))) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static String firstNonEmptyValue(@Nullable List<String> values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!IntentUriDecoder.decodeComponent(value).trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    @NonNull
    private static Map<String, List<String>> parse(@NonNull String query) {
        Map<String, List<String>> params = new LinkedHashMap<>();
        for (String part : query.split("&")) {
            if (part.isEmpty()) {
                continue;
            }
            int separator = part.indexOf('=');
            String key = separator >= 0 ? part.substring(0, separator) : part;
            String value = separator >= 0 ? part.substring(separator + 1) : "";
            String normalizedKey = IntentUriDecoder.decodeComponent(key).toLowerCase(Locale.US);
            List<String> values = params.get(normalizedKey);
            if (values == null) {
                values = new ArrayList<>();
                params.put(normalizedKey, values);
            }
            values.add(value);
        }
        return params;
    }
}
