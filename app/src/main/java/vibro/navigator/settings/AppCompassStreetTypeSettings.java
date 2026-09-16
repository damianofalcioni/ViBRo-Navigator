package vibro.navigator.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import vibro.navigator.nav.compass.CompassStreetCategory;
import vibro.navigator.nav.compass.CompassStreetType;
import vibro.navigator.nav.compass.CompassStreetVisibility;

public final class AppCompassStreetTypeSettings {
    private static final String KEY_DISABLED_CATEGORIES =
            "compass_surrounding_street_disabled_categories";
    private static final String KEY_DISABLED_TYPES =
            "compass_surrounding_street_disabled_types";

    private AppCompassStreetTypeSettings() {
    }

    @NonNull
    public static CompassStreetVisibility get(@NonNull Context context) {
        return get(preferences(context));
    }

    @NonNull
    static CompassStreetVisibility get(@NonNull SharedPreferences preferences) {
        return new CompassStreetVisibility(
                readEnums(
                        preferences.getStringSet(KEY_DISABLED_CATEGORIES, null),
                        CompassStreetCategory.class
                ),
                readEnums(
                        preferences.getStringSet(KEY_DISABLED_TYPES, null),
                        CompassStreetType.class
                )
        );
    }

    public static void set(
            @NonNull Context context,
            @NonNull CompassStreetVisibility visibility
    ) {
        set(preferences(context), visibility);
    }

    static void set(
            @NonNull SharedPreferences preferences,
            @NonNull CompassStreetVisibility visibility
    ) {
        preferences.edit()
                .putStringSet(KEY_DISABLED_CATEGORIES, names(visibility.disabledCategories()))
                .putStringSet(KEY_DISABLED_TYPES, names(visibility.disabledTypes()))
                .apply();
    }

    @NonNull
    private static <E extends Enum<E>> EnumSet<E> readEnums(
            Set<String> stored,
            @NonNull Class<E> enumClass
    ) {
        EnumSet<E> values = EnumSet.noneOf(enumClass);
        if (stored == null) {
            return values;
        }
        for (String name : stored) {
            addIfKnown(values, enumClass, name);
        }
        return values;
    }

    private static <E extends Enum<E>> void addIfKnown(
            @NonNull Set<E> target,
            @NonNull Class<E> enumClass,
            String name
    ) {
        try {
            target.add(Enum.valueOf(enumClass, name));
        } catch (IllegalArgumentException ignored) {
            // Ignore values written by a newer app version.
        }
    }

    @NonNull
    private static Set<String> names(@NonNull Set<? extends Enum<?>> values) {
        Set<String> names = new HashSet<>();
        for (Enum<?> value : values) {
            names.add(value.name());
        }
        return names;
    }

    @NonNull
    private static SharedPreferences preferences(@NonNull Context context) {
        return context.getSharedPreferences(AppSettings.PREFS, Context.MODE_PRIVATE);
    }
}
