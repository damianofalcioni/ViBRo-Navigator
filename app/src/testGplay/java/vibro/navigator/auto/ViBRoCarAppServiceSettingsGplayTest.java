package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.ComponentName;
import android.content.pm.PackageManager;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.distribution.DistributionServices;

@RunWith(RobolectricTestRunner.class)
public class ViBRoCarAppServiceSettingsGplayTest {
    private Application context;
    private ComponentName componentName;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        componentName = new ComponentName(context, ViBRoCarAppService.class);
        DistributionServices.configureAndroidAutoIntegration(context, true);
    }

    @Test
    public void gplaySupportsAndroidAutoIntegrationSetting() {
        assertTrue(DistributionServices.supportsAndroidAutoIntegration());
    }

    @Test
    public void androidAutoIntegrationSettingControlsCarServiceComponent() {
        DistributionServices.configureAndroidAutoIntegration(context, false);

        assertEquals(
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                context.getPackageManager().getComponentEnabledSetting(componentName)
        );

        DistributionServices.configureAndroidAutoIntegration(context, true);

        assertEquals(
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                context.getPackageManager().getComponentEnabledSetting(componentName)
        );
    }
}
