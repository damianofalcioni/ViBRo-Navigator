package vibro.navigator.main;

import vibro.navigator.R;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.logging.AppLogger;

final class ProfileSpinnerController {

    interface Listener {
        void onCustomProfilePickerRequested();
    }

    interface SelectionChangeListener {
        void onProfileSelectionChanged();
    }

    private static final String TAG = "ProfileSpinner";

    private final Context context;
    private final Spinner spinner;
    private final BRouterProfilesRepository profilesRepository;
    private final Listener listener;
    private final ProfileSpinnerAdapter adapter;
    private final ProfileSpinnerOptions options = new ProfileSpinnerOptions();
    private final TaskScheduler selectionScheduler = AndroidTaskScheduler.main();

    private boolean suppressSelectionCallback;
    private boolean selectionUserInitiated;
    @Nullable
    private String selectedProfileKey;
    @NonNull
    private SelectionChangeListener selectionChangeListener = () -> {
    };

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
        this.adapter = new ProfileSpinnerAdapter(context);
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

    void setSelectionChangeListener(@NonNull SelectionChangeListener listener) {
        this.selectionChangeListener = listener;
        notifyProfileSelectionChanged();
    }

    void refresh() {
        String currentSelectionKey = selectedProfileKey;
        if (currentSelectionKey == null) {
            currentSelectionKey = profilesRepository.getSelectedProfileKey(context);
        }
        String customProfile = profilesRepository.getCustomProfileUri(context) == null
                ? null : profilesRepository.getCustomProfileName(context);
        boolean bRouterInstalled = profilesRepository.isBRouterInstalled(context);
        options.replace(context, profilesRepository.listProfiles(context), customProfile);
        suppressSelectionCallback = true;
        adapter.clear();
        adapter.addAll(options.all());
        adapter.notifyDataSetChanged();
        restoreSelection(currentSelectionKey, bRouterInstalled);
        suppressSelectionCallback = false;
        notifyProfileSelectionChanged();
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
    }

    @Nullable
    ProfileSelection resolveSelectedProfileSelection() {
        ProfileSpinnerOption option = getSelectedOptionOrToast();
        if (option == null) {
            return null;
        }
        if (!profilesRepository.isBRouterInstalled(context)) {
            Toast.makeText(context, R.string.msg_brouter_not_found, Toast.LENGTH_SHORT).show();
            return null;
        }
        if (!option.isCustom()) {
            return resolveBRouterProfile(option.profileName());
        }
        return resolveCustomProfile();
    }

    @Nullable
    String selectedBRouterProfileName() {
        return ProfileSpinnerSelectedProfile.brouterProfileName(
                options.optionAt(spinner.getSelectedItemPosition()),
                profilesRepository.getCustomProfileName(context)
        );
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
    private ProfileSelection resolveCustomProfile() {
        String customProfile = profilesRepository.getCustomProfileName(context);
        if (customProfile != null && !customProfile.trim().isEmpty()) {
            return ProfileSelectionResolver.customBrouter(context, profilesRepository, customProfile);
        }
        Toast.makeText(context, R.string.msg_select_custom_profile, Toast.LENGTH_SHORT).show();
        listener.onCustomProfilePickerRequested();
        return null;
    }

    @Nullable
    private ProfileSelection resolveBRouterProfile(@Nullable String profileName) {
        return ProfileSelectionResolver.brouter(context, profilesRepository, profileName);
    }

    private void handleSelection(int position) {
        if (suppressSelectionCallback || position < 0 || position >= options.size()) {
            return;
        }
        ProfileSpinnerOption option = options.optionAt(position);
        if (option == null) {
            return;
        }
        if (option.isCustom()) {
            handleCustomSelection(option);
            return;
        }
        selectionUserInitiated = false;
        saveSelectedOption(option);
        notifyProfileSelectionChanged();
    }

    private void handleCustomSelection(@NonNull ProfileSpinnerOption option) {
        boolean openPicker = selectionUserInitiated || !option.selectionKey().equals(selectedProfileKey);
        selectionUserInitiated = false;
        if (openPicker || option.profileName() == null) {
            // Spinner can overwrite a reentrant selection change while finishing its callback.
            selectionScheduler.post(() -> restoreBeforeCustomPicker(openPicker));
        } else {
            saveSelectedOption(option);
            notifyProfileSelectionChanged();
        }
    }

    private void restoreBeforeCustomPicker(boolean openPicker) {
        restoreSelection(selectedProfileKey, profilesRepository.isBRouterInstalled(context));
        if (openPicker) {
            AppLogger.i(TAG, "Custom profile file requested");
            listener.onCustomProfilePickerRequested();
        }
    }

    private void restoreSelection(@Nullable String selectionKey, boolean bRouterInstalled) {
        setSelection(options.restoredPosition(selectionKey, bRouterInstalled));
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
        spinner.setSelection(position);
        suppressSelectionCallback = false;
        ProfileSpinnerOption option = options.optionAt(position);
        if (option != null) {
            saveSelectedOption(option);
        }
        notifyProfileSelectionChanged();
    }

    private void saveSelectedOption(@NonNull ProfileSpinnerOption option) {
        selectedProfileKey = option.selectionKey();
        profilesRepository.saveSelectedProfileKey(context, selectedProfileKey);
    }

    private void notifyProfileSelectionChanged() {
        selectionChangeListener.onProfileSelectionChanged();
    }
}


