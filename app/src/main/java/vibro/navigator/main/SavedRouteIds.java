package vibro.navigator.main;

import androidx.annotation.NonNull;

import java.util.List;

final class SavedRouteIds {
    private SavedRouteIds() {
    }

    @NonNull
    static String newRouteId(@NonNull List<SavedRoute> routes) {
        String baseId = "route-" + System.currentTimeMillis();
        String routeId = baseId;
        int suffix = 2;
        while (containsRouteId(routes, routeId)) {
            routeId = baseId + "-" + suffix;
            suffix++;
        }
        return routeId;
    }

    private static boolean containsRouteId(@NonNull List<SavedRoute> routes, @NonNull String routeId) {
        for (SavedRoute route : routes) {
            if (route.id.equals(routeId)) {
                return true;
            }
        }
        return false;
    }
}
