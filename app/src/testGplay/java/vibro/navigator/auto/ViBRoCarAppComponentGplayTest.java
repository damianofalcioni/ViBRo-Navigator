package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;

import android.app.Application;
import android.content.ComponentName;
import android.content.pm.PackageManager;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.distribution.DistributionServices;

@RunWith(RobolectricTestRunner.class)
public class ViBRoCarAppComponentGplayTest {
    private Application context;
    private ComponentName componentName;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        componentName = new ComponentName(context, ViBRoCarAppService.class);
        ViBRoCarAppService.clearActiveSessionsForTest();
        ViBRoCarAppComponent.resetPendingDisableForTest();
        ViBRoCarAppComponent.setCarModeActiveForTest(false);
        DistributionServices.configureAndroidAutoIntegration(context, true);
    }

    @After
    public void tearDown() {
        ViBRoCarAppService.clearActiveSessionsForTest();
        ViBRoCarAppComponent.resetPendingDisableForTest();
    }

    @Test
    public void disablingWithoutActiveCarSessionDisablesComponentImmediately() {
        DistributionServices.configureAndroidAutoIntegration(context, false);

        assertComponentState(PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
    }

    @Test
    public void disablingWithActiveCarSessionDefersComponentDisableUntilSessionEnds() {
        ViBRoCarAppService.setActiveSessionForTest(true);

        DistributionServices.configureAndroidAutoIntegration(context, false);

        assertComponentState(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);

        ViBRoCarAppService.setActiveSessionForTest(false);
        ViBRoCarAppComponent.onSessionDestroyed(context);

        assertComponentState(PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
    }

    @Test
    public void disablingWhileCarModeActiveDefersComponentDisableUntilCarModeExits() {
        ViBRoCarAppComponent.setCarModeActiveForTest(true);

        DistributionServices.configureAndroidAutoIntegration(context, false);

        assertComponentState(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);

        ViBRoCarAppComponent.setCarModeActiveForTest(false);
        ViBRoCarAppComponent.onCarModeExited(context);

        assertComponentState(PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
    }

    @Test
    public void disablingWhileCarModeAndSessionActiveWaitsForBothToEnd() {
        ViBRoCarAppService.setActiveSessionForTest(true);
        ViBRoCarAppComponent.setCarModeActiveForTest(true);

        DistributionServices.configureAndroidAutoIntegration(context, false);

        ViBRoCarAppComponent.setCarModeActiveForTest(false);
        ViBRoCarAppComponent.onCarModeExited(context);

        assertComponentState(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);

        ViBRoCarAppService.setActiveSessionForTest(false);
        ViBRoCarAppComponent.onSessionDestroyed(context);

        assertComponentState(PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
    }

    @Test
    public void reenablingBeforeActiveSessionEndsCancelsPendingDisable() {
        ViBRoCarAppService.setActiveSessionForTest(true);
        DistributionServices.configureAndroidAutoIntegration(context, false);

        DistributionServices.configureAndroidAutoIntegration(context, true);
        ViBRoCarAppService.setActiveSessionForTest(false);
        ViBRoCarAppComponent.onSessionDestroyed(context);

        assertComponentState(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
    }

    @Test
    public void reenablingBeforeCarModeExitsCancelsPendingDisable() {
        ViBRoCarAppComponent.setCarModeActiveForTest(true);
        DistributionServices.configureAndroidAutoIntegration(context, false);

        DistributionServices.configureAndroidAutoIntegration(context, true);
        ViBRoCarAppComponent.setCarModeActiveForTest(false);
        ViBRoCarAppComponent.onCarModeExited(context);

        assertComponentState(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
    }

    private void assertComponentState(int expectedState) {
        assertEquals(
                expectedState,
                context.getPackageManager().getComponentEnabledSetting(componentName)
        );
    }
}
