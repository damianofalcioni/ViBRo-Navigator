package vibro.navigator.main;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.settings.AppSettings;

import java.util.Collections;

final class MainActivityRouteModeController {
    private static final String STATE_ROUND_TRIP_MODE = "vibro.navigator.main.ROUND_TRIP_MODE";
    private static final String STATE_ROUND_TRIP_DISTANCE = "vibro.navigator.main.ROUND_TRIP_DISTANCE";

    @NonNull
    private final MainActivity activity;
    @NonNull
    private final RadioGroup routeModeTabs;
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

    MainActivityRouteModeController(
            @NonNull MainActivity activity,
            @NonNull RadioGroup routeModeTabs,
            @NonNull View destinationLabel,
            @NonNull View routeSetupPanel,
            @NonNull View roundTripSetupPanel,
            @NonNull TextView roundTripDistanceLabel,
            @NonNull EditText roundTripDistanceEdit
    ) {
        this.activity = activity;
        this.routeModeTabs = routeModeTabs;
        this.destinationLabel = destinationLabel;
        this.routeSetupPanel = routeSetupPanel;
        this.roundTripSetupPanel = roundTripSetupPanel;
        this.roundTripDistanceLabel = roundTripDistanceLabel;
        this.roundTripDistanceEdit = roundTripDistanceEdit;
    }

    void configure(@Nullable Bundle savedInstanceState) {
        routeModeTabs.setOnCheckedChangeListener((group, checkedId) ->
                render(checkedId == R.id.routeModeRoundTripTab));
        if (savedInstanceState != null) {
            roundTripDistanceEdit.setText(savedInstanceState.getString(STATE_ROUND_TRIP_DISTANCE, ""));
        }
        boolean roundTripMode = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_ROUND_TRIP_MODE, false);
        routeModeTabs.check(roundTripMode ? R.id.routeModeRoundTripTab : R.id.routeModeRouteTab);
        updateDistanceUnitText();
        render(roundTripMode);
    }

    void saveState(@NonNull Bundle outState) {
        outState.putBoolean(STATE_ROUND_TRIP_MODE, isRoundTripMode());
        outState.putString(STATE_ROUND_TRIP_DISTANCE, roundTripDistanceEdit.getText().toString());
    }

    boolean isRoundTripMode() {
        return routeModeTabs.getCheckedRadioButtonId() == R.id.routeModeRoundTripTab;
    }

    void showRouteMode() {
        routeModeTabs.check(R.id.routeModeRouteTab);
    }

    void updateDistanceUnitText() {
        boolean imperial = AppSettings.isImperialUnitsEnabled(activity);
        roundTripDistanceLabel.setText(imperial
                ? R.string.label_round_trip_distance_mi
                : R.string.label_round_trip_distance_m);
        roundTripDistanceEdit.setHint(imperial
                ? R.string.hint_round_trip_distance_mi
                : R.string.hint_round_trip_distance_m);
    }

    @Nullable
    Integer resolveDistanceMeters() {
        Integer meters = RoundTripDistanceInput.parseDistanceMeters(
                roundTripDistanceEdit.getText(),
                AppSettings.isImperialUnitsEnabled(activity)
        );
        if (meters == null) {
            Toast.makeText(activity, R.string.msg_invalid_round_trip_distance, Toast.LENGTH_SHORT).show();
        }
        return meters;
    }

    void startRoundTripNavigation(@NonNull ProfileSelection profileSelection) {
        Integer distanceMeters = resolveDistanceMeters();
        if (distanceMeters == null) {
            return;
        }
        NavigationRequest request = resolveRequest(profileSelection, distanceMeters);
        if (request == null) {
            return;
        }
        MainActivityNavigationLauncher.launch(activity, request);
    }

    private void render(boolean roundTripMode) {
        destinationLabel.setVisibility(roundTripMode ? View.GONE : View.VISIBLE);
        routeSetupPanel.setVisibility(roundTripMode ? View.GONE : View.VISIBLE);
        roundTripSetupPanel.setVisibility(roundTripMode ? View.VISIBLE : View.GONE);
    }

    @Nullable
    private NavigationRequest resolveRequest(@NonNull ProfileSelection profileSelection, int distanceMeters) {
        if (profileSelection.routingMode != NavigationRoutingMode.BROUTER
                || profileSelection.profileName == null
                || profileSelection.profileName.trim().isEmpty()) {
            Toast.makeText(activity, R.string.msg_round_trip_requires_brouter_profile, Toast.LENGTH_SHORT).show();
            return null;
        }
        return new NavigationRequest(
                NavigationRoutingMode.ROUND_TRIP,
                profileSelection.profileName,
                profileSelection.profileParameters,
                null,
                null,
                Collections.emptyList(),
                distanceMeters
        );
    }
}
