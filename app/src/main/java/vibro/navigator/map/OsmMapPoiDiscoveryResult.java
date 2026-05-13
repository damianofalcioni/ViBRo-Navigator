package vibro.navigator.map;

import androidx.annotation.NonNull;

import java.util.List;

final class OsmMapPoiDiscoveryResult {
    @NonNull
    final List<MapPoiCategory> categories;
    @NonNull
    final List<MapPoiMarker> markers;

    OsmMapPoiDiscoveryResult(
            @NonNull List<MapPoiCategory> categories,
            @NonNull List<MapPoiMarker> markers
    ) {
        this.categories = categories;
        this.markers = markers;
    }
}
