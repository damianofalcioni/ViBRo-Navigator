package vibro.navigator.main;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.model.NavigationRoutingMode;

final class ProfileSelection {
    @NonNull
    final NavigationRoutingMode routingMode;
    @Nullable
    final String profileName;
    @Nullable
    final String profileParameters;

    private ProfileSelection(
            @NonNull NavigationRoutingMode routingMode,
            @Nullable String profileName,
            @Nullable String profileParameters
    ) {
        this.routingMode = routingMode;
        this.profileName = profileName;
        this.profileParameters = clean(profileParameters);
    }

    @NonNull
    static ProfileSelection brouter(@NonNull String profileName) {
        return brouter(profileName, null);
    }

    @NonNull
    static ProfileSelection brouter(@NonNull String profileName, @Nullable String profileParameters) {
        return new ProfileSelection(NavigationRoutingMode.BROUTER, profileName, profileParameters);
    }

    @NonNull
    static ProfileSelection straightLine() {
        return new ProfileSelection(NavigationRoutingMode.STRAIGHT_LINE, null, null);
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
