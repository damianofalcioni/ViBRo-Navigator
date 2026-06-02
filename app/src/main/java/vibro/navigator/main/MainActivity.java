package vibro.navigator.main;

import vibro.navigator.R;


import vibro.navigator.android.intent.AndroidNavigationRequestIntentContract;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.about.AboutActivity;
import vibro.navigator.nav.ui.NavigationActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.poi.PoiHistoryStore;
import vibro.navigator.poi.search.PoiSearchClient;
import vibro.navigator.poi.search.PoiSearchClients;
import vibro.navigator.poi.ui.PoiInputController;
import vibro.navigator.logging.AppLogger;

// Android entry point: keep workflow logic delegated without hiding required screen collaborators behind a generic facade.
public class MainActivity extends Activity {

    public static final String EXTRA_OPEN_NAVIGATION = "open_navigation";

    private static final String TAG = "MainActivity";

    private PoiInputController destinationController;
    private PoiHistoryStore historyStore;
    private ProfileSpinnerController profileSpinnerController;
    private MainActivityStopController stopController;
    private MainActivityProfilePicker profilePicker;
    private MainActivityMapPickerCoordinator mapPickerCoordinator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppLogger.i(TAG, "onCreate savedState=" + (savedInstanceState != null)
                + " intent=" + MainActivityIntentHandler.describeIntent(getIntent()));
        mapPickerCoordinator = new MainActivityMapPickerCoordinator(this);
        MainActivityControls controls = MainActivityControls.bind(this);

        controls.aboutButton.setOnClickListener(v -> {
            AppLogger.i(TAG, "About button tapped");
            startActivity(new Intent(this, AboutActivity.class));
        });

        BRouterProfilesRepository profilesRepository = new BRouterProfilesRepository();
        profilePicker = new MainActivityProfilePicker(this, profilesRepository);
        profileSpinnerController = new ProfileSpinnerController(
                this,
                controls.profileSpinner,
                profilesRepository,
                profilePicker::startCustomProfilePicker
        );
        profilePicker.attachProfileSpinnerController(profileSpinnerController);

        boolean brouterInstalled = profilesRepository.isBRouterInstalled(this);
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
        stopController = new MainActivityStopController(
                this,
                controls.stopsContainer,
                historyStore,
                searchClient,
                mapPickerCoordinator::openStopMapPicker
        );

        controls.destinationMapButton.setOnClickListener(
                v -> mapPickerCoordinator.openDestinationMapPicker(destinationController)
        );

        controls.addStopButton.setOnClickListener(v -> {
            AppLogger.i(TAG, "Add stop requested");
            stopController.addStopRow(null);
        });

        controls.startNavButton.setOnClickListener(v -> startNavigationFromInputs());

        stopController.restoreRows(savedInstanceState);

        if (brouterInstalled) {
            profilePicker.refreshProfiles();
        } else {
            profileSpinnerController.refresh();
        }
        if (MainActivityIntentHandler.handleOpenNavigationIntent(this, getIntent())) {
            return;
        }
        MainActivityIntentHandler.handleIncomingIntent(this, getIntent(), destinationController);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        AppLogger.i(TAG, "onNewIntent " + MainActivityIntentHandler.describeIntent(intent));
        if (MainActivityIntentHandler.handleOpenNavigationIntent(this, intent)) {
            return;
        }
        MainActivityIntentHandler.handleIncomingIntent(this, intent, destinationController);
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
    }

    private void startNavigationFromInputs() {
        AppLogger.i(TAG, "Start navigation tapped destinationRaw=" + destinationController.getRawText().trim()
                + " stopsVisible=" + stopController.size());
        String profile = profileSpinnerController.resolveSelectedProfile();
        if (profile == null) {
            return;
        }
        NavigationInputResolver.Result input = NavigationInputResolver.resolve(
                this,
                destinationController,
                stopController.getStopControllers(),
                profile
        );
        if (input == null) {
            return;
        }
        NavigationInputResolver.rememberHistory(historyStore, input);
        launchNavigation(input.request);
    }

    private void launchNavigation(@NonNull NavigationRequest request) {
        AppLogger.i(TAG, "Starting NavigationActivity " + request.describe());
        Intent intent = new Intent(this, NavigationActivity.class);
        AndroidNavigationRequestIntentContract.putInto(intent, request);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        AppLogger.i(TAG, "onActivityResult requestCode=" + requestCode
                + " resultCode=" + resultCode
                + " hasData=" + (data != null));
        if (profilePicker != null && profilePicker.handleActivityResult(requestCode, resultCode, data)) {
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

