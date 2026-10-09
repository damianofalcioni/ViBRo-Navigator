package vibro.navigator.nav.ui;

import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class NavigationTextSizeRobolectricTest {
    @Test
    public void portraitKeepsTextSizeWhenSwitchingCompassSurface() {
        assertStableTextSize(400, 800);
    }

    @Test
    @Config(qualifiers = "land")
    public void landscapeKeepsTextSizeWhenSwitchingCompassSurface() {
        assertStableTextSize(800, 400);
    }

    private static void assertStableTextSize(int width, int height) {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            AndroidAppTheme.apply(activity);
            activity.setContentView(R.layout.activity_navigation);
            NavigationCompassSurfaces surfaces = new NavigationCompassSurfaces(activity,
                    activity.findViewById(R.id.turnInstructionRow), activity.findViewById(R.id.destinationText));
            TextView gps = activity.findViewById(R.id.gpsStatusText);
            TextView next = activity.findViewById(R.id.nextDirectionText);
            TextView following = activity.findViewById(R.id.afterNextDirectionText);
            surfaces.includeForegroundText(gps);
            NavigationActivityTextScaling.configure(next, following, gps, activity.findViewById(R.id.speedLimitText));
            gps.setText("12 km/h ↑450 m • 6 m • (12) • 15 s");
            next.setText("↱ 150 m • Turn right onto the next street");
            following.setText("↰ 300 m • Turn left at the next junction");
            surfaces.render(false, false, 0f, null);
            layout(activity, width, height);
            float gpsSize = gps.getTextSize();
            float nextSize = next.getTextSize();
            float followingSize = following.getTextSize();
            int gpsWidth = textWidth(gps);
            int nextWidth = textWidth(next);

            surfaces.render(true, false, 0f, null);
            layout(activity, width, height);
            assertMatchingPanelPadding(activity);
            assertEquals(gpsWidth, textWidth(gps));
            assertEquals(nextWidth, textWidth(next));
            assertEquals(gpsSize, gps.getTextSize(), 0f);
            assertEquals(nextSize, next.getTextSize(), 0f);
            assertEquals(followingSize, following.getTextSize(), 0f);

            surfaces.render(false, false, 0f, null);
            layout(activity, width, height);
            assertEquals(gpsSize, gps.getTextSize(), 0f);
            assertEquals(nextSize, next.getTextSize(), 0f);
            assertEquals(followingSize, following.getTextSize(), 0f);
        }
    }

    private static void assertMatchingPanelPadding(Activity activity) {
        View statistics = activity.findViewById(R.id.destinationText);
        View directions = activity.findViewById(R.id.turnInstructionRow);
        View gps = activity.findViewById(R.id.gpsStatusText);
        assertEquals(statistics.getPaddingLeft(), gps.getPaddingLeft());
        assertEquals(statistics.getPaddingTop(), gps.getPaddingTop());
        assertEquals(statistics.getPaddingRight(), gps.getPaddingRight());
        assertEquals(statistics.getPaddingBottom(), gps.getPaddingBottom());
        assertEquals(statistics.getPaddingLeft(), directions.getPaddingLeft());
        assertEquals(statistics.getPaddingTop(), directions.getPaddingTop());
        assertEquals(statistics.getPaddingRight(), directions.getPaddingRight());
        assertEquals(statistics.getPaddingBottom(), directions.getPaddingBottom());
    }

    private static int textWidth(TextView view) {
        return view.getWidth() - view.getCompoundPaddingLeft() - view.getCompoundPaddingRight();
    }

    private static void layout(Activity activity, int width, int height) {
        View content = activity.findViewById(android.R.id.content);
        for (int pass = 0; pass < 3; pass++) {
            content.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            content.layout(0, 0, width, height);
        }
    }
}
