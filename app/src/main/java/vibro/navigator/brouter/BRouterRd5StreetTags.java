package vibro.navigator.brouter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.compass.CompassStreetType;

/** Decodes the routing-geometry lookup keys from BRouter lookup version 11 way tags. */
final class BRouterRd5StreetTags {
    static final int HIGHWAY_LOOKUP_INDEX = 1;
    static final int RAILWAY_LOOKUP_INDEX = 21;
    static final int WATERWAY_LOOKUP_INDEX = 60;
    static final int ROUTE_LOOKUP_INDEX = 63;

    private static final int TAG_MISSING = 0;
    private static final int HIGHWAY_CONSTRUCTION = 23;
    private static final int HIGHWAY_PROPOSED_PLANNED_VIRTUAL = 26;
    private static final int HIGHWAY_ABANDONED_DISUSED_NO = 29;
    private static final int HIGHWAY_BUS_STOP = 32;
    private static final int ROUTE_FERRY = 2;
    private static final int ROUTE_HIKING_FOOT = 3;
    private static final int ROUTE_BICYCLE = 4;
    private static final int ROUTE_SKI_PISTE = 5;
    private static final int ROUTE_MTB = 6;
    private static final int ROUTE_CANOE = 7;
    private static final int ROUTE_BUS = 9;
    private static final int LOOKUP_VALUE_BITS = 16;
    private static final int HIGHWAY_VALUE_SHIFT = 0;
    private static final int RAILWAY_VALUE_SHIFT = LOOKUP_VALUE_BITS;
    private static final int WATERWAY_VALUE_SHIFT = LOOKUP_VALUE_BITS * 2;
    private static final int ROUTE_VALUE_SHIFT = LOOKUP_VALUE_BITS * 3;
    private static final long LOOKUP_VALUE_MASK = (1L << LOOKUP_VALUE_BITS) - 1L;
    private static final CompassStreetType[] STREET_TYPES = new CompassStreetType[36];
    private static final boolean[] HARD_EXCLUDED = new boolean[STREET_TYPES.length];
    private static final CompassStreetType[] ROUTE_TYPES = new CompassStreetType[10];

    static {
        STREET_TYPES[2] = CompassStreetType.RESIDENTIAL;
        STREET_TYPES[3] = CompassStreetType.SERVICE;
        STREET_TYPES[4] = CompassStreetType.TRACK;
        STREET_TYPES[5] = CompassStreetType.UNCLASSIFIED;
        STREET_TYPES[6] = CompassStreetType.FOOTWAY;
        STREET_TYPES[7] = CompassStreetType.TERTIARY;
        STREET_TYPES[8] = CompassStreetType.PATH;
        STREET_TYPES[9] = CompassStreetType.SECONDARY;
        STREET_TYPES[10] = CompassStreetType.PRIMARY;
        STREET_TYPES[11] = CompassStreetType.CYCLEWAY;
        STREET_TYPES[12] = CompassStreetType.TRUNK;
        STREET_TYPES[13] = CompassStreetType.LIVING_STREET;
        STREET_TYPES[14] = CompassStreetType.MOTORWAY;
        STREET_TYPES[15] = CompassStreetType.MOTORWAY_LINK;
        STREET_TYPES[16] = CompassStreetType.STEPS;
        STREET_TYPES[17] = CompassStreetType.ROAD;
        STREET_TYPES[18] = CompassStreetType.PEDESTRIAN;
        STREET_TYPES[19] = CompassStreetType.TRUNK_LINK;
        STREET_TYPES[20] = CompassStreetType.PRIMARY_LINK;
        STREET_TYPES[21] = CompassStreetType.SECONDARY_LINK;
        STREET_TYPES[22] = CompassStreetType.TERTIARY_LINK;
        STREET_TYPES[24] = CompassStreetType.BRIDLEWAY;
        STREET_TYPES[25] = CompassStreetType.PLATFORM;
        STREET_TYPES[27] = CompassStreetType.RACEWAY;
        STREET_TYPES[28] = CompassStreetType.REST_AREA;
        STREET_TYPES[30] = CompassStreetType.SERVICES;
        STREET_TYPES[31] = CompassStreetType.CORRIDOR;
        STREET_TYPES[33] = CompassStreetType.BUSWAY;
        STREET_TYPES[34] = CompassStreetType.ELEVATOR;
        STREET_TYPES[35] = CompassStreetType.VIA_FERRATA;
        HARD_EXCLUDED[HIGHWAY_CONSTRUCTION] = true;
        HARD_EXCLUDED[HIGHWAY_PROPOSED_PLANNED_VIRTUAL] = true;
        HARD_EXCLUDED[HIGHWAY_ABANDONED_DISUSED_NO] = true;
        HARD_EXCLUDED[HIGHWAY_BUS_STOP] = true;
        ROUTE_TYPES[ROUTE_FERRY] = CompassStreetType.ROUTE_FERRY;
        ROUTE_TYPES[ROUTE_HIKING_FOOT] = CompassStreetType.ROUTE_HIKING_FOOT;
        ROUTE_TYPES[ROUTE_BICYCLE] = CompassStreetType.ROUTE_BICYCLE;
        ROUTE_TYPES[ROUTE_SKI_PISTE] = CompassStreetType.ROUTE_SKI_PISTE;
        ROUTE_TYPES[ROUTE_MTB] = CompassStreetType.ROUTE_MTB;
        ROUTE_TYPES[ROUTE_CANOE] = CompassStreetType.ROUTE_CANOE;
        ROUTE_TYPES[ROUTE_BUS] = CompassStreetType.ROUTE_BUS;
    }

