package vibro.navigator.nav.routing;


import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationProviders;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.format.TestNavigationTextResources;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NavigationRouteRequestManagerTest {
    private static final String FUSED_PROVIDER = "fused";
    private static final String BLOCKED_ROAD_NOTICE = "Blocked road added. Recalculating route.";
    private static final String TREKKING_PROFILE = "trekking";
    private static final String PROFILE_PARAMETERS = "avoid_path=1";

    @Test
    public void prepare_allowsFirstRouteRequestAtZeroTimestamp() {
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset();

        NavigationRouteRequestSnapshot first = manager.prepare(
                false,
                0L,
                navigationRequest(),
                location(0.0, 0.0, 0L),
                Collections.emptyList(),
                null
        );

        assertNotNull(first);
        assertTrue(manager.isRouteCalculationInProgress());
    }

    @Test
    public void prepare_throttlesAfterCompletedRecentRecalculation() {
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
        assertNotNull(first);
        assertTrue(manager.onRouteApplied(first));

        NavigationRouteRequestSnapshot second = manager.prepare(
                false,
                11_000L,
                navigationRequest(),
                location(0.0, 0.0, 11_000L),
                Collections.emptyList(),
                null
        );

        assertNull(second);
        assertFalse(manager.isRouteCalculationInProgress());
        assertNull(manager.consumePendingRecalculation());
    }

    @Test
    public void prepare_doesNotThrottleWhenClockMovesBackward() {
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
        assertNotNull(first);
        assertTrue(manager.onRouteApplied(first));

        NavigationRouteRequestSnapshot second = manager.prepare(
                false,
                9_000L,
                navigationRequest(),
                location(0.0, 0.0, 9_000L),
                Collections.emptyList(),
                null
        );

        assertNotNull(second);
        assertTrue(manager.isRouteCalculationInProgress());
    }

    @Test
    public void ignoredSpeculativeRequestDoesNotThrottleConfirmedDeviationReroute() {
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset();

        NavigationRouteRequestSnapshot speculative = prepareSpeculative(manager, 10_000L);
        assertNotNull(speculative);
        assertTrue(speculative.speculative);
        assertTrue(manager.onSpeculativeRouteFinished(speculative, false));

        NavigationRouteRequestSnapshot confirmed = manager.prepare(
                false,
                11_000L,
                navigationRequest(),
                location(0.0, 0.0, 11_000L),
                Collections.emptyList(),
                null,
                NavigationRouteRecalculationReason.ROUTE_DEVIATION
        );

        assertNotNull(confirmed);
    }

    @Test
    public void appliedDeferredSpeculativeRequestKeepsRerouteThrottle() {
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset();

        NavigationRouteRequestSnapshot speculative = prepareSpeculative(manager, 10_000L);
        assertNotNull(speculative);
        assertTrue(manager.onSpeculativeRouteFinished(speculative, true));
        manager.onDeferredSpeculativeRouteApplied();

        NavigationRouteRequestSnapshot repeated = manager.prepare(
                false,
                11_000L,
                navigationRequest(),
                location(0.0, 0.0, 11_000L),
                Collections.emptyList(),
                null,
                NavigationRouteRecalculationReason.ROUTE_DEVIATION
        );

        assertNull(repeated);
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
    public void prepare_copiesProfileParametersIntoSnapshot() {
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset();
        NavigationRequest request = new NavigationRequest(
                NavigationRoutingMode.BROUTER,
                TREKKING_PROFILE,
                PROFILE_PARAMETERS,
                "Destination",
                new LatLon(48.2082, 16.3738),
                Collections.emptyList()
        );

        NavigationRouteRequestSnapshot snapshot = manager.prepare(
                true,
                2_000L,
                request,
                location(0.0, 0.0, 2_000L),
                Collections.emptyList(),
                null
        );

        assertNotNull(snapshot);
        assertEquals(PROFILE_PARAMETERS, snapshot.profileParameters);
    }

    @Test
    public void prepare_copiesCustomProfileSourceIntoSnapshot() {
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset();
        NavigationRequest request = new NavigationRequest(
                NavigationRoutingMode.BROUTER,
                TREKKING_PROFILE,
                true,
                PROFILE_PARAMETERS,
                "Destination",
                new LatLon(48.2082, 16.3738),
                Collections.emptyList()
        );

        NavigationRouteRequestSnapshot snapshot = manager.prepare(
                true,
                2_000L,
                request,
                location(0.0, 0.0, 2_000L),
                Collections.emptyList(),
                null
        );

        assertNotNull(snapshot);
        assertTrue(snapshot.customProfile);
        assertEquals(PROFILE_PARAMETERS, snapshot.profileParameters);
    }

    @Test
    public void prepare_copiesRoundTripModeAndDistanceIntoSnapshot() {
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset();
        NavigationRequest request = new NavigationRequest(
                NavigationRoutingMode.ROUND_TRIP,
                TREKKING_PROFILE,
                PROFILE_PARAMETERS,
                null,
                null,
                Collections.emptyList(),
                15_000,
                123
        );

        NavigationRouteRequestSnapshot snapshot = manager.prepare(
                true,
                2_000L,
                request,
                location(0.0, 0.0, 2_000L),
                Collections.emptyList(),
                null
        );

        assertNotNull(snapshot);
        assertEquals(NavigationRoutingMode.ROUND_TRIP, snapshot.routingMode);
        assertEquals(15_000, snapshot.roundTripDistanceMeters);
        assertEquals(123, snapshot.roundTripDirectionDegrees);
        assertEquals(PROFILE_PARAMETERS, snapshot.profileParameters);
        assertNull(snapshot.destination);
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

    @Test
    public void snapshot_defensivelyCopiesMutableLists() {
        List<LatLon> intermediates = new ArrayList<>();
        intermediates.add(new LatLon(48.1, 16.1));
        List<NogoPoint> blocked = new ArrayList<>();
        blocked.add(new NogoPoint(48.2, 16.2, 25.0));

        NavigationRouteRequestSnapshot snapshot = new NavigationRouteRequestSnapshot(
                1,
                1,
                new LatLon(48.0, 16.0),
                intermediates,
                new LatLon(48.3, 16.3),
                "trekking",
                null,
                blocked
        );
        intermediates.clear();
        blocked.clear();

        assertEquals(1, snapshot.intermediates.size());
        assertEquals(1, snapshot.blocked.size());
        assertCannotMutate(snapshot.intermediates);
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

    @Nullable
    private static NavigationRouteRequestSnapshot prepareSpeculative(
            @NonNull NavigationRouteRequestManager manager,
            long nowMs
    ) {
        return manager.prepare(
                false,
                nowMs,
                navigationRequest(),
                Collections.emptyList(),
                location(0.0, 0.0, nowMs),
                Collections.emptyList(),
                null,
                NavigationRouteRecalculationReason.ROUTE_DEVIATION,
                true
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

    private static void assertCannotMutate(@NonNull List<LatLon> values) {
        try {
            values.clear();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("Expected snapshot list to be immutable");
    }
}
