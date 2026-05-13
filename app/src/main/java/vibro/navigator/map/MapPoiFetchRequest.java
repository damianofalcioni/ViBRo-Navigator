package vibro.navigator.map;

import androidx.annotation.NonNull;

final class MapPoiFetchRequest {
    @NonNull
    final MapPoiCategory category;
    @NonNull
    final MapPickerBounds bounds;

    MapPoiFetchRequest(@NonNull MapPoiCategory category, @NonNull MapPickerBounds bounds) {
        this.category = category;
        this.bounds = bounds;
    }
}
