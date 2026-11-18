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
    private static final String STATE_MANUAL_PICKER = "brouter_manual_picker";
    private final AndroidBRouterStorageSetup setup;
    private final AndroidBRouterFolderRequests requests;
    private final AndroidBRouterPickerRequest picker;
    private final AndroidBRouterFolderOffers offers;
    private boolean enabled;
    private int discoveryGeneration;
    private boolean discovering;
    private boolean manualDiscovery;
    private boolean manualPending;
    private boolean manualPicker;
    private boolean suppressAutomatic;
    private Runnable deferredOffer = this::refresh;
    private View action;
    private Runnable onPickerFinished = () -> { };

    AndroidBRouterForegroundChecks(Activity activity, AndroidBRouterStorageSetup setup,
                                   AndroidBRouterFolderRequests requests, @Nullable Bundle state) {
        this.setup = setup;
        this.requests = requests;
        picker = new AndroidBRouterPickerRequest(activity, STATE_REQUEST);
        offers = new AndroidBRouterFolderOffers(state);
        manualPending = state != null && state.getBoolean(STATE_MANUAL);
        manualPicker = state != null && state.getBoolean(STATE_MANUAL_PICKER);
        Completion restored = picker.restore(state);
        if (restored != null) {
            finishOffer(restored);
        }
        picker.watch(this::onPickerResult);
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
            suppressAutomatic = false;
            return;
        }
        Runnable offer = deferredOffer;
        deferredOffer = this::refresh;
        offer.run();
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
            return;
        }
        // An explicit tap can supersede an automatic probe already in flight.
        if ((manual || !suppressAutomatic) && !hasActivePicker() && !lookupAlreadyPending(manual)) {
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
                location -> applyOffer(generation, new Offer(location, manual)));
    }

    private void applyOffer(int generation, Offer offer) {
        if (generation != discoveryGeneration) {
            return;
        }
        if (!enabled) {
            deferredOffer = () -> applyOffer(generation, offer);
            return;
        }
        discovering = false;
        if (!offer.hasLocation() || !canShowOffer()) {
            return;
        }
        if (!needsFolderGrant()) {
            refresh();
            return;
        }
        setup.showFolder(offer.location(), () -> {
            manualPending = false;
            manualPicker = offer.manual();
            offers.begin(offer.location().confirmedUri());
            picker.begin(REQUEST_FOLDER);
        }, () -> {
            manualPending = false;
            offers.record(offer.location().confirmedUri());
        }, () -> {
            manualPending = false;
            deferAutomaticOffer();
        });
        setup.refreshAfterDismiss(this::refresh);
    }

    private record Offer(@Nullable AndroidBRouterPickerLocation location, boolean manual) {
        boolean hasLocation() {
            return location != null && (manual || location.confirmedUri() != null);
        }
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
        requests.handleResult(resultCode, data, picker, this::onPickerResult);
        return true;
    }

    private void finishOffer(Completion completion) {
        offers.finish(completion);
        manualPending = completion == Completion.RETRY && manualPicker;
        manualPicker = false;
    }

    private void onPickerResult(Completion completion) {
        finishOffer(completion);
        sourcesChanged();
    }

    void sourcesChanged() {
        onPickerFinished.run();
        refresh();
    }

    void deferAutomaticOffer() {
        suppressAutomatic = true;
    }

    AndroidBRouterFolderOffers offers() {
        return offers;
    }

    void saveState(Bundle state) {
        offers.save(state);
        picker.save(state);
        state.putBoolean(STATE_MANUAL, manualPending);
        state.putBoolean(STATE_MANUAL_PICKER, manualPicker);
    }

    void dispose() {
        setEnabled(false);
        discoveryGeneration++;
        deferredOffer = this::refresh;
        picker.stopWatching();
    }
}
