package vibro.navigator.nav.foreground;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import vibro.navigator.R;
import vibro.navigator.android.foreground.AndroidNavigationNotificationDebugHelper;
import vibro.navigator.android.foreground.AndroidNavigationNotificationDebugHelper.SymbolTestGroup;
import vibro.navigator.nav.format.NavigationTextFormatter;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.format.TestNavigationTextResources;

public class NavigationNotificationSymbolTestFormatterTest {
    private final NavigationTextResources resources = TestNavigationTextResources.metric();

    @Test
    public void buildSymbolTestSummariesSplitNotificationSymbolsByVibrationGroup() {
        assertEquals(
                "← ↖ <← U↙ <○2",
                AndroidNavigationNotificationDebugHelper.buildSymbolTestSummary(resources, SymbolTestGroup.LEFT)
        );
        assertEquals(
                "⬆ U↓ !!× ◉ ■ ? 42°",
                AndroidNavigationNotificationDebugHelper.buildSymbolTestSummary(resources, SymbolTestGroup.OTHER)
        );
        assertEquals(
                "→ ↗ →> U↘ ○>3",
                AndroidNavigationNotificationDebugHelper.buildSymbolTestSummary(resources, SymbolTestGroup.RIGHT)
        );
    }

    @Test
    public void buildSymbolTestMessagesIncludeLabelsForEachGroup() {
        String leftMessage = AndroidNavigationNotificationDebugHelper.buildSymbolTestMessage(
                resources,
                SymbolTestGroup.LEFT
        );
        String otherMessage = AndroidNavigationNotificationDebugHelper.buildSymbolTestMessage(
                resources,
                SymbolTestGroup.OTHER
        );
        String rightMessage = AndroidNavigationNotificationDebugHelper.buildSymbolTestMessage(
                resources,
                SymbolTestGroup.RIGHT
        );

        assertTrue(leftMessage.contains("← " + resources.getString(R.string.direction_turn_left)));
        assertTrue(leftMessage.contains("↖ " + resources.getString(R.string.direction_slight_left)));
        assertTrue(leftMessage.contains("<← " + resources.getString(R.string.direction_sharp_left)));
        assertTrue(leftMessage.contains("U↙ " + resources.getString(R.string.direction_uturn_left)));
        assertTrue(leftMessage.contains("<○2 " + resources.getString(R.string.direction_roundabout_exit_left, 2)));
        assertTrue(otherMessage.contains("⬆ " + resources.getString(R.string.direction_continue)));
        assertTrue(otherMessage.contains("U↓ " + resources.getString(R.string.direction_uturn)));
        assertTrue(otherMessage.contains("!!× " + resources.getString(R.string.direction_offroute)));
        assertTrue(otherMessage.contains("◉ " + resources.getString(R.string.direction_beeline)));
        assertTrue(otherMessage.contains("■ " + resources.getString(R.string.direction_arrive)));
        assertTrue(otherMessage.contains("? " + resources.getString(R.string.direction_unknown)));
        assertTrue(otherMessage.contains(resources.getString(
                R.string.format_notification_symbol_test_bearing_line,
                NavigationTextFormatter.formatBearingDegrees(resources, 42.0)
        )));
        assertTrue(rightMessage.contains("→ " + resources.getString(R.string.direction_turn_right)));
        assertTrue(rightMessage.contains("↗ " + resources.getString(R.string.direction_slight_right)));
        assertTrue(rightMessage.contains("→> " + resources.getString(R.string.direction_sharp_right)));
        assertTrue(rightMessage.contains("U↘ " + resources.getString(R.string.direction_uturn_right)));
        assertTrue(rightMessage.contains("○>3 " + resources.getString(R.string.direction_roundabout_exit, 3)));
    }
}
