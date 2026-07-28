package vibro.navigator.about;

import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.nav.voice.NavigationVoiceOption;
import vibro.navigator.settings.AppSettings;

final class AboutManeuverVoiceSettings {
    @NonNull
    private final Activity activity;
    @NonNull
    private final Switch enabledSwitch;
    @NonNull
    private final AboutManeuverVoiceClientLoader voiceClientLoader;
    @NonNull
    private List<NavigationVoiceOption> availableVoiceOptions = Collections.emptyList();
    @Nullable
    private AboutManeuverVoiceOptionAdapter voiceAdapter;
    @Nullable
    private Spinner dialogSpinner;
    private boolean renderingVoiceSelection;
    private boolean renderingEnabledSwitch;
    private boolean voiceListLoaded;

    AboutManeuverVoiceSettings(
            @NonNull Activity activity,
            @NonNull View settingsButton,
            @NonNull Switch enabledSwitch
    ) {
        this(activity, settingsButton, enabledSwitch, AndroidTaskScheduler.main());
    }

    AboutManeuverVoiceSettings(
            @NonNull Activity activity,
            @NonNull View settingsButton,
            @NonNull Switch enabledSwitch,
            @NonNull TaskScheduler voiceClientScheduler
    ) {
        this.activity = activity;
        this.enabledSwitch = enabledSwitch;
        voiceClientLoader = new AboutManeuverVoiceClientLoader(
                activity,
                voiceClientScheduler,
                this::onAvailableVoicesLoaded
        );
        configureEnabledSwitch();
        AboutDeferredDialogAction.configure(activity, settingsButton, this::showDialog);
    }

    void refreshSelection() {
        refreshEnabledSwitch();
        if (dialogSpinner != null) {
            selectSavedVoiceOrUseFallback();
        }
    }

    void shutdown() {
        voiceClientLoader.shutdown();
    }

    private void onAvailableVoicesLoaded(@NonNull List<NavigationVoiceOption> options) {
        voiceListLoaded = true;
        availableVoiceOptions = options;
        voiceAdapter = null;
        if (dialogSpinner != null) {
            configureSpinner(dialogSpinner);
        }
    }

    private void configureEnabledSwitch() {
        refreshEnabledSwitch();
        enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (renderingEnabledSwitch) {
                return;
            }
            AppSettings.setManeuverSpeechEnabled(activity, isChecked);
        });
    }

    private void refreshEnabledSwitch() {
        renderingEnabledSwitch = true;
        enabledSwitch.setChecked(AppSettings.isManeuverSpeechEnabled(activity));
        renderingEnabledSwitch = false;
    }

    private void showDialog() {
        AboutManeuverVoiceDialog.show(activity, new AboutManeuverVoiceDialog.VoiceSpinnerHandler() {
            @Override
            public void configure(@NonNull Spinner spinner) {
                dialogSpinner = spinner;
                configureSpinner(spinner);
            }

            @Override
            public void playSelectedVoice() {
                speakSelectedVoicePreview();
            }

            @Override
            public void clear() {
                dialogSpinner = null;
                voiceClientLoader.onDialogDismissed();
            }
        });
        voiceClientLoader.onDialogShown();
    }

    private void speakSelectedVoicePreview() {
        NavigationVoiceOption selected = AboutManeuverVoiceOptions.selectedVoiceOption(dialogSpinner);
        if (selected != null) {
            voiceClientLoader.requestPreview(selected.voiceName);
        }
    }

    private void configureSpinner(@NonNull Spinner spinner) {
        renderingVoiceSelection = true;
        spinner.setOnItemSelectedListener(null);
        spinner.setAdapter(voiceAdapter());
        selectSavedVoiceOrUseFallback();
        renderingVoiceSelection = false;
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (renderingVoiceSelection) {
                    return;
                }
                Object selected = parent.getItemAtPosition(position);
                if (selected instanceof NavigationVoiceOption) {
                    persistSelectedVoiceIfNeeded((NavigationVoiceOption) selected);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void persistSelectedVoiceIfNeeded(@NonNull NavigationVoiceOption selected) {
        String savedVoiceName = AppSettings.getManeuverVoiceName(activity);
        if (shouldPersistSelectedVoice(voiceListLoaded, savedVoiceName, selected.voiceName)) {
            AppSettings.setManeuverVoiceName(activity, selected.voiceName);
            updateSelectedVoiceHighlight(selected.voiceName);
        }
    }

    private void selectSavedVoiceOrUseFallback() {
        String fallbackVoiceName = AppSettings.MANEUVER_VOICE_SYSTEM_DEFAULT;
        if (selectVoice(AppSettings.getManeuverVoiceName(activity))) {
            return;
        }
        if (voiceListLoaded && AppSettings.isManeuverSpeechEnabled(activity)) {
            AppSettings.setManeuverVoiceName(activity, fallbackVoiceName);
        }
        selectVoice(fallbackVoiceName);
    }

    private boolean selectVoice(@NonNull String voiceName) {
        int position = findVoicePosition(voiceName);
        if (position < 0) {
            return false;
        }
        updateSelectedVoiceHighlight(voiceName);
        if (dialogSpinner != null && dialogSpinner.getCount() > position) {
            dialogSpinner.setSelection(position);
        }
        return true;
    }

    private int findVoicePosition(@NonNull String voiceName) {
        AboutManeuverVoiceOptionAdapter adapter = voiceAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            NavigationVoiceOption option = adapter.getItem(i);
            if (option != null && option.voiceName.equals(voiceName)) {
                return i;
            }
        }
        return -1;
    }

    static boolean shouldPersistSelectedVoice(
            boolean voiceListLoaded,
            @NonNull String savedVoiceName,
            @NonNull String selectedVoiceName
    ) {
        return AboutManeuverVoiceOptions.shouldPersistSelectedVoice(
                voiceListLoaded,
                savedVoiceName,
                selectedVoiceName
        );
    }

    private void updateSelectedVoiceHighlight(@NonNull String voiceName) {
        if (voiceAdapter != null) {
            voiceAdapter.setSelectedVoiceName(voiceName);
        }
    }

    @NonNull
    private AboutManeuverVoiceOptionAdapter voiceAdapter() {
        if (voiceAdapter == null) {
            voiceAdapter = new AboutManeuverVoiceOptionAdapter(
                    activity,
                    AboutManeuverVoiceOptions.withBaseOptions(activity, availableVoiceOptions)
            );
        }
        return voiceAdapter;
    }
}
