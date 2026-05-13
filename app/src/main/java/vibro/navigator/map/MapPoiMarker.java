package vibro.navigator.map;

import androidx.annotation.NonNull;

final class MapPoiMarker {
    @NonNull
    final String name;
    final double lat;
    final double lon;
    @NonNull
    final MapPoiCategory category;

    MapPoiMarker(@NonNull String name, double lat, double lon, @NonNull MapPoiCategory category) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.category = category;
    }

    @NonNull
    String stableKey() {
        return category.id + ":" + Math.round(lat * 10000000d) + ":" + Math.round(lon * 10000000d) + ":" + name;
    }
}
