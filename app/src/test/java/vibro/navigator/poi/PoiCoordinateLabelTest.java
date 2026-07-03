package vibro.navigator.poi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PoiCoordinateLabelTest {
    @Test
    public void isCoordinateLabel_acceptsMatchingCoordinateName() {
        assertTrue(PoiCoordinateLabel.isCoordinateLabel(
                new Poi("48.208200, 16.373800", 48.2082d, 16.3738d)
        ));
    }

    @Test
    public void isCoordinateLabel_rejectsNamedPoi() {
        assertFalse(PoiCoordinateLabel.isCoordinateLabel(
                new Poi("Stephansplatz, Vienna", 48.2082d, 16.3738d)
        ));
    }

    @Test
    public void isCoordinateLabel_rejectsDifferentCoordinatesInName() {
        assertFalse(PoiCoordinateLabel.isCoordinateLabel(
                new Poi("45.464200, 9.190000", 48.2082d, 16.3738d)
        ));
    }
}
