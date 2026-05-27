package vibro.navigator.nav.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
@SuppressWarnings("deprecation")
public class NavigationLocationUpdateRequesterTest {

    @Test
    public void request_whenFusedEnabled_requestsFusedAndLegacyProvidersInParallel() {
        Application context = preparedContext();
        LocationManager locationManager = locationManager(context);
        FakeFusedLocationUpdateClient fused = new FakeFusedLocationUpdateClient(true);
        NavigationLocationUpdateRequester requester = requester(context, locationManager, fused);

        NavigationLocationUpdateRequester.Result result = requester.request(1_000L, true, -1L, null);

        assertTrue(result.hasActiveRequest());
        assertEquals("fused+gps+network", result.activeProviderSummary());
        assertEquals(1, fused.requestUpdatesCount);
        assertEquals(1, shadowOf(locationManager).getLocationUpdateListeners(LocationManager.GPS_PROVIDER).size());
        assertEquals(1, shadowOf(locationManager).getLocationUpdateListeners(LocationManager.NETWORK_PROVIDER).size());
    }

    @Test
    public void request_whenFusedRequestFails_keepsLegacyProvidersActive() {
        Application context = preparedContext();
        LocationManager locationManager = locationManager(context);
        FakeFusedLocationUpdateClient fused = new FakeFusedLocationUpdateClient(false);
        NavigationLocationUpdateRequester requester = requester(context, locationManager, fused);

        NavigationLocationUpdateRequester.Result result = requester.request(1_000L, true, -1L, null);

        assertTrue(result.hasActiveRequest());
        assertEquals("gps+network", result.activeProviderSummary());
        assertEquals(1, fused.requestUpdatesCount);
        assertEquals(1, shadowOf(locationManager).getLocationUpdateListeners(LocationManager.GPS_PROVIDER).size());
        assertEquals(1, shadowOf(locationManager).getLocationUpdateListeners(LocationManager.NETWORK_PROVIDER).size());
    }

    @Test
    public void request_whenProviderSummaryMatches_reusesExistingSubscription() {
        Application context = preparedContext();
        LocationManager locationManager = locationManager(context);
        FakeFusedLocationUpdateClient fused = new FakeFusedLocationUpdateClient(true);
        NavigationLocationUpdateRequester requester = requester(context, locationManager, fused);

        NavigationLocationUpdateRequester.Result result =
                requester.request(1_000L, true, 1_000L, "fused+gps+network");

        assertTrue(result.hasActiveRequest());
        assertEquals("fused+gps+network", result.activeProviderSummary());
        assertEquals(0, fused.requestUpdatesCount);
        assertTrue(shadowOf(locationManager).getLocationUpdateListeners(LocationManager.GPS_PROVIDER).isEmpty());
        assertTrue(shadowOf(locationManager).getLocationUpdateListeners(LocationManager.NETWORK_PROVIDER).isEmpty());
    }

    @NonNull
    private static Application preparedContext() {
        Application context = ApplicationProvider.getApplicationContext();
        shadowOf(context).grantPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        );
        LocationManager locationManager = locationManager(context);
        shadowOf(locationManager).setProviderEnabled(LocationManager.GPS_PROVIDER, true);
        shadowOf(locationManager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, true);
        return context;
    }

    @NonNull
    private static LocationManager locationManager(@NonNull Context context) {
        return (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    @NonNull
    private static NavigationLocationUpdateRequester requester(
            @NonNull Context context,
            @NonNull LocationManager locationManager,
            @NonNull FusedLocationUpdateClient fused
    ) {
        NavigationLocationProviderAccess providerAccess = new NavigationLocationProviderAccess(
                context,
                locationManager,
                location -> {
                }
        );
        return new NavigationLocationUpdateRequester(
                providerAccess,
                fused,
                new NavigationGnssStatusTracker(locationManager),
                () -> {
                },
                () -> "test availability"
        );
    }

    private static final class FakeFusedLocationUpdateClient implements FusedLocationUpdateClient {
        private final boolean requestSucceeds;
        private int requestUpdatesCount;

        FakeFusedLocationUpdateClient(boolean requestSucceeds) {
            this.requestSucceeds = requestSucceeds;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public boolean requestUpdates(long minTimeMs, boolean fineGranted, boolean coarseGranted) {
            requestUpdatesCount++;
            return requestSucceeds;
        }

        @Override
        public void requestCurrentLocationSeed(boolean fineGranted, boolean coarseGranted) {
        }

        @Override
        public void removeUpdates() {
        }

        @NonNull
        @Override
        public String describeAvailability() {
            return "fake fused";
        }
    }
}
