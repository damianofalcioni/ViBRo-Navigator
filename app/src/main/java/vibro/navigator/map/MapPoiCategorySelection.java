package vibro.navigator.map;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class MapPoiCategorySelection {
    @NonNull
    private List<MapPoiCategory> categories = new ArrayList<>();
    @NonNull
    private final Set<String> enabledCategoryIds = new LinkedHashSet<>();

    @NonNull
    List<MapPoiCategory> categories() {
        return categories;
    }

    @NonNull
    Set<String> enabledCategoryIds() {
        return enabledCategoryIds;
    }

    boolean hasEnabledCategories() {
        return !enabledCategoryIds.isEmpty();
    }

    boolean hasCategories() {
        return !categories.isEmpty();
    }

    void setCategories(@NonNull List<MapPoiCategory> discovered) {
        categories = discovered;
        retainEnabledCategories();
    }

    void clearCategories() {
        categories = new ArrayList<>();
        enabledCategoryIds.clear();
    }

    void setChecked(@NonNull MapPoiCategory category, boolean checked) {
        enabledCategoryIds.clear();
        if (checked) {
            enabledCategoryIds.add(category.id);
        }
    }

    @NonNull
    List<MapPoiCategory> enabledCategories() {
        List<MapPoiCategory> selected = new ArrayList<>();
        for (MapPoiCategory category : categories) {
            if (enabledCategoryIds.contains(category.id)) {
                selected.add(category);
            }
        }
        return selected;
    }

    private void retainEnabledCategories() {
        Set<String> availableIds = new LinkedHashSet<>();
        for (MapPoiCategory category : categories) {
            availableIds.add(category.id);
        }
        enabledCategoryIds.retainAll(availableIds);
    }
}
