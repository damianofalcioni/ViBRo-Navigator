package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;

import androidx.core.content.ContextCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.R;

@RunWith(RobolectricTestRunner.class)
public class MainRoundTripDirectionCompassViewTest {
    @Test
    public void statusRingIsGreenForUsableHeadingAccuracyAndRedForPoorHeadingAccuracy() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        MainRoundTripDirectionCompassView compassView = new MainRoundTripDirectionCompassView(activity);

        compassView.setHeading(44.6, 10f);

        assertTrue(compassView.isHeadingAccuracyOkForTest());
        assertEquals(Float.valueOf(45f), compassView.headingDegreesForTest());
        assertEquals(ContextCompat.getColor(activity, R.color.success), compassView.statusColorForTest());

        compassView.setHeading(44.6, 35f);

        assertFalse(compassView.isHeadingAccuracyOkForTest());
        assertEquals(ContextCompat.getColor(activity, R.color.danger), compassView.statusColorForTest());
    }

    @Test
    public void statusRingIsRedWithoutHeading() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        MainRoundTripDirectionCompassView compassView = new MainRoundTripDirectionCompassView(activity);

        compassView.clearHeading();

        assertFalse(compassView.isHeadingAccuracyOkForTest());
        assertEquals(ContextCompat.getColor(activity, R.color.danger), compassView.statusColorForTest());
    }

    @Test
    public void statusRingUsesSameStrokeWidthAsHeadingGuide() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        MainRoundTripDirectionCompassView compassView = new MainRoundTripDirectionCompassView(activity);

        assertEquals(
                compassView.headingGuideStrokeWidthForTest(),
                compassView.statusRingStrokeWidthForTest(),
                0.01f
        );
    }

    @Test
    public void cardinalLettersSitInsideCardinalTickMarksWithVisibleGap() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        MainRoundTripDirectionCompassView compassView = new MainRoundTripDirectionCompassView(activity);
        float radius = 100f;
        float cardinalTickInnerRadius = 62f;

        assertTrue(compassView.cardinalOrbitRadiusForTest(radius) < cardinalTickInnerRadius);
        assertEquals(44f, compassView.cardinalOrbitRadiusForTest(radius), 0.01f);
    }
}
