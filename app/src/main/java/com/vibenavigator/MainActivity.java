package com.vibenavigator;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.vibenavigator.brouter.BRouterProfilesRepository;
import com.vibenavigator.poi.Poi;
import com.vibenavigator.poi.PoiHistoryStore;
import com.vibenavigator.poi.search.PoiSearchClient;
import com.vibenavigator.poi.search.PoiSearchClients;
import com.vibenavigator.poi.ui.PoiInputController;
import com.vibenavigator.util.IntentLocationParser;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PICK_PROFILES_FOLDER = 1001;

    private Spinner profileSpinner;
    private EditText destinationEdit;
    private LinearLayout stopsContainer;
    private Button addStopButton;
    private Button startNavButton;

    private final List<PoiInputController> stopControllers = new ArrayList<>();
    private PoiInputController destinationController;

    private BRouterProfilesRepository profilesRepository;
    private ArrayAdapter<String> profilesAdapter;
    private List<String> profiles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton aboutButton = findViewById(R.id.aboutButton);
        aboutButton.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));

        profileSpinner = findViewById(R.id.profileSpinner);
        destinationEdit = findViewById(R.id.destinationEdit);
        stopsContainer = findViewById(R.id.stopsContainer);
        addStopButton = findViewById(R.id.addStopButton);
        startNavButton = findViewById(R.id.startNavButton);

        profilesRepository = new BRouterProfilesRepository();
        profilesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>());
        profileSpinner.setAdapter(profilesAdapter);

        if (!profilesRepository.isBRouterInstalled(this)) {
            Toast.makeText(this, R.string.msg_brouter_not_found, Toast.LENGTH_LONG).show();
        }

        PoiHistoryStore historyStore = new PoiHistoryStore(this);
        PoiSearchClient searchClient = PoiSearchClients.createDefault();
        destinationController = new PoiInputController(
                this,
                destinationEdit,
                historyStore,
                searchClient,
                poi -> {
                }
        );

        addStopButton.setOnClickListener(v -> addStopRow(null, historyStore, searchClient));

        startNavButton.setOnClickListener(v -> {
            Poi dest = destinationController.getSelectedPoi();
            if (dest == null) {
                dest = destinationController.parseCurrentPoi();
            }
            if (dest == null) {
                Toast.makeText(this, R.string.msg_missing_destination, Toast.LENGTH_SHORT).show();
                return;
            }
            if (Double.isNaN(dest.lat) || Double.isNaN(dest.lon)) {
                Toast.makeText(this, R.string.msg_invalid_coordinates, Toast.LENGTH_SHORT).show();
                return;
            }

            if (profiles.isEmpty()) {
                Toast.makeText(this, R.string.msg_profile_folder_not_selected, Toast.LENGTH_SHORT).show();
                maybePromptProfilesFolder();
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
                    Toast.makeText(this, R.string.msg_invalid_stop, Toast.LENGTH_SHORT).show();
                    return;
                }
                stops.add(stop.lat + "," + stop.lon);
            }

            String profile = profiles.get(Math.max(0, profileSpinner.getSelectedItemPosition()));

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
                for (String t : stopTexts) {
                    addStopRow(t, historyStore, searchClient);
                }
            }
        }

        refreshProfiles();
        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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

        remove.setOnClickListener(v -> {
            controller.dispose();
            stopControllers.remove(controller);
            stopsContainer.removeView(row);
        });

        stopsContainer.addView(row);
    }

    private void handleIncomingIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }
        String query = IntentLocationParser.parseToQuery(intent);
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        destinationController.setText(query.trim());
        destinationController.getEditText().requestFocus();
    }

    private void refreshProfiles() {
        profiles = profilesRepository.listProfiles(this);
        profilesAdapter.clear();
        if (profiles.isEmpty()) {
            profilesAdapter.add(getString(R.string.msg_profiles_empty));
            profilesAdapter.notifyDataSetChanged();
            maybePromptProfilesFolder();
            return;
        }
        profilesAdapter.addAll(profiles);
        profilesAdapter.notifyDataSetChanged();
    }

    private void maybePromptProfilesFolder() {
        if (profilesRepository.getProfilesTreeUri(this) != null) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.msg_select_brouter_profiles_folder_title)
                .setMessage(R.string.msg_select_brouter_profiles_folder_body)
                .setPositiveButton(R.string.action_pick_folder, (d, w) -> startProfilesFolderPicker())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void startProfilesFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQ_PICK_PROFILES_FOLDER);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_PROFILES_FOLDER || resultCode != RESULT_OK || data == null) {
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            return;
        }
        int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            getContentResolver().takePersistableUriPermission(uri, flags);
        } catch (SecurityException ignored) {
            // ignore
        }
        profilesRepository.saveProfilesTreeUri(this, uri);
        refreshProfiles();
    }
}
