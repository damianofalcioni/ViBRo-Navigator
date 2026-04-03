package com.vibenavigator;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.vibenavigator.brouter.BRouterProfilesRepository;
import com.vibenavigator.nav.NavigationRequest;
import com.vibenavigator.poi.CoordinateParser;
import com.vibenavigator.poi.Poi;
import com.vibenavigator.poi.PoiHistoryStore;
import com.vibenavigator.poi.search.PoiSearchClient;
import com.vibenavigator.poi.search.PoiSearchClients;
import com.vibenavigator.poi.ui.PoiInputController;
import com.vibenavigator.util.AppLogger;
import com.vibenavigator.util.IntentLocationParser;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_NAVIGATION = "open_navigation";

    private static final int REQ_PICK_PROFILES_FOLDER = 1001;
    private static final int REQ_PICK_CUSTOM_PROFILE = 1002;
    private static final String TAG = "MainActivity";

    private Spinner profileSpinner;
    private EditText destinationEdit;
    private LinearLayout stopsContainer;
    private Button addStopButton;
    private Button startNavButton;

    private final List<PoiInputController> stopControllers = new ArrayList<>();
    private PoiInputController destinationController;

    private BRouterProfilesRepository profilesRepository;
    private PoiHistoryStore historyStore;
    private PoiSearchClient searchClient;
    private ProfileSpinnerController profileSpinnerController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppLogger.i(TAG, "onCreate savedState=" + (savedInstanceState != null)
                + " intent=" + describeIntent(getIntent()));

        ImageButton aboutButton = findViewById(R.id.aboutButton);
        aboutButton.setOnClickListener(v -> {
            AppLogger.i(TAG, "About button tapped");
            startActivity(new Intent(this, AboutActivity.class));
        });

        profileSpinner = findViewById(R.id.profileSpinner);
        destinationEdit = findViewById(R.id.destinationEdit);
        stopsContainer = findViewById(R.id.stopsContainer);
        addStopButton = findViewById(R.id.addStopButton);
        startNavButton = findViewById(R.id.startNavButton);

        profilesRepository = new BRouterProfilesRepository();
        profileSpinnerController = new ProfileSpinnerController(
                this,
                profileSpinner,
                profilesRepository,
                this::startCustomProfilePicker
        );

        boolean brouterInstalled = profilesRepository.isBRouterInstalled(this);
        AppLogger.i(TAG, "BRouter installed=" + brouterInstalled);
        if (!brouterInstalled) {
            Toast.makeText(this, R.string.msg_brouter_not_found, Toast.LENGTH_LONG).show();
        }

        historyStore = new PoiHistoryStore(this);
        searchClient = PoiSearchClients.createDefault();
        AppLogger.i(TAG, "Selected POI search client=" + searchClient.getClass().getSimpleName());
        destinationController = new PoiInputController(
                this,
                destinationEdit,
                historyStore,
                searchClient,
                poi -> {
                }
        );

        addStopButton.setOnClickListener(v -> {
            AppLogger.i(TAG, "Add stop requested");
            addStopRow(null);
        });

        startNavButton.setOnClickListener(v -> {
            AppLogger.i(TAG, "Start navigation tapped destinationRaw=" + destinationController.getRawText().trim()
                    + " stopsVisible=" + stopControllers.size());
            String profile = profileSpinnerController.resolveSelectedProfile();
            if (profile == null) {
                return;
            }
            NavigationInputResolver.Result input = NavigationInputResolver.resolve(
                    this,
                    destinationController,
                    stopControllers,
                    profile
            );
            if (input == null) {
                return;
            }
            NavigationInputResolver.rememberHistory(historyStore, input);
            launchNavigation(input.request);
        });

        if (savedInstanceState != null) {
            ArrayList<String> stopTexts = savedInstanceState.getStringArrayList("stopTexts");
            if (stopTexts != null) {
                AppLogger.i(TAG, "Restoring stop rows count=" + stopTexts.size());
                for (String t : stopTexts) {
                    addStopRow(t);
                }
            }
        }

        refreshProfiles();
        if (handleOpenNavigationIntent(getIntent())) {
            return;
        }
        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        AppLogger.i(TAG, "onNewIntent " + describeIntent(intent));
        if (handleOpenNavigationIntent(intent)) {
            return;
        }
        handleIncomingIntent(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        ArrayList<String> stopTexts = new ArrayList<>();
        for (PoiInputController c : stopControllers) {
            stopTexts.add(c.getRawText());
        }
        outState.putStringArrayList("stopTexts", stopTexts);
        AppLogger.d(TAG, "Saved instance state stopCount=" + stopTexts.size());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppLogger.i(TAG, "onDestroy stopControllers=" + stopControllers.size());
        if (destinationController != null) {
            destinationController.dispose();
        }
        for (PoiInputController c : stopControllers) {
            c.dispose();
        }
    }

    private void addStopRow(@Nullable String initialText) {
        android.view.View row = getLayoutInflater().inflate(R.layout.item_stop_row, stopsContainer, false);
        EditText stopEdit = row.findViewById(R.id.stopEdit);
        android.widget.ImageButton remove = row.findViewById(R.id.removeStopButton);

        PoiInputController controller = new PoiInputController(
                this,
                stopEdit,
                historyStore,
                searchClient,
                poi -> {
                }
        );
        stopControllers.add(controller);

        if (initialText != null) {
            controller.setText(initialText);
        }
        AppLogger.i(TAG, "Added stop row initialText=" + safe(initialText) + " totalStops=" + stopControllers.size());

        remove.setOnClickListener(v -> {
            controller.dispose();
            stopControllers.remove(controller);
            stopsContainer.removeView(row);
            AppLogger.i(TAG, "Removed stop row remainingStops=" + stopControllers.size());
        });

        stopsContainer.addView(row);
    }

    private void handleIncomingIntent(@Nullable Intent intent) {
        if (intent == null) {
            AppLogger.d(TAG, "handleIncomingIntent ignored null intent");
            return;
        }
        String query = IntentLocationParser.parseToQuery(intent);
        if (query == null || query.trim().isEmpty()) {
            if (Intent.ACTION_VIEW.equals(intent.getAction()) || Intent.ACTION_SEND.equals(intent.getAction())) {
                Toast.makeText(this, R.string.msg_intent_unrecognized, Toast.LENGTH_SHORT).show();
            }
            AppLogger.d(TAG, "No destination extracted from intent");
            return;
        }
        String trimmedQuery = query.trim();
        Poi parsedPoi = CoordinateParser.tryParse(trimmedQuery, trimmedQuery);
        if (parsedPoi != null) {
            destinationController.setPoi(parsedPoi);
            AppLogger.i(TAG, "Applied incoming destination POI=" + parsedPoi.displayLabel()
                    + " (" + parsedPoi.lat + "," + parsedPoi.lon + ")");
        } else {
            destinationController.setText(trimmedQuery);
            AppLogger.i(TAG, "Applied incoming destination query=" + trimmedQuery);
        }
        destinationController.getEditText().requestFocus();
    }

    private boolean handleOpenNavigationIntent(@Nullable Intent intent) {
        if (intent == null || !intent.getBooleanExtra(EXTRA_OPEN_NAVIGATION, false)) {
            return false;
        }
        AppLogger.i(TAG, "Forwarding notification tap to NavigationActivity");
        Intent navigationIntent = new Intent(this, NavigationActivity.class);
        navigationIntent.putExtra(NavigationActivity.EXTRA_RESUME_EXISTING, true);
        NavigationRequest.fromIntent(intent).putInto(navigationIntent);

        intent.removeExtra(EXTRA_OPEN_NAVIGATION);
        intent.removeExtra(NavigationActivity.EXTRA_RESUME_EXISTING);
        setIntent(intent);
        startActivity(navigationIntent);
        return true;
    }

    private void launchNavigation(@NonNull NavigationRequest request) {
        AppLogger.i(TAG, "Starting NavigationActivity " + request.describe());
        Intent intent = new Intent(this, NavigationActivity.class);
        request.putInto(intent);
        startActivity(intent);
    }

    private void refreshProfiles() {
        profileSpinnerController.refresh();
        if (profileSpinnerController.shouldPromptForProfilesFolder()) {
            AppLogger.w(TAG, "No routing profiles found");
            maybePromptProfilesFolder();
        }
    }

    private void maybePromptProfilesFolder() {
        if (profilesRepository.getProfilesTreeUri(this) != null) {
            AppLogger.d(TAG, "Profiles folder prompt skipped because a folder is already saved");
            return;
        }
        AppLogger.i(TAG, "Prompting user to select profiles folder");
        new AlertDialog.Builder(this)
                .setTitle(R.string.msg_select_brouter_profiles_folder_title)
                .setMessage(R.string.msg_select_brouter_profiles_folder_body)
                .setPositiveButton(R.string.action_pick_file, (d, w) -> startCustomProfilePicker())
                .setNeutralButton(R.string.action_pick_folder, (d, w) -> startProfilesFolderPicker())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void startProfilesFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(
                    DocumentsContract.EXTRA_INITIAL_URI,
                    profilesRepository.getProfilesFolderPickerInitialUri(this)
            );
        }
        AppLogger.i(TAG, "Launching profiles folder picker");
        startActivityForResult(intent, REQ_PICK_PROFILES_FOLDER);
    }

    private void startCustomProfilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(
                    DocumentsContract.EXTRA_INITIAL_URI,
                    profilesRepository.getCustomProfilePickerInitialUri(this)
            );
        }
        AppLogger.i(TAG, "Launching custom profile picker");
        startActivityForResult(intent, REQ_PICK_CUSTOM_PROFILE);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        AppLogger.i(TAG, "onActivityResult requestCode=" + requestCode
                + " resultCode=" + resultCode
                + " hasData=" + (data != null));
        if (requestCode == REQ_PICK_PROFILES_FOLDER) {
            handleProfilesFolderPickerResult(resultCode, data);
            return;
        }
        if (requestCode == REQ_PICK_CUSTOM_PROFILE) {
            handleCustomProfilePickerResult(resultCode, data);
        }
    }

    private void handleProfilesFolderPickerResult(int resultCode, @Nullable Intent data) {
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            AppLogger.w(TAG, "Profiles folder picker returned without URI");
            return;
        }
        int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            getContentResolver().takePersistableUriPermission(uri, flags);
            AppLogger.i(TAG, "Persisted profiles folder permission uri=" + uri);
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Failed to persist profiles folder permission uri=" + uri, e);
        }
        profilesRepository.saveProfilesTreeUri(this, uri);
        refreshProfiles();
    }

    private void handleCustomProfilePickerResult(int resultCode, @Nullable Intent data) {
        if (resultCode != RESULT_OK || data == null) {
            profileSpinnerController.onCustomProfilePickerCancelled();
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            AppLogger.w(TAG, "Custom profile picker returned without URI");
            profileSpinnerController.onCustomProfilePickerCancelled();
            return;
        }
        int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            getContentResolver().takePersistableUriPermission(uri, flags);
            AppLogger.i(TAG, "Persisted custom profile permission uri=" + uri);
        } catch (SecurityException e) {
            AppLogger.w(TAG, "Failed to persist custom profile permission uri=" + uri, e);
        }
        String displayName = queryDisplayName(uri);
        String profileName = profilesRepository.normalizeProfileName(displayName);
        if (profileName == null) {
            AppLogger.w(TAG, "Custom profile picker returned a non-.brf file uri=" + uri
                    + " displayName=" + safe(displayName));
            Toast.makeText(this, R.string.msg_invalid_custom_profile, Toast.LENGTH_SHORT).show();
            profileSpinnerController.onCustomProfilePickerCancelled();
            return;
        }
        profilesRepository.saveCustomProfile(this, uri, profileName);
        profileSpinnerController.onCustomProfileSaved();
        if (profileSpinnerController.shouldPromptForProfilesFolder()) {
            maybePromptProfilesFolder();
        }
    }

    @Nullable
    private String queryDisplayName(@NonNull Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameCol = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameCol >= 0) {
                    return cursor.getString(nameCol);
                }
            }
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to query display name for uri=" + uri, e);
        }
        return uri.getLastPathSegment();
    }

    @NonNull
    private static String describeIntent(@Nullable Intent intent) {
        if (intent == null) {
            return "null";
        }
        return "action=" + safe(intent.getAction())
                + ", data=" + safe(intent.getDataString())
                + ", type=" + safe(intent.getType());
    }

    @NonNull
    private static String safe(@Nullable String value) {
        return value == null ? "null" : value;
    }
}
