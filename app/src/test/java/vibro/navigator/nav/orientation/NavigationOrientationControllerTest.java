package vibro.navigator.nav.orientation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NavigationOrientationControllerTest {

    @Test
    public void shouldDispatchCompassUiRequiresActiveRouteVisibleUiAndInteractiveScreen() {
        assertTrue(NavigationOrientationController.shouldDispatchCompassUi(true, true, true));
        assertFalse(NavigationOrientationController.shouldDispatchCompassUi(false, true, true));
        assertFalse(NavigationOrientationController.shouldDispatchCompassUi(true, false, true));
        assertFalse(NavigationOrientationController.shouldDispatchCompassUi(true, true, false));
    }

    @Test
    public void shouldEvaluateStationaryOrientationRequiresActiveRouteAndNoReroute() {
        assertTrue(NavigationOrientationController.shouldEvaluateStationaryOrientation(true, false));
        assertFalse(NavigationOrientationController.shouldEvaluateStationaryOrientation(false, false));
        assertFalse(NavigationOrientationController.shouldEvaluateStationaryOrientation(true, true));
    }

}
