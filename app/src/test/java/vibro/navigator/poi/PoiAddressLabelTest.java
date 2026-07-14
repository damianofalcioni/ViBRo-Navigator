package vibro.navigator.poi;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

public class PoiAddressLabelTest {
    @Test
    public void conciseLabel_keepsPlaceStreetAndCity() {
        Map<String, String> address = new LinkedHashMap<>();
        address.put("amenity", "Cafe Central");
        address.put("road", "Herrengasse");
        address.put("house_number", "14");
        address.put("suburb", "Innere Stadt");
        address.put("city", "Vienna");
        address.put("state", "Vienna");
        address.put("postcode", "1010");
        address.put("country", "Austria");

        String label = PoiAddressLabel.conciseLabel(
                "Cafe Central, Herrengasse 14, Innere Stadt, Vienna, Austria",
                address
        );

        assertEquals("Cafe Central, Herrengasse 14, Vienna", label);
    }

    @Test
    public void conciseLabel_removesProviderTailForPlainAddress() {
        Map<String, String> address = new LinkedHashMap<>();
        address.put("house_number", "1600");
        address.put("road", "Amphitheatre Parkway");
        address.put("city", "Mountain View");
        address.put("state", "California");
        address.put("postcode", "94043");
        address.put("country", "United States");

        String label = PoiAddressLabel.conciseLabel(
                "1600 Amphitheatre Parkway, Mountain View, CA 94043, USA",
                address
        );

        assertEquals("1600 Amphitheatre Parkway, Mountain View", label);
    }

    @Test
    public void conciseLabel_fallsBackWhenAddressDetailsAreMissing() {
        String label = PoiAddressLabel.conciseLabel(
                "Stephansplatz, Vienna, Austria",
                new LinkedHashMap<>()
        );

        assertEquals("Stephansplatz, Vienna, Austria", label);
    }
}
