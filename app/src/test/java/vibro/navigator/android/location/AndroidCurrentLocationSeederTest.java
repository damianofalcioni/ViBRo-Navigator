package vibro.navigator.android.location;

import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLocationManager;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.R)
public class AndroidCurrentLocationSeederTest {

    @Test
    public void requestSeedForEnabledProvider_replacesPreviousProviderSeed() {
        LocationManager locationManager = locationManager();
        ShadowLocationManager shadowLocationManager = shadowOf(locationManager);
        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, true);
        AndroidCurrentLocationSeeder seeder = seeder(locationManager);

        seeder.requestSeedForEnabledProvider(LocationManager.GPS_PROVIDER);
        seeder.requestSeedForEnabledProvider(LocationManager.GPS_PROVIDER);

        assertEquals(1, shadowLocationManager.getLocationRequests(LocationManager.GPS_PROVIDER).size());
    }

    @Test
    public void requestSeeds_cancelsProviderSeedWhenPermissionNoLongerAllowsIt() {
        LocationManager locationManager = locationManager();
        ShadowLocationManager shadowLocationManager = shadowOf(locationManager);
        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, true);
        shadowLocationManager.setProviderEnabled(LocationManager.NETWORK_PROVIDER, true);
        AndroidCurrentLocationSeeder seeder = seeder(locationManager);

        seeder.requestSeeds(true, true);
        seeder.requestSeeds(false, true);

        assertEquals(0, shadowLocationManager.getLocationRequests(LocationManager.GPS_PROVIDER).size());
        assertEquals(1, shadowLocationManager.getLocationRequests(LocationManager.NETWORK_PROVIDER).size());
    }

    @Test
    public void cancelPendingCurrentLocationRequests_cancelsAllProviderSeeds() {
        LocationManager locationManager = locationManager();
        ShadowLocationManager shadowLocationManager = shadowOf(locationManager);
        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, true);
        shadowLocationManager.setProviderEnabled(LocationManager.NETWORK_PROVIDER, true);
        AndroidCurrentLocationSeeder seeder = seeder(locationManager);

        seeder.requestSeeds(true, true);
        seeder.cancelPendingCurrentLocationRequests();

        assertEquals(0, shadowLocationManager.getLocationRequests(LocationManager.GPS_PROVIDER).size());
        assertEquals(0, shadowLocationManager.getLocationRequests(LocationManager.NETWORK_PROVIDER).size());
    }

    @NonNull
    private static LocationManager locationManager() {
        Context context = ApplicationProvider.getApplicationContext();
        return (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    @NonNull
    private static AndroidCurrentLocationSeeder seeder(@NonNull LocationManager locationManager) {
        return new AndroidCurrentLocationSeeder(locationManager, new NoOpLocationListener(), Runnable::run);
    }

    private static final class NoOpLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(@NonNull Location location) {
        }
    }
}
