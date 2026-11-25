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

import androidx.annotation.NonNull;
import androidx.car.app.AppInfo;
import androidx.car.app.CarContext;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Action;
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
import java.lang.reflect.Field;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.distribution.DistributionServices;
import vibro.navigator.nav.model.NavGpsStatus;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavPauseStatus;
import vibro.navigator.nav.model.NavProgressStatus;
import vibro.navigator.nav.model.NavRouteStatus;
import vibro.navigator.nav.model.NavState;

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
        ViBRoCarAppComponent.resetPendingDisableForTest(context);
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
    public void activeNavigationTemplateUsesHostControls() throws Exception {
        ViBRoCarTemplates templates = new ViBRoCarTemplates(
                testCarContext(),
                () -> {
                }
        );

        Template template = templates.build(CarNavigationTestStates.active(false, false));

        assertTrue(template instanceof NavigationTemplate);
        NavigationTemplate navigationTemplate = (NavigationTemplate) template;
        List<Action> actions = navigationTemplate.getActionStrip().getActions();
        assertEquals(List.of(R.drawable.ic_blocked_road, R.drawable.ic_stop, R.drawable.ic_pause),
                actions.stream().map(action -> action.getIcon().getIcon().getResId()).toList());
        assertTrue(actions.stream().allMatch(action -> action.getTitle() == null));
        assertEquals(Action.PAN, navigationTemplate.getMapActionStrip().getActions().get(0));
    }

    @Test
    public void apiOneHostKeepsNavigationWithoutGestureActionStrip() throws Exception {
        ViBRoCarTemplates templates = new ViBRoCarTemplates(testCarContext(1), () -> { });
        NavigationTemplate template = (NavigationTemplate) templates.build(activeNavigationState());
        assertNull(template.getMapActionStrip());
    }

    @Test
    public void guidanceAndEtaUseHostTemplateComponents() throws Exception {
        ViBRoCarTemplates templates = new ViBRoCarTemplates(testCarContext(), () -> { });
        NavigationTemplate active = (NavigationTemplate) templates.build(CarNavigationTestStates.active(false, false));
        androidx.car.app.navigation.model.RoutingInfo routing =
                (androidx.car.app.navigation.model.RoutingInfo) active.getNavigationInfo();
        assertEquals("Turn right · 12 s", routing.getCurrentStep().getCue().toString());
        assertEquals(120, routing.getCurrentDistance().getDisplayDistance(), 0);
        assertEquals(50, active.getDestinationTravelEstimate().getRemainingTimeSeconds());
        assertEquals(3, active.getActionStrip().getActions().size());
        NavigationTemplate paused = (NavigationTemplate) templates.build(CarNavigationTestStates.active(true, false));
        assertTrue(paused.getNavigationInfo() instanceof androidx.car.app.navigation.model.MessageInfo);
        assertNull(paused.getDestinationTravelEstimate());
        assertEquals(2, paused.getActionStrip().getActions().size());
    }

    @Test
    public void apiTwoHostEnablesPanAndZoomCallbacks() throws Exception {
        ViBRoCarTemplates templates = new ViBRoCarTemplates(testCarContext(2), () -> { });
        NavigationTemplate template = (NavigationTemplate) templates.build(activeNavigationState());
        assertEquals(Action.PAN, template.getMapActionStrip().getActions().get(0));
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
    public void templateInvalidationOnlyTracksActiveNavigationTransitions() throws Exception {
        ViBRoCarTemplates templates = new ViBRoCarTemplates(
                testCarContext(),
                () -> {
                }
        );
        NavState active = activeNavigationState();
        NavState anotherActive = activeNavigationState();
        NavState noActive = noActiveNavigationState();

        assertTrue(templates.requiresInvalidation(null, active));
        assertFalse(templates.requiresInvalidation(active, anotherActive));
        assertTrue(templates.requiresInvalidation(active, noActive));
        assertFalse(templates.requiresInvalidation(noActive, null));
    }

    @NonNull
    CarContext testCarContext() throws Exception {
        return testCarContext(5);
    }

    @NonNull
    CarContext testCarContext(int carApiLevel) throws Exception {
        CarContext carContext = CarContext.create(new TestLifecycleOwner().getLifecycle());
        Method attachBaseContext = CarContext.class.getDeclaredMethod(
                "attachBaseContext",
                Context.class,
                Configuration.class
        );
        attachBaseContext.setAccessible(true);
        attachBaseContext.invoke(carContext, context, context.getResources().getConfiguration());
        Field apiLevel = CarContext.class.getDeclaredField("mCarAppApiLevel");
        apiLevel.setAccessible(true);
        apiLevel.setInt(carContext, carApiLevel);
        return carContext;
    }

    @NonNull
    private static NavState activeNavigationState() {
        return new NavState(
                new NavRouteStatus(
                        new NavGuidanceStatus("Turn right", "Continue"),
                        new NavProgressStatus("13:51", "466 m", ""),
                        null
                ),
                new NavGpsStatus("0 km/h", NavState.NO_DEADLINE),
                new NavPauseStatus(false)
        );
    }

    @NonNull
    private NavState noActiveNavigationState() {
        return new NavState(
                new NavRouteStatus(
                        new NavGuidanceStatus(context.getString(R.string.nav_no_route), ""),
                        new NavProgressStatus("", "", ""),
                        null
                ),
                new NavGpsStatus("--", NavState.NO_DEADLINE),
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

}
