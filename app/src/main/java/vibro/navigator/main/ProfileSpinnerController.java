package vibro.navigator.main;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import vibro.navigator.R;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.android.brouter.AndroidBRouterProfileLoader;
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
    private final RepeatSelectSpinner spinner;
    private final BRouterProfilesRepository profilesRepository;
    private final Listener listener;
    private final ProfileSpinnerAdapter adapter;
    private final AndroidBRouterProfileLoader loader;
    private final ProfileSpinnerOptions options = new ProfileSpinnerOptions();
    private final TaskScheduler selectionScheduler = AndroidTaskScheduler.main();

    private AndroidBRouterProfileLoader.Result lastResult =
            new AndroidBRouterProfileLoader.Result(Collections.emptyList(), Collections.emptyList(), false);
    @Nullable
    private String selectedProfileKey;
    @Nullable
    private ProfileSpinnerOption displayedOption;
    @NonNull
    private SelectionChangeListener selectionChangeListener = () -> {
    };

    ProfileSpinnerController(
            @NonNull Context context,
            @NonNull RepeatSelectSpinner spinner,
            @NonNull BRouterProfilesRepository profilesRepository,
            @NonNull Listener listener
    ) {
        this.context = context;
        this.spinner = spinner;
        this.profilesRepository = profilesRepository;
        this.listener = listener;
        this.adapter = new ProfileSpinnerAdapter(context);
        loader = new AndroidBRouterProfileLoader(context, profilesRepository, this::updateOptions);
        selectedProfileKey = profilesRepository.getSelectedProfileKey(context);
        this.spinner.setSaveEnabled(false);
        this.spinner.setAdapter(adapter);
        this.spinner.onDropdownSelection(this::handleSelection);
    }

    void setSelectionChangeListener(@NonNull SelectionChangeListener listener) {
        this.selectionChangeListener = listener;
        notifyProfileSelectionChanged();
    }

    void refresh() {
        if (adapter.isEmpty()) {
            updateOptions(lastResult);
        }
        loader.refresh();
    }

    void refreshAfterAccessChange() {
        loader.refreshAfterAccessChange();
    }

    void refreshOnResume() {
        loader.refreshOnResume();
    }

    private void updateOptions(AndroidBRouterProfileLoader.Result result) {
        lastResult = result;
        String customProfile = profilesRepository.getCustomProfileUri(context) == null
                ? null : profilesRepository.getCustomProfileName(context);
        boolean bRouterInstalled = profilesRepository.isBRouterInstalled(context);
        options.replaceFromLoad(context, result.profiles(), result.bundled(), result.complete(), customProfile);
        adapter.setNotifyOnChange(false);
        adapter.clear();
        adapter.addAll(options.all());
        adapter.notifyDataSetChanged();
        restoreSelection(bRouterInstalled);
        notifyProfileSelectionChanged();
        AppLogger.i(TAG, "Loaded routing profiles count=" + options.profilesForLog().size()
                + " profiles=" + options.profilesForLog()
                + " customProfile=" + ProfileSpinnerOption.safe(customProfile));
    }

    void onCustomProfileSaved() {
        selectedProfileKey = ProfileSpinnerOption.CUSTOM_KEY;
        profilesRepository.saveSelectedProfileKey(context, selectedProfileKey);
        updateOptions(lastResult);
    }

    void dispose() {
        loader.dispose();
    }

    void onCustomProfilePickerCancelled() {
        AppLogger.i(TAG, "Custom profile picker cancelled");
    }

    @Nullable
    ProfileSelection resolveSelectedProfileSelection() {
        ProfileSpinnerOption option = selectedOption();
        if (profilesRepository.isBRouterInstalled(context) && awaitingProfile(option)) {
            Toast.makeText(context, R.string.msg_brouter_profile_not_ready, Toast.LENGTH_SHORT).show();
            return null;
        }
        return ProfileSelectionResolver.selected(context, profilesRepository, option,
                listener::onCustomProfilePickerRequested);
    }

    @Nullable
    String selectedBRouterProfileName() {
        ProfileSpinnerOption option = selectedOption();
        return ProfileSpinnerSelectedProfile.brouterProfileName(
                awaitingProfile(option) ? null : option,
                profilesRepository.getCustomProfileName(context)
        );
    }

    @Nullable
    ProfileSpinnerOption selectedOption() {
        return displayedOption;
    }

    private void handleSelection(int position) {
        ProfileSpinnerOption option = options.optionAt(position);
        if (option == null || option.isPrompt()) {
            return;
        }
        if (option.isCustom()) {
            selectionScheduler.post(this::restoreBeforeCustomPicker);
        } else {
            displayedOption = option;
            saveSelectedOption(option);
            notifyProfileSelectionChanged();
        }
    }

    private void restoreBeforeCustomPicker() {
        restoreSelection(profilesRepository.isBRouterInstalled(context));
        notifyProfileSelectionChanged();
        AppLogger.i(TAG, "Custom profile file requested");
        listener.onCustomProfilePickerRequested();
    }

    private boolean awaitingProfile(@Nullable ProfileSpinnerOption option) {
        if (lastResult.complete()) {
            return false;
        }
        return option == null || (selectedProfileKey != null && !selectedProfileKey.equals(option.selectionKey()))
                || (!option.isCustom() && !option.isPrompt() && !lastResult.profiles().contains(option.profileName()));
    }

    private void restoreSelection(boolean bRouterInstalled) {
        int position = options.restoredPosition(selectedProfileKey, bRouterInstalled);
        ProfileSpinnerOption option = options.optionAt(position);
        if (option == null) {
            return;
        }
        displayedOption = option;
        spinner.renderSelection(position);
        if (lastResult.complete() && !option.isPrompt() && (!option.isCustom() || option.profileName() != null)) {
            saveSelectedOption(option);
        }
    }

    private void saveSelectedOption(@NonNull ProfileSpinnerOption option) {
        selectedProfileKey = option.selectionKey();
        profilesRepository.saveSelectedProfileKey(context, selectedProfileKey);
    }

    private void notifyProfileSelectionChanged() {
        selectionChangeListener.onProfileSelectionChanged();
    }
}


