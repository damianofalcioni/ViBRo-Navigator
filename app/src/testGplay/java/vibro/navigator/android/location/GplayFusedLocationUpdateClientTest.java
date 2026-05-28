package vibro.navigator.android.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.Priority;

import org.junit.Test;

public class GplayFusedLocationUpdateClientTest {

    @Test
    public void buildRequest_doesNotWaitForAccurateFixBeforeDeliveringNavigationUpdates() {
        LocationRequest request = GplayFusedLocationUpdateClient.buildRequest(60_000L, true);

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
        CurrentLocationRequest request = GplayFusedLocationUpdateClient.buildCurrentLocationSeedRequest(true);

        assertEquals(Priority.PRIORITY_HIGH_ACCURACY, request.getPriority());
        assertEquals(15_000L, request.getDurationMillis());
        assertEquals(15_000L, request.getMaxUpdateAgeMillis());
    }

    @Test
    public void buildCurrentLocationSeedRequest_usesBalancedSeedForCoarseOnlyLocation() {
        CurrentLocationRequest request = GplayFusedLocationUpdateClient.buildCurrentLocationSeedRequest(false);

        assertEquals(Priority.PRIORITY_BALANCED_POWER_ACCURACY, request.getPriority());
    }
}
