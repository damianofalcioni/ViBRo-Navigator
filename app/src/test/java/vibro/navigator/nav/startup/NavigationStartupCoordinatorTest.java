package vibro.navigator.nav.startup;


import vibro.navigator.nav.model.NavigationRequest;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import vibro.navigator.geo.LatLon;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class NavigationStartupCoordinatorTest {

    @Test
    public void ensureReadyThenStart_requestsPermissionsAfterRationaleWhenNeeded() {
        TestHost host = new TestHost();
        NavigationStartupCoordinator coordinator = new NavigationStartupCoordinator(
                host,
                activity -> NavigationPreflight.Status.create(
                        Arrays.asList(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.POST_NOTIFICATIONS
                        ),
                        true,
                        true,
                        true,
                        false
                )
        );

        coordinator.setAutoStartNavigation(true);
        coordinator.ensureReadyThenStart();

        assertNotNull(host.permissionRationaleMessage);
        assertNull(host.requestedPermissions);
        assertNull(host.startedRequest);

        host.permissionRationaleAction.run();

        assertArrayEquals(
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.POST_NOTIFICATIONS
                },
                host.requestedPermissions
        );
        assertEquals(NavigationStartupCoordinator.REQUEST_PERMISSIONS, host.requestPermissionsCode);
    }

    @Test
    public void ensureReadyThenStart_showsLocationSettingsAndDoesNotStart() {
        TestHost host = new TestHost();
        NavigationStartupCoordinator coordinator = new NavigationStartupCoordinator(
                host,
                activity -> NavigationPreflight.Status.create(
                        Collections.emptyList(),
                        false,
                        false,
                        true,
                        false
                )
        );

        coordinator.setAutoStartNavigation(true);
        coordinator.ensureReadyThenStart();

        assertEquals(vibro.navigator.R.string.msg_location_disabled, host.settingsDialogMessageResId.intValue());
        assertEquals(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS, host.settingsIntent.getAction());
        assertNull(host.startedRequest);
    }

    @Test
    public void ensureReadyThenStart_showsNotificationSettingsAndDoesNotStart() {
        TestHost host = new TestHost();
        NavigationStartupCoordinator coordinator = new NavigationStartupCoordinator(
                host,
                activity -> NavigationPreflight.Status.create(
                        Collections.emptyList(),
                        false,
                        true,
                        false,
                        false
                )
        );

        coordinator.setAutoStartNavigation(true);
        coordinator.ensureReadyThenStart();

        assertEquals(vibro.navigator.R.string.msg_enable_notifications, host.settingsDialogMessageResId.intValue());
        assertNull(host.batteryOptimizationIntent);
        assertNull(host.startedRequest);
        assertTrue(coordinator.isAutoStartNavigation());
    }

    @Test
    public void ensureReadyThenStart_showsBatteryOptimizationDialogAndDoesNotStart() {
        TestHost host = new TestHost();
        NavigationStartupCoordinator coordinator = new NavigationStartupCoordinator(
                host,
                activity -> NavigationPreflight.Status.create(
                        Collections.emptyList(),
                        false,
                        true,
                        true,
                        true
                )
        );

        coordinator.setAutoStartNavigation(true);
        coordinator.ensureReadyThenStart();

        assertNotNull(host.batteryOptimizationIntent);
        assertEquals(
                android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
                host.batteryOptimizationIntent.getAction()
        );
        assertNull(host.startedRequest);
        assertTrue(coordinator.isAutoStartNavigation());
    }

    @Test
    public void onResume_doesNotRecheckPreflightWithoutPauseAfterSettingsOpened() {
        TestHost host = new TestHost();
        NavigationStartupCoordinator coordinator = new NavigationStartupCoordinator(
                host,
                new NavigationStartupCoordinator.PreflightInspector() {
                    private int callCount;

                    @NonNull
                    @Override
                    public NavigationPreflight.Status inspect(@NonNull Activity activity) {
                        callCount++;
                        if (callCount == 1) {
                            return NavigationPreflight.Status.create(
                                    Collections.emptyList(),
                                    false,
                                    true,
                                    false,
                                    false
                            );
                        }
                        return NavigationPreflight.Status.create(
                                Collections.emptyList(),
                                false,
                                true,
                                true,
                                false
                        );
                    }
                }
        );

        coordinator.setAutoStartNavigation(true);
        coordinator.ensureReadyThenStart();
        assertNull(host.startedRequest);

        coordinator.onSettingsOpened();
        coordinator.onResume();

        assertNull(host.startedRequest);
        assertTrue(coordinator.isAutoStartNavigation());
    }

    @Test
    public void onResume_rechecksPreflightAfterPauseAndSettingsOpened() {
        TestHost host = new TestHost();
        NavigationStartupCoordinator coordinator = new NavigationStartupCoordinator(
                host,
                new NavigationStartupCoordinator.PreflightInspector() {
                    private int callCount;

                    @NonNull
                    @Override
                    public NavigationPreflight.Status inspect(@NonNull Activity activity) {
                        callCount++;
                        if (callCount == 1) {
                            return NavigationPreflight.Status.create(
                                    Collections.emptyList(),
                                    false,
                                    true,
                                    false,
                                    false
                            );
                        }
                        return NavigationPreflight.Status.create(
                                Collections.emptyList(),
                                false,
                                true,
                                true,
                                false
                        );
                    }
                }
        );

        coordinator.setAutoStartNavigation(true);
        coordinator.ensureReadyThenStart();
        assertNull(host.startedRequest);

        coordinator.onSettingsOpened();
        coordinator.onPause();
        coordinator.onResume();

        assertNotNull(host.startedRequest);
        assertFalse(coordinator.isAutoStartNavigation());
    }

    @Test
    public void ensureReadyThenStart_startsNavigationAndClearsAutoStart() {
        TestHost host = new TestHost();
        NavigationStartupCoordinator coordinator = new NavigationStartupCoordinator(
                host,
                activity -> NavigationPreflight.Status.create(
                        Collections.emptyList(),
                        false,
                        true,
                        true,
                        false
                )
        );

        coordinator.setAutoStartNavigation(true);
        coordinator.ensureReadyThenStart();

        assertNotNull(host.startedRequest);
        assertEquals(host.request.profile, host.startedRequest.profile);
        assertEquals(host.request.destinationName, host.startedRequest.destinationName);
        assertFalse(coordinator.isAutoStartNavigation());
    }

    private static final class TestHost implements NavigationStartupCoordinator.Host {
        private final Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        private final NavigationRequest request = new NavigationRequest(
                "trekking",
                "Vienna Center",
                new LatLon(48.2082d, 16.3738d),
                Collections.emptyList()
        );

        private String[] requestedPermissions;
        private int requestPermissionsCode = -1;
        private String permissionRationaleMessage;
        private Runnable permissionRationaleAction;
        private Integer settingsDialogMessageResId;
        private Intent settingsIntent;
        private Intent batteryOptimizationIntent;
        private NavigationRequest startedRequest;

        @NonNull
        @Override
        public Activity getActivity() {
            return activity;
        }

        @NonNull
        @Override
        public NavigationRequest getNavigationRequest() {
            return request;
        }

        @Override
        public void requestPermissions(@NonNull String[] permissions, int requestCode) {
            requestedPermissions = permissions;
            requestPermissionsCode = requestCode;
        }

        @Override
        public void showPermissionRationale(@NonNull String message, @NonNull Runnable onContinue) {
            permissionRationaleMessage = message;
            permissionRationaleAction = onContinue;
        }

        @Override
        public void showSettingsRedirectDialog(int messageResId, @NonNull Intent settingsIntent) {
            settingsDialogMessageResId = messageResId;
            this.settingsIntent = settingsIntent;
        }

        @Override
        public void showBatteryOptimizationDialog(@NonNull Intent settingsIntent) {
            batteryOptimizationIntent = settingsIntent;
        }

        @Override
        public void startNavigationService(@NonNull NavigationRequest request) {
            startedRequest = request;
        }
    }
}
