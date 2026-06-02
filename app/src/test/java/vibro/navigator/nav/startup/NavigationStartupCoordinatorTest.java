package vibro.navigator.nav.startup;


import vibro.navigator.nav.model.NavigationRequest;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import vibro.navigator.geo.LatLon;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class NavigationStartupCoordinatorTest {

    @Test
    public void ensureReadyThenStart_requestsPermissionsAfterRationaleWhenNeeded() {
        TestHost host = new TestHost();
        NavigationStartupCoordinator coordinator = new NavigationStartupCoordinator(
                host,
                () -> NavigationPreflight.Status.create(
                        Arrays.asList(
                                NavigationPreflight.PERMISSION_FINE_LOCATION,
                                NavigationPreflight.PERMISSION_POST_NOTIFICATIONS
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
                        NavigationPreflight.PERMISSION_FINE_LOCATION,
                        NavigationPreflight.PERMISSION_POST_NOTIFICATIONS
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
                () -> NavigationPreflight.Status.create(
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
        assertEquals(NavigationStartupCoordinator.SettingsTarget.LOCATION, host.settingsTarget);
        assertNull(host.startedRequest);
        assertNotNull(host.settingsDialogCancelAction);
    }

    @Test
    public void settingsDialogCancel_rechecksAndStartsWhenLocationWasEnabledWhileDialogOpen() {
        TestHost host = new TestHost();
        NavigationStartupCoordinator coordinator = new NavigationStartupCoordinator(
                host,
                new NavigationStartupCoordinator.PreflightInspector() {
                    private int callCount;

                    @NonNull
                    @Override
                    public NavigationPreflight.Status inspect() {
                        callCount++;
                        if (callCount == 1) {
                            return NavigationPreflight.Status.create(
                                    Collections.emptyList(),
                                    false,
                                    false,
                                    true,
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

        host.settingsDialogCancelAction.run();

        assertNotNull(host.startedRequest);
        assertFalse(host.startupCancelled);
        assertFalse(coordinator.isAutoStartNavigation());
    }

    @Test
    public void settingsDialogCancel_cancelsStartupWhenLocationRemainsDisabled() {
        TestHost host = new TestHost();
        NavigationStartupCoordinator coordinator = new NavigationStartupCoordinator(
                host,
                () -> NavigationPreflight.Status.create(
                        Collections.emptyList(),
                        false,
                        false,
                        true,
                        false
                )
        );

        coordinator.setAutoStartNavigation(true);
        coordinator.ensureReadyThenStart();

        host.settingsDialogCancelAction.run();

        assertNull(host.startedRequest);
        assertTrue(host.startupCancelled);
        assertFalse(coordinator.isAutoStartNavigation());
    }

    @Test
    public void ensureReadyThenStart_showsNotificationSettingsAndDoesNotStart() {
        TestHost host = new TestHost();
        NavigationStartupCoordinator coordinator = new NavigationStartupCoordinator(
                host,
                () -> NavigationPreflight.Status.create(
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
        assertFalse(host.batteryOptimizationDialogShown);
        assertNull(host.startedRequest);
        assertTrue(coordinator.isAutoStartNavigation());
    }

    @Test
    public void ensureReadyThenStart_showsBatteryOptimizationDialogAndDoesNotStart() {
        TestHost host = new TestHost();
        NavigationStartupCoordinator coordinator = new NavigationStartupCoordinator(
                host,
                () -> NavigationPreflight.Status.create(
                        Collections.emptyList(),
                        false,
                        true,
                        true,
                        true
                )
        );

        coordinator.setAutoStartNavigation(true);
        coordinator.ensureReadyThenStart();

        assertTrue(host.batteryOptimizationDialogShown);
        assertNotNull(host.batteryOptimizationCancelAction);
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
                    public NavigationPreflight.Status inspect() {
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
                    public NavigationPreflight.Status inspect() {
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
                () -> NavigationPreflight.Status.create(
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
        private NavigationStartupCoordinator.SettingsTarget settingsTarget;
        private Runnable settingsDialogCancelAction;
        private boolean batteryOptimizationDialogShown;
        private Runnable batteryOptimizationCancelAction;
        private NavigationRequest startedRequest;
        private boolean startupCancelled;

        @NonNull
        @Override
        public NavigationRequest getNavigationRequest() {
            return request;
        }

        @NonNull
        @Override
        public String getString(int messageResId) {
            return "message:" + messageResId;
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
        public void showSettingsRedirectDialog(
                int messageResId,
                @NonNull NavigationStartupCoordinator.SettingsTarget settingsTarget,
                @NonNull Runnable onCancel
        ) {
            settingsDialogMessageResId = messageResId;
            this.settingsTarget = settingsTarget;
            settingsDialogCancelAction = onCancel;
        }

        @Override
        public void showBatteryOptimizationDialog(@NonNull Runnable onCancel) {
            batteryOptimizationDialogShown = true;
            batteryOptimizationCancelAction = onCancel;
        }

        @Override
        public void startNavigationService(@NonNull NavigationRequest request) {
            startedRequest = request;
        }

        @Override
        public void cancelNavigationStartup() {
            startupCancelled = true;
        }
    }
}
