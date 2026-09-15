package vibro.navigator.nav.compass.ui;

import android.graphics.Path;

import androidx.annotation.NonNull;

import vibro.navigator.nav.compass.CompassStreetCategory;

final class NavigationStreetPaths {
    @NonNull
    private final Path[] paths = new Path[CompassStreetCategory.values().length];

    NavigationStreetPaths() {
        for (int index = 0; index < paths.length; index++) {
            paths[index] = new Path();
        }
    }

    @NonNull
    Path pathFor(@NonNull CompassStreetCategory category) {
        return paths[category.ordinal()];
    }
}
