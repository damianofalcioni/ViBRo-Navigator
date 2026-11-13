package vibro.navigator.android.brouter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Collections;

import vibro.navigator.android.brouter.AndroidBRouterPickerRequest.Completion;

import static vibro.navigator.android.brouter.AndroidBRouterStorageSetup.REQUEST_FOLDER;

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
    private View action;

    AndroidBRouterForegroundChecks(Activity activity, AndroidBRouterStorageSetup setup,
                                   AndroidBRouterFolderRequests requests, @Nullable Bundle state) {
        this.setup = setup;
        this.requests = requests;
        picker = new AndroidBRouterPickerRequest(activity, STATE_REQUEST);
        offers = new AndroidBRouterFolderOffers(state);
        if (picker.restore(state) == Completion.RETRY) {
            offers.forgetCurrent();
        }
    }

    void bindAction(View action) {
        this.action = action;
        action.setOnClickListener(view -> discover(true));
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
        discover(false);
    }

    private void discover(boolean manual) {
        if (!enabled) {
            return;
        }
        boolean needsGrant = needsFolderGrant();
        updateAction(needsGrant);
        // An explicit tap can supersede an automatic probe already in flight.
        if (needsGrant && !hasActivePicker() && !lookupAlreadyPending(manual)) {
            queueDiscovery(manual);
        }
    }

    private boolean needsFolderGrant() {
        return requests.needsFolderAccess() && requests.needsGrant();
    }

    private void updateAction(boolean needsGrant) {
        if (action != null) {
            action.setVisibility(needsGrant ? View.VISIBLE : View.GONE);
        }
    }

    private boolean lookupAlreadyPending(boolean manual) {
        return discovering && (!manual || manualDiscovery);
    }

    private void queueDiscovery(boolean manual) {
        int generation = ++discoveryGeneration;
        discovering = true;
        manualDiscovery = manual;
        requests.findLocation(manual ? Collections.emptyList() : offers.copy(),
                manual ? AndroidBRouterPickerLocation.browse() : null,
                location -> applyOffer(generation, location, manual));
    }

    private void applyOffer(int generation, @Nullable AndroidBRouterPickerLocation location, boolean manual) {
        if (generation != discoveryGeneration) {
            return;
        }
        discovering = false;
        if (location == null || (!manual && location.confirmedUri() == null) || !canShowOffer()) {
            return;
        }
        if (!needsFolderGrant()) {
            refresh();
            return;
        }
        setup.showFolder(location, () -> {
            offers.begin(location.confirmedUri());
            picker.begin(REQUEST_FOLDER);
        }, () -> { });
        setup.refreshAfterDismiss(this::refresh);
    }

    private boolean canShowOffer() {
        return setup.canShowDialog() && !hasActivePicker();
    }

    private boolean hasActivePicker() {
        return picker.isPending() || setup.isDialogShowing();
    }

    boolean handleResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (!picker.matches(requestCode)) {
            return false;
        }
        requests.handleResult(resultCode, data, picker, this::refresh, () -> {
            offers.forgetCurrent();
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
}
