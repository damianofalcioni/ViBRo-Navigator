package vibro.navigator;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.util.AppLogger;

import java.util.ArrayList;
import java.util.List;

final class ProfileSpinnerController {

    interface Listener {
        void onCustomProfilePickerRequested();
    }

    private static final String TAG = "ProfileSpinner";

    private final Context context;
    private final Spinner spinner;
    private final BRouterProfilesRepository profilesRepository;
    private final Listener listener;
    private final ArrayAdapter<ProfileOption> adapter;
    private final List<String> profiles = new ArrayList<>();
    private final List<ProfileOption> options = new ArrayList<>();

    private boolean suppressSelectionCallback;
    private boolean selectionUserInitiated;

    ProfileSpinnerController(
            @NonNull Context context,
            @NonNull Spinner spinner,
            @NonNull BRouterProfilesRepository profilesRepository,
            @NonNull Listener listener
    ) {
        this.context = context;
        this.spinner = spinner;
        this.profilesRepository = profilesRepository;
        this.listener = listener;
        this.adapter = new ArrayAdapter<>(context, R.layout.item_profile_spinner, new ArrayList<>());
        this.adapter.setDropDownViewResource(R.layout.item_profile_spinner_dropdown);
        this.spinner.setAdapter(adapter);
        this.spinner.setOnTouchListener((v, event) -> {
            if (event == null) {
                return false;
            }
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                selectionUserInitiated = true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
            }
            return false;
        });
        this.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                handleSelection(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    void refresh() {
        String currentSelectionKey = getSelectedProfileKey();
        if (currentSelectionKey == null) {
            currentSelectionKey = profilesRepository.getSelectedProfileKey(context);
        }
        profiles.clear();
        profiles.addAll(profilesRepository.listProfiles(context));
        String customProfile = profilesRepository.getCustomProfileName(context);
        suppressSelectionCallback = true;
        adapter.clear();
        options.clear();
        for (String profile : profiles) {
            options.add(new ProfileOption(profile, profile, false));
        }
        options.add(buildCustomOption(customProfile));
        adapter.addAll(options);
        adapter.notifyDataSetChanged();
        restoreSelection(currentSelectionKey, customProfile);
        suppressSelectionCallback = false;
        AppLogger.i(TAG, "Loaded routing profiles count=" + profiles.size()
                + " profiles=" + profiles
                + " customProfile=" + safe(customProfile));
    }

    void onCustomProfileSaved() {
        refresh();
        selectCustomProfileOption();
    }

    void onCustomProfilePickerCancelled() {
        AppLogger.i(TAG, "Custom profile picker cancelled");
        if (profilesRepository.getCustomProfileName(context) == null) {
            selectFirstRegularProfile();
        }
    }

    @Nullable
    String resolveSelectedProfile() {
        if (!profilesRepository.isBRouterInstalled(context)) {
            Toast.makeText(context, R.string.msg_brouter_not_found, Toast.LENGTH_SHORT).show();
            return null;
        }
        int position = spinner.getSelectedItemPosition();
        if (position < 0 || position >= options.size()) {
            Toast.makeText(context, R.string.msg_select_custom_profile, Toast.LENGTH_SHORT).show();
            return null;
        }
        ProfileOption option = options.get(position);
        if (!option.isCustom()) {
            return option.profileName;
        }
        String customProfile = profilesRepository.getCustomProfileName(context);
        if (customProfile == null || customProfile.trim().isEmpty()) {
            Toast.makeText(context, R.string.msg_select_custom_profile, Toast.LENGTH_SHORT).show();
            listener.onCustomProfilePickerRequested();
            return null;
        }
        return customProfile;
    }

    private void handleSelection(int position) {
        if (suppressSelectionCallback || position < 0 || position >= options.size()) {
            return;
        }
        ProfileOption option = options.get(position);
        saveSelectedOption(option);
        if (!option.isCustom()) {
            selectionUserInitiated = false;
            return;
        }
        if (!selectionUserInitiated) {
            AppLogger.d(TAG, "Ignoring custom profile auto-selection");
            return;
        }
        selectionUserInitiated = false;
        AppLogger.i(TAG, "Custom profile entry selected");
        listener.onCustomProfilePickerRequested();
    }

    @Nullable
    private String getSelectedProfileKey() {
        int position = spinner.getSelectedItemPosition();
        if (position < 0 || position >= options.size()) {
            return null;
        }
        return options.get(position).selectionKey();
    }

    private void restoreSelection(@Nullable String selectionKey, @Nullable String customProfile) {
        int target = findProfileOptionPosition(selectionKey);
        if (target < 0 && profiles.isEmpty() && customProfile != null) {
            target = findCustomProfilePosition();
        }
        if (target < 0) {
            target = 0;
        }
        setSelection(target);
    }

    private void selectFirstRegularProfile() {
        if (!profiles.isEmpty()) {
            setSelection(0);
            return;
        }
        int customPosition = findCustomProfilePosition();
        if (customPosition >= 0) {
            setSelection(customPosition);
        }
    }

    private void selectCustomProfileOption() {
        int position = findCustomProfilePosition();
        if (position >= 0) {
            setSelection(position);
        }
    }

    private int findCustomProfilePosition() {
        return findProfileOptionPosition(ProfileOption.CUSTOM_KEY);
    }

    private int findProfileOptionPosition(@Nullable String selectionKey) {
        if (selectionKey == null) {
            return -1;
        }
        for (int i = 0; i < options.size(); i++) {
            if (selectionKey.equals(options.get(i).selectionKey())) {
                return i;
            }
        }
        return -1;
    }

    private void setSelection(int position) {
        if (position < 0 || position >= options.size()) {
            return;
        }
        suppressSelectionCallback = true;
        spinner.setSelection(position, false);
        suppressSelectionCallback = false;
        saveSelectedOption(options.get(position));
    }

    private void saveSelectedOption(@NonNull ProfileOption option) {
        profilesRepository.saveSelectedProfileKey(context, option.selectionKey());
    }

    @NonNull
    private ProfileOption buildCustomOption(@Nullable String customProfile) {
        if (customProfile == null || customProfile.trim().isEmpty()) {
            return new ProfileOption(context.getString(R.string.label_vehicle_profile_custom), null, true);
        }
        return new ProfileOption(
                context.getString(R.string.label_vehicle_profile_custom_with_name, customProfile),
                customProfile,
                true
        );
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
