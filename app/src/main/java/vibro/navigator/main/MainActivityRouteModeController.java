package vibro.navigator.main;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.settings.AppMainUiSettings;
import vibro.navigator.settings.AppSettings;

final class MainActivityRouteModeController {
    private static final String STATE_ROUTING_MODE = "vibro.navigator.main.ROUTING_MODE";
    private static final String STATE_ROUND_TRIP_MODE = "vibro.navigator.main.ROUND_TRIP_MODE";
    private static final String STATE_ROUND_TRIP_DISTANCE = "vibro.navigator.main.ROUND_TRIP_DISTANCE";
    private static final String STATE_ROUND_TRIP_DIRECTION = "vibro.navigator.main.ROUND_TRIP_DIRECTION";
    private static final float ENABLED_ALPHA = 1.0f;
    private static final float DISABLED_ALPHA = 0.38f;

    @NonNull
    private final Activity activity;
    @NonNull
    private final Spinner routeModeSpinner;
    @NonNull
    private final TextView profileLabel;
    @NonNull
    private final View profileSelectionPanel;
    @NonNull
    private final Spinner profileSpinner;
    @NonNull
    private final View destinationLabel;
    @NonNull
    private final View routeSetupPanel;
    @NonNull
    private final View roundTripSetupPanel;
    @NonNull
    private final TextView roundTripDistanceLabel;
    @NonNull
    private final EditText roundTripDistanceEdit;
    @NonNull
    private final EditText roundTripDirectionEdit;
    @NonNull
    private final MainActivityRouteModeFocusController focusController;
    @NonNull
    private final MainActivityRoundTripRequestFactory roundTripRequestFactory;
    private boolean brouterInstalled;
    private boolean restoringSelection;
    @NonNull
    private ModeChangeListener modeChangeListener = mode -> {
    };

    MainActivityRouteModeController(
            @NonNull Activity activity,
            @NonNull Spinner routeModeSpinner,
            @NonNull TextView profileLabel,
            @NonNull View profileSelectionPanel,
            @NonNull Spinner profileSpinner,
            @NonNull View destinationLabel,
            @NonNull View routeSetupPanel,
            @NonNull View roundTripSetupPanel,
            @NonNull TextView roundTripDistanceLabel,
            @NonNull EditText roundTripDistanceEdit,
            @NonNull EditText roundTripDirectionEdit
    ) {
        this.activity = activity;
        this.routeModeSpinner = routeModeSpinner;
        this.profileLabel = profileLabel;
        this.profileSelectionPanel = profileSelectionPanel;
        this.profileSpinner = profileSpinner;
        this.destinationLabel = destinationLabel;
        this.routeSetupPanel = routeSetupPanel;
        this.roundTripSetupPanel = roundTripSetupPanel;
        this.roundTripDistanceLabel = roundTripDistanceLabel;
        this.roundTripDistanceEdit = roundTripDistanceEdit;
        this.roundTripDirectionEdit = roundTripDirectionEdit;
        roundTripRequestFactory = new MainActivityRoundTripRequestFactory(
                activity,
                roundTripDistanceEdit,
                roundTripDirectionEdit
        );
        focusController = new MainActivityRouteModeFocusController(
                activity,
                routeModeSpinner,
                routeSetupPanel,
                roundTripSetupPanel
        );
    }

