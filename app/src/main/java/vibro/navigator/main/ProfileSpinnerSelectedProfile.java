package vibro.navigator.main;

import androidx.annotation.Nullable;

final class ProfileSpinnerSelectedProfile {
    private ProfileSpinnerSelectedProfile() {
    }

    @Nullable
    static String brouterProfileName(
            @Nullable ProfileSpinnerOption option,
            @Nullable String customProfileName
    ) {
        if (option == null) {
            return null;
        }
        String profileName = option.isCustom() ? customProfileName : option.profileName();
        return clean(profileName);
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
