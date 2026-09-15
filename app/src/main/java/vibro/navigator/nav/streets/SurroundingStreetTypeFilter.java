package vibro.navigator.nav.streets;

import androidx.annotation.NonNull;

import vibro.navigator.nav.compass.CompassStreetCategory;
import vibro.navigator.nav.compass.CompassStreetSegment;
import vibro.navigator.nav.policy.NavigationSpeedBucket;

final class SurroundingStreetTypeFilter {
    boolean isVisible(
            @NonNull CompassStreetSegment segment,
            @NonNull NavigationSpeedBucket bucket
    ) {
        switch (bucket) {
            case LOW:
                return true;
            case MEDIUM:
                return segment.type.category() != CompassStreetCategory.WALKING_CYCLING;
            case HIGH:
                return segment.type.category() == CompassStreetCategory.HIGHWAY
                        || segment.type.category() == CompassStreetCategory.SPECIAL_ROUTING;
            default:
                throw new IllegalArgumentException("Unknown surrounding street speed bucket: " + bucket);
        }
    }
}
