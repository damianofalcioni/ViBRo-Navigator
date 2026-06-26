package vibro.navigator.nav.compass.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.app.Activity;
import android.graphics.Paint;

import androidx.core.content.ContextCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.R;

@RunWith(RobolectricTestRunner.class)
public class NavigationCompassStreetRendererTest {
    @Test
    public void surroundingStreetOverlayUsesOrangeLinePaint() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        NavigationCompassStreetRenderer renderer = new NavigationCompassStreetRenderer();
        Paint streetPaint = renderer.paintForTest(activity);

        assertEquals(ContextCompat.getColor(activity, R.color.compass_accent), streetPaint.getColor());
        assertEquals(Paint.Style.STROKE, streetPaint.getStyle());
        assertEquals(180, streetPaint.getAlpha());
        assertNull(streetPaint.getPathEffect());
    }
}
