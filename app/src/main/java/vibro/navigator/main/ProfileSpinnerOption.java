package vibro.navigator.main;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class ProfileSpinnerOption {
    static final String CUSTOM_KEY = "__custom__";

    @NonNull
    private final String label;
    @Nullable
    private final String profileName;
    private final boolean custom;

    ProfileSpinnerOption(@NonNull String label, @Nullable String profileName, boolean custom) {
        this.label = label;
        this.profileName = profileName;
        this.custom = custom;
    }

    boolean isCustom() {
        return custom;
    }

    @Nullable
    String profileName() {
        return profileName;
    }

    @NonNull
    String selectionKey() {
        return custom ? CUSTOM_KEY : safe(profileName);
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
}

