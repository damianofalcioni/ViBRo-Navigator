package vibro.navigator.nav.ui;

import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import vibro.navigator.R;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.nav.format.TestNavigationTextResources;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavRouteStatus;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.presentation.NavStateResourceComposer;
import vibro.navigator.settings.AppThemeSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class NavigationInstructionColorTest {
    private static final String NEXT = "Continue";
    private static final String FOLLOWING = "Turn left";

    @Test
    public void darkThemeChangesColorWithoutAddingText() {
        assertColorTransitions(false);
    }

    @Test
    public void lightThemeRestoresItsNormalInstructionColor() {
        assertColorTransitions(true);
    }

    @Test
    @Config(qualifiers = "land")
    public void landscapeChangesColorWithoutAddingText() {
        assertColorTransitions(false);
    }

    private static void assertColorTransitions(boolean lightTheme) {
        AppThemeSettings.setLightThemeEnabled(ApplicationProvider.getApplicationContext(), lightTheme);
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        AndroidAppTheme.apply(activity);
        activity.setContentView(R.layout.activity_navigation);
        NavigationActivityRenderer renderer = new NavigationActivityRenderer(
                activity, AndroidTaskScheduler.main(), () -> 1_000L, () -> {});
        TextView next = activity.findViewById(R.id.nextDirectionText);
        TextView following = activity.findViewById(R.id.afterNextDirectionText);
        int orange = ContextCompat.getColor(activity, R.color.compass_accent);
        int normal = AndroidAppTheme.color(activity, R.attr.vibroTextPrimaryColor);

        renderer.render(state(new NavGuidanceStatus(NEXT, FOLLOWING, true, false)), null);
        assertEquals(NEXT, next.getText().toString());
        assertEquals(orange, next.getCurrentTextColor());
        assertEquals(normal, following.getCurrentTextColor());

        renderer.render(state(new NavGuidanceStatus(NEXT, FOLLOWING, false, false)), null);
        assertEquals(NEXT, next.getText().toString());
        assertEquals(normal, next.getCurrentTextColor());

        NavGuidanceStatus uncertain = new NavGuidanceStatus(NEXT, FOLLOWING, true, true);
        renderer.render(state(uncertain), null);
        assertEquals(FOLLOWING, following.getText().toString());
        assertEquals(orange, following.getCurrentTextColor());

        renderer.render(state(uncertain.withDisplayedLines("Arrive", "")), null);
        assertEquals(normal, next.getCurrentTextColor());
        assertEquals(normal, following.getCurrentTextColor());
        assertEquals("", following.getText().toString());
    }

    private static NavState state(NavGuidanceStatus guidance) {
        NavState base = NavStateResourceComposer.waiting(TestNavigationTextResources.metric());
        return new NavState(new NavRouteStatus(guidance, base.routeStatus.progress, null),
                base.gpsStatus, base.pauseStatus);
    }
}
