package vibro.navigator.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class MapPickerCenterTest {

    @Test
    public void parseJavascriptResult_readsCenterCoordinates() throws Exception {
        MapPickerCenter center = MapPickerCenter.parseJavascriptResult(
                "\"{\\\"centerLat\\\":48.2082,\\\"centerLon\\\":16.3738}\""
        );

        assertEquals(48.2082d, center.lat, 0.0);
        assertEquals(16.3738d, center.lon, 0.0);
    }

    @Test
    public void parseJavascriptResult_returnsNullWhenCenterIsInvalid() throws Exception {
        MapPickerCenter center = MapPickerCenter.parseJavascriptResult(
                "\"{\\\"centerLat\\\":91.0,\\\"centerLon\\\":16.3738}\""
        );

        assertNull(center);
    }

    @Test
    public void parseJavascriptResult_returnsNullWhenJavascriptIsNotReady() throws Exception {
        assertNull(MapPickerCenter.parseJavascriptResult(null));
        assertNull(MapPickerCenter.parseJavascriptResult("null"));
    }
}
