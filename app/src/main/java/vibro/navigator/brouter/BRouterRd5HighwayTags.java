package vibro.navigator.brouter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.compass.CompassStreetType;

final class BRouterRd5HighwayTags {
    private static final int HIGHWAY_LOOKUP_INDEX = 1;
    private static final int HIGHWAY_MISSING = 0;
    private static final int HIGHWAY_CONSTRUCTION = 23;
    private static final int HIGHWAY_PROPOSED_PLANNED_VIRTUAL = 26;
    private static final int HIGHWAY_ABANDONED_DISUSED_NO = 29;
    private static final int HIGHWAY_BUS_STOP = 32;
    private static final CompassStreetType[] STREET_TYPES = new CompassStreetType[36];
    private static final boolean[] HARD_EXCLUDED = new boolean[STREET_TYPES.length];

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
    }

    private BRouterRd5HighwayTags() {
    }

    @Nullable
    static CompassStreetType streetType(@Nullable Rd5TagValueCoder.TagValue tags) {
        return streetTypeForHighwayValue(highwayValueIndex(tags));
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

    private static int highwayValueIndex(@Nullable Rd5TagValueCoder.TagValue tags) {
        if (tags == null) {
            return HIGHWAY_MISSING;
        }
        return decodeHighwayValueIndex(tags.data());
    }

    private static int decodeHighwayValueIndex(@NonNull byte[] data) {
        Rd5BitCoderContext context = new Rd5BitCoderContext(data);
        int lookupIndex = 1;
        for (;;) {
            int delta = context.decodeVarBits();
            if (delta == 0) {
                return HIGHWAY_MISSING;
            }
            int targetLookupIndex = lookupIndex + delta - 1;
            int valueIndex = decodeLookupValueIndex(context.decodeVarBits());
            if (targetLookupIndex == HIGHWAY_LOOKUP_INDEX) {
                return valueIndex;
            }
            if (targetLookupIndex > HIGHWAY_LOOKUP_INDEX) {
                return HIGHWAY_MISSING;
            }
            lookupIndex = targetLookupIndex + 1;
        }
    }

    private static int decodeLookupValueIndex(int encodedValueIndex) {
        return encodedValueIndex == 7
                ? 1
                : encodedValueIndex < 7 ? encodedValueIndex + 2 : encodedValueIndex + 1;
    }
}
