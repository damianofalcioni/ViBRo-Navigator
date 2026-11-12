package vibro.navigator.android.brouter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Toast;

import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.android.brouter.AndroidBRouterFolderAccess.Kind;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.brouter.BRouterSegmentsRepository;
import vibro.navigator.settings.AppCompassSettings;

/** Sequences first-run folder grants and owns manual and automatic main-screen requests. */
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
        AndroidTaskScheduler.main().postDelayed(() -> discoverFirstRun(requestCode, beforeLaunch, onCancel),
                ViewConfiguration.getPressedStateDuration());
        return true;
    }

    private void discoverFirstRun(int requestCode, Runnable beforeLaunch, Runnable onCancel) {
        if (canShowDialog()) {
            AndroidBRouterFolderDiscovery.find(() -> findLocation(requestCode), AndroidBRouterPickerLocation.browse(),
                    location -> {
                        if (canShowDialog()) {
                            showFolder(requestCode, location, beforeLaunch, onCancel);
                        }
                    });
        }
    }

    public void bindManualActions(View profilesAction, View segmentsAction) {
        foreground.bindActions(profilesAction, segmentsAction);
    }

    public void setForegroundChecksEnabled(boolean enabled) {
        foreground.setEnabled(enabled);
    }

    @Nullable
    public String skippedSegmentsFolder() {
        return foreground.segmentsOffered;
    }

    private boolean needsGrant(int requestCode) {
        return requestCode == REQUEST_PROFILES
                ? !profiles.hasPersistedProfilesTreeAccess(activity)
                : AppCompassSettings.isSurroundingStreetsEnabled(activity)
                        && !segments.hasPersistedSegmentsTreeAccess(activity);
    }

    private AndroidBRouterPickerLocation findLocation(int requestCode) {
        return requestCode == REQUEST_PROFILES
                ? AndroidBRouterProfilesTreeAccessPrompt.findLocation(activity, profiles)
                : AndroidBRouterSegmentsTreeAccessPrompt.findLocation(activity, segments);
    }

    private void showFolder(int requestCode, AndroidBRouterPickerLocation location, Runnable beforeLaunch, Runnable onCancel) {
        Runnable launch = () -> {
            foreground.setOffered(requestCode, location.confirmedUri());
            beforeLaunch.run();
        };
        Runnable cancel = () -> {
            foreground.setOffered(requestCode, location.confirmedUri());
            onCancel.run();
        };
        folderDialog = requestCode == REQUEST_PROFILES
                ? AndroidBRouterProfilesTreeAccessPrompt.showNow(activity, location.initialUri, requestCode, TAG,
                        launch, cancel, cancel)
                : AndroidBRouterSegmentsTreeAccessPrompt.showNow(activity, location.initialUri, requestCode, TAG,
                        launch, cancel, cancel);
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
            if (resultCode == Activity.RESULT_OK) {
                foreground.setOffered(requestCode, null);
            }
            onAdvance.run();
        }
        return true;
    }

    private boolean persist(int requestCode, @Nullable Intent data) {
        Kind kind = requestCode == REQUEST_PROFILES ? Kind.PROFILES : Kind.SEGMENTS;
        Uri uri = AndroidBRouterFolderAccess.persistSelection(activity, data, kind);
        if (uri == null) {
            return false;
        }
        if (kind == Kind.PROFILES) {
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
        foreground.setEnabled(false);
        if (folderDialog != null) {
            folderDialog.dismiss();
        }
    }

    private final class ForegroundChecks {
        private static final String STATE_PROFILES_OFFERED = "brouter_profiles_offered_uri";
        private static final String STATE_SEGMENTS_OFFERED = "brouter_segments_offered_uri";
        private static final String STATE_REQUEST = "brouter_foreground_request";
        private final AndroidBRouterPickerRequest picker = new AndroidBRouterPickerRequest(activity, STATE_REQUEST);
        private boolean enabled;
        private String profilesOffered;
        private String segmentsOffered;
        private int discoveryGeneration;
        private boolean discovering;
        private View profilesAction;
        private View segmentsAction;

        ForegroundChecks(@Nullable Bundle state) {
            if (state != null) {
                profilesOffered = state.getString(STATE_PROFILES_OFFERED);
                segmentsOffered = state.getString(STATE_SEGMENTS_OFFERED);
            }
            picker.restore(state);
        }

        void bindActions(View profilesAction, View segmentsAction) {
            this.profilesAction = profilesAction;
            this.segmentsAction = segmentsAction;
            profilesAction.setOnClickListener(view -> discover(REQUEST_PROFILES));
            segmentsAction.setOnClickListener(view -> discover(REQUEST_SEGMENTS));
        }

        void setEnabled(boolean enabled) {
            this.enabled = enabled;
            if (!enabled) {
                discoveryGeneration++;
                discovering = false;
                return;
            }
            refresh();
        }

        void refresh() {
            discover(0);
        }

        private void discover(int manualRequest) {
            if (!enabled) {
                return;
            }
            boolean supported = needsFolderAccess();
            boolean checkProfiles = supported && needsGrant(REQUEST_PROFILES);
            boolean checkSegments = supported && needsGrant(REQUEST_SEGMENTS);
            updateAction(profilesAction, checkProfiles);
            updateAction(segmentsAction, checkSegments);
            if (hasActivePicker() || (!checkProfiles && !checkSegments)) {
                return;
            }
            // An explicit tap can supersede an automatic probe already in flight.
            if (discovering && manualRequest == 0) {
                return;
            }
            queueDiscovery(manualRequest, checkProfiles, checkSegments);
        }

        private void queueDiscovery(int manualRequest, boolean checkProfiles, boolean checkSegments) {
            int generation = ++discoveryGeneration;
            discovering = true;
            String lastProfilesOffer = profilesOffered;
            String lastSegmentsOffer = segmentsOffered;
            if (manualRequest == 0) {
                AndroidBRouterFolderDiscovery.find(
                        () -> findAutomaticOffer(checkProfiles, checkSegments, lastProfilesOffer, lastSegmentsOffer),
                        null, offer -> applyOffer(generation, offer));
            } else {
                AndroidBRouterFolderDiscovery.find(() -> findLocation(manualRequest), AndroidBRouterPickerLocation.browse(),
                        location -> applyOffer(generation, new FolderOffer(manualRequest, location)));
            }
        }

        private FolderOffer findAutomaticOffer(boolean checkProfiles, boolean checkSegments,
                                               @Nullable String lastProfilesOffer, @Nullable String lastSegmentsOffer) {
            if (checkProfiles) {
                AndroidBRouterPickerLocation location = findLocation(REQUEST_PROFILES);
                if (isNewOffer(location, lastProfilesOffer)) {
                    return new FolderOffer(REQUEST_PROFILES, location);
                }
            }
            if (checkSegments) {
                AndroidBRouterPickerLocation location = findLocation(REQUEST_SEGMENTS);
                if (isNewOffer(location, lastSegmentsOffer)) {
                    return new FolderOffer(REQUEST_SEGMENTS, location);
                }
            }
            return null;
        }

        private boolean isNewOffer(AndroidBRouterPickerLocation location, @Nullable String offered) {
            Uri uri = location.confirmedUri();
            return uri != null && !uri.toString().equals(offered);
        }

        private void applyOffer(int generation, @Nullable FolderOffer offer) {
            if (generation != discoveryGeneration) {
                return;
            }
            discovering = false;
            if (offer == null || !canShowDialog() || hasActivePicker()) {
                return;
            }
            if (!needsFolderAccess() || !needsGrant(offer.requestCode)) {
                refresh();
                return;
            }
            showFolder(offer.requestCode, offer.location, () -> picker.begin(offer.requestCode), () -> { });
            folderDialog.setOnDismissListener(dialog -> refresh());
        }

        private void updateAction(@Nullable View action, boolean visible) {
            if (action != null) {
                action.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        }

        private boolean hasActivePicker() {
            return picker.isPending() || (folderDialog != null && folderDialog.isShowing());
        }

        boolean handleResult(int requestCode, int resultCode, @Nullable Intent data) {
            if (!picker.matches(requestCode)) {
                return false;
            }
            picker.complete();
            return AndroidBRouterStorageSetup.this.handleResult(requestCode, resultCode, data, this::refresh, () -> {
                setOffered(requestCode, null);
                refresh();
            });
        }

        void setOffered(int requestCode, @Nullable Uri uri) {
            String offered = uri == null ? null : uri.toString();
            if (requestCode == REQUEST_PROFILES) {
                profilesOffered = offered;
            } else {
                segmentsOffered = offered;
            }
        }

        void saveState(Bundle state) {
            state.putString(STATE_PROFILES_OFFERED, profilesOffered);
            state.putString(STATE_SEGMENTS_OFFERED, segmentsOffered);
            picker.save(state);
        }
    }

    private static final class FolderOffer {
        final int requestCode;
        final AndroidBRouterPickerLocation location;

        FolderOffer(int requestCode, AndroidBRouterPickerLocation location) {
            this.requestCode = requestCode;
            this.location = location;
        }
    }
}
