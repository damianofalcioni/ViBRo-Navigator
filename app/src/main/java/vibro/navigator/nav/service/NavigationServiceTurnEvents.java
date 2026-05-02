package vibro.navigator.nav.service;


import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.guidance.NavigationTurnEventDispatcher;
import vibro.navigator.nav.session.NavigationSession;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public final class NavigationServiceTurnEvents implements NavigationServiceRouteCallback.TurnEventDispatcher {

    private final NavigationSession navigationSession;
    @Nullable
    private NavigationTurnEventDispatcher dispatcher;

    public NavigationServiceTurnEvents(@NonNull NavigationSession navigationSession) {
        this.navigationSession = navigationSession;
    }

    public void attachDispatcher(@NonNull NavigationTurnEventDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void dispatch(@NonNull List<NavigationTurnEvent> turnEvents) {
        if (navigationSession.isPaused() || dispatcher == null) {
            return;
        }
        dispatcher.dispatch(turnEvents);
    }
}
