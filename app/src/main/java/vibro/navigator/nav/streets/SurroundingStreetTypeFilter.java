package vibro.navigator.nav.streets;

import androidx.annotation.NonNull;

import java.util.EnumSet;
import java.util.Set;

import vibro.navigator.nav.compass.CompassStreetSegment;
import vibro.navigator.nav.compass.CompassStreetType;

final class SurroundingStreetTypeFilter {
    @NonNull
    private static final Set<CompassStreetType> LOW_SPEED_TYPES = EnumSet.allOf(CompassStreetType.class);
    @NonNull
    private static final Set<CompassStreetType> MEDIUM_SPEED_TYPES = EnumSet.of(
            CompassStreetType.MOTORWAY,
            CompassStreetType.MOTORWAY_LINK,
            CompassStreetType.TRUNK,
            CompassStreetType.TRUNK_LINK,
            CompassStreetType.PRIMARY,
            CompassStreetType.PRIMARY_LINK,
            CompassStreetType.SECONDARY,
            CompassStreetType.SECONDARY_LINK,
            CompassStreetType.TERTIARY,
            CompassStreetType.TERTIARY_LINK,
            CompassStreetType.UNCLASSIFIED,
            CompassStreetType.RESIDENTIAL,
            CompassStreetType.SERVICE,
            CompassStreetType.ROAD,
            CompassStreetType.BUSWAY,
            CompassStreetType.REST_AREA,
            CompassStreetType.SERVICES,
            CompassStreetType.RACEWAY
    );
    @NonNull
    private static final Set<CompassStreetType> HIGH_SPEED_TYPES = EnumSet.of(
            CompassStreetType.MOTORWAY,
            CompassStreetType.MOTORWAY_LINK,
            CompassStreetType.TRUNK,
            CompassStreetType.TRUNK_LINK,
            CompassStreetType.PRIMARY,
            CompassStreetType.PRIMARY_LINK,
            CompassStreetType.SECONDARY,
            CompassStreetType.SECONDARY_LINK,
            CompassStreetType.REST_AREA,
            CompassStreetType.SERVICES,
            CompassStreetType.RACEWAY
    );

    boolean isVisible(
            @NonNull CompassStreetSegment segment,
            @NonNull SurroundingStreetSpeedBucket bucket
    ) {
        return visibleTypes(bucket).contains(segment.type);
    }

    @NonNull
    private static Set<CompassStreetType> visibleTypes(@NonNull SurroundingStreetSpeedBucket bucket) {
        switch (bucket) {
            case LOW:
                return LOW_SPEED_TYPES;
            case MEDIUM:
                return MEDIUM_SPEED_TYPES;
            case HIGH:
                return HIGH_SPEED_TYPES;
            default:
                throw new IllegalArgumentException("Unknown surrounding street speed bucket: " + bucket);
        }
    }
}
