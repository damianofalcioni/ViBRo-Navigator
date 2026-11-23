package vibro.navigator.nav.location;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocationControllerTest.FakeLocationProvider;
import vibro.navigator.nav.location.NavigationLocationControllerTest.FakeGnssTracker;
import vibro.navigator.nav.location.NavigationLocationControllerTest.FakeFusedLocationUpdateClient;
import vibro.navigator.nav.simulation.NavigationSimulationConfig;
import vibro.navigator.nav.simulation.NavigationSimulationPath;
import vibro.navigator.nav.simulation.SimulationTestDriver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NavigationLocationControllerSimulationTest {
    private static final String GPS = "gps";
    @Test
    public void recoveryRetriesMissingOriginAndStopsSeedingOnceItArrives() {
        SimulationTestDriver driver = new SimulationTestDriver(
                () -> new NavigationSimulationPath(null, null, null, false), ignored -> { });
        driver.source.configure(true, new NavigationSimulationConfig(10, null));
        FakeLocationProvider provider = new FakeLocationProvider(GPS);
        FakeFusedLocationUpdateClient fused = new FakeFusedLocationUpdateClient();
        NavigationLocationController controller = new NavigationLocationController(provider, new FakeGnssTracker(),
                fused, () -> true, driver::nowMs, driver.source);
        controller.requestFastLocationUpdates();
        assertEquals(1, fused.currentSeedRequestCount);
        controller.restartActiveLocationUpdates(3000);
        assertEquals(2, fused.currentSeedRequestCount);
        NavigationLocation origin = new NavigationLocation(GPS);
        origin.setTime(System.currentTimeMillis(), driver.nowMs());
        origin.setAccuracy(5);
        assertTrue(driver.source.seed(origin));
        controller.finishSimulationOrigin();
        controller.restartActiveLocationUpdates(3000);
        assertEquals(2, fused.currentSeedRequestCount);
        assertEquals(2, provider.requestProviderUpdatesCount);
        assertTrue(provider.requestedProviders.isEmpty());
    }

    @Test
    public void blankOriginUsesPlatformUpdatesWithoutFusedLocation() {
        SimulationTestDriver driver = new SimulationTestDriver(
                () -> new NavigationSimulationPath(null, null, null, false), ignored -> { });
        driver.source.configure(true, new NavigationSimulationConfig(10, null));
        FakeLocationProvider provider = new FakeLocationProvider(GPS);
        FakeFusedLocationUpdateClient fused = new FakeFusedLocationUpdateClient();
        NavigationLocationController controller = new NavigationLocationController(provider, new FakeGnssTracker(),
                fused, () -> false, driver::nowMs, driver.source);

        controller.requestFastLocationUpdates();
        assertEquals(Collections.singletonList(GPS), provider.requestedProviders);
        assertEquals(0, fused.currentSeedRequestCount);
        controller.stopTracking();
        assertTrue(provider.requestedProviders.isEmpty());
    }

    @Test
    public void configuredOriginSkipsPhysicalProvidersAndPausesWithController() {
        List<NavigationLocation> fixes = new ArrayList<>();
        SimulationTestDriver driver = new SimulationTestDriver(
                () -> new NavigationSimulationPath(null, new LatLon(0, 0.001), null, false), fixes::add);
        driver.source.configure(true, new NavigationSimulationConfig(10, new LatLon(0, 0)));
        FakeLocationProvider provider = new FakeLocationProvider(GPS);
        FakeGnssTracker gnss = new FakeGnssTracker();
        NavigationLocationController controller = new NavigationLocationController(provider, gnss,
                new FakeFusedLocationUpdateClient(), () -> true, driver::nowMs, driver.source);
        controller.requestLocationUpdates(30000);
        controller.requestCurrentLocationSeeds();
        controller.onProviderEnabledFast(GPS, true);
        controller.setGnssStatusTrackingAllowed(true);
        assertEquals("simulation", controller.activeProviderSummary());
        assertEquals("simulation", controller.getBestStartupLastKnownLocation(driver.nowMs()).getProvider());
        assertEquals(0, provider.requestProviderUpdatesCount);
        assertEquals(0, provider.requestSeedForEnabledProviderCount);
        assertFalse(gnss.trackingAllowed);
        driver.advance(1000);
        assertEquals(1, fixes.size());
        controller.stopTracking();
        driver.advance(60000);
        assertEquals(1, fixes.size());
        controller.requestFastLocationUpdates();
        driver.advance(1000);
        assertEquals(2, fixes.size());
    }
}
