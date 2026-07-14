package vibro.navigator.poi.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiDetails;

public final class PoiSuggestion {
    @NonNull
    public final Poi poi;
    public final boolean deletable;

    public PoiSuggestion(@NonNull Poi poi, boolean deletable) {
        this.poi = poi;
        this.deletable = deletable;
    }

    @NonNull
    String displayLabel(@NonNull Context context) {
        PoiDetails details = poi.details();
        if (details == null || !details.isEntrance()) {
            return poi.displayLabel();
        }
        return context.getString(
                R.string.format_poi_entrance_label,
                entranceLabel(context, details.entranceType()),
                parentLabel(details)
        );
    }

    @NonNull
    Poi selectedPoi(@NonNull Context context) {
        String label = displayLabel(context);
        if (label.equals(poi.displayLabel())) {
            return poi;
        }
        return new Poi(label, poi.lat, poi.lon, poi.details());
    }

    boolean hasDetails() {
        PoiDetails details = poi.details();
        return details != null
                && (details.hasExtraTags() || details.hasEntrances() || details.isEntrance());
    }

    @NonNull
    private static String parentLabel(@NonNull PoiDetails details) {
        String parentName = details.parentName();
        return parentName != null ? parentName : "";
    }

    @NonNull
    private static String entranceLabel(@NonNull Context context, @Nullable String type) {
        if (type == null || type.trim().isEmpty() || "yes".equalsIgnoreCase(type.trim())) {
            return context.getString(R.string.label_poi_entrance);
        }
        return context.getString(R.string.format_poi_entrance_type, type.trim());
    }
}
