package vibro.navigator.android.location;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.location.LocationListener;
import android.location.LocationManager;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLocationManager;

import java.util.Arrays;
import java.util.Collections;

import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 26, 35})
public class AndroidLocationProviderAccessTest {
    private static final String GPS_PROVIDER = "gps";
    private static final String NETWORK_PROVIDER = "network";
    private ShadowLocationManager manager;
    private AndroidLocationProviderAccess access;
    private int statuses;

    @Before
    public void setUp() {
        Application context = ApplicationProvider.getApplicationContext();
        shadowOf(context).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION);
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        manager = shadowOf(locationManager);
        manager.setProviderEnabled(GPS_PROVIDER, true);
        manager.setProviderEnabled(NETWORK_PROVIDER, true);
        access = new AndroidLocationProviderAccess(context, locationManager, new Listener());
    }

    @Test
    public void changingNetworkIntervalPreservesGpsSubscriptionAndIndependentIntervals() {
        access.requestProviderUpdates(Arrays.asList(GPS_PROVIDER, NETWORK_PROVIDER), 3_000L, 3_000L);
        Object gpsRequest = manager.getLocationRequests(GPS_PROVIDER).get(0);
        access.requestProviderUpdates(Arrays.asList(GPS_PROVIDER, NETWORK_PROVIDER), 3_000L, 30_000L);
        assertSame(gpsRequest, manager.getLocationRequests(GPS_PROVIDER).get(0));
        assertEquals(1, manager.getLocationRequests(NETWORK_PROVIDER).size());
        assertEquals(30_000L, manager.getLegacyLocationRequests(NETWORK_PROVIDER).get(0).getIntervalMillis());
        assertEquals(3_000L, manager.getLegacyLocationRequests(GPS_PROVIDER).get(0).getIntervalMillis());
    }

    @Test
    public void removingOneProviderPreservesOtherAndStopRemovesEverything() {
        access.requestProviderUpdates(Arrays.asList(GPS_PROVIDER, NETWORK_PROVIDER), 3_000L);
        Object gpsRequest = manager.getLocationRequests(GPS_PROVIDER).get(0);
        access.requestProviderUpdates(Collections.singletonList(GPS_PROVIDER), 3_000L);
        assertSame(gpsRequest, manager.getLocationRequests(GPS_PROVIDER).get(0));
        assertEquals(0, manager.getLocationRequests(NETWORK_PROVIDER).size());
        access.removeUpdates();
        assertEquals(0, manager.getLocationRequests(GPS_PROVIDER).size());
        access.requestProviderUpdates(Collections.singletonList(GPS_PROVIDER), 3_000L);
        assertEquals(1, manager.getLocationRequests(GPS_PROVIDER).size());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void statusDiagnosticsOnlyReceiveTransitions() {
        access.requestProviderUpdates(Collections.singletonList(GPS_PROVIDER), 3_000L);
        LocationListener listener = manager.getLocationUpdateListeners(GPS_PROVIDER).get(0);
        listener.onStatusChanged(GPS_PROVIDER, 2, null);
        listener.onStatusChanged(GPS_PROVIDER, 2, null);
        listener.onStatusChanged(GPS_PROVIDER, 1, null);
        assertEquals(2, statuses);
    }

    private final class Listener implements NavigationLocationListener {
        @Override
        public void onLocationChanged(NavigationLocation location) { }

        @Override
        public void onProviderEnabled(String provider) { }

        @Override
        public void onProviderDisabled(String provider) { }

        @Override
        public void onProviderStatusChanged(String provider, int status) {
            statuses++;
        }
    }
}
