package vibro.navigator.nav.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.time.Duration;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NavigationInstructionMetricRobolectricTest {
    @Test
    public void portraitAlternatesBothRowsWithoutShiftingTheirInstructions() {
        assertAlternatingRows();
    }

    @Test
    @Config(qualifiers = "land-mdpi")
    public void landscapeAlternatesBothRowsWithoutShiftingTheirInstructions() {
        assertAlternatingRows();
    }

    private static void assertAlternatingRows() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            AndroidAppTheme.apply(activity);
            activity.setContentView(R.layout.activity_navigation);
            controller.windowFocusChanged(true);
            for (int id : new int[]{R.id.nextDirectionText, R.id.afterNextDirectionText}) {
                render(activity, id, "↰ 20m - 5s - Turn left");
            }
            layout(activity);
            TextView prefix = activity.findViewById(R.id.nextDirectionPrefixText);
            TextView following = activity.findViewById(R.id.afterNextDirectionPrefixText);
            TextView instruction = activity.findViewById(R.id.nextDirectionText);
            TextView separator = activity.findViewById(R.id.nextDirectionSeparatorText);
            int width = prefix.getWidth();
            int separatorLeft = separator.getLeft();
            int instructionLeft = ((View) instruction.getParent()).getLeft();
            assertTrue(width > 0);
            assertEquals("↰ 20m", prefix.getText().toString());
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3));
            layout(activity);
            assertEquals("↰ 5s", prefix.getText().toString());
            assertEquals("↰ 5s", following.getText().toString());
            assertEquals(width, prefix.getWidth());
            assertEquals(" - ", separator.getText().toString());
            assertEquals(separatorLeft, separator.getLeft());
            assertEquals(instructionLeft, ((View) instruction.getParent()).getLeft());
            render(activity, R.id.nextDirectionText, "↰ 18m - 4s - Turn left");
            assertEquals("↰ 4s", prefix.getText().toString());
            assertEquals("↰ 18m - 4s - Turn left", instruction.getContentDescription());
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3));
            assertEquals("↰ 18m", prefix.getText().toString());
            controller.windowFocusChanged(false);
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3));
            assertEquals("↰ 18m", prefix.getText().toString());
            controller.windowFocusChanged(true);
            assertEquals("↰ 18m", prefix.getText().toString());
            render(activity, R.id.nextDirectionText, "↰ 8m - 2s - Turn left");
            layout(activity);
            assertEquals(width, prefix.getWidth());
            assertEquals(separatorLeft, separator.getLeft());
            assertEquals(instructionLeft, ((View) instruction.getParent()).getLeft());
            render(activity, R.id.nextDirectionText, "Arrive");
            assertEquals(View.GONE, prefix.getVisibility());
            assertEquals(View.GONE, separator.getVisibility());
            assertEquals("Arrive", instruction.getText().toString());
        }
    }

    private static void render(Activity activity, int id, String line) {
        int prefixId = id == R.id.nextDirectionText ? R.id.nextDirectionPrefixText : R.id.afterNextDirectionPrefixText;
        TextView instruction = activity.findViewById(id);
        int separatorId = id == R.id.nextDirectionText ? R.id.nextDirectionSeparatorText : R.id.afterNextDirectionSeparatorText;
        NavigationInstructionRow.render(activity.findViewById(prefixId), activity.findViewById(separatorId),
                instruction, line, instruction.getCurrentTextColor());
    }

    private static void layout(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        content.measure(View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY));
        content.layout(0, 0, 640, 400);
    }
}
