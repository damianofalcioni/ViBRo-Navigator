package vibro.navigator.main;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class ProfileSpinnerOption {
    static final String CUSTOM_KEY = "__custom__";
    static final String STRAIGHT_LINE_KEY = "__straight_line__";

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

    @NonNull
    static ProfileSpinnerOption straightLine(@NonNull String label) {
        return new ProfileSpinnerOption(label, null, Kind.STRAIGHT_LINE);
    }

    boolean isCustom() {
        return kind == Kind.CUSTOM;
    }

    boolean isStraightLine() {
        return kind == Kind.STRAIGHT_LINE;
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
        if (kind == Kind.STRAIGHT_LINE) {
            return STRAIGHT_LINE_KEY;
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
        CUSTOM,
        STRAIGHT_LINE
    }
}

