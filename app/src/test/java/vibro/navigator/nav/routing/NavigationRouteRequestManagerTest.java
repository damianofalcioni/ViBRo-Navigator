package vibro.navigator.nav.routing;


import vibro.navigator.nav.model.NavigationRequest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationProviders;

import androidx.annotation.NonNull;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.format.TestNavigationTextResources;

import org.junit.Test;

import java.util.Collections;

public class NavigationRouteRequestManagerTest {
    private static final String FUSED_PROVIDER = "fused";
    private static final String BLOCKED_ROAD_NOTICE = "Blocked road added. Recalculating route.";

    @Test
    public void prepare_throttlesBackToBackRecalculations() {
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset();

        NavigationRouteRequestSnapshot first = manager.prepare(
                false,
                10_000L,
                navigationRequest(),
                location(0.0, 0.0, 10_000L),
                Collections.emptyList(),
                null
        );
        NavigationRouteRequestSnapshot second = manager.prepare(
                false,
                11_000L,
                navigationRequest(),
                location(0.0, 0.0, 11_000L),
                Collections.emptyList(),
                null
        );

        assertNotNull(first);
        assertNull(second);
        assertTrue(manager.isRouteCalculationInProgress());
    }

    @Test
    public void prepare_queuesRecalculationWhileRequestIsInProgress() {
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset();

        NavigationRouteRequestSnapshot first = manager.prepare(
                true,
                2_000L,
                navigationRequest(),
                location(0.0, 0.0, 2_000L),
                Collections.emptyList(),
                null
        );
        NavigationRouteRequestSnapshot second = manager.prepare(
                false,
                3_000L,
                navigationRequest(),
                location(0.0, 0.0, 3_000L),
                Collections.emptyList(),
                null
        );

        assertNotNull(first);
        assertNull(second);
        assertTrue(manager.isRouteCalculationInProgress());
        assertNotNull(manager.consumePendingRecalculation());
        assertNull(manager.consumePendingRecalculation());
    }

    @Test
    public void prepare_skipsQueuedStartupRefreshWhenLatestFixDoesNotMateriallyImproveStart() {
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset();

        NavigationRouteRequestSnapshot first = manager.prepare(
                false,
                2_000L,
                navigationRequest(),
                location(NavigationLocationProviders.NETWORK_PROVIDER, 48.198767, 16.3657927, 2_000L, 19.667f),
                Collections.emptyList(),
                null,
                NavigationRouteRecalculationReason.NO_ACTIVE_ROUTE
        );
        NavigationRouteRequestSnapshot second = manager.prepare(
                false,
                3_000L,
                navigationRequest(),
                location(FUSED_PROVIDER, 48.19876181016738, 16.3658536569431, 3_000L, 18.256f),
                Collections.emptyList(),
                null,
                NavigationRouteRecalculationReason.NO_ACTIVE_ROUTE
        );

        assertNotNull(first);
        assertNull(second);
        assertNull(manager.consumePendingRecalculation());
    }

    @Test
    public void prepare_queuesStartupRefreshWhenLatestFixMovesStartMaterially() {
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset();

        manager.prepare(
                false,
                2_000L,
                navigationRequest(),
                location(NavigationLocationProviders.NETWORK_PROVIDER, 48.198767, 16.3657927, 2_000L, 19.667f),
                Collections.emptyList(),
                null,
                NavigationRouteRecalculationReason.NO_ACTIVE_ROUTE
        );
        NavigationRouteRequestSnapshot second = manager.prepare(
                false,
                3_000L,
                navigationRequest(),
                location(FUSED_PROVIDER, 48.1989, 16.36595, 3_000L, 18.256f),
                Collections.emptyList(),
                null,
                NavigationRouteRecalculationReason.NO_ACTIVE_ROUTE
        );

        assertNull(second);
        assertNotNull(manager.consumePendingRecalculation());
    }

    @Test
    public void prepare_queuesStartupRefreshWhenAccuracyImprovesMaterially() {
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset();

        manager.prepare(
                false,
                2_000L,
                navigationRequest(),
                location(NavigationLocationProviders.NETWORK_PROVIDER, 48.198767, 16.3657927, 2_000L, 24f),
                Collections.emptyList(),
                null,
                NavigationRouteRecalculationReason.NO_ACTIVE_ROUTE
        );
        NavigationRouteRequestSnapshot second = manager.prepare(
                false,
                3_000L,
                navigationRequest(),
                location(FUSED_PROVIDER, 48.198767, 16.3657927, 3_000L, 10f),
                Collections.emptyList(),
                null,
                NavigationRouteRecalculationReason.NO_ACTIVE_ROUTE
        );

        assertNull(second);
        assertNotNull(manager.consumePendingRecalculation());
    }