    private BRouterRd5StreetTags() {
    }

    @Nullable
    static CompassStreetType streetType(@Nullable Rd5TagValueCoder.TagValue tags) {
        long values = tags == null ? 0L : decodeRoutingLookupValues(tags.data());
        return classify(
                lookupValue(values, HIGHWAY_VALUE_SHIFT),
                lookupValue(values, RAILWAY_VALUE_SHIFT),
                lookupValue(values, WATERWAY_VALUE_SHIFT),
                lookupValue(values, ROUTE_VALUE_SHIFT)
        );
    }

    private static long decodeRoutingLookupValues(@NonNull byte[] data) {
        Rd5BitCoderContext context = new Rd5BitCoderContext(data);
        int lookupIndex = 1;
        long values = 0L;
        for (;;) {
            int delta = context.decodeVarBits();
            if (delta == 0) {
                return values;
            }
            int targetLookupIndex = lookupIndex + delta - 1;
            int valueIndex = decodeLookupValueIndex(context.decodeVarBits());
            int shift = lookupValueShift(targetLookupIndex);
            if (shift >= 0) {
                values |= ((long) valueIndex & LOOKUP_VALUE_MASK) << shift;
            }
            if (targetLookupIndex > ROUTE_LOOKUP_INDEX) {
                return values;
            }
            lookupIndex = targetLookupIndex + 1;
        }
    }

    private static int lookupValueShift(int lookupIndex) {
        switch (lookupIndex) {
            case HIGHWAY_LOOKUP_INDEX:
                return HIGHWAY_VALUE_SHIFT;
            case RAILWAY_LOOKUP_INDEX:
                return RAILWAY_VALUE_SHIFT;
            case WATERWAY_LOOKUP_INDEX:
                return WATERWAY_VALUE_SHIFT;
            case ROUTE_LOOKUP_INDEX:
                return ROUTE_VALUE_SHIFT;
            default:
                return -1;
        }
    }

    private static int lookupValue(long values, int shift) {
        return (int) ((values >>> shift) & LOOKUP_VALUE_MASK);
    }

    @Nullable
    private static CompassStreetType classify(
            int highwayValueIndex,
            int railwayValueIndex,
            int waterwayValueIndex,
            int routeValueIndex
    ) {
        if (railwayValueIndex != TAG_MISSING) {
            return CompassStreetType.RAILWAY;
        }
        if (waterwayValueIndex != TAG_MISSING) {
            return CompassStreetType.WATERWAY;
        }
        CompassStreetType routeType = streetTypeForRouteValue(routeValueIndex);
        return routeType == null ? streetTypeForHighwayValue(highwayValueIndex) : routeType;
    }

    @Nullable
    private static CompassStreetType streetTypeForRouteValue(int routeValueIndex) {
        return routeValueIndex >= 0 && routeValueIndex < ROUTE_TYPES.length
                ? ROUTE_TYPES[routeValueIndex]
                : null;
    }

    @Nullable
    static CompassStreetType streetTypeForHighwayValue(int highwayValueIndex) {
        if (isHardExcluded(highwayValueIndex)) {
            return null;
        }
        CompassStreetType type = streetTypeOrNull(highwayValueIndex);
        return type == null ? CompassStreetType.OTHER : type;
    }

    private static boolean isHardExcluded(int highwayValueIndex) {
        return highwayValueIndex >= 0
                && highwayValueIndex < HARD_EXCLUDED.length
                && HARD_EXCLUDED[highwayValueIndex];
    }

    @Nullable
    private static CompassStreetType streetTypeOrNull(int highwayValueIndex) {
        return highwayValueIndex >= 0 && highwayValueIndex < STREET_TYPES.length
                ? STREET_TYPES[highwayValueIndex]
                : null;
    }

    private static int decodeLookupValueIndex(int encodedValueIndex) {
        return encodedValueIndex == 7
                ? 1
                : encodedValueIndex < 7 ? encodedValueIndex + 2 : encodedValueIndex + 1;
    }
}
