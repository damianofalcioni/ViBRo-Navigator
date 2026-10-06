package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;

import java.util.concurrent.TimeUnit;

import vibro.navigator.R;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.nav.ui.NavigationActivity;
import vibro.navigator.settings.AppMainUiSettings;
import vibro.navigator.settings.AppThemeSettings;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.R)
public class MainActivityWelcomeScreenTest {
    private static final String SHARED_ADDRESS = "Cafe Central";

    @Before
    public void setUp() throws Exception {
        Application context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("vibro.navigator.settings", Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences("vibenavigator_brouter", Context.MODE_PRIVATE).edit().clear().commit();
        setInstallTimes(1_000L, 1_000L);
    }

    @Test
    public void welcomePrecedesInstallPromptAndCompletionSurvivesUpdate() throws Exception {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            Dialog welcome = welcomeDialog();
            assertNull(ShadowAlertDialog.getLatestAlertDialog());
            assertFalse(AppMainUiSettings.isWelcomeCompleted(activity));
            assertNull(shadowOf(activity).getNextStartedActivityForResult());

            continueWelcome(welcome);

            assertFalse(welcome.isShowing());
            assertTrue(AppMainUiSettings.isWelcomeCompleted(activity));
            assertEquals(activity.getString(R.string.msg_brouter_install_prompt),
                    String.valueOf(shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getMessage()));
        }
        setInstallTimes(1_000L, 2_000L);
        try (ActivityController<MainActivity> reopened = Robolectric.buildActivity(MainActivity.class).setup()) {
            assertTrue(AppMainUiSettings.isWelcomeCompleted(reopened.get()));
            assertNull(ShadowDialog.getLatestDialog().findViewById(R.id.welcomeContinueButton));
        }
    }

