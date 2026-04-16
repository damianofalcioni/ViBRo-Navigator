package vibro.navigator.nav;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

final class NavigationStateBroadcaster {

    private final List<NavigationService.Listener> listeners = new ArrayList<>();

    void register(@NonNull NavigationService.Listener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    void unregister(@NonNull NavigationService.Listener listener) {
        listeners.remove(listener);
    }

    void clear() {
        listeners.clear();
    }

    int size() {
        return listeners.size();
    }

    void dispatch(@NonNull NavState state) {
        for (NavigationService.Listener listener : new ArrayList<>(listeners)) {
            try {
                listener.onState(state);
            } catch (Exception ignored) {
                // Listener failures must not break navigation updates for other listeners.
            }
        }
    }
}
