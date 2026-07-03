package vibro.navigator.poi;

import androidx.annotation.NonNull;

public final class PoiCoordinateLabel {
    private PoiCoordinateLabel() {
    }

    public static boolean isCoordinateLabel(@NonNull Poi poi) {
        Poi parsedName = CoordinateParser.tryParse(poi.name, null);
        return parsedName != null && parsedName.stableKey().equals(poi.stableKey());
    }
}
