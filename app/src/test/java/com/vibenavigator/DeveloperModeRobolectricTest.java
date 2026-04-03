package com.vibenavigator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import com.vibenavigator.util.AppLogger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowToast;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@RunWith(RobolectricTestRunner.class)
public class DeveloperModeRobolectricTest {

    @Before
    public void setUp() {
        Application context = ApplicationProvider.getApplicationContext();
        AppLogger.init(context);
        ShadowToast.reset();
    }

    @Test
    public void developerModeStartsDisabledOnInit() {
        Application context = ApplicationProvider.getApplicationContext();

        assertFalse(AppLogger.isDeveloperModeEnabled(context));

        assertTrue(AppLogger.enableDeveloperMode(context));
        assertTrue(AppLogger.isDeveloperModeEnabled(context));

        AppLogger.init(context);

        assertFalse(AppLogger.isDeveloperModeEnabled(context));
    }

    @Test
    public void enablingDeveloperModeTwiceKeepsCurrentLogSession() {
        Application context = ApplicationProvider.getApplicationContext();

        assertTrue(AppLogger.enableDeveloperMode(context));
        String firstPath = AppLogger.getLogFilePath(context);

        assertFalse(AppLogger.enableDeveloperMode(context));
        assertEquals(firstPath, AppLogger.getLogFilePath(context));
    }

    @Test
    public void enablingDeveloperModeAfterRestartRecreatesLogFile() throws Exception {
        Application context = ApplicationProvider.getApplicationContext();

        assertTrue(AppLogger.enableDeveloperMode(context));
        String firstPath = AppLogger.getLogFilePath(context);
        AppLogger.i("DeveloperModeTest", "first run marker");
        String firstContent = new String(
                Files.readAllBytes(new File(firstPath).toPath()),
                StandardCharsets.UTF_8
        );
        assertTrue(firstContent.contains("first run marker"));

        AppLogger.init(context);

        assertTrue(AppLogger.enableDeveloperMode(context));
        String secondPath = AppLogger.getLogFilePath(context);
        String secondContent = new String(
                Files.readAllBytes(new File(secondPath).toPath()),
                StandardCharsets.UTF_8
        );

        assertFalse(secondContent.contains("first run marker"));
    }

    @Test
    public void aboutPageShowsAlreadyEnabledMessageOnSecondUnlockGesture() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        View root = activity.findViewById(R.id.aboutRoot);

        performDeveloperUnlockGesture(root);
        assertEquals(
                activity.getString(R.string.msg_developer_mode_enabled),
                ShadowToast.getTextOfLatestToast()
        );

        performDeveloperUnlockGesture(root);
        assertEquals(
                activity.getString(R.string.msg_developer_mode_already_enabled),
                ShadowToast.getTextOfLatestToast()
        );
    }

    private static void performDeveloperUnlockGesture(View view) {
        for (int i = 0; i < 5; i++) {
            view.performClick();
        }
    }
}
