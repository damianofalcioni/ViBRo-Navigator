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
    private static final String STATE_MANUAL = "brouter_manual_pending";
    private final AndroidBRouterStorageSetup setup;
    private final AndroidBRouterFolderRequests requests;
    private final AndroidBRouterPickerRequest picker;
    private final AndroidBRouterFolderOffers offers;
    private boolean enabled;
    private int discoveryGeneration;
    private boolean discovering;
    private boolean manualDiscovery;
    private boolean manualPending;
    private View action;
    private Runnable onPickerFinished = () -> { };

    AndroidBRouterForegroundChecks(Activity activity, AndroidBRouterStorageSetup setup,
                                   AndroidBRouterFolderRequests requests, @Nullable Bundle state) {
        this.setup = setup;
        this.requests = requests;
        picker = new AndroidBRouterPickerRequest(activity, STATE_REQUEST);
        offers = new AndroidBRouterFolderOffers(state);
        manualPending = state != null && state.getBoolean(STATE_MANUAL);
        if (picker.restore(state) == Completion.RETRY) {
            offers.forgetCurrent();
        }
        picker.watch(completion -> {
            if (completion == Completion.RETRY) {
                offers.forgetCurrent();
            }
            pickerFinished();
        });
    }

    void bindAction(View action, Runnable onPickerFinished) {
        this.action = action;
        this.onPickerFinished = onPickerFinished;
        action.setOnClickListener(view -> {
            manualPending = true;
            refresh();
        });
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
        discover(manualPending);
    }

    private void discover(boolean manual) {
        if (!enabled) {
            return;
        }
        boolean needsGrant = needsFolderGrant();
        updateAction(needsGrant);
        if (!needsGrant) {
            manualPending = false;
        }
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
        manualPending = false;
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
        requests.handleResult(resultCode, data, picker, this::pickerFinished, () -> {
            offers.forgetCurrent();
            refresh();
        });
        return true;
    }

    private void pickerFinished() {
        onPickerFinished.run();
        refresh();
    }

    AndroidBRouterFolderOffers offers() {
        return offers;
    }

    void saveState(Bundle state) {
        offers.save(state);
        picker.save(state);
        state.putBoolean(STATE_MANUAL, manualPending);
    }

    void dispose() {
        setEnabled(false);
        picker.stopWatching();
    }
}
