package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import vibro.navigator.nav.compass.CompassStreetType;

public class BRouterRd5HighwayTagsTest {
    @Test
    public void streetType_decodesBRouterHighwayValues() {
        assertEquals(CompassStreetType.MOTORWAY, streetType(14));
        assertEquals(CompassStreetType.RACEWAY, streetType(27));
        assertEquals(CompassStreetType.ELEVATOR, streetType(34));
        assertEquals(CompassStreetType.VIA_FERRATA, streetType(35));
    }

    @Test
    public void streetType_returnsNullForExtractionExcludedHighways() {
        assertNull(streetType(23));
        assertNull(streetType(26));
        assertNull(streetType(29));
        assertNull(streetType(32));
    }

    @Test
    public void streetType_usesOtherWhenHighwayTagIsMissing() {
        assertEquals(CompassStreetType.OTHER, BRouterRd5HighwayTags.streetType(tagValue(2, 2)));
    }

    private static CompassStreetType streetType(int highwayValueIndex) {
        return BRouterRd5HighwayTags.streetType(tagValue(1, highwayValueIndex));
    }

    private static Rd5TagValueCoder.TagValue tagValue(int lookupDelta, int valueIndex) {
        byte[] data = new byte[16];
        Rd5VarBitsWriter writer = new Rd5VarBitsWriter(data);
        writer.reset();
        writer.encodeVarBits(lookupDelta);
        writer.encodeVarBits(encodeLookupValueIndex(valueIndex));
        writer.encodeVarBits(0);
        return new Rd5TagValueCoder.TagValue(data, writer.closeAndGetEncodedLength());
    }

    private static int encodeLookupValueIndex(int valueIndex) {
        return valueIndex < 2 ? 7 : valueIndex < 9 ? valueIndex - 2 : valueIndex - 1;
    }
}
