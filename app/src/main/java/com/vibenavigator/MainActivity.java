package com.vibenavigator;

import android.app.Activity;
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

import com.vibenavigator.brouter.BRouterProfilesRepository;
import com.vibenavigator.nav.NavigationRequest;
import com.vibenavigator.poi.PoiHistoryStore;
import com.vibenavigator.poi.search.PoiSearchClient;
import com.vibenavigator.poi.search.PoiSearchClients;
import com.vibenavigator.poi.ui.PoiInputController;
import com.vibenavigator.util.AppLogger;

public class MainActivity extends Activity {

    public static final String EXTRA_OPEN_NAVIGATION = "open_navigation";

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
        if (!brouterInstalled) {
            Toast.makeText(this, R.string.msg_brouter_not_found, Toast.LENGTH_LONG).show();
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
        stopController = new MainActivityStopController(this, stopsContainer, historyStore, searchClient);

        addStopButton.setOnClickListener(v -> {
            AppLogger.i(TAG, "Add stop requested");
            stopController.addStopRow(null);
        });

        startNavButton.setOnClickListener(v -> startNavigationFromInputs());

        stopController.restoreState(savedInstanceState);

        profilePicker.refreshProfiles();
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
        if (stopController != null) {
            stopController.saveState(outState);
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
    }
}
