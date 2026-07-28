package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.nav.service.NavigationServiceBinder;
import vibro.navigator.settings.AppNavigationCustomButtonSettings;
import vibro.navigator.settings.AppNavigationCustomButtonSettings.Target;
import vibro.navigator.settings.AppNotificationSettings;
import vibro.navigator.settings.AppThemeSettings;

@RunWith(RobolectricTestRunner.class)
public class ViBRoAutoCustomButtonControllerTest {
    private Application context;
    private Host host;
    private ViBRoAutoCustomButtonController controller;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("vibro.navigator.settings", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
        host = new Host();
        controller = new ViBRoAutoCustomButtonController(context, host);
    }

    @Test
    public void disabledCustomButtonDoesNothing() {
        AppNavigationCustomButtonSettings.setEnabled(context, false);
        AppNavigationCustomButtonSettings.setTarget(context, Target.NOTIFICATIONS);
        AppNotificationSettings.setNavigationNotificationsEnabled(context, true);

        controller.toggleSelectedSetting();

        assertTrue(AppNotificationSettings.areNavigationNotificationsEnabled(context));
        assertEquals(0, host.surfaceRefreshes);
    }

    @Test
    public void togglesSelectedSettingAndRefreshesSurface() {
        AppNavigationCustomButtonSettings.setEnabled(context, true);
        AppNavigationCustomButtonSettings.setTarget(context, Target.NOTIFICATIONS);
        AppNotificationSettings.setNavigationNotificationsEnabled(context, true);

        controller.toggleSelectedSetting();

        assertFalse(AppNotificationSettings.areNavigationNotificationsEnabled(context));
        assertEquals(1, host.surfaceRefreshes);
    }

    @Test
    public void lightThemeTargetRefreshesSurfaceTheme() {
        AppNavigationCustomButtonSettings.setEnabled(context, true);
        AppNavigationCustomButtonSettings.setTarget(context, Target.LIGHT_THEME);
        AppThemeSettings.setLightThemeEnabled(context, false);

        controller.toggleSelectedSetting();

        assertTrue(AppThemeSettings.isLightThemeEnabled(context));
        assertEquals(1, host.themeRefreshes);
        assertEquals(0, host.surfaceRefreshes);
    }

    private static final class Host implements ViBRoAutoCustomButtonController.Host {
        private int surfaceRefreshes;
        private int themeRefreshes;

        @Nullable
        @Override
        public NavigationServiceBinder currentBinder() {
            return null;
        }

        @Override
        public void openPhoneSettings() {
        }

        @Override
        public void refreshSurfaceTheme() {
            themeRefreshes++;
        }

        @Override
        public void refreshSurface() {
            surfaceRefreshes++;
        }

        @Override
        public void showToast(int messageResId) {
        }
    }
}
