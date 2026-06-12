package vibro.navigator.main;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.model.NavigationRoutingMode;

final class ProfileSelection {
    @NonNull
    final NavigationRoutingMode routingMode;
    @Nullable
    final String profileName;

    private ProfileSelection(
            @NonNull NavigationRoutingMode routingMode,
            @Nullable String profileName
    ) {
        this.routingMode = routingMode;
        this.profileName = profileName;
    }

    @NonNull
    static ProfileSelection brouter(@NonNull String profileName) {
        return new ProfileSelection(NavigationRoutingMode.BROUTER, profileName);
    }

    @NonNull
    static ProfileSelection straightLine() {
        return new ProfileSelection(NavigationRoutingMode.STRAIGHT_LINE, null);
    }
}
