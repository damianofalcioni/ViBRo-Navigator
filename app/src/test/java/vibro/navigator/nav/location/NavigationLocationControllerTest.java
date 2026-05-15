package vibro.navigator.nav.location;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.location.LocationManager;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowSystemClock;

import java.time.Duration;

import vibro.navigator.settings.AppSettings;

@RunWith(RobolectricTestRunner.class)
public class NavigationLocationControllerTest {
    private static final String GPS_AND_NETWORK = "gps+network";

    @After
    public void resetSettings() {
        AppSettings.setFusedLocationEnabled(ApplicationProvider.getApplicationContext(), true);
    }

    @Test
    public void shouldReuseActiveLocationRequest_returnsTrueForMatchingIntervalAndProviders() {
        assertTrue(NavigationLocationController.shouldReuseActiveLocationRequest(
                1_000L,
                GPS_AND_NETWORK,
                1_000L,
                GPS_AND_NETWORK
        ));
    }

    @Test
    public void shouldReuseActiveLocationRequest_returnsFalseForChangedInterval() {
        assertFalse(NavigationLocationController.shouldReuseActiveLocationRequest(
                2_000L,
                GPS_AND_NETWORK,
                1_000L,
                GPS_AND_NETWORK
        ));
    }

    @Test
    public void shouldReuseActiveLocationRequest_returnsFalseForChangedProviders() {
        assertFalse(NavigationLocationController.shouldReuseActiveLocationRequest(
                1_000L,
                "gps",
                1_000L,
                GPS_AND_NETWORK
        ));
    }

    @Test
    public void shouldReuseActiveLocationRequest_returnsFalseWhenNoProviderSummaryExists() {
        assertFalse(NavigationLocationController.shouldReuseActiveLocationRequest(
                1_000L,
                null,
                1_000L,
                GPS_AND_NETWORK
        ));
    }

    @Test
    public void canUseProvider_requiresFinePermissionForGps() {
        assertFalse(NavigationLocationController.canUseProvider(
                LocationManager.GPS_PROVIDER,
                false,
                true
        ));
        assertTrue(NavigationLocationController.canUseProvider(
                LocationManager.GPS_PROVIDER,
                true,
                false
        ));
    }

    @Test
    public void canUseProvider_allowsNetworkWithCoarsePermission() {
        assertTrue(NavigationLocationController.canUseProvider(
                LocationManager.NETWORK_PROVIDER,
                false,
                true
        ));
        assertTrue(NavigationLocationController.canUseProvider(
                LocationManager.PASSIVE_PROVIDER,
                false,
                true
        ));
    }

    @Test
    public void recordAcceptedLocationUpdate_refreshesActiveRequestDeadline() {
        Application context = ApplicationProvider.getApplicationContext();
        AppSettings.setFusedLocationEnabled(context, false);
        shadowOf(context).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        shadowOf(locationManager).setProviderEnabled(LocationManager.GPS_PROVIDER, true);
        NavigationLocationController controller = new NavigationLocationController(context, location -> {
        });

        controller.requestLocationUpdates(10_000L);
        long initialDeadline = controller.getNextEvaluationDeadlineElapsedMs();

        ShadowSystemClock.advanceBy(Duration.ofSeconds(2));
        controller.recordAcceptedLocationUpdate();

        assertTrue(controller.getNextEvaluationDeadlineElapsedMs() >= initialDeadline + 2_000L);
    }

    @Test
    public void requestLocationUpdates_afterStopTrackingRequestsProviderAgainWithLastInterval() {
        Application context = ApplicationProvider.getApplicationContext();
        AppSettings.setFusedLocationEnabled(context, false);
        shadowOf(context).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        shadowOf(locationManager).setProviderEnabled(LocationManager.GPS_PROVIDER, true);
        NavigationLocationController controller = new NavigationLocationController(context, location -> {
        });

        controller.requestLocationUpdates(10_000L);
        controller.stopTracking();

        assertEquals(10_000L, controller.getLastRequestedLocationMinTimeMsOrDefault(1_000L));
        assertTrue(shadowOf(locationManager).getLocationUpdateListeners(LocationManager.GPS_PROVIDER).isEmpty());

        controller.requestLocationUpdates(controller.getLastRequestedLocationMinTimeMsOrDefault(1_000L));

        assertEquals(1, shadowOf(locationManager).getLocationUpdateListeners(LocationManager.GPS_PROVIDER).size());
    }

}
