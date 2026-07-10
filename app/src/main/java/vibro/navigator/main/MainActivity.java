package vibro.navigator.main;

import vibro.navigator.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.poi.PoiHistoryStore;
import vibro.navigator.poi.search.PoiSearchClient;
import vibro.navigator.poi.search.PoiSearchClients;
import vibro.navigator.poi.ui.PoiInputController;
import vibro.navigator.poi.ui.PoiReverseGeocodeController;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.android.display.AndroidDisplayRotationProvider;
import vibro.navigator.android.sensor.AndroidGeomagneticOrientationMonitor;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.android.time.AndroidElapsedRealtimeClock;
import vibro.navigator.nav.model.NavigationRoutingMode;

// Android entry point: keep workflow logic delegated without hiding required screen collaborators behind a generic facade.
public class MainActivity extends Activity {

    public static final String EXTRA_OPEN_NAVIGATION = "open_navigation";

    private static final String TAG = "MainActivity";

    private PoiInputController destinationController;
    private PoiHistoryStore historyStore;
    private MainActivityProfileCoordinator profileCoordinator;
    private MainActivityStopController stopController;
    private MainActivityMapPickerCoordinator mapPickerCoordinator;
    private PoiReverseGeocodeController reverseGeocodeController;
    private MainActivityRouteModeController routeModeController;
    private MainActivityRoundTripDirectionController roundTripDirectionController;
    private boolean appliedLightTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        appliedLightTheme = AndroidAppTheme.apply(this);
        super.onCreate(savedInstanceState);
        if (MainActivityIntentHandler.redirectGpxImportDuringActiveNavigation(this, getIntent())) {
            finish();
            return;
        }
        setContentView(R.layout.activity_main);
        AppLogger.i(TAG, "onCreate savedState=" + (savedInstanceState != null)
                + " intent=" + MainActivityIntentHandler.describeIntent(getIntent()));
        reverseGeocodeController = PoiReverseGeocodeController.createDefault(this);
        mapPickerCoordinator = new MainActivityMapPickerCoordinator(this, reverseGeocodeController);
        MainActivityControls controls = MainActivityControls.bind(this);

        MainActivityAboutLauncher.configure(this, controls.aboutButton);
        profileCoordinator = MainActivityProfileCoordinator.configure(this, controls);
        boolean brouterInstalled = profileCoordinator.isBRouterInstalled();
        routeModeController = new MainActivityRouteModeController(
                this,
                controls.routeModeSpinner,
                controls.profileLabel,
                controls.profileSelectionPanel,
                controls.profileSpinner,
                controls.destinationLabel,
                controls.routeSetupPanel,
                controls.roundTripSetupPanel,
                controls.roundTripDistanceLabel,
                controls.roundTripDistanceEdit,
                controls.roundTripDirectionEdit
        );
        roundTripDirectionController = new MainActivityRoundTripDirectionController(
                controls.roundTripDirectionEdit,
                controls.roundTripDirectionCompass,
                callback -> new AndroidGeomagneticOrientationMonitor(this, callback),
                new AndroidDisplayRotationProvider(this),
                AndroidElapsedRealtimeClock.INSTANCE
        );
        routeModeController.configure(savedInstanceState, brouterInstalled);
        routeModeController.setModeChangeListener(mode -> {
            profileCoordinator.onRouteModeChanged(mode);
            controls.routeRailView.setStraightLineMode(mode == NavigationRoutingMode.STRAIGHT_LINE);
            roundTripDirectionController.onRouteModeChanged(mode);
        });
        AppLogger.i(TAG, "BRouter installed=" + brouterInstalled);
        if (!brouterInstalled && savedInstanceState == null) {
            MainActivityBRouterInstallPrompt.show(this);
        }

        historyStore = new PoiHistoryStore(this);
        PoiSearchClient searchClient = PoiSearchClients.createDefault(this);
        AppLogger.i(TAG, "Selected POI search client=" + searchClient.getClass().getSimpleName());
        destinationController = new PoiInputController(
                this,
                controls.destinationEdit,
                historyStore,
                searchClient,
                poi -> {
                }
        );
        controls.routeRailView.setRouteAnchors(controls.destinationRow, controls.startNavButton);
        stopController = new MainActivityStopController(
                this,
                controls.stopsContainer,
                historyStore,
                searchClient,
                this::openStopMapPicker,
                controls.routeRailView
        );
        MainActivitySavedRouteController savedRouteController = new MainActivitySavedRouteController(
                this,
                () -> destinationController,
                stopController
        );
        savedRouteController.configure(controls.saveRouteButton, controls.restoreRouteButton);

