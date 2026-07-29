package vibro.navigator.poi.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.PoiDetails;

public final class PoiSuggestion {
    @Nullable
    private final Poi poi;
    @Nullable
    private final String externalMapSearchQuery;
    public final boolean deletable;

    public PoiSuggestion(@NonNull Poi poi, boolean deletable) {
        this(poi, null, deletable);
    }

    private PoiSuggestion(
            @Nullable Poi poi,
            @Nullable String externalMapSearchQuery,
            boolean deletable
    ) {
        this.poi = poi;
        this.externalMapSearchQuery = externalMapSearchQuery;
        this.deletable = deletable;
    }

    @NonNull
    static PoiSuggestion externalMapSearch(@NonNull String query) {
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("External map search query must not be blank");
        }
        return new PoiSuggestion(null, trimmed, false);
    }

    boolean isExternalMapSearch() {
        return externalMapSearchQuery != null;
    }

    @NonNull
    String displayLabel(@NonNull Context context) {
        if (externalMapSearchQuery != null) {
            return context.getString(R.string.action_search_google_maps);
        }
        Poi poi = poi();
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
        Poi poi = poi();
        String label = displayLabel(context);
        if (label.equals(poi.displayLabel())) {
            return poi;
        }
        return new Poi(label, poi.lat, poi.lon, poi.details());
    }

    @NonNull
    Poi poi() {
        if (poi == null) {
            throw new IllegalStateException("Suggestion does not contain a POI");
        }
        return poi;
    }

    @NonNull
    String externalMapSearchQuery() {
        if (externalMapSearchQuery == null) {
            throw new IllegalStateException("Suggestion does not contain an external map search query");
        }
        return externalMapSearchQuery;
    }

    boolean hasInfo() {
        return isExternalMapSearch() || hasDetails();
    }

    boolean hasDetails() {
        if (isExternalMapSearch()) {
            return false;
        }
        Poi poi = poi();
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
