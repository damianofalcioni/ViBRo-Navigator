package vibro.navigator.poi.ui;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;

import vibro.navigator.R;
import vibro.navigator.poi.PoiDetails;

public class PoiDetailsFormatterTest {
    private final PoiTextResources textResources = new TestPoiTextResources();

    @Test
    public void format_includesEntrancesFromOriginalResultDetails() {
        PoiDetails details = new PoiDetails(
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.singletonList(new PoiDetails.Entrance(
                        48.2d,
                        16.3d,
                        "main",
                        Collections.singletonMap("wheelchair", "yes")
                ))
        );

        String formatted = PoiDetailsFormatter.format(textResources, details);

        assertTrue(formatted.contains("Entrances"));
        assertTrue(formatted.contains("Entrance 1"));
        assertTrue(formatted.contains("Coordinates: 48.200000, 16.300000"));
        assertTrue(formatted.contains("wheelchair: yes"));
        assertTrue(formatted.endsWith(textResources.getString(R.string.msg_poi_details_map_check_hint)));
    }
}
