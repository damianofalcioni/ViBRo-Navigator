package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.car.app.AppInfo;
import androidx.car.app.CarContext;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Method;

import vibro.navigator.R;
import vibro.navigator.distribution.DistributionServices;
import vibro.navigator.nav.model.NavGpsStatus;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavPauseStatus;
import vibro.navigator.nav.model.NavProgressStatus;
import vibro.navigator.nav.model.NavRouteStatus;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.settings.AppNavigationCustomButtonSettings;

@RunWith(RobolectricTestRunner.class)
public class ViBRoCarAppServiceSettingsGplayTest {
    private Application context;
    private ComponentName componentName;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("vibro.navigator.settings", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
        componentName = new ComponentName(context, ViBRoCarAppService.class);
        ViBRoCarAppService.clearActiveSessionsForTest();
        ViBRoCarAppComponent.resetPendingDisableForTest();
        ViBRoCarAppComponent.setCarModeActiveForTest(false);
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
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                context.getPackageManager().getComponentEnabledSetting(componentName)
        );
    }

    @Test
    public void carServiceDeclaresHostDisplayMetadata() throws Exception {
        ServiceInfo serviceInfo = context.getPackageManager().getServiceInfo(componentName, 0);

        assertEquals(R.string.app_name, serviceInfo.labelRes);
        assertEquals(context.getApplicationInfo().icon, serviceInfo.icon);
    }

    @Test
    public void carAppMinApiLevelIsDeclaredForLibraryHandshake() {
        assertEquals(1, AppInfo.retrieveMinCarAppApiLevel(context));
    }

    @Test
    public void activeNavigationTemplateBuildsWithMinimalRequiredActionStrip() throws Exception {
        ViBRoCarTemplates templates = new ViBRoCarTemplates(
                testCarContext(),
                () -> {
                }
        );

        Template template = templates.build(activeNavigationState());

        assertTrue(template instanceof NavigationTemplate);
        NavigationTemplate navigationTemplate = (NavigationTemplate) template;
        assertEquals(1, navigationTemplate.getActionStrip().getActions().size());
        assertNull(navigationTemplate.getActionStrip().getActions().get(0).getTitle());
    }

    @Test
    public void nullStateShowsNoActiveNavigationWithoutPhoneLaunchAction() throws Exception {
        ViBRoCarTemplates templates = new ViBRoCarTemplates(
                testCarContext(),
                () -> {
                }
        );

        Template noActiveTemplate = templates.build(null);

        assertTrue(noActiveTemplate instanceof PaneTemplate);
        PaneTemplate paneTemplate = (PaneTemplate) noActiveTemplate;
        assertEquals(
                context.getString(R.string.auto_no_active_navigation_title),
                paneTemplate.getPane().getRows().get(0).getTitle().toString()
        );
        assertEquals(0, paneTemplate.getPane().getActions().size());
    }

    @Test
    public void compassOverlayIgnoresFormerSettingsAndExportButtonAreas() throws Exception {
        AppNavigationCustomButtonSettings.setEnabled(context, false);
        CarContext carContext = testCarContext();
        carContext.setTheme(R.style.Theme_ViBRoNavigator);
        RecordingAutoControls controls = new RecordingAutoControls();
        ViBRoAutoCompassOverlayPainter painter = new ViBRoAutoCompassOverlayPainter(carContext, controls);
        Bitmap bitmap = Bitmap.createBitmap(220, 220, Bitmap.Config.ARGB_8888);

        painter.draw(new Canvas(bitmap), activeNavigationState(), new RectF(10f, 10f, 210f, 210f), 1f);

        assertFalse(painter.handleClick(32f, 188f));
        assertFalse(painter.handleClick(188f, 188f));
        assertEquals(0, controls.customButtonToggles);
    }

    @NonNull
    private CarContext testCarContext() throws Exception {
        CarContext carContext = CarContext.create(new TestLifecycleOwner().getLifecycle());
        Method attachBaseContext = CarContext.class.getDeclaredMethod(
                "attachBaseContext",
                Context.class,
                Configuration.class
        );
        attachBaseContext.setAccessible(true);
        attachBaseContext.invoke(carContext, context, context.getResources().getConfiguration());
        return carContext;
    }

    @NonNull
    private static NavState activeNavigationState() {
        return new NavState(
                new NavRouteStatus(
                        new NavGuidanceStatus("Turn right", "Continue"),
                        new NavProgressStatus("ETA 13:51", "466 m", ""),
                        null
                ),
                new NavGpsStatus("0 km/h", NavState.NO_DEADLINE),
                new NavPauseStatus(false)
        );
    }

    private static final class TestLifecycleOwner implements LifecycleOwner {
        private final LifecycleRegistry lifecycle = new LifecycleRegistry(this);

        @Override
        @NonNull
        public Lifecycle getLifecycle() {
            return lifecycle;
        }
    }

    private static final class RecordingAutoControls implements ViBRoAutoSurfaceRenderer.Controls {
        private int customButtonToggles;

        @Override
        public void onBlockedRoad() {
        }

        @Override
        public void onStopNavigation() {
        }

        @Override
        public void onTogglePaused() {
        }

        @Override
        public void onToggleCustomButton() {
            customButtonToggles++;
        }

        @Override
        @NonNull
        public String buildCurrentDirectionDetailsText() {
            return "";
        }
    }
}