    void configure(@Nullable Bundle savedInstanceState, boolean brouterInstalled) {
        this.brouterInstalled = brouterInstalled;
        restoringSelection = true;
        routeModeSpinner.setAdapter(new MainActivityRouteModeAdapter(activity, brouterInstalled));
        routeModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                NavigationRoutingMode selectedMode = currentRoutingMode();
                if (!isModeEnabled(selectedMode)) {
                    selectMode(NavigationRoutingMode.STRAIGHT_LINE);
                    return;
                }
                render(selectedMode);
                if (!restoringSelection) {
                    AppMainUiSettings.setRoutingMode(activity, selectedMode);
                }
                modeChangeListener.onRouteModeChanged(selectedMode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        if (savedInstanceState != null) {
            roundTripDistanceEdit.setText(savedInstanceState.getString(STATE_ROUND_TRIP_DISTANCE, ""));
            roundTripDirectionEdit.setText(savedInstanceState.getString(STATE_ROUND_TRIP_DIRECTION, ""));
        }
        selectMode(restoredMode(savedInstanceState));
        restoringSelection = false;
        updateDistanceUnitText();
        render(currentRoutingMode());
    }

    void setModeChangeListener(@NonNull ModeChangeListener listener) {
        modeChangeListener = listener;
        listener.onRouteModeChanged(currentRoutingMode());
    }

    void saveState(@NonNull Bundle outState) {
        outState.putString(STATE_ROUTING_MODE, currentRoutingMode().serializedName());
        outState.putBoolean(STATE_ROUND_TRIP_MODE, isRoundTripMode());
        outState.putString(STATE_ROUND_TRIP_DISTANCE, roundTripDistanceEdit.getText().toString());
        outState.putString(STATE_ROUND_TRIP_DIRECTION, roundTripDirectionEdit.getText().toString());
    }

    @NonNull
    NavigationRoutingMode currentRoutingMode() {
        return MainActivityRouteModeOption.modeAt(routeModeSpinner.getSelectedItemPosition());
    }

    boolean isRoundTripMode() {
        return currentRoutingMode() == NavigationRoutingMode.ROUND_TRIP;
    }

    boolean isStraightLineMode() {
        return currentRoutingMode() == NavigationRoutingMode.STRAIGHT_LINE;
    }

    void showRouteMode() {
        selectMode(availableMode(NavigationRoutingMode.BROUTER, brouterInstalled));
        render(currentRoutingMode());
        AppMainUiSettings.setRoutingMode(activity, currentRoutingMode());
        modeChangeListener.onRouteModeChanged(currentRoutingMode());
    }

    void updateDistanceUnitText() {
        boolean imperial = AppSettings.isImperialUnitsEnabled(activity);
        roundTripDistanceLabel.setText(imperial
                ? R.string.label_round_trip_distance_mi
                : R.string.label_round_trip_distance_km);
        roundTripDistanceEdit.setHint(imperial
                ? R.string.hint_round_trip_distance_mi
                : R.string.hint_round_trip_distance_km);
    }

    void startRoundTripNavigation(@NonNull ProfileSelection profileSelection) {
        NavigationRequest request = roundTripRequestFactory.resolveRequest(profileSelection);
        if (request == null) {
            return;
        }
        MainActivityNavigationLauncher.launch(activity, request);
    }

    private void render(@NonNull NavigationRoutingMode mode) {
        boolean roundTripMode = mode == NavigationRoutingMode.ROUND_TRIP;
        boolean restoringRouteSetup = focusController.prepareForRender(roundTripMode);
        destinationLabel.setVisibility(roundTripMode ? View.GONE : View.VISIBLE);
        routeSetupPanel.setVisibility(roundTripMode ? View.GONE : View.VISIBLE);
        roundTripSetupPanel.setVisibility(roundTripMode ? View.VISIBLE : View.GONE);
        renderBRouterProfileSelection(mode != NavigationRoutingMode.STRAIGHT_LINE);
        focusController.completeRender(restoringRouteSetup);
    }

    private void renderBRouterProfileSelection(boolean enabled) {
        int visibility = enabled ? View.VISIBLE : View.GONE;
        profileLabel.setVisibility(visibility);
        profileSelectionPanel.setVisibility(visibility);
        profileLabel.setEnabled(enabled);
        profileSpinner.setEnabled(enabled);
        profileLabel.setAlpha(enabled ? ENABLED_ALPHA : DISABLED_ALPHA);
        profileSpinner.setAlpha(enabled ? ENABLED_ALPHA : DISABLED_ALPHA);
    }

    @NonNull
    private NavigationRoutingMode restoredMode(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return availableMode(AppMainUiSettings.getRoutingMode(activity), brouterInstalled);
        }
        NavigationRoutingMode restored;
        if (savedInstanceState.containsKey(STATE_ROUTING_MODE)) {
            restored = NavigationRoutingMode.fromSerializedName(
                    savedInstanceState.getString(STATE_ROUTING_MODE)
            );
        } else {
            restored = savedInstanceState.getBoolean(STATE_ROUND_TRIP_MODE, false)
                    ? NavigationRoutingMode.ROUND_TRIP
                    : NavigationRoutingMode.BROUTER;
        }
        return availableMode(restored, brouterInstalled);
    }

    @NonNull
    private static NavigationRoutingMode availableMode(
            @NonNull NavigationRoutingMode mode,
            boolean brouterInstalled
    ) {
        return brouterInstalled || mode == NavigationRoutingMode.STRAIGHT_LINE
                ? mode
                : NavigationRoutingMode.STRAIGHT_LINE;
    }

    private boolean isModeEnabled(@NonNull NavigationRoutingMode mode) {
        return availableMode(mode, brouterInstalled) == mode;
    }

    private void selectMode(@NonNull NavigationRoutingMode mode) {
        routeModeSpinner.setSelection(MainActivityRouteModeOption.positionOf(mode), false);
    }

    interface ModeChangeListener {
        void onRouteModeChanged(@NonNull NavigationRoutingMode mode);
    }
}
