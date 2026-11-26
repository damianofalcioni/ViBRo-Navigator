package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.car.app.CarContext;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.navigation.model.NavigationTemplate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.R;
import vibro.navigator.settings.AppNavigationCustomButtonSettings;
import vibro.navigator.settings.AppNavigationCustomButtonSettings.Target;
import vibro.navigator.settings.AppNavigationCustomButtonTargetState;

@RunWith(RobolectricTestRunner.class)
public class ViBRoCarCustomButtonTest {
    private final ViBRoCarAppServiceSettingsGplayTest contexts = new ViBRoCarAppServiceSettingsGplayTest();

    @Before
    public void setUp() {
        contexts.setUp();
    }

    @Test
    public void mapTargetUsesPhoneIconsAndTracksChangesWhilePaused() throws Exception {
        CarContext context = contexts.testCarContext();
        AppNavigationCustomButtonSettings.setTarget(context, Target.FULLSCREEN_ROUTE);
        AppNavigationCustomButtonTargetState.setEnabled(context, Target.FULLSCREEN_ROUTE, true);
        var state = CarNavigationTestStates.active(true, false);
        var templates = new ViBRoCarTemplates(context, () -> { });
        var template = (NavigationTemplate) templates.build(state);
        assertEquals(2, template.getActionStrip().getActions().size());
        assertEquals(Action.PAN, template.getMapActionStrip().getActions().get(0));
        assertEquals(R.drawable.ic_custom_fullscreen_route_enabled, lastIcon(template.getMapActionStrip()));

        var button = new ViBRoCarCustomButton(context, () -> { });
        var presented = ViBRoCarCustomButton.State.read(context);
        assertTrue(button.toggle(presented));
        assertFalse(button.toggle(presented));
        assertTrue(templates.customButtonChanged());
        template = (NavigationTemplate) templates.build(state);
        assertEquals(R.drawable.ic_custom_fullscreen_route_disabled, lastIcon(template.getMapActionStrip()));
        assertFalse(templates.customButtonChanged());
    }

    @Test
    public void oldHostUsesGeneralStripAndDisablingRemovesTheAction() throws Exception {
        CarContext context = contexts.testCarContext(1);
        AppNavigationCustomButtonSettings.setTarget(context, Target.FULLSCREEN_ROUTE);
        AppNavigationCustomButtonTargetState.setEnabled(context, Target.FULLSCREEN_ROUTE, false);
        var state = CarNavigationTestStates.active(false, false);
        var templates = new ViBRoCarTemplates(context, () -> { });
        var template = (NavigationTemplate) templates.build(state);
        assertNull(template.getMapActionStrip());
        assertEquals(R.drawable.ic_custom_fullscreen_route_disabled, lastIcon(template.getActionStrip()));

        AppNavigationCustomButtonSettings.setEnabled(context, false);
        assertTrue(templates.customButtonChanged());
        template = (NavigationTemplate) templates.build(state);
        assertEquals(3, template.getActionStrip().getActions().size());
        assertFalse(templates.customButtonChanged());
    }

    @Test
    public void dynamicGpsRefreshesTheLiveServiceAndAnOldTargetCannotChangeTheNewOne() throws Exception {
        CarContext context = contexts.testCarContext();
        AppNavigationCustomButtonSettings.setTarget(context, Target.DYNAMIC_GPS_INTERVAL);
        int[] refreshes = {0};
        var button = new ViBRoCarCustomButton(context, () -> refreshes[0]++);
        var presented = ViBRoCarCustomButton.State.read(context);
        assertTrue(button.toggle(presented));
        assertEquals(1, refreshes[0]);
        var updated = ViBRoCarCustomButton.State.read(context);
        AppNavigationCustomButtonSettings.setTarget(context, Target.SPEECH_DIRECTIONS);
        assertFalse(button.toggle(updated));
        assertEquals(1, refreshes[0]);
    }

    private static int lastIcon(ActionStrip strip) {
        var actions = strip.getActions();
        return actions.get(actions.size() - 1).getIcon().getIcon().getResId();
    }
}
