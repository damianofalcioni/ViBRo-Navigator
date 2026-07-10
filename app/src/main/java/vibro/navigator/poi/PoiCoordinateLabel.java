package vibro.navigator.poi;

import androidx.annotation.NonNull;

public final class PoiCoordinateLabel {
    private PoiCoordinateLabel() {
    }

    public static boolean isCoordinateLabel(@NonNull Poi poi) {
        if (!poi.hasValidCoordinates()) {
            return false;
        }
        String trimmedName = poi.name.trim();
        if (trimmedName.isEmpty()) {
            return true;
        }
        Poi parsedName = CoordinateParser.tryParse(trimmedName, null);
        return parsedName != null && parsedName.stableKey().equals(poi.stableKey());
    }
}
