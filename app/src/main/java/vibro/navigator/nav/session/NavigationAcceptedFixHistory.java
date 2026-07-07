package vibro.navigator.nav.session;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.nav.location.NavigationLocation;

final class NavigationAcceptedFixHistory {
    @NonNull
    private final List<NavigationLocation> acceptedFixes = new ArrayList<>();

    void reset() {
        acceptedFixes.clear();
    }

    void record(@NonNull NavigationLocation location) {
        acceptedFixes.add(new NavigationLocation(location));
    }

    @NonNull
    List<NavigationLocation> snapshot() {
        if (acceptedFixes.isEmpty()) {
            return Collections.emptyList();
        }
        List<NavigationLocation> copy = new ArrayList<>(acceptedFixes.size());
        for (NavigationLocation location : acceptedFixes) {
            copy.add(new NavigationLocation(location));
        }
        return Collections.unmodifiableList(copy);
    }
}
