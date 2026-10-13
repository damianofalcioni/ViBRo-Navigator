package vibro.navigator.nav.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NavigationLandscapeTextLayoutRobolectricTest {
    @Test
    public void narrowLandscapeWrapsGpsAndCentersDirectionsAboveDestination() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            AndroidAppTheme.apply(activity);
            activity.setContentView(R.layout.activity_navigation);
            TextView gps = activity.findViewById(R.id.gpsStatusText);
            TextView next = activity.findViewById(R.id.nextDirectionText);
            TextView following = activity.findViewById(R.id.afterNextDirectionText);
            TextView destination = activity.findViewById(R.id.destinationText);
            View directions = activity.findViewById(R.id.turnInstructionRow);
            NavigationActivityTextScaling.configure(next, following, gps, activity.findViewById(R.id.speedLimitText));
            gps.setText("123 km/h ↑12450 m • 12.7 m • (32) • 15 s");
            next.setText("↱ 150 m • Turn right onto the next street towards the station");
            following.setText("↰ 300 m • Turn left at the next junction");
            destination.setText("13:51\n466 m remaining");
            layout(activity, 640, 360);

            assertFullyVisibleAndWrapped(gps);
            assertFullyVisibleAndWrapped(next);
            assertTrue(directions.getTop() >= gps.getBottom());
            assertTrue(directions.getBottom() <= destination.getTop());
            assertEquals(directions.getHeight() / 2f, (next.getTop() + following.getBottom()) / 2f, 2f);
            assertTrue(destination.getBottom() <= activity.findViewById(R.id.navigationActionRow).getTop());
        }
    }

    private static void assertFullyVisibleAndWrapped(TextView view) {
        int lines = view.getLayout().getLineCount();
        assertTrue("Text should wrap: " + view.getText(), lines > 1);
        assertEquals(view.getText().length(), view.getLayout().getLineEnd(lines - 1));
        assertEquals(0, view.getLayout().getEllipsisCount(lines - 1));
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
