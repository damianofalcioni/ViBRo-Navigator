package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.R;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.settings.AppMainUiSettings;

import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class MainActivityRouteModeControllerTest {
    private static final int ROUTE_MODE_ITEM_PADDING_DP = 14;
    private static final int ROUTE_MODE_ITEM_TEXT_SIZE_SP = 16;
    private static final String PREFS_APP_SETTINGS = "vibro.navigator.settings";

    @Before
    public void setUp() {
        ApplicationProvider.getApplicationContext()
                .getSharedPreferences(PREFS_APP_SETTINGS, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

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
    public void configure_disablesBRouterBackedRouteModesWhenBRouterIsMissing() {
        Fixture fixture = Fixture.create();

        fixture.controller.configure(null, false);

        ListAdapter adapter = (ListAdapter) fixture.routeModeSpinner.getAdapter();
        assertEquals(3, adapter.getCount());
        assertFalse(adapter.areAllItemsEnabled());
        assertFalse(adapter.isEnabled(MainActivityRouteModeOption.positionOf(NavigationRoutingMode.BROUTER)));
        assertFalse(adapter.isEnabled(MainActivityRouteModeOption.positionOf(NavigationRoutingMode.ROUND_TRIP)));
        assertTrue(adapter.isEnabled(MainActivityRouteModeOption.positionOf(NavigationRoutingMode.STRAIGHT_LINE)));
    }

    @Test
    public void configure_showsInfoButtonForEveryRouteModeDropdownEntry() {
        Fixture fixture = Fixture.create();

        fixture.controller.configure(null, true);

        SpinnerAdapter adapter = fixture.routeModeSpinner.getAdapter();
        for (int position = 0; position < adapter.getCount(); position++) {
            View row = adapter.getDropDownView(position, null, fixture.routeModeSpinner);
            ImageButton infoButton = row.findViewById(R.id.profileInfoButton);
            String label = fixture.activity.getString(MainActivityRouteModeOption.labelResAt(position));
            assertNotNull(infoButton);
            assertEquals(View.VISIBLE, infoButton.getVisibility());
            assertEquals(
                    fixture.activity.getString(R.string.format_route_mode_info_content_description, label),
                    infoButton.getContentDescription().toString()
            );
            assertFalse(infoButton.isClickable());
            assertFalse(infoButton.isFocusable());
            assertFalse(infoButton.isFocusableInTouchMode());
        }
    }

    @Test
    public void configure_keepsInfoButtonVisibleForDisabledBRouterModes() {
        Fixture fixture = Fixture.create();

        fixture.controller.configure(null, false);

        SpinnerAdapter adapter = fixture.routeModeSpinner.getAdapter();
        assertInfoButtonVisibleFor(adapter, fixture, NavigationRoutingMode.BROUTER);
        assertInfoButtonVisibleFor(adapter, fixture, NavigationRoutingMode.ROUND_TRIP);
    }

    @Test
    public void configure_coercesRestoredBRouterModeToStraightLineWhenBRouterIsMissing() {
        Fixture savedFixture = Fixture.create();
        savedFixture.controller.configure(null, true);
        savedFixture.routeModeSpinner.setSelection(MainActivityRouteModeOption.positionOf(
                NavigationRoutingMode.ROUND_TRIP
        ));
        Bundle state = new Bundle();
        savedFixture.controller.saveState(state);

        Fixture fixture = Fixture.create();
        fixture.controller.configure(state, false);

        assertEquals(NavigationRoutingMode.STRAIGHT_LINE, fixture.controller.currentRoutingMode());
        assertEquals(View.GONE, fixture.roundTripSetupPanel.getVisibility());
        assertEquals(View.VISIBLE, fixture.routeSetupPanel.getVisibility());
    }

    @Test
    public void configure_restoresPersistedRouteModeOnColdStart() {
        Fixture fixture = Fixture.create();
        AppMainUiSettings.setRoutingMode(fixture.activity, NavigationRoutingMode.ROUND_TRIP);

        fixture.controller.configure(null, true);

        assertEquals(NavigationRoutingMode.ROUND_TRIP, fixture.controller.currentRoutingMode());
        assertEquals(View.VISIBLE, fixture.roundTripSetupPanel.getVisibility());
        assertEquals(View.GONE, fixture.routeSetupPanel.getVisibility());
    }

    @Test
    public void configure_coercesPersistedBRouterModeToStraightLineWhenBRouterIsMissing() {
        Fixture fixture = Fixture.create();
        AppMainUiSettings.setRoutingMode(fixture.activity, NavigationRoutingMode.ROUND_TRIP);

        fixture.controller.configure(null, false);

        assertEquals(NavigationRoutingMode.STRAIGHT_LINE, fixture.controller.currentRoutingMode());
        assertEquals(View.GONE, fixture.roundTripSetupPanel.getVisibility());
        assertEquals(View.VISIBLE, fixture.routeSetupPanel.getVisibility());
    }

    @Test
    public void selectingRouteModePersistsForNextColdStart() {
        Fixture selectedFixture = Fixture.create();
        selectedFixture.controller.configure(null, true);
        selectedFixture.routeModeSpinner.setSelection(MainActivityRouteModeOption.positionOf(
                NavigationRoutingMode.STRAIGHT_LINE
        ));

        Fixture restoredFixture = Fixture.create();
        restoredFixture.controller.configure(null, true);

        assertEquals(NavigationRoutingMode.STRAIGHT_LINE, restoredFixture.controller.currentRoutingMode());
        assertEquals(View.GONE, restoredFixture.profileLabel.getVisibility());
        assertEquals(View.GONE, restoredFixture.profileSelectionPanel.getVisibility());
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
    public void selectingRouteModeAfterRoundTripSuppressesRestoredDestinationFocus() {
        assertReturningFromRoundTripSuppressesRestoredDestinationFocus(NavigationRoutingMode.BROUTER);
    }

    @Test
    public void selectingStraightLineModeAfterRoundTripSuppressesRestoredDestinationFocus() {
        assertReturningFromRoundTripSuppressesRestoredDestinationFocus(NavigationRoutingMode.STRAIGHT_LINE);
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

    private static void assertReturningFromRoundTripSuppressesRestoredDestinationFocus(
            @NonNull NavigationRoutingMode targetMode
    ) {
        Fixture fixture = Fixture.create();
        fixture.controller.configure(null, true);
        fixture.routeModeSpinner.setSelection(MainActivityRouteModeOption.positionOf(
                NavigationRoutingMode.ROUND_TRIP
        ));
        fixture.roundTripDistanceEdit.requestFocus();

        fixture.routeModeSpinner.setSelection(MainActivityRouteModeOption.positionOf(targetMode));
        fixture.destinationEdit.requestFocus();
        shadowOf(Looper.getMainLooper()).idleFor(150, TimeUnit.MILLISECONDS);

        assertFalse(fixture.destinationEdit.hasFocus());
        assertFalse(fixture.roundTripDistanceEdit.hasFocus());
        assertTrue(fixture.routeModeSpinner.hasFocus());
    }

    private static void assertInfoButtonVisibleFor(
            @NonNull SpinnerAdapter adapter,
            @NonNull Fixture fixture,
            @NonNull NavigationRoutingMode mode
    ) {
        View row = adapter.getDropDownView(
                MainActivityRouteModeOption.positionOf(mode),
                null,
                fixture.routeModeSpinner
        );
        ImageButton infoButton = row.findViewById(R.id.profileInfoButton);
        assertNotNull(infoButton);
        assertEquals(View.VISIBLE, infoButton.getVisibility());
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
        final EditText destinationEdit;
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
                @NonNull EditText roundTripDistanceEdit,
                @NonNull EditText destinationEdit
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
            this.destinationEdit = destinationEdit;
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
            LinearLayout root = new LinearLayout(activity);
            root.setFocusableInTouchMode(true);
            Spinner routeModeSpinner = new Spinner(activity);
            LinearLayout profileSelectionPanel = new LinearLayout(activity);
            Spinner profileSpinner = new Spinner(activity);
            LinearLayout routeSetupPanel = new LinearLayout(activity);
            EditText destinationEdit = new EditText(activity);
            LinearLayout roundTripSetupPanel = new LinearLayout(activity);
            EditText roundTripDistanceEdit = new EditText(activity);
            routeSetupPanel.addView(destinationEdit);
            roundTripSetupPanel.addView(roundTripDistanceEdit);
            root.addView(routeModeSpinner);
            root.addView(profileSelectionPanel);
            root.addView(routeSetupPanel);
            root.addView(roundTripSetupPanel);
            activity.setContentView(root);
            return new Fixture(
                    activity,
                    routeModeSpinner,
                    new TextView(activity),
                    profileSelectionPanel,
                    profileSpinner,
                    new TextView(activity),
                    routeSetupPanel,
                    roundTripSetupPanel,
                    new TextView(activity),
                    roundTripDistanceEdit,
                    destinationEdit
            );
        }
    }
}
