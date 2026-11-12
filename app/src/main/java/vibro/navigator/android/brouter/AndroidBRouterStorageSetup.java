package vibro.navigator.android.brouter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewConfiguration;
import android.widget.Toast;

import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.brouter.BRouterSegmentsRepository;
import vibro.navigator.settings.AppCompassSettings;

/** Sequences BRouter folder grants during first run and checks for newly available folders on return. */
public final class AndroidBRouterStorageSetup {
    private static final int REQUEST_PROFILES = 1102;
    private static final int REQUEST_SEGMENTS = 1103;
    private static final String TAG = "BRouterStorageSetup";
    private final Activity activity;
    private final BRouterProfilesRepository profiles = AndroidBRouterProfilesRepositoryFactory.create();
    private final BRouterSegmentsRepository segments = AndroidBRouterSegmentsRepositoryFactory.create();
    private final ForegroundChecks foreground;
    private AlertDialog folderDialog;

    public AndroidBRouterStorageSetup(Activity activity, @Nullable Bundle state) {
        this.activity = activity;
        foreground = new ForegroundChecks(state);
    }

    public boolean requestProfiles(Runnable beforeLaunch, Runnable onCancel) {
        return requestForFirstRun(REQUEST_PROFILES, beforeLaunch, onCancel);
    }

    public boolean requestSegments(Runnable beforeLaunch, Runnable onCancel) {
        return requestForFirstRun(REQUEST_SEGMENTS, beforeLaunch, onCancel);
    }

    private boolean requestForFirstRun(int requestCode, Runnable beforeLaunch, Runnable onCancel) {
        if (!needsFolderAccess() || !needsGrant(requestCode)) {
            return false;
        }
        AndroidTaskScheduler.main().postDelayed(() -> {
            if (canShowDialog()) {
                showFolder(requestCode, findInitialUri(requestCode), beforeLaunch, onCancel);
            }
        }, ViewConfiguration.getPressedStateDuration());
        return true;
    }

    public void setForegroundChecksEnabled(boolean enabled) {
        foreground.enabled = enabled;
        foreground.refresh();
    }

    public boolean wasSegmentsFolderOffered() {
        return foreground.segmentsOffered;
    }

    private boolean needsGrant(int requestCode) {
        return requestCode == REQUEST_PROFILES
                ? !profiles.hasPersistedProfilesTreeAccess(activity)
                : AppCompassSettings.isSurroundingStreetsEnabled(activity)
                        && !segments.hasPersistedSegmentsTreeAccess(activity);
    }

    @Nullable
    private Uri findInitialUri(int requestCode) {
        return requestCode == REQUEST_PROFILES
                ? AndroidBRouterProfilesTreeAccessPrompt.findInitialUri(activity, profiles)
                : AndroidBRouterSegmentsTreeAccessPrompt.findInitialUri(activity, segments);
    }

    private void showFolder(int requestCode, @Nullable Uri initialUri, Runnable beforeLaunch, Runnable onCancel) {
        Runnable launch = () -> {
            if (initialUri != null) {
                foreground.setOffered(requestCode, true);
            }
            beforeLaunch.run();
        };
        Runnable cancel = () -> {
            if (initialUri != null) {
                foreground.setOffered(requestCode, true);
            }
            onCancel.run();
        };
        folderDialog = requestCode == REQUEST_PROFILES
                ? AndroidBRouterProfilesTreeAccessPrompt.showNow(activity, initialUri, requestCode, TAG, launch, cancel)
                : AndroidBRouterSegmentsTreeAccessPrompt.showNow(activity, initialUri, requestCode, TAG, launch, cancel);
    }

    public boolean handleForegroundResult(int requestCode, int resultCode, @Nullable Intent data) {
        return foreground.handleResult(requestCode, resultCode, data);
    }

