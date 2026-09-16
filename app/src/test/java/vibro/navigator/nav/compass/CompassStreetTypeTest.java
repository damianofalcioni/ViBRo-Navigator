package vibro.navigator.nav.compass;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

public class CompassStreetTypeTest {
    @Test
    public void categoriesFollowCumulativeSpeedFilterLayers() {
        assertCategory(
                CompassStreetCategory.HIGHWAY,
                CompassStreetType.MOTORWAY,
                CompassStreetType.MOTORWAY_LINK,
                CompassStreetType.TRUNK,
                CompassStreetType.TRUNK_LINK,
                CompassStreetType.PRIMARY,
                CompassStreetType.PRIMARY_LINK,
                CompassStreetType.SECONDARY,
                CompassStreetType.SECONDARY_LINK,
                CompassStreetType.REST_AREA,
                CompassStreetType.SERVICES
        );
        assertCategory(
                CompassStreetCategory.NORMAL,
                CompassStreetType.TERTIARY,
                CompassStreetType.TERTIARY_LINK,
                CompassStreetType.UNCLASSIFIED,
                CompassStreetType.RESIDENTIAL,
                CompassStreetType.SERVICE,
                CompassStreetType.ROAD
        );
        assertCategory(
                CompassStreetCategory.WALKING_CYCLING,
                CompassStreetType.LIVING_STREET,
                CompassStreetType.TRACK,
                CompassStreetType.PEDESTRIAN,
                CompassStreetType.FOOTWAY,
                CompassStreetType.PATH,
                CompassStreetType.CYCLEWAY,
                CompassStreetType.STEPS,
                CompassStreetType.PLATFORM,
                CompassStreetType.CORRIDOR,
                CompassStreetType.ELEVATOR,
                CompassStreetType.ROUTE_WALKING_CYCLING,
                CompassStreetType.ROUTE_HIKING_FOOT,
                CompassStreetType.ROUTE_BICYCLE,
                CompassStreetType.ROUTE_MTB
        );
        assertCategory(
                CompassStreetCategory.SPECIAL_ROUTING,
                CompassStreetType.BRIDLEWAY,
                CompassStreetType.VIA_FERRATA,
                CompassStreetType.RACEWAY,
                CompassStreetType.BUSWAY,
                CompassStreetType.OTHER,
                CompassStreetType.RAILWAY,
                CompassStreetType.WATERWAY,
                CompassStreetType.SPECIAL_ROUTE,
                CompassStreetType.ROUTE_FERRY,
                CompassStreetType.ROUTE_SKI_PISTE,
                CompassStreetType.ROUTE_CANOE,
                CompassStreetType.ROUTE_BUS
        );
    }

    private static void assertCategory(
            CompassStreetCategory category,
            CompassStreetType... expectedTypes
    ) {
        Set<CompassStreetType> expected = EnumSet.copyOf(Arrays.asList(expectedTypes));
        Set<CompassStreetType> actual = EnumSet.noneOf(CompassStreetType.class);
        for (CompassStreetType type : CompassStreetType.values()) {
            if (type.category() == category) {
                actual.add(type);
            }
        }
        assertEquals(expected, actual);
    }
}
