package vibro.navigator.map;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class MapPoiCache {
    @NonNull
    private final Map<String, Entry> entries = new HashMap<>();

    @NonNull
    synchronized List<MapPoiMarker> visibleMarkers(
            @NonNull MapPickerBounds bounds,
            @NonNull List<MapPoiCategory> categories
    ) {
        Map<String, MapPoiMarker> visible = new LinkedHashMap<>();
        for (MapPoiCategory category : categories) {
            Entry entry = entries.get(category.id);
            if (entry != null) {
                entry.addVisibleMarkers(bounds, visible);
            }
        }
        return new ArrayList<>(visible.values());
    }

    @NonNull
    synchronized List<MapPoiFetchRequest> missingRequests(
            @NonNull MapPickerBounds target,
            @NonNull List<MapPoiCategory> categories
    ) {
        List<MapPoiFetchRequest> requests = new ArrayList<>();
        Set<String> requestKeys = new LinkedHashSet<>();
        for (MapPoiCategory category : categories) {
            addMissingRequests(requests, requestKeys, target, category);
        }
        return requests;
    }

    synchronized void remember(
            @NonNull MapPoiCategory category,
            @NonNull MapPickerBounds bounds,
            @NonNull List<MapPoiMarker> markers
    ) {
        entryFor(category).remember(bounds, markers);
    }

    synchronized void rememberAll(
            @NonNull MapPickerBounds bounds,
            @NonNull List<MapPoiCategory> categories,
            @NonNull List<MapPoiMarker> markers
    ) {
        Map<String, List<MapPoiMarker>> byCategory = markersByCategory(markers);
        for (MapPoiCategory category : categories) {
            List<MapPoiMarker> categoryMarkers = byCategory.remove(category.id);
            entryFor(category).remember(bounds, categoryMarkers == null ? new ArrayList<>() : categoryMarkers);
        }
        for (List<MapPoiMarker> uncategorizedMarkers : byCategory.values()) {
            if (!uncategorizedMarkers.isEmpty()) {
                entryFor(uncategorizedMarkers.get(0).category).remember(bounds, uncategorizedMarkers);
            }
        }
    }

    @NonNull
    private Entry entryFor(@NonNull MapPoiCategory category) {
        Entry entry = entries.get(category.id);
        if (entry == null) {
            entry = new Entry();
            entries.put(category.id, entry);
        }
        return entry;
    }

    @NonNull
    private static List<MapPickerBounds> single(@NonNull MapPickerBounds bounds) {
        List<MapPickerBounds> list = new ArrayList<>();
        list.add(bounds);
        return list;
    }

    private void addMissingRequests(
            @NonNull List<MapPoiFetchRequest> requests,
            @NonNull Set<String> requestKeys,
            @NonNull MapPickerBounds target,
            @NonNull MapPoiCategory category
    ) {
        Entry entry = entries.get(category.id);
        List<MapPickerBounds> missing = entry == null ? single(target) : entry.missingBounds(target);
        for (MapPickerBounds bounds : missing) {
            addRequestIfAbsent(requests, requestKeys, category, bounds);
        }
    }

    private static void addRequestIfAbsent(
            @NonNull List<MapPoiFetchRequest> requests,
            @NonNull Set<String> requestKeys,
            @NonNull MapPoiCategory category,
            @NonNull MapPickerBounds bounds
    ) {
        if (requestKeys.add(requestKey(category, bounds))) {
            requests.add(new MapPoiFetchRequest(category, bounds));
        }
    }

    @NonNull
    private static String requestKey(@NonNull MapPoiCategory category, @NonNull MapPickerBounds bounds) {
        return new StringBuilder(category.id)
                .append('|').append(bounds.south)
                .append('|').append(bounds.west)
                .append('|').append(bounds.north)
                .append('|').append(bounds.east)
                .append('|').append(bounds.zoom)
                .toString();
    }

    @NonNull
    private static Map<String, List<MapPoiMarker>> markersByCategory(@NonNull List<MapPoiMarker> markers) {
        Map<String, List<MapPoiMarker>> byCategory = new LinkedHashMap<>();
        for (MapPoiMarker marker : markers) {
            List<MapPoiMarker> group = byCategory.get(marker.category.id);
            if (group == null) {
                group = new ArrayList<>();
                byCategory.put(marker.category.id, group);
            }
            group.add(marker);
        }
        return byCategory;
    }

    private static final class Entry {
        @NonNull
        private final Map<String, MapPoiMarker> markers = new LinkedHashMap<>();
        @NonNull
        private final List<MapPickerBounds> coveredBounds = new ArrayList<>();

        void remember(@NonNull MapPickerBounds bounds, @NonNull List<MapPoiMarker> nextMarkers) {
            coveredBounds.add(bounds);
            for (MapPoiMarker marker : nextMarkers) {
                markers.put(marker.stableKey(), marker);
            }
        }

        void addVisibleMarkers(
                @NonNull MapPickerBounds bounds,
                @NonNull Map<String, MapPoiMarker> out
        ) {
            for (MapPoiMarker marker : markers.values()) {
                if (bounds.contains(marker.lat, marker.lon)) {
                    out.put(marker.stableKey(), marker);
                }
            }
        }

        @NonNull
        List<MapPickerBounds> missingBounds(@NonNull MapPickerBounds target) {
            List<MapPickerBounds> missing = single(target);
            for (MapPickerBounds covered : coveredBounds) {
                missing = subtractCovered(missing, covered);
            }
            return missing;
        }

        @NonNull
        private static List<MapPickerBounds> subtractCovered(
                @NonNull List<MapPickerBounds> candidates,
                @NonNull MapPickerBounds covered
        ) {
            List<MapPickerBounds> out = new ArrayList<>();
            for (MapPickerBounds candidate : candidates) {
                out.addAll(subtract(candidate, covered));
            }
            return out;
        }

        @NonNull
        private static List<MapPickerBounds> subtract(
                @NonNull MapPickerBounds source,
                @NonNull MapPickerBounds covered
        ) {
            if (!source.intersects(covered)) {
                return single(source);
            }
            return boundsAroundIntersection(source, covered);
        }

        @NonNull
        private static List<MapPickerBounds> boundsAroundIntersection(
                @NonNull MapPickerBounds source,
                @NonNull MapPickerBounds covered
        ) {
            List<MapPickerBounds> out = new ArrayList<>();
            double south = Math.max(source.south, covered.south);
            double north = Math.min(source.north, covered.north);
            addIfValid(out, source.south, source.west, south, source.east, source.zoom);
            addIfValid(out, north, source.west, source.north, source.east, source.zoom);
            addIfValid(out, south, source.west, north, Math.max(source.west, covered.west), source.zoom);
            addIfValid(out, south, Math.min(source.east, covered.east), north, source.east, source.zoom);
            return out;
        }

        private static void addIfValid(
                @NonNull List<MapPickerBounds> out,
                double south,
                double west,
                double north,
                double east,
                int zoom
        ) {
            if (north > south && east > west) {
                out.add(MapPickerBounds.of(south, west, north, east, zoom));
            }
        }
    }
}