    @Test
    public void prepare_keepsDeviationRerouteQueuedWhenStartIsClose() {
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset();

        manager.prepare(
                false,
                2_000L,
                navigationRequest(),
                location(NavigationLocationProviders.NETWORK_PROVIDER, 48.198767, 16.3657927, 2_000L, 19.667f),
                Collections.emptyList(),
                null,
                NavigationRouteRecalculationReason.NO_ACTIVE_ROUTE
        );
        NavigationRouteRequestSnapshot second = manager.prepare(
                false,
                3_000L,
                navigationRequest(),
                location(FUSED_PROVIDER, 48.19876181016738, 16.3658536569431, 3_000L, 18.256f),
                Collections.emptyList(),
                null,
                NavigationRouteRecalculationReason.ROUTE_DEVIATION
        );

        assertNull(second);
        assertNotNull(manager.consumePendingRecalculation());
    }

    @Test
    public void onRouteApplied_clearsInProgressAndAllowsQueuedRetry() {
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset();

        NavigationRouteRequestSnapshot first = manager.prepare(
                true,
                2_000L,
                navigationRequest(),
                location(0.0, 0.0, 2_000L),
                Collections.emptyList(),
                null
        );
        manager.prepare(
                false,
                3_000L,
                navigationRequest(),
                location(0.0, 0.0, 3_000L),
                Collections.emptyList(),
                null
        );

        assertTrue(manager.onRouteApplied(first));
        assertFalse(manager.isRouteCalculationInProgress());
        assertNotNull(manager.consumePendingRecalculation());
    }

    @Test
    public void onRouteFailure_summarizesNestedMessageAndStopsProgress() {
        NavigationTextResources context = TestNavigationTextResources.metric();
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset();

        NavigationRouteRequestSnapshot snapshot = manager.prepare(
                true,
                2_000L,
                navigationRequest(),
                location(0.0, 0.0, 2_000L),
                Collections.emptyList(),
                null
        );

        manager.onRouteFailure(
                context,
                snapshot,
                new IllegalStateException(null, new RuntimeException("route failed\nbecause blocked"))
        );

        assertFalse(manager.isRouteCalculationInProgress());
        assertNotNull(manager.getLastRouteFailure());
        assertEquals("route failed because blocked", manager.getLastRouteFailure().getCause().getMessage().replace('\n', ' '));
    }

    @Test
    public void prepare_tracksInProgressNoticeUntilCompletion() {
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset();

        NavigationRouteRequestSnapshot snapshot = manager.prepare(
                true,
                2_000L,
                navigationRequest(),
                location(0.0, 0.0, 2_000L),
                Collections.emptyList(),
                BLOCKED_ROAD_NOTICE
        );

        assertEquals("Blocked road added. Recalculating route.", manager.getInProgressNotice());
        assertTrue(manager.onRouteApplied(snapshot));
        assertNull(manager.getInProgressNotice());
    }

    @Test
    public void prepare_preservesPendingRecalculationNoticeAndReason() {
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset();

        manager.prepare(
                true,
                2_000L,
                navigationRequest(),
                location(0.0, 0.0, 2_000L),
                Collections.emptyList(),
                null
        );
        NavigationRouteRequestSnapshot second = manager.prepare(
                true,
                3_000L,
                navigationRequest(),
                location(0.0, 0.0, 3_000L),
                Collections.emptyList(),
                BLOCKED_ROAD_NOTICE,
                NavigationRouteRecalculationReason.EXPLICIT
        );

        PendingRouteRecalculation pending = manager.consumePendingRecalculation();
        assertNull(second);
        assertNotNull(pending);
        assertTrue(pending.force);
        assertEquals(NavigationRouteRecalculationReason.EXPLICIT, pending.reason);
        assertEquals(BLOCKED_ROAD_NOTICE, pending.inProgressNotice);
    }

    @NonNull
    private static NavigationRequest navigationRequest() {
        return new NavigationRequest(
                "trekking",
                "Destination",
                new LatLon(48.2082, 16.3738),
                Collections.emptyList()
        );
    }

    @NonNull
    private static NavigationLocation location(double lat, double lon, long timeMs) {
        return location("gps", lat, lon, timeMs, Float.NaN);
    }

    @NonNull
    private static NavigationLocation location(String provider, double lat, double lon, long timeMs, float accuracyMeters) {
        NavigationLocation location = new NavigationLocation(provider);
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setTime(timeMs);
        if (!Float.isNaN(accuracyMeters)) {
            location.setAccuracy(accuracyMeters);
        }
        return location;
    }
}
