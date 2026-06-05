package vibro.navigator.android.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.Priority;

import org.junit.Test;

public class GplayFusedLocationClientTest {

    @Test
    public void buildRequest_doesNotWaitForAccurateFixBeforeDeliveringNavigationUpdates() {
        LocationRequest request = GplayFusedLocationClient.buildRequest(60_000L, true);

        assertEquals(Priority.PRIORITY_HIGH_ACCURACY, request.getPriority());
        assertEquals(60_000L, request.getIntervalMillis());
        assertEquals(30_000L, request.getMinUpdateIntervalMillis());
        assertEquals(60_000L, request.getMaxUpdateDelayMillis());
        assertEquals(0f, request.getMinUpdateDistanceMeters(), 0f);
        assertFalse(request.isBatched());
        assertFalse(request.isWaitForAccurateLocation());
    }

    @Test
    public void buildCurrentLocationSeedRequest_usesFreshHighAccuracySeedForFineLocation() {
        CurrentLocationRequest request = GplayFusedLocationClient.buildCurrentLocationSeedRequest(true);

        assertEquals(Priority.PRIORITY_HIGH_ACCURACY, request.getPriority());
        assertEquals(15_000L, request.getDurationMillis());
        assertEquals(15_000L, request.getMaxUpdateAgeMillis());
    }

    @Test
    public void buildCurrentLocationSeedRequest_usesBalancedSeedForCoarseOnlyLocation() {
        CurrentLocationRequest request = GplayFusedLocationClient.buildCurrentLocationSeedRequest(false);

        assertEquals(Priority.PRIORITY_BALANCED_POWER_ACCURACY, request.getPriority());
    }

    @Test
    public void currentLocationSeedTracker_rejectsOlderCompletionAfterNewRequestBegins() {
        GplayCurrentLocationSeedTracker tracker = new GplayCurrentLocationSeedTracker();

        int firstRequest = tracker.begin();
        int secondRequest = tracker.begin();

        assertFalse(tracker.completeIfActive(firstRequest));
        assertTrue(tracker.completeIfActive(secondRequest));
    }

    @Test
    public void currentLocationSeedTracker_rejectsCompletionAfterCancel() {
        GplayCurrentLocationSeedTracker tracker = new GplayCurrentLocationSeedTracker();

        int request = tracker.begin();
        tracker.cancel();

        assertFalse(tracker.completeIfActive(request));
    }
}
