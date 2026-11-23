package vibro.navigator.nav.location;

import org.junit.Test;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.simulation.NavigationSimulationConfig;
import vibro.navigator.nav.simulation.NavigationSimulationPath;
import vibro.navigator.nav.simulation.SimulationTestDriver;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NavigationLocationSimulationTest {
    @Test
    public void configuredOriginSkipsPhysicalSeedAndGnss() {
        SimulationTestDriver driver = driver();
        driver.source.configure(true, new NavigationSimulationConfig(10, new LatLon(48, 16)));
        NavigationLocationSimulation policy = new NavigationLocationSimulation(driver.source);
        assertNotNull(policy.startup(() -> { throw new AssertionError(); }, () -> { throw new AssertionError(); }));
        assertFalse(policy.needsOrigin());
        assertTrue(policy.suppressPhysicalSeed(false));
        assertFalse(policy.allowGnss(true));
        assertTrue(policy.startIfEnabled(() -> { throw new AssertionError(); }));
    }

    @Test
    public void blankOriginAllowsOnePhysicalSeedUntilAvailable() {
        SimulationTestDriver driver = driver();
        driver.source.configure(true, new NavigationSimulationConfig(10, null));
        NavigationLocationSimulation policy = new NavigationLocationSimulation(driver.source);
        assertNull(policy.startup(() -> null, () -> { throw new AssertionError(); }));
        assertTrue(policy.needsOrigin());
        NavigationLocation seed = new NavigationLocation("gps");
        seed.setTime(System.currentTimeMillis(), driver.nowMs());
        seed.setAccuracy(5);
        seed.setLatitude(48);
        seed.setLongitude(16);
        boolean[] finished = {false};
        assertNotNull(policy.startup(() -> seed, () -> finished[0] = true));
        assertTrue(finished[0]);
        assertFalse(policy.needsOrigin());
    }

    @Test
    public void disabledOverrideRetainsPhysicalTrackingPolicy() {
        NavigationLocationSimulation policy = new NavigationLocationSimulation(null);
        assertFalse(policy.startIfEnabled(() -> { throw new AssertionError(); }));
        assertTrue(policy.allowGnss(true));
        assertFalse(policy.needsOrigin());
        policy.pause();
    }

    private static SimulationTestDriver driver() {
        return new SimulationTestDriver(() -> new NavigationSimulationPath(null, null, null, false), ignored -> { });
    }
}
