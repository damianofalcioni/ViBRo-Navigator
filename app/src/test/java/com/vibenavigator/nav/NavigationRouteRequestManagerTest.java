package com.vibenavigator.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.vibenavigator.geo.LatLon;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class NavigationRouteRequestManagerTest {

    @Test
    public void prepare_throttlesBackToBackRecalculations() {
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset(1_000L);

        NavigationSession.RouteRequestSnapshot first = manager.prepare(
                false,
                10_000L,
                navigationRequest(),
                location(0.0, 0.0, 10_000L),
                Collections.emptyList(),
                null
        );
        NavigationSession.RouteRequestSnapshot second = manager.prepare(
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

        NavigationSession.RouteRequestSnapshot first = manager.prepare(
                true,
                2_000L,
                navigationRequest(),
                location(0.0, 0.0, 2_000L),
                Collections.emptyList(),
                null
        );
        NavigationSession.RouteRequestSnapshot second = manager.prepare(
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
    public void onRouteApplied_clearsInProgressAndAllowsQueuedRetry() {
        NavigationRouteRequestManager manager = new NavigationRouteRequestManager();
        manager.reset(1_000L);

        NavigationSession.RouteRequestSnapshot first = manager.prepare(
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

        NavigationSession.RouteRequestSnapshot snapshot = manager.prepare(
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

        NavigationSession.RouteRequestSnapshot snapshot = manager.prepare(
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
    private static Location location(double lat, double lon, long timeMs) {
        Location location = new Location("gps");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setTime(timeMs);
        return location;
    }
}
