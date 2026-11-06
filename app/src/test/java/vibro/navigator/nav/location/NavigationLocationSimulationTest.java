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
        assertNotNull(policy.startup(() -> { throw new AssertionError(); }));
        assertTrue(policy.suppressSeeds(false));
        assertTrue(policy.suppressProviderSeed(false));
        assertFalse(policy.allowGnss(true));
        assertTrue(policy.startIfEnabled());
    }

    @Test
    public void blankOriginAllowsOnePhysicalSeedUntilAvailable() {
        SimulationTestDriver driver = driver();
        driver.source.configure(true, new NavigationSimulationConfig(10, null));
        NavigationLocationSimulation policy = new NavigationLocationSimulation(driver.source);
        assertNull(policy.startup(() -> null));
        assertFalse(policy.suppressSeeds(false));
        NavigationLocation seed = new NavigationLocation("gps");
        seed.setLatitude(48);
        seed.setLongitude(16);
        assertNotNull(policy.startup(() -> seed));
        assertTrue(policy.suppressSeeds(false));
    }

    @Test
    public void disabledOverrideRetainsPhysicalTrackingPolicy() {
        NavigationLocationSimulation policy = new NavigationLocationSimulation(null);
        assertFalse(policy.startIfEnabled());
        assertTrue(policy.allowGnss(true));
        assertFalse(policy.suppressSeeds(false));
        assertTrue(policy.suppressSeeds(true));
        policy.pause();
    }

    private static SimulationTestDriver driver() {
        return new SimulationTestDriver(() -> new NavigationSimulationPath(null, null, null, false), ignored -> { });
    }
}
