package vibro.navigator.map;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class OsmMapPoiMarkerParserTest {
    @Test
    public void parse_ignoresElementsWithInvalidCoordinates() throws Exception {
        MapPoiCategory category = MapPoiCategoryFilter.fromNames(Collections.singletonList("Fuel")).get(0);
        String body = "{"
                + "\"elements\":["
                + "{\"lat\":91.0,\"lon\":16.3738,\"tags\":{\"amenity\":\"fuel\",\"name\":\"Invalid\"}},"
                + "{\"lat\":48.2082,\"lon\":16.3738,\"tags\":{\"amenity\":\"fuel\",\"name\":\"Valid\"}}"
                + "]"
                + "}";

        List<MapPoiMarker> markers = new OsmMapPoiMarkerParser().parse(
                body,
                Collections.singletonList(category)
        );

        assertEquals(1, markers.size());
        assertEquals("Valid", markers.get(0).name);
    }
}
