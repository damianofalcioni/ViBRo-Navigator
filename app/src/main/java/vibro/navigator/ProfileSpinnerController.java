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

final class ProfileSpinnerController {

    interface Listener {
        void onCustomProfilePickerRequested();
    }

    private static final String TAG = "ProfileSpinner";

    private final Context context;
    private final Spinner spinner;
    private final BRouterProfilesRepository profilesRepository;
    private final Listener listener;
    private final ArrayAdapter<ProfileSpinnerOption> adapter;
    private final ProfileSpinnerOptions options = new ProfileSpinnerOptions();

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
        String customProfile = profilesRepository.getCustomProfileName(context);
        options.replace(context, profilesRepository.listProfiles(context), customProfile);
        suppressSelectionCallback = true;
        adapter.clear();
        adapter.addAll(options.all());
        adapter.notifyDataSetChanged();
        restoreSelection(currentSelectionKey, customProfile);
        suppressSelectionCallback = false;
        AppLogger.i(TAG, "Loaded routing profiles count=" + options.profilesForLog().size()
                + " profiles=" + options.profilesForLog()
                + " customProfile=" + ProfileSpinnerOption.safe(customProfile));
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
        ProfileSpinnerOption option = getSelectedOptionOrToast();
        if (option == null) {
            return null;
        }
        if (!option.isCustom()) {
            return option.profileName();
        }
        return resolveCustomProfile();
    }

    @Nullable
    private ProfileSpinnerOption getSelectedOptionOrToast() {
        ProfileSpinnerOption option = options.optionAt(spinner.getSelectedItemPosition());
        if (option == null) {
            Toast.makeText(context, R.string.msg_select_custom_profile, Toast.LENGTH_SHORT).show();
        }
        return option;
    }

    @Nullable
    private String resolveCustomProfile() {
        String customProfile = profilesRepository.getCustomProfileName(context);
        if (customProfile != null && !customProfile.trim().isEmpty()) {
            return customProfile;
        }
        Toast.makeText(context, R.string.msg_select_custom_profile, Toast.LENGTH_SHORT).show();
        listener.onCustomProfilePickerRequested();
        return null;
    }

    private void handleSelection(int position) {
        if (suppressSelectionCallback || position < 0 || position >= options.size()) {
            return;
        }
        ProfileSpinnerOption option = options.optionAt(position);
        if (option == null) {
            return;
        }
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
        ProfileSpinnerOption option = options.optionAt(position);
        if (option == null) {
            return null;
        }
        return option.selectionKey();
    }

    private void restoreSelection(@Nullable String selectionKey, @Nullable String customProfile) {
        setSelection(options.restoredPosition(selectionKey, customProfile));
    }

    private void selectFirstRegularProfile() {
        setSelection(options.firstRegularOrCustomPosition());
    }

    private void selectCustomProfileOption() {
        int position = options.findCustomPosition();
        if (position >= 0) {
            setSelection(position);
        }
    }

    private void setSelection(int position) {
        if (position < 0 || position >= options.size()) {
            return;
        }
        suppressSelectionCallback = true;
        spinner.setSelection(position, false);
        suppressSelectionCallback = false;
        ProfileSpinnerOption option = options.optionAt(position);
        if (option != null) {
            saveSelectedOption(option);
        }
    }

    private void saveSelectedOption(@NonNull ProfileSpinnerOption option) {
        profilesRepository.saveSelectedProfileKey(context, option.selectionKey());
    }
}
