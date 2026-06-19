package vibro.navigator.main;

import vibro.navigator.R;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

final class MainActivityControls {

    @NonNull
    final ImageButton aboutButton;
    @NonNull
    final Spinner routeModeSpinner;
    @NonNull
    final TextView profileLabel;
    @NonNull
    final Spinner profileSpinner;
    @NonNull
    final ImageButton profileSettingsButton;
    @NonNull
    final View destinationLabel;
    @NonNull
    final View routeSetupPanel;
    @NonNull
    final View roundTripSetupPanel;
    @NonNull
    final TextView roundTripDistanceLabel;
    @NonNull
    final EditText roundTripDistanceEdit;
    @NonNull
    final EditText destinationEdit;
    @NonNull
    final ImageButton destinationMapButton;
    @NonNull
    final View destinationRow;
    @NonNull
    final MainRouteRailView routeRailView;
    @NonNull
    final LinearLayout stopsContainer;
    @NonNull
    final ImageButton saveRouteButton;
    @NonNull
    final Button addStopButton;
    @NonNull
    final ImageButton restoreRouteButton;
    @NonNull
    final ImageButton startNavButton;
    @NonNull
    final ImageButton roundTripStartNavButton;

    private MainActivityControls(
            @NonNull ImageButton aboutButton,
            @NonNull Spinner routeModeSpinner,
            @NonNull TextView profileLabel,
            @NonNull Spinner profileSpinner,
            @NonNull ImageButton profileSettingsButton,
            @NonNull View destinationLabel,
            @NonNull View routeSetupPanel,
            @NonNull View roundTripSetupPanel,
            @NonNull TextView roundTripDistanceLabel,
            @NonNull EditText roundTripDistanceEdit,
            @NonNull EditText destinationEdit,
            @NonNull ImageButton destinationMapButton,
            @NonNull View destinationRow,
            @NonNull MainRouteRailView routeRailView,
            @NonNull LinearLayout stopsContainer,
            @NonNull ImageButton saveRouteButton,
            @NonNull Button addStopButton,
            @NonNull ImageButton restoreRouteButton,
            @NonNull ImageButton startNavButton,
            @NonNull ImageButton roundTripStartNavButton
    ) {
        this.aboutButton = aboutButton;
        this.routeModeSpinner = routeModeSpinner;
        this.profileLabel = profileLabel;
        this.profileSpinner = profileSpinner;
        this.profileSettingsButton = profileSettingsButton;
        this.destinationLabel = destinationLabel;
        this.routeSetupPanel = routeSetupPanel;
        this.roundTripSetupPanel = roundTripSetupPanel;
        this.roundTripDistanceLabel = roundTripDistanceLabel;
        this.roundTripDistanceEdit = roundTripDistanceEdit;
        this.destinationEdit = destinationEdit;
        this.destinationMapButton = destinationMapButton;
        this.destinationRow = destinationRow;
        this.routeRailView = routeRailView;
        this.stopsContainer = stopsContainer;
        this.saveRouteButton = saveRouteButton;
        this.addStopButton = addStopButton;
        this.restoreRouteButton = restoreRouteButton;
        this.startNavButton = startNavButton;
        this.roundTripStartNavButton = roundTripStartNavButton;
    }

    @NonNull
    static MainActivityControls bind(@NonNull MainActivity activity) {
        return new MainActivityControls(
                activity.findViewById(R.id.aboutButton),
                activity.findViewById(R.id.routeModeSpinner),
                activity.findViewById(R.id.profileLabel),
                activity.findViewById(R.id.profileSpinner),
                activity.findViewById(R.id.profileSettingsButton),
                activity.findViewById(R.id.destinationLabel),
                activity.findViewById(R.id.routeSetupPanel),
                activity.findViewById(R.id.roundTripSetupPanel),
                activity.findViewById(R.id.roundTripDistanceLabel),
                activity.findViewById(R.id.roundTripDistanceEdit),
                activity.findViewById(R.id.destinationEdit),
                activity.findViewById(R.id.destinationMapButton),
                activity.findViewById(R.id.destinationRow),
                activity.findViewById(R.id.routeRailView),
                activity.findViewById(R.id.stopsContainer),
                activity.findViewById(R.id.saveRouteButton),
                activity.findViewById(R.id.addStopButton),
                activity.findViewById(R.id.restoreRouteButton),
                activity.findViewById(R.id.startNavButton),
                activity.findViewById(R.id.roundTripStartNavButton)
        );
    }
}

