package vibro.navigator.nav.compass.ui;

import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.annotation.NonNull;

import vibro.navigator.nav.compass.CompassStreetCategory;

final class NavigationStreetBatches {
    @NonNull
    private final NavigationStreetGeometry[] batches =
            new NavigationStreetGeometry[CompassStreetCategory.values().length];

    NavigationStreetBatches() {
        for (int index = 0; index < batches.length; index++) {
            batches[index] = new NavigationStreetGeometry();
        }
    }

    NavigationStreetGeometry geometryFor(CompassStreetCategory category) {
        return batches[category.ordinal()];
    }

    void draw(Canvas canvas, CompassStreetCategory category, Paint paint) {
        geometryFor(category).draw(canvas, paint);
    }
}
