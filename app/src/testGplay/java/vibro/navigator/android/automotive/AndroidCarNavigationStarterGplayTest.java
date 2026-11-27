package vibro.navigator.android.automotive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.android.brouter.AndroidBRouterProfilesRepositoryFactory;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.poi.Poi;
import vibro.navigator.settings.AppMainUiSettings;

@RunWith(RobolectricTestRunner.class)
public class AndroidCarNavigationStarterGplayTest {
    private Application context;
    private BRouterProfilesRepository profiles;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("vibenavigator_brouter", Context.MODE_PRIVATE).edit().clear().commit();
        profiles = AndroidBRouterProfilesRepositoryFactory.create();
        PackageInfo brouter = new PackageInfo();
        brouter.packageName = BRouterProfilesRepository.BROUTER_PACKAGE_NAME;
        shadowOf(context.getPackageManager()).installPackage(brouter);
        AppMainUiSettings.completeWelcome(context);
    }

    @Test
    public void incompleteProfileSetupDoesNotStartAnInventedDefaultRoute() {
        Poi destination = new Poi("Test", 48.2, 16.3);
        assertNull(AndroidCarNavigationStarter.prepare(context, destination));
        profiles.saveSelectedProfileKey(context, "__unselected__");
        assertNull(AndroidCarNavigationStarter.prepare(context, destination));
        profiles.saveSelectedProfileKey(context, "__custom__");
        assertNull(AndroidCarNavigationStarter.prepare(context, destination));
    }

    @Test
    public void savedProfileAndDestinationAreUsedForStandardCarNavigation() {
        profiles.saveSelectedProfileKey(context, "car-fast");
        NavigationRequest request = AndroidCarNavigationStarter.prepare(context, new Poi("Test", 48.2, 16.3));
        assertEquals("car-fast", request.profile);
        assertEquals(48.2, request.destination.lat, 0);
        assertEquals(16.3, request.destination.lon, 0);
        assertFalse(request.customProfile);
        assertFalse(request.isStraightLine());
    }
}
