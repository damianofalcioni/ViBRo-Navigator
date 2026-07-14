package vibro.navigator.poi.ui;

import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;

import vibro.navigator.poi.PoiDetails;

@RunWith(RobolectricTestRunner.class)
public class PoiDetailsFormatterTest {
    @Test
    public void format_includesEntrancesFromOriginalResultDetails() {
        Context context = ApplicationProvider.getApplicationContext();
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

        String formatted = PoiDetailsFormatter.format(context, details);

        assertTrue(formatted.contains("Entrances"));
        assertTrue(formatted.contains("Entrance 1"));
        assertTrue(formatted.contains("Coordinates: 48.200000, 16.300000"));
        assertTrue(formatted.contains("wheelchair: yes"));
    }
}
