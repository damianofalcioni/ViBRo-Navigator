package vibro.navigator.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import vibro.navigator.poi.Poi;

public class MapPickerIntentContractTest {
    @Test
    public void parseInitialPoi_rejectsInvalidCoordinates() {
        assertNull(MapPickerIntentContract.restorePoi("Invalid", 91.0d, 16.3738d));
    }

    @Test
    public void parseResult_rejectsInvalidCoordinates() {
        assertNull(MapPickerIntentContract.restoreResultPoi("Invalid", 48.2082d, 181.0d, "fallback"));
    }

    @Test
    public void parseResult_usesFallbackNameWhenResultNameIsBlank() {
        Poi poi = MapPickerIntentContract.restoreResultPoi(" ", 48.2082d, 16.3738d, "48.208200, 16.373800");

        assertEquals("48.208200, 16.373800", poi.name);
    }
}
