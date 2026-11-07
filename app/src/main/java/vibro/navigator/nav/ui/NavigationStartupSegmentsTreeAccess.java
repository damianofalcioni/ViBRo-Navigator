package vibro.navigator.nav.ui;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.android.brouter.AndroidBRouterFolderAccess;
import vibro.navigator.android.brouter.AndroidBRouterProfilesRepositoryFactory;
import vibro.navigator.android.brouter.AndroidBRouterSegmentsRepositoryFactory;
import vibro.navigator.android.brouter.AndroidBRouterSegmentsTreeAccessPrompt;
import vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.brouter.BRouterSegmentsRepository;
import vibro.navigator.settings.AppCompassSettings;

final class NavigationStartupSegmentsTreeAccess {
    private static final String TAG = "NavigationActivity";
    private static final int REQUEST_SEGMENTS_TREE = 3005;

    @NonNull
    private final Activity activity;
    @NonNull
    private final BRouterProfilesRepository profilesRepository =
            AndroidBRouterProfilesRepositoryFactory.create();
    @NonNull
    private final BRouterSegmentsRepository segmentsRepository =
            AndroidBRouterSegmentsRepositoryFactory.create();
    private boolean resumed;
    @Nullable
    private Runnable pendingStartup;

    NavigationStartupSegmentsTreeAccess(@NonNull Activity activity) {
        this.activity = activity;
    }

    void onResume() {
        resumed = true;
        Runnable startup = pendingStartup;
        pendingStartup = null;
        if (startup != null) {
            runWhenResumed(startup);
        }
    }

    void onPause() {
        resumed = false;
    }

    boolean shouldRequest(boolean autoStartNavigation) {
        return autoStartNavigation
                && profilesRepository.isBRouterInstalled(activity)
                && AppCompassSettings.isSurroundingStreetsEnabled(activity)
                && !AndroidLegacyExternalStorageAccess.isRuntimeReadPermissionRelevant()
                && !segmentsRepository.hasPersistedSegmentsTreeAccess(activity);
    }

    void startPicker(@NonNull Runnable continueStartup) {
        AndroidBRouterSegmentsTreeAccessPrompt.showIfAvailable(
                activity,
                segmentsRepository,
                REQUEST_SEGMENTS_TREE,
                TAG,
                () -> {
                    denySurroundingStreetStorage();
                    runWhenResumed(continueStartup);
                },
                () -> runWhenResumed(continueStartup)
        );
    }

    boolean handleActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data,
            @NonNull Runnable afterHandled
    ) {
        if (requestCode != REQUEST_SEGMENTS_TREE) {
            return false;
        }
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null
                && AndroidBRouterFolderAccess.persistReadPermission(activity, data, data.getData())) {
            segmentsRepository.saveSegmentsTreeUri(activity, data.getData());
        } else {
            denySurroundingStreetStorage();
        }
        runWhenResumed(afterHandled);
        return true;
    }

    private void runWhenResumed(@NonNull Runnable startup) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if (resumed) {
            startup.run();
        } else {
            pendingStartup = startup;
        }
    }

    private void denySurroundingStreetStorage() {
        AppCompassSettings.setSurroundingStreetsEnabled(activity, false);
        Toast.makeText(
                activity,
                R.string.msg_compass_surrounding_streets_storage_permission_required,
                Toast.LENGTH_SHORT
        ).show();
    }
}
