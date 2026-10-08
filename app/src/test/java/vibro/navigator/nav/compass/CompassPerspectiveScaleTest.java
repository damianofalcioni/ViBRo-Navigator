package vibro.navigator.nav.compass;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CompassPerspectiveScaleTest {
    @Test
    public void orthographicTiltKeepsInclinationAndExpandsViewportByVerticalCompression() {
        float defaultViewport = 1f / 0.58f;
        for (float progress : new float[]{0f, 0.25f, 0.5f, 1f}) {
            assertEquals(1f - 0.42f * progress, CompassPerspectiveScale.verticalScale(progress), 0.0001f);
            assertEquals(1f, CompassPerspectiveScale.verticalScale(progress)
                    * CompassPerspectiveScale.viewportMultiplier(progress), 0.0001f);
        }
        assertEquals(defaultViewport, CompassPerspectiveScale.viewportMultiplier(1f), 0.0001f);
        assertTrue(CompassPerspectiveScale.maximumViewportMultiplier() > defaultViewport);
    }

    @Test
    public void preparedViewportCoversExtraInclinationWithPositiveVerticalScale() {
        float maximum = CompassPerspectiveScale.maximumViewportMultiplier();
        for (float progress : new float[]{0f, 0.25f, 1f, 1.125f, 1.25f}) {
            assertTrue(CompassPerspectiveScale.verticalScale(progress) > 0.45f);
            assertTrue(CompassPerspectiveScale.viewportMultiplier(progress) <= maximum + 0.0001f);
        }
        assertEquals(maximum, CompassPerspectiveScale.viewportMultiplier(1.25f), 0.0001f);
        assertTrue(CompassPerspectiveScale.verticalScale(1.25f) < CompassPerspectiveScale.verticalScale(1f));
    }

    @Test
    public void invalidAndExcessiveInclinationsAreBounded() {
        assertEquals(1.25f, CompassPerspectiveScale.clampProgress(10f), 0f);
        assertEquals(0f, CompassPerspectiveScale.clampProgress(-1f), 0f);
        assertEquals(0f, CompassPerspectiveScale.clampProgress(Float.NaN), 0f);
        assertEquals(0f, CompassPerspectiveScale.clampProgress(Float.POSITIVE_INFINITY), 0f);
    }
}
