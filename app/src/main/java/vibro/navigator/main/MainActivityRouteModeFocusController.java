package vibro.navigator.main;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;

final class MainActivityRouteModeFocusController {
    private static final long RESTORED_TEXT_FOCUS_GUARD_MS = 100L;

    @NonNull
    private final Activity activity;
    @NonNull
    private final Spinner routeModeSpinner;
    @NonNull
    private final View routeSetupPanel;
    @NonNull
    private final View roundTripSetupPanel;

    MainActivityRouteModeFocusController(
            @NonNull Activity activity,
            @NonNull Spinner routeModeSpinner,
            @NonNull View routeSetupPanel,
            @NonNull View roundTripSetupPanel
    ) {
        this.activity = activity;
        this.routeModeSpinner = routeModeSpinner;
        this.routeSetupPanel = routeSetupPanel;
        this.roundTripSetupPanel = roundTripSetupPanel;
    }

    boolean prepareForRender(boolean roundTripMode) {
        if (roundTripMode) {
            clearTextFocusInside(routeSetupPanel);
            return false;
        }
        boolean restoringRouteSetup = roundTripSetupPanel.getVisibility() == View.VISIBLE;
        if (restoringRouteSetup) {
            clearTextFocusInside(roundTripSetupPanel);
            blockRouteSetupDescendantFocusTemporarily();
            parkFocusOnRouteModeSelector();
        }
        return restoringRouteSetup;
    }

    void completeRender(boolean restoringRouteSetup) {
        if (!restoringRouteSetup) {
            return;
        }
        suppressRestoredRouteSetupTextFocus();
    }

    private void suppressRestoredRouteSetupTextFocus() {
        clearTextFocusInside(routeSetupPanel);
        routeSetupPanel.post(() -> clearTextFocusInside(routeSetupPanel));
        routeSetupPanel.postDelayed(
                () -> clearTextFocusInside(routeSetupPanel),
                RESTORED_TEXT_FOCUS_GUARD_MS
        );
    }

    private void clearTextFocusInside(@NonNull View container) {
        View focusedView = activity.getCurrentFocus();
        if (!(focusedView instanceof EditText) || !isInside(focusedView, container)) {
            return;
        }
        hideKeyboard(focusedView);
        focusedView.clearFocus();
        parkFocusOnRouteModeSelector();
    }

    private void hideKeyboard(@NonNull View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void parkFocusOnRouteModeSelector() {
        routeModeSpinner.setFocusableInTouchMode(true);
        routeModeSpinner.requestFocus();
    }

    private void blockRouteSetupDescendantFocusTemporarily() {
        if (!(routeSetupPanel instanceof ViewGroup)) {
            return;
        }
        ViewGroup routeSetupGroup = (ViewGroup) routeSetupPanel;
        int originalFocusability = routeSetupGroup.getDescendantFocusability();
        routeSetupGroup.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        routeSetupGroup.post(() -> restoreRouteSetupDescendantFocus(
                routeSetupGroup,
                originalFocusability
        ));
        routeSetupGroup.postDelayed(() -> restoreRouteSetupDescendantFocus(
                routeSetupGroup,
                originalFocusability
        ), RESTORED_TEXT_FOCUS_GUARD_MS);
    }

    private static void restoreRouteSetupDescendantFocus(
            @NonNull ViewGroup routeSetupGroup,
            int originalFocusability
    ) {
        if (routeSetupGroup.getDescendantFocusability() == ViewGroup.FOCUS_BLOCK_DESCENDANTS) {
            routeSetupGroup.setDescendantFocusability(originalFocusability);
        }
    }

    private static boolean isInside(@NonNull View child, @NonNull View possibleAncestor) {
        View current = child;
        while (true) {
            if (current == possibleAncestor) {
                return true;
            }
            ViewParent parent = current.getParent();
            if (!(parent instanceof View)) {
                return false;
            }
            current = (View) parent;
        }
    }
}
