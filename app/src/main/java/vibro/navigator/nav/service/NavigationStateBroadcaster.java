package vibro.navigator.nav.service;


import vibro.navigator.nav.model.NavState;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class NavigationStateBroadcaster {

    private final List<NavigationService.Listener> listeners = new ArrayList<>();

    public void register(@NonNull NavigationService.Listener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregister(@NonNull NavigationService.Listener listener) {
        listeners.remove(listener);
    }

    public void clear() {
        listeners.clear();
    }

    public int size() {
        return listeners.size();
    }

    public void dispatch(@NonNull NavState state) {
        for (NavigationService.Listener listener : new ArrayList<>(listeners)) {
            try {
                listener.onState(state);
            } catch (Exception ignored) {
                // Listener failures must not break navigation updates for other listeners.
            }
        }
    }
}
