package vibro.navigator.main;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class ProfileSpinnerOption {
    static final String CUSTOM_KEY = "__custom__";

    @NonNull
    private final String label;
    @Nullable
    private final String profileName;
    @NonNull
    private final Kind kind;

    ProfileSpinnerOption(@NonNull String label, @Nullable String profileName, boolean custom) {
        this(label, profileName, custom ? Kind.CUSTOM : Kind.BROUTER);
    }

    private ProfileSpinnerOption(@NonNull String label, @Nullable String profileName, @NonNull Kind kind) {
        this.label = label;
        this.profileName = profileName;
        this.kind = kind;
    }

    boolean isCustom() {
        return kind == Kind.CUSTOM;
    }

    @Nullable
    String profileName() {
        return profileName;
    }

    @NonNull
    String selectionKey() {
        if (kind == Kind.CUSTOM) {
            return CUSTOM_KEY;
        }
        return safe(profileName);
    }

    @NonNull
    @Override
    public String toString() {
        return label;
    }

    @NonNull
    static String safe(@Nullable String value) {
        return value == null ? "null" : value;
    }

    private enum Kind {
        BROUTER,
        CUSTOM
    }
}

