package vibro.navigator.nav;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

final class NavigationServiceTurnEvents implements NavigationServiceRouteCallback.TurnEventDispatcher {

    private final NavigationSession navigationSession;
    @Nullable
    private NavigationTurnEventDispatcher dispatcher;

    NavigationServiceTurnEvents(@NonNull NavigationSession navigationSession) {
        this.navigationSession = navigationSession;
    }

    void attachDispatcher(@NonNull NavigationTurnEventDispatcher dispatcher) {
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
