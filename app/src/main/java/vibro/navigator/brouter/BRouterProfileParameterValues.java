package vibro.navigator.brouter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class BRouterProfileParameterValues {
    private BRouterProfileParameterValues() {
    }

    @Nullable
    public static String toExtraParams(@Nullable Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> keys = sortedKeys(values);
        StringBuilder out = new StringBuilder();
        for (String key : keys) {
            appendExtraParam(out, key, values.get(key));
        }
        return out.length() == 0 ? null : out.toString();
    }

    @NonNull
    private static List<String> sortedKeys(@NonNull Map<String, String> values) {
        List<String> keys = new ArrayList<>(values.keySet());
        Collections.sort(keys);
        return keys;
    }

    private static void appendExtraParam(
            @NonNull StringBuilder out,
            @NonNull String key,
            @Nullable String value
    ) {
        if (!isUsableExtraParamPart(key) || value == null || !isUsableExtraParamPart(value)) {
            return;
        }
        if (out.length() > 0) {
            out.append('&');
        }
        out.append(encode(key)).append('=').append(encode(value));
    }

    public static boolean containsExtraParamsSeparator(@NonNull String value) {
        // BRouter URL-decodes the entire string before splitting on these separators.
        return value.indexOf('?') >= 0 || value.indexOf('&') >= 0 || value.indexOf('=') >= 0;
    }

    private static boolean isUsableExtraParamPart(@Nullable String value) {
        return value != null
                && !value.trim().isEmpty()
                && !containsExtraParamsSeparator(value);
    }

    @NonNull
    private static String encode(@NonNull String value) {
        try {
            // The Charset overload is unavailable on older supported Android versions.
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 encoding unavailable", e);
        }
    }
}
