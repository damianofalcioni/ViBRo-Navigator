package vibro.navigator.brouter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class BRouterProfileParameterTypeParser {
    private BRouterProfileParameterTypeParser() {
    }

    @NonNull
    static BRouterProfileParameter.ValueType valueType(@Nullable String rawType) {
        String normalized = rawType == null ? "" : rawType.trim().toLowerCase(Locale.ROOT);
        if ("boolean".equals(normalized)) {
            return BRouterProfileParameter.ValueType.BOOLEAN;
        }
        if ("number".equals(normalized) || "integer".equals(normalized)) {
            return BRouterProfileParameter.ValueType.NUMBER;
        }
        if (isSelectionType(normalized)) {
            return BRouterProfileParameter.ValueType.SELECTION;
        }
        return BRouterProfileParameter.ValueType.STRING;
    }

    @NonNull
    static List<BRouterProfileParameterOption> options(@Nullable String rawType) {
        if (!isSelectionType(rawType)) {
            return Collections.emptyList();
        }
        String rawOptions = optionListText(rawType);
        String[] parts = rawOptions.split(",");
        List<BRouterProfileParameterOption> out = new ArrayList<>();
        for (String rawOption : parts) {
            BRouterProfileParameterOption option = option(rawOption);
            if (option != null) {
                out.add(option);
            }
        }
        return out;
    }

    @NonNull
    private static String optionListText(@NonNull String rawType) {
        String trimmed = rawType.trim();
        return trimmed.substring(trimmed.indexOf('[') + 1, trimmed.indexOf(']'));
    }

    @Nullable
    private static BRouterProfileParameterOption option(@Nullable String rawOption) {
        if (rawOption == null) {
            return null;
        }
        String label = rawOption.trim();
        if (label.isEmpty()) {
            return null;
        }
        String[] valueParts = label.split("=", 2);
        String value = valueParts[0].trim();
        return value.isEmpty() ? null : new BRouterProfileParameterOption(value, label);
    }

    private static boolean isSelectionType(@Nullable String rawType) {
        if (rawType == null) {
            return false;
        }
        int open = rawType.indexOf('[');
        int close = rawType.indexOf(']');
        return open >= 0 && close > open + 1;
    }
}
