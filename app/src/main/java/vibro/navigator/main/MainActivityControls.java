package vibro.navigator.main;

import vibro.navigator.R;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;

final class MainActivityControls {

    @NonNull
    final ImageButton aboutButton;
    @NonNull
    final Spinner profileSpinner;
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
    final Button addStopButton;
    @NonNull
    final ImageButton startNavButton;

    private MainActivityControls(
            @NonNull ImageButton aboutButton,
            @NonNull Spinner profileSpinner,
            @NonNull EditText destinationEdit,
            @NonNull ImageButton destinationMapButton,
            @NonNull View destinationRow,
            @NonNull MainRouteRailView routeRailView,
            @NonNull LinearLayout stopsContainer,
            @NonNull Button addStopButton,
            @NonNull ImageButton startNavButton
    ) {
        this.aboutButton = aboutButton;
        this.profileSpinner = profileSpinner;
        this.destinationEdit = destinationEdit;
        this.destinationMapButton = destinationMapButton;
        this.destinationRow = destinationRow;
        this.routeRailView = routeRailView;
        this.stopsContainer = stopsContainer;
        this.addStopButton = addStopButton;
        this.startNavButton = startNavButton;
    }

    @NonNull
    static MainActivityControls bind(@NonNull MainActivity activity) {
        return new MainActivityControls(
                activity.findViewById(R.id.aboutButton),
                activity.findViewById(R.id.profileSpinner),
                activity.findViewById(R.id.destinationEdit),
                activity.findViewById(R.id.destinationMapButton),
                activity.findViewById(R.id.destinationRow),
                activity.findViewById(R.id.routeRailView),
                activity.findViewById(R.id.stopsContainer),
                activity.findViewById(R.id.addStopButton),
                activity.findViewById(R.id.startNavButton)
        );
    }
}

