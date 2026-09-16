package vibro.navigator.about;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import vibro.navigator.R;
import vibro.navigator.nav.compass.CompassStreetCategory;
import vibro.navigator.nav.compass.CompassStreetType;

/** Resource labels and theme attributes for the BRouter street tag selector. */
final class AboutStreetTypeLabels {
    private AboutStreetTypeLabels() {
    }

    @NonNull
    static String typeLabel(@NonNull Context context, @NonNull CompassStreetType type) {
        return context.getString(typeText(type).labelResId);
    }

    @NonNull
    static String typeDescription(@NonNull Context context, @NonNull CompassStreetType type) {
        return context.getString(typeText(type).descriptionResId);
    }

    private static final TypeText[] TYPE_TEXT = new TypeText[CompassStreetType.values().length];

    static {
        TYPE_TEXT[CompassStreetType.MOTORWAY.ordinal()] = new TypeText(
                R.string.street_type_motorway, R.string.street_type_motorway_info);
        TYPE_TEXT[CompassStreetType.MOTORWAY_LINK.ordinal()] = new TypeText(
                R.string.street_type_motorway_link, R.string.street_type_motorway_link_info);
        TYPE_TEXT[CompassStreetType.TRUNK.ordinal()] = new TypeText(
                R.string.street_type_trunk, R.string.street_type_trunk_info);
        TYPE_TEXT[CompassStreetType.TRUNK_LINK.ordinal()] = new TypeText(
                R.string.street_type_trunk_link, R.string.street_type_trunk_link_info);
        TYPE_TEXT[CompassStreetType.PRIMARY.ordinal()] = new TypeText(
                R.string.street_type_primary, R.string.street_type_primary_info);
        TYPE_TEXT[CompassStreetType.PRIMARY_LINK.ordinal()] = new TypeText(
                R.string.street_type_primary_link, R.string.street_type_primary_link_info);
        TYPE_TEXT[CompassStreetType.SECONDARY.ordinal()] = new TypeText(
                R.string.street_type_secondary, R.string.street_type_secondary_info);
        TYPE_TEXT[CompassStreetType.SECONDARY_LINK.ordinal()] = new TypeText(
                R.string.street_type_secondary_link, R.string.street_type_secondary_link_info);
        TYPE_TEXT[CompassStreetType.TERTIARY.ordinal()] = new TypeText(
                R.string.street_type_tertiary, R.string.street_type_tertiary_info);
        TYPE_TEXT[CompassStreetType.TERTIARY_LINK.ordinal()] = new TypeText(
                R.string.street_type_tertiary_link, R.string.street_type_tertiary_link_info);
        TYPE_TEXT[CompassStreetType.UNCLASSIFIED.ordinal()] = new TypeText(
                R.string.street_type_unclassified, R.string.street_type_unclassified_info);
        TYPE_TEXT[CompassStreetType.RESIDENTIAL.ordinal()] = new TypeText(
                R.string.street_type_residential, R.string.street_type_residential_info);
        TYPE_TEXT[CompassStreetType.LIVING_STREET.ordinal()] = new TypeText(
                R.string.street_type_living_street, R.string.street_type_living_street_info);
        TYPE_TEXT[CompassStreetType.SERVICE.ordinal()] = new TypeText(
                R.string.street_type_service, R.string.street_type_service_info);
        TYPE_TEXT[CompassStreetType.TRACK.ordinal()] = new TypeText(
                R.string.street_type_track, R.string.street_type_track_info);
        TYPE_TEXT[CompassStreetType.ROAD.ordinal()] = new TypeText(
                R.string.street_type_road, R.string.street_type_road_info);
        TYPE_TEXT[CompassStreetType.BUSWAY.ordinal()] = new TypeText(
                R.string.street_type_busway, R.string.street_type_busway_info);
        TYPE_TEXT[CompassStreetType.PEDESTRIAN.ordinal()] = new TypeText(
                R.string.street_type_pedestrian, R.string.street_type_pedestrian_info);
        TYPE_TEXT[CompassStreetType.FOOTWAY.ordinal()] = new TypeText(
                R.string.street_type_footway, R.string.street_type_footway_info);
        TYPE_TEXT[CompassStreetType.PATH.ordinal()] = new TypeText(
                R.string.street_type_path, R.string.street_type_path_info);
        TYPE_TEXT[CompassStreetType.CYCLEWAY.ordinal()] = new TypeText(
                R.string.street_type_cycleway, R.string.street_type_cycleway_info);
        TYPE_TEXT[CompassStreetType.BRIDLEWAY.ordinal()] = new TypeText(
                R.string.street_type_bridleway, R.string.street_type_bridleway_info);
        TYPE_TEXT[CompassStreetType.STEPS.ordinal()] = new TypeText(
                R.string.street_type_steps, R.string.street_type_steps_info);
        TYPE_TEXT[CompassStreetType.PLATFORM.ordinal()] = new TypeText(
                R.string.street_type_platform, R.string.street_type_platform_info);
        TYPE_TEXT[CompassStreetType.CORRIDOR.ordinal()] = new TypeText(
                R.string.street_type_corridor, R.string.street_type_corridor_info);
        TYPE_TEXT[CompassStreetType.REST_AREA.ordinal()] = new TypeText(
                R.string.street_type_rest_area, R.string.street_type_rest_area_info);
        TYPE_TEXT[CompassStreetType.SERVICES.ordinal()] = new TypeText(
                R.string.street_type_services, R.string.street_type_services_info);
        TYPE_TEXT[CompassStreetType.ELEVATOR.ordinal()] = new TypeText(
                R.string.street_type_elevator, R.string.street_type_elevator_info);
        TYPE_TEXT[CompassStreetType.VIA_FERRATA.ordinal()] = new TypeText(
                R.string.street_type_via_ferrata, R.string.street_type_via_ferrata_info);
        TYPE_TEXT[CompassStreetType.RACEWAY.ordinal()] = new TypeText(
                R.string.street_type_raceway, R.string.street_type_raceway_info);
        TYPE_TEXT[CompassStreetType.OTHER.ordinal()] = new TypeText(
                R.string.street_type_other, R.string.street_type_other_info);
        TYPE_TEXT[CompassStreetType.RAILWAY.ordinal()] = new TypeText(
                R.string.street_type_railway, R.string.street_type_railway_info);
        TYPE_TEXT[CompassStreetType.WATERWAY.ordinal()] = new TypeText(
                R.string.street_type_waterway, R.string.street_type_waterway_info);
        TYPE_TEXT[CompassStreetType.ROUTE_FERRY.ordinal()] = new TypeText(
                R.string.street_type_route_ferry, R.string.street_type_route_ferry_info);
        TYPE_TEXT[CompassStreetType.ROUTE_HIKING_FOOT.ordinal()] = new TypeText(
                R.string.street_type_route_hiking_foot, R.string.street_type_route_hiking_foot_info);
        TYPE_TEXT[CompassStreetType.ROUTE_BICYCLE.ordinal()] = new TypeText(
                R.string.street_type_route_bicycle, R.string.street_type_route_bicycle_info);
        TYPE_TEXT[CompassStreetType.ROUTE_SKI_PISTE.ordinal()] = new TypeText(
                R.string.street_type_route_ski_piste, R.string.street_type_route_ski_piste_info);
        TYPE_TEXT[CompassStreetType.ROUTE_MTB.ordinal()] = new TypeText(
                R.string.street_type_route_mtb, R.string.street_type_route_mtb_info);
        TYPE_TEXT[CompassStreetType.ROUTE_CANOE.ordinal()] = new TypeText(
                R.string.street_type_route_canoe, R.string.street_type_route_canoe_info);
        TYPE_TEXT[CompassStreetType.ROUTE_BUS.ordinal()] = new TypeText(
                R.string.street_type_route_bus, R.string.street_type_route_bus_info);
    }

    @NonNull
    private static TypeText typeText(@NonNull CompassStreetType type) {
        TypeText text = TYPE_TEXT[type.ordinal()];
        if (text == null) {
            throw new IllegalArgumentException("Street type has no selector row: " + type);
        }
        return text;
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

    private static final class TypeText {
        @StringRes final int labelResId;
        @StringRes final int descriptionResId;

        TypeText(@StringRes int labelResId, @StringRes int descriptionResId) {
            this.labelResId = labelResId;
            this.descriptionResId = descriptionResId;
        }
    }
}
