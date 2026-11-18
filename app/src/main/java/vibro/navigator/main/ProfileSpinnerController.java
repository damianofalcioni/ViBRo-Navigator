package vibro.navigator.main;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.Objects;

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
    private final Spinner spinner;
    private final BRouterProfilesRepository profilesRepository;
    private final Listener listener;
    private final ProfileSpinnerAdapter adapter;
    private final AndroidBRouterProfileLoader loader;
    private final ProfileSpinnerOptions options = new ProfileSpinnerOptions();
    private final TaskScheduler selectionScheduler = AndroidTaskScheduler.main();

    private AndroidBRouterProfileLoader.Result lastResult =
            new AndroidBRouterProfileLoader.Result(Collections.emptyList(), Collections.emptyList(), false);
    private String renderedSelectionKey;
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
        loader = new AndroidBRouterProfileLoader(context, profilesRepository, this::updateOptions);
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
        loader.refresh();
    }

    void refreshAfterAccessChange() {
        loader.refreshAfterAccessChange();
    }

    private void updateOptions(AndroidBRouterProfileLoader.Result result) {
        lastResult = result;
        String currentSelectionKey = selectedProfileKey;
        if (currentSelectionKey == null) {
            currentSelectionKey = profilesRepository.getSelectedProfileKey(context);
        }
        String customProfile = profilesRepository.getCustomProfileUri(context) == null
                ? null : profilesRepository.getCustomProfileName(context);
        boolean bRouterInstalled = profilesRepository.isBRouterInstalled(context);
        options.replaceFromLoad(context, result.profiles(), result.bundled(), result.complete(), customProfile);
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
        return ProfileSelectionResolver.selected(context, profilesRepository, selectedOption(),
                listener::onCustomProfilePickerRequested);
    }

    @Nullable
    String selectedBRouterProfileName() {
        return ProfileSpinnerSelectedProfile.brouterProfileName(
                selectedOption(),
                profilesRepository.getCustomProfileName(context)
        );
    }

    @Nullable
    ProfileSpinnerOption selectedOption() {
        return options.optionAt(spinner.getSelectedItemPosition());
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
        boolean userChangedSelection = selectionUserInitiated || !Objects.equals(renderedSelectionKey, option.selectionKey());
        selectionUserInitiated = false;
        if (userChangedSelection) {
            renderedSelectionKey = option.selectionKey();
            saveSelectedOption(option);
        }
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
        setSelection(options.restoredPosition(selectionKey, bRouterInstalled),
                lastResult.complete() || options.contains(selectionKey));
    }

    private void setSelection(int position, boolean commit) {
        if (position < 0 || position >= options.size()) {
            return;
        }
        ProfileSpinnerOption option = options.optionAt(position);
        renderedSelectionKey = option == null ? null : option.selectionKey();
        suppressSelectionCallback = true;
        spinner.setSelection(position);
        suppressSelectionCallback = false;
        if (commit && option != null) {
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


