package vibro.navigator.nav.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.animation.ValueAnimator;
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
import org.robolectric.util.ReflectionHelpers;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NavigationInstructionScrollRobolectricTest {
    @Test
    public void portraitScrollsBothInstructionsToTheEndAndRepeats() {
        assertInstructionsScroll(320, 640);
    }

    @Test
    @Config(qualifiers = "land-mdpi")
    public void landscapeScrollsBothInstructionsToTheEndAndRepeats() {
        assertInstructionsScroll(640, 320);
    }

    @Test
    public void liveCountdownRetainsProgressAndNewTurnRestarts() {
        try (ActivityController<Activity> controller = createActivity()) {
            Activity activity = controller.get();
            TextView text = activity.findViewById(R.id.nextDirectionText);
            NavigationInstructionScrollView row = (NavigationInstructionScrollView) text.getParent();
            render(activity, R.id.nextDirectionText, "↰ 100m - 10s - Svolta leggermente a Sinistra");
            layout(activity, 240, 480);
            ValueAnimator scrolling = animator(row);
            assertNotNull(scrolling);
            scrolling.setCurrentPlayTime(scrolling.getDuration() / 2);
            int offset = row.getScrollX();
            assertTrue(offset > 0);

            render(activity, R.id.nextDirectionText, "↰ 090m - 09s - Svolta leggermente a Sinistra");
            layout(activity, 240, 480);
            assertSame(scrolling, animator(row));
            assertEquals(offset, row.getScrollX());
            TextView prefix = activity.findViewById(R.id.nextDirectionPrefixText);
            assertEquals("↰ 090m", prefix.getText().toString());
            assertEquals("Svolta leggermente a Sinistra", text.getText().toString());
            assertEquals("↰ 090m - 09s - Svolta leggermente a Sinistra", text.getContentDescription());

            render(activity, R.id.nextDirectionText, "↱ 080m - 08s - Svolta leggermente a Destra");
            layout(activity, 240, 480);
            assertNotSame(scrolling, animator(row));
            assertEquals(0, row.getScrollX());
            controller.windowFocusChanged(false);
            assertNull(animator(row));
            controller.windowFocusChanged(true);
            assertNotNull(animator(row));

            render(activity, R.id.nextDirectionText, "Arrivo");
            layout(activity, 240, 480);
            assertNull(animator(row));
            assertEquals(0, row.getScrollX());
            assertEquals(View.GONE, prefix.getVisibility());
            assertEquals("Arrivo", text.getText().toString());
        }
    }

    private static void assertInstructionsScroll(int width, int height) {
        try (ActivityController<Activity> controller = createActivity()) {
            Activity activity = controller.get();
            for (int id : new int[]{R.id.nextDirectionText, R.id.afterNextDirectionText}) {
                render(activity, id, "↰ 100m - 10s - Svolta leggermente a Sinistra - verso la prossima strada");
            }
            layout(activity, width, height);
            for (int id : new int[]{R.id.nextDirectionText, R.id.afterNextDirectionText}) {
                TextView prefix = activity.findViewById(prefixId(id));
                int prefixLeft = prefix.getLeft();
                assertScrollCycle(activity.findViewById(id));
                assertEquals("↰ 100m", prefix.getText().toString());
                assertEquals(prefixLeft, prefix.getLeft());
                assertEquals(0, prefix.getScrollX());
                assertEquals(0, prefix.getLayout().getEllipsisCount(0));
                assertTrue(prefix.getWidth() < ((View) prefix.getParent()).getWidth());
            }
        }
    }

    private static void assertScrollCycle(TextView text) {
        NavigationInstructionScrollView row = (NavigationInstructionScrollView) text.getParent();
        assertEquals("Svolta leggermente a Sinistra - verso la prossima strada", text.getText().toString());
        assertEquals(1, text.getLayout().getLineCount());
        assertEquals(text.getText().length(), text.getLayout().getLineEnd(0));
        assertEquals(0, text.getLayout().getEllipsisCount(0));
        ValueAnimator scrolling = animator(row);
        assertNotNull(scrolling);
        scrolling.setCurrentPlayTime(600);
        assertEquals(0, row.getScrollX());
        scrolling.setCurrentPlayTime(scrolling.getDuration() / 2);
        assertTrue(row.getScrollX() > 0);
        scrolling.setCurrentPlayTime(scrolling.getDuration() - 600);
        assertEquals(text.getWidth() - row.getWidth(), row.getScrollX());
        scrolling.setCurrentPlayTime(scrolling.getDuration() + 600);
        assertEquals(0, row.getScrollX());
    }

    private static ValueAnimator animator(NavigationInstructionScrollView row) {
        return ReflectionHelpers.getField(row, "animator");
    }

    private static void render(Activity activity, int instructionId, String line) {
        TextView text = activity.findViewById(instructionId);
        int separatorId = instructionId == R.id.nextDirectionText
                ? R.id.nextDirectionSeparatorText : R.id.afterNextDirectionSeparatorText;
        NavigationInstructionRow.render(activity.findViewById(prefixId(instructionId)),
                activity.findViewById(separatorId), text,
                line, text.getCurrentTextColor());
    }

    private static int prefixId(int instructionId) {
        return instructionId == R.id.nextDirectionText
                ? R.id.nextDirectionPrefixText : R.id.afterNextDirectionPrefixText;
    }

    private static ActivityController<Activity> createActivity() {
        ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup();
        Activity activity = controller.get();
        AndroidAppTheme.apply(activity);
        activity.setContentView(R.layout.activity_navigation);
        controller.windowFocusChanged(true);
        return controller;
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
