package vibro.navigator.poi.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class OsmNominatimClientTest {
    @Test
    public void parseReverseDisplayName_returnsDisplayName() throws Exception {
        String body = "{\"display_name\":\"Stephansplatz, Vienna, Austria\"}";

        assertEquals("Stephansplatz, Vienna, Austria", OsmNominatimClient.parseReverseDisplayName(body));
    }

    @Test
    public void parseReverseDisplayName_returnsNullWhenMissing() throws Exception {
        assertNull(OsmNominatimClient.parseReverseDisplayName("{}"));
    }
}
