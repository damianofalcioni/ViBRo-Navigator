package vibro.navigator.about;

import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;

import java.util.concurrent.TimeUnit;

final class AboutActivityTestSupport {
    private AboutActivityTestSupport() {
    }

    @NonNull
    static AboutActivity setupWithSettings() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        idleSettingsInitialization();
        return activity;
    }

    @NonNull
    static AboutActivity setupWithSettings(@NonNull Intent intent) {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class, intent).setup().get();
        idleSettingsInitialization();
        return activity;
    }

    @NonNull
    static ActivityController<AboutActivity> setupControllerWithSettings() {
        ActivityController<AboutActivity> controller = Robolectric.buildActivity(AboutActivity.class).setup();
        idleSettingsInitialization();
        return controller;
    }

    static void idleSettingsInitialization() {
        shadowOf(Looper.getMainLooper()).idleFor(
                AboutActivity.SETTINGS_INITIALIZATION_DELAY_MS + 50,
                TimeUnit.MILLISECONDS
        );
    }
}
