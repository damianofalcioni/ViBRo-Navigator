package vibro.navigator.nav;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NavigationServiceTest {

    @Test
    public void shouldDispatchCompassUiRequiresActiveRouteVisibleUiAndInteractiveScreen() {
        assertTrue(NavigationService.shouldDispatchCompassUi(true, true, true));
        assertFalse(NavigationService.shouldDispatchCompassUi(false, true, true));
        assertFalse(NavigationService.shouldDispatchCompassUi(true, false, true));
        assertFalse(NavigationService.shouldDispatchCompassUi(true, true, false));
    }

    @Test
    public void shouldEvaluateStationaryOrientationRequiresActiveRouteAndNoReroute() {
        assertTrue(NavigationService.shouldEvaluateStationaryOrientation(true, false));
        assertFalse(NavigationService.shouldEvaluateStationaryOrientation(false, false));
        assertFalse(NavigationService.shouldEvaluateStationaryOrientation(true, true));
    }

}
