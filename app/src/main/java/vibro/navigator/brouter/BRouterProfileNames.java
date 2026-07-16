package vibro.navigator.brouter;

import androidx.annotation.Nullable;

final class BRouterProfileNames {
    private BRouterProfileNames() {
    }

    @Nullable
    static String clean(@Nullable String profileName) {
        if (profileName == null) {
            return null;
        }
        String cleanName = profileName.trim();
        return cleanName.isEmpty() ? null : cleanName;
    }
}