    public boolean handleResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data,
            Runnable onAdvance,
            Runnable onRetry
    ) {
        if (requestCode != REQUEST_PROFILES && requestCode != REQUEST_SEGMENTS) {
            return false;
        }
        if (resultCode == Activity.RESULT_OK && !persist(requestCode, data)) {
            int message = requestCode == REQUEST_PROFILES
                    ? R.string.msg_brouter_profiles_storage_permission_required
                    : R.string.msg_compass_surrounding_streets_storage_permission_required;
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
            onRetry.run();
        } else {
            onAdvance.run();
        }
        return true;
    }

    private boolean persist(int requestCode, @Nullable Intent data) {
        if (data == null) {
            return false;
        }
        Uri uri = data.getData();
        if (uri == null || !AndroidBRouterFolderAccess.persistReadPermission(activity, data, uri)) {
            return false;
        }
        if (requestCode == REQUEST_PROFILES) {
            profiles.saveProfilesTreeUri(activity, uri);
        } else {
            segments.saveSegmentsTreeUri(activity, uri);
        }
        return true;
    }

    private boolean needsFolderAccess() {
        return !AndroidLegacyExternalStorageAccess.isRuntimeReadPermissionRelevant()
                && profiles.isBRouterInstalled(activity);
    }

    private boolean canShowDialog() {
        return !activity.isFinishing() && !activity.isDestroyed();
    }

    public void saveState(Bundle state) {
        foreground.saveState(state);
    }

    public void dispose() {
        foreground.enabled = false;
        if (folderDialog != null) {
            folderDialog.dismiss();
        }
    }

    private final class ForegroundChecks {
        private static final String STATE_PROFILES_OFFERED = "brouter_profiles_offered";
        private static final String STATE_SEGMENTS_OFFERED = "brouter_segments_offered";
        private static final String STATE_REQUEST = "brouter_foreground_request";

        private boolean enabled;
        private boolean profilesOffered;
        private boolean segmentsOffered;
        private int pendingRequest;

        ForegroundChecks(@Nullable Bundle state) {
            if (state != null) {
                profilesOffered = state.getBoolean(STATE_PROFILES_OFFERED);
                segmentsOffered = state.getBoolean(STATE_SEGMENTS_OFFERED);
                pendingRequest = state.getInt(STATE_REQUEST);
            }
        }

        void refresh() {
            if (!enabled || hasActiveRequest() || !canShowDialog() || !needsFolderAccess()) {
                return;
            }
            if (!offerAvailableFolder(REQUEST_PROFILES, profilesOffered)) {
                offerAvailableFolder(REQUEST_SEGMENTS, segmentsOffered);
            }
        }

        private boolean hasActiveRequest() {
            return pendingRequest != 0 || (folderDialog != null && folderDialog.isShowing());
        }

        private boolean offerAvailableFolder(int requestCode, boolean alreadyOffered) {
            if (!needsGrant(requestCode)) {
                setOffered(requestCode, false);
                return false;
            }
            if (alreadyOffered) {
                return false;
            }
            Uri initialUri = findInitialUri(requestCode);
            if (initialUri == null) {
                return false;
            }
            showFolder(requestCode, initialUri, () -> pendingRequest = requestCode, () -> { });
            folderDialog.setOnDismissListener(dialog -> refresh());
            return true;
        }

        boolean handleResult(int requestCode, int resultCode, @Nullable Intent data) {
            if (pendingRequest == 0 || requestCode != pendingRequest) {
                return false;
            }
            pendingRequest = 0;
            return AndroidBRouterStorageSetup.this.handleResult(requestCode, resultCode, data, this::refresh, this::refresh);
        }

        void setOffered(int requestCode, boolean offered) {
            if (requestCode == REQUEST_PROFILES) {
                profilesOffered = offered;
            } else {
                segmentsOffered = offered;
            }
        }

        void saveState(Bundle state) {
            state.putBoolean(STATE_PROFILES_OFFERED, profilesOffered);
            state.putBoolean(STATE_SEGMENTS_OFFERED, segmentsOffered);
            state.putInt(STATE_REQUEST, pendingRequest);
        }
    }
}