        controls.destinationMapButton.setOnClickListener(
                v -> mapPickerCoordinator.openDestinationMapPicker(destinationController)
        );

        controls.addStopButton.setOnClickListener(v -> {
            AppLogger.i(TAG, "Add stop requested");
            stopController.addStopRow(null);
        });

        controls.startNavButton.setOnClickListener(v -> startNavigationFromInputs());
        controls.roundTripStartNavButton.setOnClickListener(v -> startNavigationFromInputs());

        stopController.restoreRows(savedInstanceState);

        profileCoordinator.refresh(brouterInstalled);
        if (MainActivityIntentHandler.handleOpenNavigationIntent(this, getIntent())) {
            return;
        }
        MainActivityIntentHandler.handleIncomingIntent(
                this,
                getIntent(),
                destinationController,
                stopController,
                historyStore,
                routeModeController,
                reverseGeocodeController
        );
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        AppLogger.i(TAG, "onNewIntent " + MainActivityIntentHandler.describeIntent(intent));
        if (MainActivityIntentHandler.redirectGpxImportDuringActiveNavigation(this, intent)) {
            finish();
            return;
        }
        if (MainActivityIntentHandler.handleOpenNavigationIntent(this, intent)) {
            return;
        }
        if (routeModeController != null) {
            routeModeController.showRouteMode();
        }
        MainActivityIntentHandler.handleIncomingIntent(
                this,
                intent,
                destinationController,
                stopController,
                historyStore,
                routeModeController,
                reverseGeocodeController
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        AndroidAppTheme.recreateIfThemeChanged(this, appliedLightTheme);
        if (routeModeController != null) {
            routeModeController.updateDistanceUnitText();
        }
        if (roundTripDirectionController != null) {
            roundTripDirectionController.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (roundTripDirectionController != null) {
            roundTripDirectionController.onPause();
        }
        super.onPause();
    }

    private void openStopMapPicker(@NonNull PoiInputController stopInputController) {
        if (stopController == null) {
            AppLogger.w(TAG, "Stop map picker requested before stop controller was ready");
            return;
        }
        mapPickerCoordinator.openStopMapPicker(stopController, stopInputController);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (destinationController != null) {
            MainActivityDestinationState.save(outState, destinationController);
        }
        if (stopController != null) {
            stopController.saveState(outState);
        }
        if (routeModeController != null) {
            routeModeController.saveState(outState);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        restoreDestinationState(savedInstanceState);
        if (stopController != null) {
            stopController.restoreValues(savedInstanceState);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (destinationController != null) {
            destinationController.dispose();
        }
        if (stopController != null) {
            stopController.dispose();
        }
        if (reverseGeocodeController != null) {
            reverseGeocodeController.dispose();
        }
        if (roundTripDirectionController != null) {
            roundTripDirectionController.dispose();
        }
    }

    private void startNavigationFromInputs() {
        AppLogger.i(TAG, "Start navigation tapped destinationRaw=" + destinationController.getRawText().trim()
                + " stopsVisible=" + stopController.size());
        ProfileSelection profileSelection = resolveProfileSelectionForCurrentMode();
        if (profileSelection == null) {
            return;
        }
        if (routeModeController.isRoundTripMode()) {
            routeModeController.startRoundTripNavigation(profileSelection);
            return;
        }
        NavigationInputResolver.Result input = NavigationInputResolver.resolve(
                this,
                destinationController,
                stopController.getStopControllers(),
                profileSelection
        );
        if (input == null) {
            return;
        }
        NavigationInputResolver.rememberHistory(historyStore, input);
        launchNavigation(input);
    }

    private void launchNavigation(@NonNull NavigationInputResolver.Result input) {
        MainActivityNavigationLauncher.launch(this, input.request);
    }

    @Nullable
    private ProfileSelection resolveProfileSelectionForCurrentMode() {
        if (routeModeController.isStraightLineMode()) {
            return ProfileSelection.straightLine();
        }
        return profileCoordinator.resolveSelectedProfileSelection();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        AppLogger.i(TAG, "onActivityResult requestCode=" + requestCode
                + " resultCode=" + resultCode
                + " hasData=" + (data != null));
        if (profileCoordinator != null && profileCoordinator.handleActivityResult(requestCode, resultCode, data)) {
            return;
        }
        mapPickerCoordinator.handleActivityResult(
                requestCode,
                resultCode,
                data,
                destinationController,
                stopController
        );
    }

    private void restoreDestinationState(@NonNull Bundle savedInstanceState) {
        if (destinationController == null) {
            return;
        }
        MainActivityDestinationState.restore(savedInstanceState, destinationController);
    }

}

