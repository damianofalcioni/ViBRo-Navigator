package vibro.navigator.android.brouter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;

import vibro.navigator.android.brouter.AndroidBRouterPickerRequest.Completion;

import static vibro.navigator.android.brouter.AndroidBRouterStorageSetup.REQUEST_PROFILES;
import static vibro.navigator.android.brouter.AndroidBRouterStorageSetup.REQUEST_SEGMENTS;

final class AndroidBRouterForegroundChecks {
    private static final String STATE_REQUEST = "brouter_foreground_request";
    private final AndroidBRouterStorageSetup setup;
    private final AndroidBRouterFolderRequests requests;
    private final AndroidBRouterPickerRequest picker;
    private final AndroidBRouterFolderOffers offers;
    private boolean enabled;
    private int discoveryGeneration;
    private boolean discovering;
    private boolean manualDiscovery;
    private View profilesAction;
    private View segmentsAction;

    AndroidBRouterForegroundChecks(Activity activity, AndroidBRouterStorageSetup setup,
                                   AndroidBRouterFolderRequests requests, @Nullable Bundle state) {
        this.setup = setup;
        this.requests = requests;
        picker = new AndroidBRouterPickerRequest(activity, STATE_REQUEST);
        offers = new AndroidBRouterFolderOffers(state);
        if (picker.restore(state) == Completion.RETRY) {
            offers.forgetCurrent(state.getInt(STATE_REQUEST));
        }
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
        boolean supported = requests.needsFolderAccess();
        boolean checkProfiles = supported && requests.needsGrant(REQUEST_PROFILES);
        boolean checkSegments = supported && requests.needsGrant(REQUEST_SEGMENTS);
        updateAction(profilesAction, checkProfiles);
        updateAction(segmentsAction, checkSegments);
        if (hasActivePicker() || (!checkProfiles && !checkSegments)) {
            return;
        }
        // An explicit tap can supersede an automatic probe already in flight.
        if (isAlreadyDiscovering(manualRequest)) {
            return;
        }
        queueDiscovery(manualRequest, checkProfiles, checkSegments);
    }

    private boolean isAlreadyDiscovering(int manualRequest) {
        return discovering && (manualRequest == 0 || manualDiscovery);
    }

    private void queueDiscovery(int manualRequest, boolean checkProfiles, boolean checkSegments) {
        int generation = ++discoveryGeneration;
        discovering = true;
        manualDiscovery = manualRequest != 0;
        if (manualDiscovery) {
            requests.findLocation(manualRequest, Collections.emptySet(), AndroidBRouterPickerLocation.browse(),
                    location -> applyOffer(generation, new FolderOffer(manualRequest, location)));
        } else if (checkProfiles) {
            findAutomaticOffer(generation, REQUEST_PROFILES, checkSegments);
        } else {
            findAutomaticOffer(generation, REQUEST_SEGMENTS, false);
        }
    }

    private void findAutomaticOffer(int generation, int requestCode, boolean checkSegmentsNext) {
        ArrayList<String> excluded = offers.copy(requestCode);
        requests.findLocation(requestCode, excluded, null,
                location -> onAutomaticOffer(generation, requestCode, location, checkSegmentsNext));
    }

    private void onAutomaticOffer(int generation, int requestCode, @Nullable AndroidBRouterPickerLocation location,
                                  boolean checkSegmentsNext) {
        if (generation != discoveryGeneration) {
            return;
        }
        if (location != null && location.confirmedUri() != null) {
            applyOffer(generation, new FolderOffer(requestCode, location));
        } else if (checkSegmentsNext) {
            findAutomaticOffer(generation, REQUEST_SEGMENTS, false);
        } else {
            applyOffer(generation, null);
        }
    }

    private void applyOffer(int generation, @Nullable FolderOffer offer) {
        if (generation != discoveryGeneration) {
            return;
        }
        discovering = false;
        if (offer == null || !setup.canShowDialog() || hasActivePicker()) {
            return;
        }
        if (!requests.needsFolderAccess() || !requests.needsGrant(offer.requestCode)) {
            refresh();
            return;
        }
        setup.showFolder(offer.requestCode, offer.location, () -> {
            offers.begin(offer.location.confirmedUri());
            picker.begin(offer.requestCode);
        }, () -> { });
        setup.refreshAfterDismiss(this::refresh);
    }

    private void updateAction(@Nullable View action, boolean visible) {
        if (action != null) {
            action.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private boolean hasActivePicker() {
        return picker.isPending() || setup.isDialogShowing();
    }

    boolean handleResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (!picker.matches(requestCode)) {
            return false;
        }
        requests.handleResult(requestCode, resultCode, data, picker, this::refresh, () -> {
            offers.forgetCurrent(requestCode);
            refresh();
        });
        return true;
    }

    AndroidBRouterFolderOffers offers() {
        return offers;
    }

    void saveState(Bundle state) {
        offers.save(state);
        picker.save(state);
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
