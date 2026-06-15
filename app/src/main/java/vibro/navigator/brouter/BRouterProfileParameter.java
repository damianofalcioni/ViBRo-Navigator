package vibro.navigator.brouter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BRouterProfileParameter {
    public enum ValueType {
        BOOLEAN,
        NUMBER,
        SELECTION,
        STRING
    }

    @NonNull
    public final String name;
    @Nullable
    public final String description;
    @NonNull
    public final String defaultValue;
    @NonNull
    public final ValueType valueType;
    @NonNull
    public final List<BRouterProfileParameterOption> options;

    public BRouterProfileParameter(
            @NonNull String name,
            @Nullable String description,
            @NonNull String defaultValue,
            @NonNull ValueType valueType,
            @Nullable List<BRouterProfileParameterOption> options
    ) {
        this.name = name;
        this.description = clean(description);
        this.defaultValue = defaultValue;
        this.valueType = valueType;
        this.options = immutableCopy(options);
    }

    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }

    @NonNull
    private static List<BRouterProfileParameterOption> immutableCopy(
            @Nullable List<BRouterProfileParameterOption> options
    ) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(options));
    }

    @Nullable
    private static String clean(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