    @Test
    public void updateWithoutCompletionFlagSkipsWelcomeAndAppliesIncomingDestination() throws Exception {
        Application context = ApplicationProvider.getApplicationContext();
        assertFalse(context.getSharedPreferences("vibro.navigator.settings", Context.MODE_PRIVATE)
                .contains("welcome_completed"));
        setInstallTimes(1_000L, 2_000L);

        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class, sharedAddress()).setup()) {
            assertNull(ShadowDialog.getLatestDialog().findViewById(R.id.welcomeContinueButton));
            assertEquals(SHARED_ADDRESS, destination(controller.get()));
            assertTrue(context.getSharedPreferences("vibro.navigator.settings", Context.MODE_PRIVATE)
                    .getBoolean("welcome_completed", false));
            assertEquals(controller.get().getString(R.string.msg_brouter_install_prompt),
                    String.valueOf(shadowOf(ShadowAlertDialog.getLatestAlertDialog()).getMessage()));
            controller.recreate();
            assertEquals(SHARED_ADDRESS, destination(controller.get()));
            assertEquals(0, ((LinearLayout) controller.get().findViewById(R.id.stopsContainer)).getChildCount());
        }
    }

    @Test
    public void folderAccessPromptWaitsUntilWelcomeCompletes() {
        Application context = ApplicationProvider.getApplicationContext();
        PackageInfo brouter = new PackageInfo();
        brouter.packageName = BRouterProfilesRepository.BROUTER_PACKAGE_NAME;
        shadowOf(context.getPackageManager()).installPackage(brouter);
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            Dialog welcome = welcomeDialog();
            assertNull(ShadowAlertDialog.getLatestAlertDialog());
            continueWelcome(welcome);
            shadowOf(Looper.getMainLooper()).idleFor(android.view.ViewConfiguration.getPressedStateDuration(),
                    TimeUnit.MILLISECONDS);
            AlertDialog accessPrompt = ShadowAlertDialog.getLatestAlertDialog();
            assertNotNull(accessPrompt);
            assertTrue(String.valueOf(shadowOf(accessPrompt).getMessage()).contains("profiles2"));
            assertNull(shadowOf(controller.get()).getNextStartedActivityForResult());
        }
    }

    @Test
    public void initialIncomingDestinationIsDeferredAndAppliedOnceAfterRecreation() {
        try (ActivityController<MainActivity> controller =
                     Robolectric.buildActivity(MainActivity.class, sharedAddress()).setup()) {
            assertEquals("", destination(controller.get()));
            Dialog first = welcomeDialog();
            controller.recreate();
            assertFalse(first.isShowing());
            assertEquals("", destination(controller.get()));

            continueWelcome(welcomeDialog());

            assertEquals(SHARED_ADDRESS, destination(controller.get()));
            controller.recreate();
            assertEquals(SHARED_ADDRESS, destination(controller.get()));
            LinearLayout stops = controller.get().findViewById(R.id.stopsContainer);
            assertEquals(0, stops.getChildCount());
        }
    }

    @Test
    public void newIntentDuringWelcomeIsAppliedAfterContinue() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            controller.newIntent(sharedAddress());
            assertEquals("", destination(controller.get()));
            continueWelcome(welcomeDialog());
            assertEquals(SHARED_ADDRESS, destination(controller.get()));
        }
    }

    @Test
    public void notificationCanResumeExistingNavigationWhileWelcomeIsOpen() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            controller.newIntent(new Intent().putExtra(MainActivity.EXTRA_OPEN_NAVIGATION, true));
            Intent navigation = shadowOf(controller.get()).getNextStartedActivity();
            assertNotNull(navigation);
            assertEquals(NavigationActivity.class.getName(), navigation.getComponent().getClassName());
            assertFalse(AppMainUiSettings.isWelcomeCompleted(controller.get()));
            assertTrue(welcomeDialog().isShowing());
        }
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.M)
    public void scrollPositionSurvivesRecreationOnMinimumAndroidVersion() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            Dialog first = welcomeDialog();
            layoutWelcome(first);
            ScrollView scroll = first.findViewById(R.id.welcomeScroll);
            scroll.scrollTo(0, 150);
            assertEquals(150, scroll.getScrollY());

            controller.recreate();

            Dialog restored = welcomeDialog();
            layoutWelcome(restored);
            assertEquals(150, ((ScrollView) restored.findViewById(R.id.welcomeScroll)).getScrollY());
            continueWelcome(restored);
            assertTrue(AppMainUiSettings.isWelcomeCompleted(controller.get()));
        }
    }

    @Test
    public void destroyingActivityCancelsPendingContinue() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            Dialog welcome = welcomeDialog();
            welcome.findViewById(R.id.welcomeContinueButton).performClick();
            controller.pause().stop().destroy();
            shadowOf(Looper.getMainLooper()).idleFor(150, TimeUnit.MILLISECONDS);
            assertFalse(welcome.isShowing());
            assertFalse(AppMainUiSettings.isWelcomeCompleted(controller.get()));
        }
    }

    @Test
    public void cancelLeavesWelcomeIncompleteAndClosesActivity() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            welcomeDialog().cancel();
            shadowOf(Looper.getMainLooper()).idle();
            assertTrue(controller.get().isFinishing());
            assertFalse(AppMainUiSettings.isWelcomeCompleted(controller.get()));
        }
        try (ActivityController<MainActivity> reopened = Robolectric.buildActivity(MainActivity.class).setup()) {
            assertFalse(AppMainUiSettings.isWelcomeCompleted(reopened.get()));
            assertTrue(welcomeDialog().isShowing());
        }
    }

    @Test
    @Config(qualifiers = "land")
    public void landscapeWelcomeUsesLightThemeAndKeepsContinueOutsideScrollableText() {
        AppThemeSettings.setLightThemeEnabled(ApplicationProvider.getApplicationContext(), true);
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            Dialog welcome = welcomeDialog();
            TextView title = welcome.findViewById(R.id.welcomeTitle);
            assertEquals(ContextCompat.getColor(controller.get(), R.color.light_text_primary), title.getCurrentTextColor());
            ScrollView scroll = welcome.findViewById(R.id.welcomeScroll);
            assertNull(scroll.findViewById(R.id.welcomeContinueButton));
            assertTrue(welcome.findViewById(R.id.welcomeContinueButton).isEnabled());
        }
    }

    private static void setInstallTimes(long firstInstallTime, long lastUpdateTime) throws Exception {
        Application context = ApplicationProvider.getApplicationContext();
        PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        info.firstInstallTime = firstInstallTime;
        info.lastUpdateTime = lastUpdateTime;
        shadowOf(context.getPackageManager()).installPackage(info);
    }

    private static Dialog welcomeDialog() {
        Dialog dialog = ShadowDialog.getLatestDialog();
        assertNotNull(dialog);
        assertNotNull(dialog.findViewById(R.id.welcomeTitle));
        assertTrue(dialog.isShowing());
        return dialog;
    }

    private static void continueWelcome(Dialog dialog) {
        dialog.findViewById(R.id.welcomeContinueButton).performClick();
        shadowOf(Looper.getMainLooper()).idleFor(150, TimeUnit.MILLISECONDS);
    }

    private static void layoutWelcome(Dialog dialog) {
        View content = dialog.findViewById(android.R.id.content);
        content.measure(View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY));
        content.layout(0, 0, 320, 480);
    }

    private static Intent sharedAddress() {
        return new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, SHARED_ADDRESS);
    }

    private static String destination(MainActivity activity) {
        return ((EditText) activity.findViewById(R.id.destinationEdit)).getText().toString();
    }
}
