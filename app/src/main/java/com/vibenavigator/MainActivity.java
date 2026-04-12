package com.vibenavigator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vibenavigator.brouter.BRouterInstallLauncher;
import com.vibenavigator.brouter.BRouterProfilesRepository;
import com.vibenavigator.nav.NavigationRequest;
import com.vibenavigator.poi.PoiHistoryStore;
import com.vibenavigator.poi.search.PoiSearchClient;
import com.vibenavigator.poi.search.PoiSearchClients;
import com.vibenavigator.poi.ui.PoiInputController;
import com.vibenavigator.util.AppLogger;

public class MainActivity extends Activity {

    public static final String EXTRA_OPEN_NAVIGATION = "open_navigation";
    private static final int REQ_PICK_DESTINATION_ON_MAP = 2001;
    private static final int REQ_PICK_STOP_ON_MAP_BASE = 3000;
    private static final String STATE_DESTINATION_TEXT = "destinationText";
    private static final String STATE_DESTINATION_SELECTED_NAME = "destinationSelectedName";
    private static final String STATE_DESTINATION_SELECTED_LAT = "destinationSelectedLat";
    private static final String STATE_DESTINATION_SELECTED_LON = "destinationSelectedLon";

    private static final String TAG = "MainActivity";

    private PoiInputController destinationController;
    private PoiHistoryStore historyStore;
    private ProfileSpinnerController profileSpinnerController;
    private MainActivityStopController stopController;
    private MainActivityProfilePicker profilePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppLogger.i(TAG, "onCreate savedState=" + (savedInstanceState != null)
                + " intent=" + MainActivityIntentHandler.describeIntent(getIntent()));

        ImageButton aboutButton = findViewById(R.id.aboutButton);
        aboutButton.setOnClickListener(v -> {
            AppLogger.i(TAG, "About button tapped");
            startActivity(new Intent(this, AboutActivity.class));
        });

        Spinner profileSpinner = findViewById(R.id.profileSpinner);
        EditText destinationEdit = findViewById(R.id.destinationEdit);
        ImageButton destinationMapButton = findViewById(R.id.destinationMapButton);
        LinearLayout stopsContainer = findViewById(R.id.stopsContainer);
        Button addStopButton = findViewById(R.id.addStopButton);
        Button startNavButton = findViewById(R.id.startNavButton);

        BRouterProfilesRepository profilesRepository = new BRouterProfilesRepository();
        profilePicker = new MainActivityProfilePicker(this, profilesRepository);
        profileSpinnerController = new ProfileSpinnerController(
                this,
                profileSpinner,
                profilesRepository,
                profilePicker::startCustomProfilePicker
        );
        profilePicker.attachProfileSpinnerController(profileSpinnerController);

        boolean brouterInstalled = profilesRepository.isBRouterInstalled(this);
        AppLogger.i(TAG, "BRouter installed=" + brouterInstalled);
        if (!brouterInstalled && savedInstanceState == null) {
            showBRouterInstallPrompt();
        }

        historyStore = new PoiHistoryStore(this);
        PoiSearchClient searchClient = PoiSearchClients.createDefault();
        AppLogger.i(TAG, "Selected POI search client=" + searchClient.getClass().getSimpleName());
        destinationController = new PoiInputController(
                this,
                destinationEdit,
                historyStore,
                searchClient,
                poi -> {
                }
        );
        stopController = new MainActivityStopController(
                this,
                stopsContainer,
                historyStore,
                searchClient,
                this::openStopMapPicker
        );

        destinationMapButton.setOnClickListener(v -> openDestinationMapPicker());

        addStopButton.setOnClickListener(v -> {
            AppLogger.i(TAG, "Add stop requested");
            stopController.addStopRow(null);
        });

