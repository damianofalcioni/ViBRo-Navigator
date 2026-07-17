package vibro.navigator.nav.session;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

import vibro.navigator.nav.guidance.NavigationTurnEvent;

final class NavigationInitialTurnEvents {
    private NavigationInitialTurnEvents() {
    }

    @NonNull
    static List<NavigationTurnEvent> suppressForSingleInstructionMode(
            @NonNull List<NavigationTurnEvent> events,
            boolean singleInstructionMode
    ) {
        return singleInstructionMode ? Collections.emptyList() : events;
    }
}
