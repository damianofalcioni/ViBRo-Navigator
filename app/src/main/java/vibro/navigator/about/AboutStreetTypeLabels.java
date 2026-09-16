package vibro.navigator.about;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Locale;

import vibro.navigator.R;
import vibro.navigator.nav.compass.CompassStreetCategory;
import vibro.navigator.nav.compass.CompassStreetType;

/** Resource labels and theme attributes for the BRouter street tag selector. */
final class AboutStreetTypeLabels {
    private AboutStreetTypeLabels() {
    }

    @NonNull
    static String typeLabel(@NonNull Context context, @NonNull CompassStreetType type) {
        switch (type) {
            case RAILWAY:
                return context.getString(R.string.street_type_railway);
            case WATERWAY:
                return context.getString(R.string.street_type_waterway);
            case OTHER:
                return context.getString(R.string.street_type_other);
            case ROAD:
                return context.getString(R.string.street_type_road);
            case ROUTE_HIKING_FOOT:
                return context.getString(R.string.street_type_route_hiking_foot);
            case ROUTE_SKI_PISTE:
                return context.getString(R.string.street_type_route_ski_piste);
            default:
                return genericTypeLabel(context, type);
        }
    }

    @NonNull
    private static String genericTypeLabel(@NonNull Context context, @NonNull CompassStreetType type) {
        String name = type.name().toLowerCase(Locale.ROOT);
        if (name.startsWith("route_")) {
            return context.getString(R.string.format_street_route_type, name.substring("route_".length()));
        }
        return context.getString(R.string.format_street_highway_type, name);
    }

    static int categoryLabel(@NonNull CompassStreetCategory category) {
        switch (category) {
            case HIGHWAY:
                return R.string.street_category_highway;
            case NORMAL:
                return R.string.street_category_normal;
            case WALKING_CYCLING:
                return R.string.street_category_walking_cycling;
            case SPECIAL_ROUTING:
                return R.string.street_category_special_routing;
            default:
                throw new IllegalArgumentException("Unknown street category: " + category);
        }
    }

    static int colorAttribute(@NonNull CompassStreetCategory category) {
        switch (category) {
            case HIGHWAY:
                return R.attr.vibroCompassStreetHighwayColor;
            case NORMAL:
                return R.attr.vibroCompassStreetNormalColor;
            case WALKING_CYCLING:
                return R.attr.vibroCompassStreetWalkingCyclingColor;
            case SPECIAL_ROUTING:
                return R.attr.vibroCompassStreetSpecialRoutingColor;
            default:
                throw new IllegalArgumentException("Unknown street category: " + category);
        }
    }
}