        startNavButton.setOnClickListener(v -> startNavigationFromInputs());

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
            outState.putString(STATE_DESTINATION_TEXT, destinationController.getRawText());
            com.vibenavigator.poi.Poi selectedPoi = destinationController.getSelectedPoi();
            if (selectedPoi != null) {
                outState.putString(STATE_DESTINATION_SELECTED_NAME, selectedPoi.name);
                outState.putDouble(STATE_DESTINATION_SELECTED_LAT, selectedPoi.lat);
                outState.putDouble(STATE_DESTINATION_SELECTED_LON, selectedPoi.lon);
            }
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
        request.putInto(intent);
        startActivity(intent);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        AppLogger.i(TAG, "onActivityResult requestCode=" + requestCode
                + " resultCode=" + resultCode
                + " hasData=" + (data != null));
        if (profilePicker != null && profilePicker.handleActivityResult(requestCode, resultCode, data)) {
            return;
        }
        if (requestCode == REQ_PICK_DESTINATION_ON_MAP) {
            handleDestinationMapResult(resultCode, data);
            return;
        }
        if (requestCode >= REQ_PICK_STOP_ON_MAP_BASE && requestCode < REQ_PICK_STOP_ON_MAP_BASE + 1000) {
            handleStopMapResult(requestCode - REQ_PICK_STOP_ON_MAP_BASE, resultCode, data);
        }
    }

    private void restoreDestinationState(@NonNull Bundle savedInstanceState) {
        if (destinationController == null) {
            return;
        }
        String selectedName = savedInstanceState.getString(STATE_DESTINATION_SELECTED_NAME);
        double selectedLat = savedInstanceState.getDouble(STATE_DESTINATION_SELECTED_LAT, Double.NaN);
        double selectedLon = savedInstanceState.getDouble(STATE_DESTINATION_SELECTED_LON, Double.NaN);
        if (selectedName != null && !Double.isNaN(selectedLat) && !Double.isNaN(selectedLon)) {
            destinationController.restorePoi(new com.vibenavigator.poi.Poi(selectedName, selectedLat, selectedLon));
            AppLogger.i(TAG, "Restored selected destination POI=" + selectedName
                    + " (" + selectedLat + "," + selectedLon + ")");
            return;
        }

        String destinationText = savedInstanceState.getString(STATE_DESTINATION_TEXT);
        if (destinationText != null && !destinationText.isEmpty()) {
            destinationController.restoreText(destinationText);
            AppLogger.i(TAG, "Restored destination text=" + destinationText);
        }
    }

    @SuppressWarnings("deprecation")
    private void openDestinationMapPicker() {
        AppLogger.i(TAG, "Opening map picker for destination");
        startActivityForResult(
                MapPickerActivity.createIntent(
                        this,
                        getString(R.string.title_map_picker_destination),
                        resolveInitialPoi(destinationController)
                ),
                REQ_PICK_DESTINATION_ON_MAP
        );
    }

    @SuppressWarnings("deprecation")
    private void openStopMapPicker(int stopIndex, @Nullable com.vibenavigator.poi.Poi initialPoi) {
        AppLogger.i(TAG, "Opening map picker for stop index=" + stopIndex);
        startActivityForResult(
                MapPickerActivity.createIntent(
                        this,
                        getString(R.string.title_map_picker_stop, stopIndex + 1),
                        initialPoi
                ),
                REQ_PICK_STOP_ON_MAP_BASE + stopIndex
        );
    }

    private void handleDestinationMapResult(int resultCode, @Nullable Intent data) {
        if (resultCode != RESULT_OK || destinationController == null) {
            return;
        }
        com.vibenavigator.poi.Poi poi = MapPickerActivity.parseResult(this, data);
        if (poi == null) {
            AppLogger.w(TAG, "Destination map picker returned without POI");
            return;
        }
        destinationController.setPoi(poi);
        AppLogger.i(TAG, "Destination selected from map=" + poi.displayLabel());
    }

    private void handleStopMapResult(int stopIndex, int resultCode, @Nullable Intent data) {
        if (resultCode != RESULT_OK || stopController == null) {
            return;
        }
        com.vibenavigator.poi.Poi poi = MapPickerActivity.parseResult(this, data);
        if (poi == null) {
            AppLogger.w(TAG, "Stop map picker returned without POI index=" + stopIndex);
            return;
        }
        stopController.setStopPoi(stopIndex, poi);
        AppLogger.i(TAG, "Stop selected from map index=" + stopIndex + " poi=" + poi.displayLabel());
    }

    @Nullable
    private static com.vibenavigator.poi.Poi resolveInitialPoi(@NonNull PoiInputController controller) {
        com.vibenavigator.poi.Poi selectedPoi = controller.getSelectedPoi();
        if (selectedPoi != null) {
            return selectedPoi;
        }
        return controller.parseCurrentPoi();
    }

    private void showBRouterInstallPrompt() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_brouter_not_found)
                .setMessage(R.string.msg_brouter_install_prompt)
                .setPositiveButton(R.string.action_open_play_store, (dialog, which) -> {
                    if (!BRouterInstallLauncher.launchPlayStore(this)) {
                        Toast.makeText(this, R.string.msg_open_brouter_store_failed, Toast.LENGTH_LONG).show();
                    }
                })
                .setNeutralButton(R.string.action_open_fdroid, (dialog, which) -> {
                    if (!BRouterInstallLauncher.launchFdroid(this)) {
                        Toast.makeText(this, R.string.msg_open_brouter_store_failed, Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
