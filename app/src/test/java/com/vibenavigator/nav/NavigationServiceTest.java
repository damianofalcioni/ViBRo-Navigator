package com.vibenavigator.nav;

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
}
