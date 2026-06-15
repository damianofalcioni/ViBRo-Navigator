package vibro.navigator.brouter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class BRouterProfileParameterAnnotation {
    @NonNull
    final String name;
    @NonNull
    final String assignment;
    @Nullable
    final String description;
    @NonNull
    final String rawType;

    private BRouterProfileParameterAnnotation(
            @NonNull String name,
            @NonNull String assignment,
            @Nullable String description,
            @NonNull String rawType
    ) {
        this.name = name;
        this.assignment = assignment;
        this.description = description;
        this.rawType = rawType;
    }

    @Nullable
    static BRouterProfileParameterAnnotation fromLine(@Nullable String line) {
        if (line == null || !line.contains("#") || !line.contains("%")) {
            return null;
        }
        int hash = line.indexOf('#');
        String name = annotatedName(line, hash);
        if (name == null) {
            return null;
        }
        String[] metadata = line.substring(hash + 1).split("\\|", -1);
        return new BRouterProfileParameterAnnotation(
                name,
                line.substring(0, hash),
                metadata.length > 1 ? metadata[1].trim() : null,
                metadata.length > 2 ? metadata[2].trim() : ""
        );
    }

    @Nullable
    private static String annotatedName(@NonNull String line, int hash) {
        int firstPercent = line.indexOf('%');
        int secondPercent = line.indexOf('%', firstPercent + 1);
        if (firstPercent < 0 || secondPercent <= firstPercent || hash < 0 || hash > firstPercent) {
            return null;
        }
        String name = line.substring(firstPercent + 1, secondPercent).trim();
        return name.isEmpty() ? null : name;
    }
}
