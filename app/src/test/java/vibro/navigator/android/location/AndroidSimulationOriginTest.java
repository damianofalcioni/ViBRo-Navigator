package vibro.navigator.android.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.util.ReflectionHelpers.getField;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.os.SystemClock;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLocationManager;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import vibro.navigator.android.intent.AndroidNavigationRequestIntentContract;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.service.NavigationService;
import vibro.navigator.nav.session.NavigationSession;
import vibro.navigator.nav.simulation.NavigationSimulator;
import vibro.navigator.settings.AppSettings;
import vibro.navigator.settings.AppSimulationSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 26)
public class AndroidSimulationOriginTest {
    @Test
    public void blankOriginAcquiresGpsOnLegacyAndroidAndReleasesPhysicalProviders() {
        Application app = ApplicationProvider.getApplicationContext();
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        AppSettings.setFusedLocationEnabled(app, false);
        AppSimulationSettings.setEnabled(app, true);
        LocationManager location = (LocationManager) app.getSystemService(Context.LOCATION_SERVICE);
        ShadowLocationManager manager = shadowOf(location);
        manager.setProviderEnabled(LocationManager.GPS_PROVIDER, true);
        manager.setProviderEnabled(LocationManager.NETWORK_PROVIDER, true);
        ServiceController<NavigationService> controller = Robolectric.buildService(NavigationService.class).create();
        NavigationService service = controller.get();
        Intent start = new Intent(NavigationService.ACTION_START);
        AndroidNavigationRequestIntentContract.putInto(start, new NavigationRequest(NavigationRoutingMode.STRAIGHT_LINE,
                null, "Destination", new LatLon(48.001, 16), Collections.emptyList()));
        service.onStartCommand(start, 0, 1);
        assertEquals(1, manager.getLocationRequests(LocationManager.GPS_PROVIDER).size());

        manager.simulateLocation(fix(LocationManager.NETWORK_PROVIDER, 16.006, 600));
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, manager.getLocationRequests(LocationManager.GPS_PROVIDER).size());

        manager.simulateLocation(fix(LocationManager.GPS_PROVIDER, 16, 5));
        shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS);
        assertEquals(0, manager.getLocationRequests(LocationManager.GPS_PROVIDER).size());
        assertEquals(0, manager.getLocationRequests(LocationManager.NETWORK_PROVIDER).size());
        NavigationSession session = getField(service, "navigationSession");
        NavigationLocation filtered = session.getLastFilteredLocation();
        assertNotNull(filtered);
        assertEquals(NavigationSimulator.PROVIDER, filtered.getProvider());
        assertEquals(16, filtered.getLongitude(), 0.00001);
        controller.destroy();
    }

    private static Location fix(String provider, double longitude, float accuracy) {
        Location fix = new Location(provider);
        fix.setLatitude(48);
        fix.setLongitude(longitude);
        fix.setAccuracy(accuracy);
        fix.setTime(System.currentTimeMillis());
        fix.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        return fix;
    }
}
