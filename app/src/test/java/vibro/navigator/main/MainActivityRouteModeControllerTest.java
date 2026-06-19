package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.R;
import vibro.navigator.nav.model.NavigationRoutingMode;

@RunWith(RobolectricTestRunner.class)
public class MainActivityRouteModeControllerTest {
    private static final int ROUTE_MODE_ITEM_PADDING_DP = 14;
    private static final int ROUTE_MODE_ITEM_TEXT_SIZE_SP = 16;

    @Test
    public void configure_usesProfileSpinnerItemStyleForRouteModeSpinner() {
        Fixture fixture = Fixture.create();

        fixture.controller.configure(null, true);

        View selectedView = fixture.routeModeSpinner.getAdapter()
                .getView(0, null, fixture.routeModeSpinner);
        TextView label = selectedView.findViewById(android.R.id.text1);
        assertNotNull(label);
        assertEquals(dp(fixture.activity, ROUTE_MODE_ITEM_PADDING_DP), label.getPaddingTop());
        assertEquals(dp(fixture.activity, ROUTE_MODE_ITEM_PADDING_DP), label.getPaddingBottom());
        assertEquals(sp(fixture.activity, ROUTE_MODE_ITEM_TEXT_SIZE_SP), label.getTextSize(), 0.1f);
    }

    @Test
    public void configure_defaultsToStraightLineAndHidesBRouterProfileControlsWhenBRouterIsMissing() {
        Fixture fixture = Fixture.create();

        fixture.controller.configure(null, false);

        assertEquals(NavigationRoutingMode.STRAIGHT_LINE, fixture.controller.currentRoutingMode());
        assertEquals(View.GONE, fixture.profileLabel.getVisibility());
        assertEquals(View.GONE, fixture.profileSelectionPanel.getVisibility());
        assertFalse(fixture.profileSpinner.isEnabled());
        assertEquals(View.VISIBLE, fixture.destinationLabel.getVisibility());
        assertEquals(View.VISIBLE, fixture.routeSetupPanel.getVisibility());
        assertEquals(View.GONE, fixture.roundTripSetupPanel.getVisibility());
    }

    @Test
    public void selectingStraightLineModeHidesBRouterProfileControls() {
        Fixture fixture = Fixture.create();
        fixture.controller.configure(null, true);

        fixture.routeModeSpinner.setSelection(MainActivityRouteModeOption.positionOf(
                NavigationRoutingMode.STRAIGHT_LINE
        ));

        assertTrue(fixture.controller.isStraightLineMode());
        assertEquals(View.GONE, fixture.profileLabel.getVisibility());
        assertEquals(View.GONE, fixture.profileSelectionPanel.getVisibility());
        assertFalse(fixture.profileSpinner.isEnabled());
        assertEquals(View.VISIBLE, fixture.destinationLabel.getVisibility());
        assertEquals(View.VISIBLE, fixture.routeSetupPanel.getVisibility());
        assertEquals(View.GONE, fixture.roundTripSetupPanel.getVisibility());
    }

    @Test
    public void selectingRoundTripModeKeepsBRouterProfileControlsAndShowsDistanceInput() {
        Fixture fixture = Fixture.create();
        fixture.controller.configure(null, true);

        fixture.routeModeSpinner.setSelection(MainActivityRouteModeOption.positionOf(
                NavigationRoutingMode.ROUND_TRIP
        ));

        assertTrue(fixture.controller.isRoundTripMode());
        assertEquals(View.VISIBLE, fixture.profileLabel.getVisibility());
        assertEquals(View.VISIBLE, fixture.profileSelectionPanel.getVisibility());
        assertTrue(fixture.profileSpinner.isEnabled());
        assertEquals(View.GONE, fixture.destinationLabel.getVisibility());
        assertEquals(View.GONE, fixture.routeSetupPanel.getVisibility());
        assertEquals(View.VISIBLE, fixture.roundTripSetupPanel.getVisibility());
    }

    @Test
    public void updateDistanceUnitText_usesKilometersForMetricRoundTripDistance() {
        Fixture fixture = Fixture.create();

        fixture.controller.configure(null, true);

        assertEquals(
                fixture.activity.getString(R.string.label_round_trip_distance_km),
                fixture.roundTripDistanceLabel.getText().toString()
        );
        assertEquals(
                fixture.activity.getString(R.string.hint_round_trip_distance_km),
                fixture.roundTripDistanceEdit.getHint().toString()
        );
    }

    private static int dp(@NonNull Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static float sp(@NonNull Activity activity, int value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                value,
                activity.getResources().getDisplayMetrics()
        );
    }

    private static final class Fixture {
        @NonNull
        final Activity activity;
        @NonNull
        final Spinner routeModeSpinner;
        @NonNull
        final TextView profileLabel;
        @NonNull
        final View profileSelectionPanel;
        @NonNull
        final Spinner profileSpinner;
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
        final MainActivityRouteModeController controller;

        private Fixture(
                @NonNull Activity activity,
                @NonNull Spinner routeModeSpinner,
                @NonNull TextView profileLabel,
                @NonNull View profileSelectionPanel,
                @NonNull Spinner profileSpinner,
                @NonNull View destinationLabel,
                @NonNull View routeSetupPanel,
                @NonNull View roundTripSetupPanel,
                @NonNull TextView roundTripDistanceLabel,
                @NonNull EditText roundTripDistanceEdit
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
            this.controller = new MainActivityRouteModeController(
                    activity,
                    routeModeSpinner,
                    profileLabel,
                    profileSelectionPanel,
                    profileSpinner,
                    destinationLabel,
                    routeSetupPanel,
                    roundTripSetupPanel,
                    roundTripDistanceLabel,
                    roundTripDistanceEdit
            );
        }

        @NonNull
        static Fixture create() {
            Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
            return new Fixture(
                    activity,
                    new Spinner(activity),
                    new TextView(activity),
                    new LinearLayout(activity),
                    new Spinner(activity),
                    new TextView(activity),
                    new LinearLayout(activity),
                    new LinearLayout(activity),
                    new TextView(activity),
                    new EditText(activity)
            );
        }
    }
}
