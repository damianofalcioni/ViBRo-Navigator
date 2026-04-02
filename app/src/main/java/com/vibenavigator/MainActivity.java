package com.vibenavigator;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
    private ArrayAdapter<ProfileOption> profilesAdapter;
    private List<String> profiles = new ArrayList<>();
    private final List<ProfileOption> profileOptions = new ArrayList<>();
    private boolean suppressProfileSelectionCallback;
    private boolean profileSelectionUserInitiated;

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
        profilesAdapter = new ArrayAdapter<>(this, R.layout.item_profile_spinner, new ArrayList<>());
        profilesAdapter.setDropDownViewResource(R.layout.item_profile_spinner_dropdown);
        profileSpinner.setAdapter(profilesAdapter);
        profileSpinner.setOnTouchListener((v, event) -> {
            profileSelectionUserInitiated = true;
            return false;
        });
        profileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressProfileSelectionCallback || position < 0 || position >= profileOptions.size()) {
                    return;
                }
                ProfileOption option = profileOptions.get(position);
                persistSelectedProfileOption(option);
                if (!option.isCustom()) {
                    profileSelectionUserInitiated = false;
                    return;
                }
                if (!profileSelectionUserInitiated) {
                    AppLogger.d(TAG, "Ignoring custom profile auto-selection");
                    return;
                }
                profileSelectionUserInitiated = false;
                AppLogger.i(TAG, "Custom profile entry selected");
                startCustomProfilePicker();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        boolean brouterInstalled = profilesRepository.isBRouterInstalled(this);
        AppLogger.i(TAG, "BRouter installed=" + brouterInstalled);
        if (!brouterInstalled) {
            Toast.makeText(this, R.string.msg_brouter_not_found, Toast.LENGTH_LONG).show();
        }

        PoiHistoryStore historyStore = new PoiHistoryStore(this);
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

        addStopButton.setOnClickListener(v -> {
            AppLogger.i(TAG, "Add stop requested");
            addStopRow(null, historyStore, searchClient);
        });

        startNavButton.setOnClickListener(v -> {
            AppLogger.i(TAG, "Start navigation tapped destinationRaw=" + destinationController.getRawText().trim()
                    + " stopsVisible=" + stopControllers.size());
            Poi dest = destinationController.getSelectedPoi();
            if (dest == null) {
                dest = destinationController.parseCurrentPoi();
            }
            if (dest == null) {
                AppLogger.w(TAG, "Navigation blocked because destination is missing or unparsable");
                Toast.makeText(this, R.string.msg_missing_destination, Toast.LENGTH_SHORT).show();
                return;
            }
            if (Double.isNaN(dest.lat) || Double.isNaN(dest.lon)) {
                AppLogger.w(TAG, "Navigation blocked because destination coordinates are invalid destination=" + formatPoi(dest));
                Toast.makeText(this, R.string.msg_invalid_coordinates, Toast.LENGTH_SHORT).show();
                return;
            }

            ArrayList<String> stops = new ArrayList<>();
            for (PoiInputController c : stopControllers) {
                String raw = c.getRawText().trim();
                if (raw.isEmpty()) {
                    continue;
                }
                Poi stop = c.getSelectedPoi();
                if (stop == null) {
                    stop = c.parseCurrentPoi();
                }
                if (stop == null || Double.isNaN(stop.lat) || Double.isNaN(stop.lon)) {
                    AppLogger.w(TAG, "Navigation blocked because a stop is invalid raw=" + raw);
                    Toast.makeText(this, R.string.msg_invalid_stop, Toast.LENGTH_SHORT).show();
                    return;
                }
                stops.add(stop.lat + "," + stop.lon);
            }

            String profile = resolveSelectedProfile();
            if (profile == null || profile.trim().isEmpty()) {
                AppLogger.w(TAG, "Navigation blocked because a profile is not selected");
                return;
            }
            AppLogger.i(TAG, "Starting NavigationActivity profile=" + profile
                    + " destination=" + formatPoi(dest)
                    + " stops=" + stops);

            Intent i = new Intent(this, NavigationActivity.class);
            i.putExtra(NavigationActivity.EXTRA_PROFILE, profile);
            i.putExtra(NavigationActivity.EXTRA_DEST_NAME, dest.name);
            i.putExtra(NavigationActivity.EXTRA_DEST_LAT, dest.lat);
            i.putExtra(NavigationActivity.EXTRA_DEST_LON, dest.lon);
            i.putStringArrayListExtra(NavigationActivity.EXTRA_STOPS, stops);
            startActivity(i);
        });

        if (savedInstanceState != null) {
            ArrayList<String> stopTexts = savedInstanceState.getStringArrayList("stopTexts");
            if (stopTexts != null) {
                AppLogger.i(TAG, "Restoring stop rows count=" + stopTexts.size());
                for (String t : stopTexts) {
                    addStopRow(t, historyStore, searchClient);
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

    private void addStopRow(@Nullable String initialText, PoiHistoryStore historyStore, PoiSearchClient searchClient) {
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
            AppLogger.d(TAG, "No destination extracted from intent");
            return;
        }
        destinationController.setText(query.trim());
        destinationController.getEditText().requestFocus();
        AppLogger.i(TAG, "Applied incoming destination query=" + query.trim());
    }

    private boolean handleOpenNavigationIntent(@Nullable Intent intent) {
        if (intent == null || !intent.getBooleanExtra(EXTRA_OPEN_NAVIGATION, false)) {
            return false;
        }
        AppLogger.i(TAG, "Forwarding notification tap to NavigationActivity");
        Intent navigationIntent = new Intent(this, NavigationActivity.class);
        navigationIntent.putExtra(NavigationActivity.EXTRA_RESUME_EXISTING, true);

        String profile = intent.getStringExtra(NavigationActivity.EXTRA_PROFILE);
        if (profile != null) {
            navigationIntent.putExtra(NavigationActivity.EXTRA_PROFILE, profile);
        }
        String destinationName = intent.getStringExtra(NavigationActivity.EXTRA_DEST_NAME);
        if (destinationName != null) {
            navigationIntent.putExtra(NavigationActivity.EXTRA_DEST_NAME, destinationName);
        }
        if (intent.hasExtra(NavigationActivity.EXTRA_DEST_LAT)) {
            navigationIntent.putExtra(
                    NavigationActivity.EXTRA_DEST_LAT,
                    intent.getDoubleExtra(NavigationActivity.EXTRA_DEST_LAT, Double.NaN)
            );
        }
        if (intent.hasExtra(NavigationActivity.EXTRA_DEST_LON)) {
            navigationIntent.putExtra(
                    NavigationActivity.EXTRA_DEST_LON,
                    intent.getDoubleExtra(NavigationActivity.EXTRA_DEST_LON, Double.NaN)
            );
        }
        ArrayList<String> stops = intent.getStringArrayListExtra(NavigationActivity.EXTRA_STOPS);
        if (stops != null) {
            navigationIntent.putStringArrayListExtra(NavigationActivity.EXTRA_STOPS, stops);
        }

        intent.removeExtra(EXTRA_OPEN_NAVIGATION);
        intent.removeExtra(NavigationActivity.EXTRA_RESUME_EXISTING);
        setIntent(intent);
        startActivity(navigationIntent);
        return true;
    }

    private void refreshProfiles() {
        String currentSelectionKey = getSelectedProfileKey();
        if (currentSelectionKey == null) {
            currentSelectionKey = profilesRepository.getSelectedProfileKey(this);
        }
        profiles = profilesRepository.listProfiles(this);
        String customProfile = profilesRepository.getCustomProfileName(this);
        suppressProfileSelectionCallback = true;
        profilesAdapter.clear();
        profileOptions.clear();
        for (String profile : profiles) {
            profileOptions.add(new ProfileOption(profile, profile, false));
        }
        profileOptions.add(buildCustomOption(customProfile));
        profilesAdapter.addAll(profileOptions);
        profilesAdapter.notifyDataSetChanged();
        restoreProfileSelection(currentSelectionKey, customProfile);
        suppressProfileSelectionCallback = false;
        if (profiles.isEmpty() && customProfile == null) {
            AppLogger.w(TAG, "No routing profiles found");
            maybePromptProfilesFolder();
        }
        AppLogger.i(TAG, "Loaded routing profiles count=" + profiles.size()
                + " profiles=" + profiles
                + " customProfile=" + safe(customProfile));
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
            AppLogger.i(TAG, "Custom profile picker cancelled");
            if (profilesRepository.getCustomProfileName(this) == null) {
                selectFirstRegularProfile();
            }
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            AppLogger.w(TAG, "Custom profile picker returned without URI");
            if (profilesRepository.getCustomProfileName(this) == null) {
                selectFirstRegularProfile();
            }
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
            selectFirstRegularProfile();
            return;
        }
        profilesRepository.saveCustomProfile(this, uri, profileName);
        refreshProfiles();
        selectCustomProfileOption();
    }

    @Nullable
    private String resolveSelectedProfile() {
        int position = profileSpinner.getSelectedItemPosition();
        if (position < 0 || position >= profileOptions.size()) {
            Toast.makeText(this, R.string.msg_select_custom_profile, Toast.LENGTH_SHORT).show();
            return null;
        }
        ProfileOption option = profileOptions.get(position);
        if (!option.isCustom()) {
            return option.profileName;
        }
        String customProfile = profilesRepository.getCustomProfileName(this);
        if (customProfile == null || customProfile.trim().isEmpty()) {
            Toast.makeText(this, R.string.msg_select_custom_profile, Toast.LENGTH_SHORT).show();
            startCustomProfilePicker();
            return null;
        }
        return customProfile;
    }

    @Nullable
    private String getSelectedProfileKey() {
        int position = profileSpinner.getSelectedItemPosition();
        if (position < 0 || position >= profileOptions.size()) {
            return null;
        }
        return profileOptions.get(position).selectionKey();
    }

    private void restoreProfileSelection(@Nullable String selectionKey, @Nullable String customProfile) {
        int target = findProfileOptionPosition(selectionKey);
        if (target < 0 && profiles.isEmpty() && customProfile != null) {
            target = findCustomProfilePosition();
        }
        if (target < 0) {
            target = 0;
        }
        setProfileSelection(target);
    }

    private void selectFirstRegularProfile() {
        if (!profiles.isEmpty()) {
            setProfileSelection(0);
            return;
        }
        int customPosition = findCustomProfilePosition();
        if (customPosition >= 0) {
            setProfileSelection(customPosition);
        }
    }

    private void selectCustomProfileOption() {
        int position = findCustomProfilePosition();
        if (position >= 0) {
            setProfileSelection(position);
        }
    }

    private int findCustomProfilePosition() {
        return findProfileOptionPosition(ProfileOption.CUSTOM_KEY);
    }

    private int findProfileOptionPosition(@Nullable String selectionKey) {
        if (selectionKey == null) {
            return -1;
        }
        for (int i = 0; i < profileOptions.size(); i++) {
            if (selectionKey.equals(profileOptions.get(i).selectionKey())) {
                return i;
            }
        }
        return -1;
    }

    private void setProfileSelection(int position) {
        if (position < 0 || position >= profileOptions.size()) {
            return;
        }
        suppressProfileSelectionCallback = true;
        profileSpinner.setSelection(position, false);
        suppressProfileSelectionCallback = false;
        persistSelectedProfileOption(profileOptions.get(position));
    }

    private void persistSelectedProfileOption(@NonNull ProfileOption option) {
        profilesRepository.saveSelectedProfileKey(this, option.selectionKey());
    }

    @NonNull
    private ProfileOption buildCustomOption(@Nullable String customProfile) {
        if (customProfile == null || customProfile.trim().isEmpty()) {
            return new ProfileOption(getString(R.string.label_vehicle_profile_custom), null, true);
        }
        return new ProfileOption(
                getString(R.string.label_vehicle_profile_custom_with_name, customProfile),
                customProfile,
                true
        );
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
    private static String formatPoi(@NonNull Poi poi) {
        return poi.displayLabel() + " (" + poi.lat + "," + poi.lon + ")";
    }

    @NonNull
    private static String safe(@Nullable String value) {
        return value == null ? "null" : value;
    }

    private static final class ProfileOption {
        private static final String CUSTOM_KEY = "__custom__";

        @NonNull
        private final String label;
        @Nullable
        private final String profileName;
        private final boolean custom;

        private ProfileOption(@NonNull String label, @Nullable String profileName, boolean custom) {
            this.label = label;
            this.profileName = profileName;
            this.custom = custom;
        }

        private boolean isCustom() {
            return custom;
        }

        @NonNull
        private String selectionKey() {
            return custom ? CUSTOM_KEY : safe(profileName);
        }

        @NonNull
        @Override
        public String toString() {
            return label;
        }
    }
}
