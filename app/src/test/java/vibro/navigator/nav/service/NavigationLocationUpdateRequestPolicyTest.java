package vibro.navigator.nav.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import vibro.navigator.nav.location.NavigationLocationController;

public class NavigationLocationUpdateRequestPolicyTest {

    @Test
    public void effectiveIntervalMs_keepsSuggestedIntervalWhenDynamicIntervalIsEnabled() {
        assertEquals(60_000L, NavigationLocationUpdateRequestPolicy.effectiveIntervalMs(true, 60_000L));
    }

    @Test
    public void effectiveIntervalMs_forcesOneSecondIntervalWhenDynamicIntervalIsDisabled() {
        assertEquals(
                NavigationLocationController.STARTUP_UPDATE_INTERVAL_MS,
                NavigationLocationUpdateRequestPolicy.effectiveIntervalMs(false, 60_000L)
        );
    }
}
