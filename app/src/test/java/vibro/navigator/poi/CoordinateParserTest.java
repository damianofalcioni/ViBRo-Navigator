package vibro.navigator.poi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CoordinateParserTest {

    @Test
    public void tryParse_acceptsExplicitPositiveSigns() {
        Poi poi = CoordinateParser.tryParse("+48.2082,+16.3738", null);

        assertNotNull(poi);
        assertEquals(48.2082, poi.lat, 0.0);
        assertEquals(16.3738, poi.lon, 0.0);
    }

    @Test
    public void tryParse_rejectsOutOfRangeCoordinates() {
        assertNull(CoordinateParser.tryParse("+91.0,+16.3738", null));
    }
}
