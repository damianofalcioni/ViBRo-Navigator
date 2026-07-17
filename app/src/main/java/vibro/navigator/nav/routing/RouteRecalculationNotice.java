package vibro.navigator.nav.routing;

import androidx.annotation.Nullable;

final class RouteRecalculationNotice {
    private RouteRecalculationNotice() {
    }

    @Nullable
    static String sanitize(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
