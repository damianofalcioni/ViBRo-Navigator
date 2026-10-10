package vibro.navigator.about;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import vibro.navigator.android.storage.AndroidOutputPermissionRequest;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.settings.AppGpxSettings;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;

/** Save switches stay off until selected-folder access and write verification succeed. */
final class AboutOutputSavingSettings {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private final Activity activity;
    private final Runnable render;
    private final AndroidOutputPermissionRequest access;

    AboutOutputSavingSettings(Activity activity, Runnable render) {
        this.activity = activity;
        this.render = render;
        access = new AndroidOutputPermissionRequest(activity, this::complete, WORKER);
    }

    void change(Kind kind, boolean enabled) {
        access.cancel(kind);
        WORKER.execute(() -> {
            persist(kind, false);
            activity.runOnUiThread(() -> {
                if (!activity.isDestroyed() && !activity.isFinishing()) {
                    render.run();
                }
            });
        });
        if (enabled) {
            access.request(kind);
        }
    }

    private void complete(Kind kind, boolean allowed) {
        WORKER.execute(() -> {
            persist(kind, allowed);
            activity.runOnUiThread(() -> {
                if (!activity.isDestroyed()) {
                    render.run();
                }
            });
        });
    }

    private void persist(Kind kind, boolean enabled) {
        if (kind == Kind.LOGS) {
            AppLogger.setLoggingEnabled(activity, enabled);
        } else {
            AppGpxSettings.setAutoSaveOnStopEnabled(activity, enabled);
        }
    }

    boolean handleActivityResult(int code, int resultCode, Intent data) {
        return access.handleActivityResult(code, resultCode, data);
    }

    boolean handlePermissionResult(int code) {
        return access.handlePermissionResult(code);
    }

    void saveState(Bundle state) {
        access.saveState(state);
    }

    void restoreState(Bundle state) {
        access.restoreState(state);
    }

    void flush() {
        try {
            WORKER.submit(() -> {}).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new IllegalStateException("Could not flush saving settings", e);
        }
    }
}
