package vibro.navigator.map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MapPickerBoundsTest {

    @Test
    public void isReadyForPoiFetch_acceptsFiniteWorldLongitudeBounds() {
        assertTrue(MapPickerBounds.of(48.0d, -180.0d, 49.0d, 180.0d, 12).isReadyForPoiFetch());
    }

    @Test
    public void isReadyForPoiFetch_rejectsNonFiniteCoordinates() {
        assertFalse(MapPickerBounds.of(48.0d, 16.0d, 49.0d, Double.POSITIVE_INFINITY, 12)
                .isReadyForPoiFetch());
        assertFalse(MapPickerBounds.of(Double.NaN, 16.0d, 49.0d, 17.0d, 12)
                .isReadyForPoiFetch());
    }

    @Test
    public void isReadyForPoiFetch_rejectsOutOfRangeLongitudeBounds() {
        assertFalse(MapPickerBounds.of(48.0d, -181.0d, 49.0d, 17.0d, 12).isReadyForPoiFetch());
        assertFalse(MapPickerBounds.of(48.0d, 16.0d, 49.0d, 181.0d, 12).isReadyForPoiFetch());
    }
}
