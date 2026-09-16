package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import vibro.navigator.nav.compass.CompassStreetType;

public class BRouterRd5StreetTagsTest {
    @Test
    public void streetType_decodesBRouterHighwayValues() {
        assertEquals(CompassStreetType.MOTORWAY, highwayType(14));
        assertEquals(CompassStreetType.BRIDLEWAY, highwayType(24));
        assertEquals(CompassStreetType.RACEWAY, highwayType(27));
        assertEquals(CompassStreetType.ELEVATOR, highwayType(34));
        assertEquals(CompassStreetType.VIA_FERRATA, highwayType(35));
    }

    @Test
    public void streetType_returnsNullForExtractionExcludedHighways() {
        assertNull(highwayType(23));
        assertNull(highwayType(26));
        assertNull(highwayType(29));
        assertNull(highwayType(32));
    }

    @Test
    public void streetType_classifiesEveryRailwayAndWaterwayValueAsSpecialRouting() {
        for (int railwayValue = 1; railwayValue <= 11; railwayValue++) {
            assertEquals(CompassStreetType.RAILWAY, streetType(
                    BRouterRd5StreetTags.RAILWAY_LOOKUP_INDEX, railwayValue
            ));
        }
        for (int waterwayValue = 1; waterwayValue <= 13; waterwayValue++) {
            assertEquals(CompassStreetType.WATERWAY, streetType(
                    BRouterRd5StreetTags.WATERWAY_LOOKUP_INDEX, waterwayValue
            ));
        }
    }

    @Test
    public void streetType_classifiesSelectedRoutesBeforeHighway() {
        int[] values = {2, 3, 4, 5, 6, 7, 9};
        CompassStreetType[] types = {
                CompassStreetType.ROUTE_FERRY,
                CompassStreetType.ROUTE_HIKING_FOOT,
                CompassStreetType.ROUTE_BICYCLE,
                CompassStreetType.ROUTE_SKI_PISTE,
                CompassStreetType.ROUTE_MTB,
                CompassStreetType.ROUTE_CANOE,
                CompassStreetType.ROUTE_BUS
        };
        for (int i = 0; i < values.length; i++) {
            assertEquals(types[i], streetType(
                    BRouterRd5StreetTags.HIGHWAY_LOOKUP_INDEX, 10,
                    BRouterRd5StreetTags.ROUTE_LOOKUP_INDEX, values[i]
            ));
        }
    }

    @Test
    public void streetType_givesRailwayAndWaterwayPriorityOverRouteAndHighway() {
        assertEquals(CompassStreetType.RAILWAY, streetType(
                BRouterRd5StreetTags.HIGHWAY_LOOKUP_INDEX, 8,
                BRouterRd5StreetTags.RAILWAY_LOOKUP_INDEX, 2,
                BRouterRd5StreetTags.ROUTE_LOOKUP_INDEX, 3
        ));
        assertEquals(CompassStreetType.WATERWAY, streetType(
                BRouterRd5StreetTags.HIGHWAY_LOOKUP_INDEX, 8,
                BRouterRd5StreetTags.WATERWAY_LOOKUP_INDEX, 2,
                BRouterRd5StreetTags.ROUTE_LOOKUP_INDEX, 4
        ));
    }

    @Test
    public void streetType_routeRoadDefersToHighwayOrGrayFallback() {
        assertEquals(CompassStreetType.RESIDENTIAL, streetType(
                BRouterRd5StreetTags.HIGHWAY_LOOKUP_INDEX, 2,
                BRouterRd5StreetTags.ROUTE_LOOKUP_INDEX, 8
        ));
        assertEquals(CompassStreetType.OTHER, streetType(
                BRouterRd5StreetTags.ROUTE_LOOKUP_INDEX, 8
        ));
    }

    @Test
    public void streetType_usesGrayFallbackWhenRoutingGeometryHasNoKnownBaseType() {
        assertEquals(CompassStreetType.OTHER, streetType(2, 2));
    }

    private static CompassStreetType highwayType(int highwayValueIndex) {
        return BRouterRd5StreetTags.streetTypeForHighwayValue(highwayValueIndex);
    }

    private static CompassStreetType streetType(int... lookupAndValuePairs) {
        byte[] data = new byte[64];
        Rd5VarBitsWriter writer = new Rd5VarBitsWriter(data);
        writer.reset();
        int nextLookupIndex = 1;
        for (int pairIndex = 0; pairIndex < lookupAndValuePairs.length; pairIndex += 2) {
            int lookupIndex = lookupAndValuePairs[pairIndex];
            int valueIndex = lookupAndValuePairs[pairIndex + 1];
            writer.encodeVarBits(lookupIndex - nextLookupIndex + 1);
            writer.encodeVarBits(encodeLookupValueIndex(valueIndex));
            nextLookupIndex = lookupIndex + 1;
        }
        writer.encodeVarBits(0);
        return BRouterRd5StreetTags.streetType(new Rd5TagValueCoder.TagValue(
                data,
                writer.closeAndGetEncodedLength()
        ));
    }

    private static int encodeLookupValueIndex(int valueIndex) {
        return valueIndex < 2 ? 7 : valueIndex < 9 ? valueIndex - 2 : valueIndex - 1;
    }
}
