package vibro.navigator.nav.routing;


import vibro.navigator.nav.model.NavigationRequest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationProviders;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import vibro.navigator.geo.LatLon;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class NavigationRouteRequestManagerTest {
    private static final String FUSED_PROVIDER = "fused";

    @Test
    public void prepare_throttlesBackToBackRecalculations() {
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset(1_000L);

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
        manager.reset(1_000L);

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
        assertTrue(manager.consumePendingRecalculation());
        assertFalse(manager.consumePendingRecalculation());
    }

    @Test
    public void prepare_skipsQueuedStartupRefreshWhenLatestFixDoesNotMateriallyImproveStart() {
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset(1_000L);

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
        assertFalse(manager.consumePendingRecalculation());
    }

    @Test
    public void prepare_queuesStartupRefreshWhenLatestFixMovesStartMaterially() {
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset(1_000L);

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
        assertTrue(manager.consumePendingRecalculation());
    }

    @Test
    public void prepare_queuesStartupRefreshWhenAccuracyImprovesMaterially() {
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset(1_000L);

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
        assertTrue(manager.consumePendingRecalculation());
    }

    @Test
    public void prepare_keepsDeviationRerouteQueuedWhenStartIsClose() {
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset(1_000L);

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
        assertTrue(manager.consumePendingRecalculation());
    }

    @Test
    public void onRouteApplied_clearsInProgressAndAllowsQueuedRetry() {
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset(1_000L);

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
        assertTrue(manager.consumePendingRecalculation());
    }

    @Test
    public void onRouteFailure_summarizesNestedMessageAndStopsProgress() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset(1_000L);

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
        manager.reset(1_000L);

        NavigationRouteRequestSnapshot snapshot = manager.prepare(
                true,
                2_000L,
                navigationRequest(),
                location(0.0, 0.0, 2_000L),
                Collections.emptyList(),
                "Blocked road added. Recalculating route."
        );

        assertEquals("Blocked road added. Recalculating route.", manager.getInProgressNotice());
        assertTrue(manager.onRouteApplied(snapshot));
        assertNull(manager.getInProgressNotice());
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
