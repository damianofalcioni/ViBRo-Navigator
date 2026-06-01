package vibro.navigator.about;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import vibro.navigator.R;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.dispatch.TaskScheduler;
import vibro.navigator.distribution.DistributionServices;
import vibro.navigator.distribution.GooglePoiApiKeyValidationResult;
import vibro.navigator.settings.AppSettings;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class AboutGooglePoiApiKeySettings {
    @NonNull
    private final Activity activity;
    @NonNull
    private final View container;
    @NonNull
    private final View editButton;
    @NonNull
    private final Switch enabledSwitch;
    @NonNull
    private final ApiKeyValidator validator;
    private boolean renderingSwitch;
    @Nullable
    private static ApiKeyValidator testValidator;

    AboutGooglePoiApiKeySettings(
            @NonNull Activity activity,
            @NonNull View container,
            @NonNull View editButton,
            @NonNull Switch enabledSwitch
    ) {
        this.activity = activity;
        this.container = container;
        this.editButton = editButton;
        this.enabledSwitch = enabledSwitch;
        this.validator = testValidator == null ? new BackgroundApiKeyValidator() : testValidator;
    }

    static void setApiKeyValidatorForTests(@Nullable ApiKeyValidator validator) {
        testValidator = validator;
    }

    void configure() {
        boolean supported = DistributionServices.supportsUserGooglePoiApiKey();
        container.setVisibility(supported ? View.VISIBLE : View.GONE);
        editButton.setEnabled(supported);
        if (!supported) {
            enabledSwitch.setEnabled(false);
            return;
        }
        refresh();
        editButton.setOnClickListener(v -> showDialog());
        enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> setGoogleSearchEnabled(isChecked));
    }

    void shutdown() {
        validator.shutdown();
    }

    void refresh() {
        if (!DistributionServices.supportsUserGooglePoiApiKey()) {
            return;
        }
        boolean hasValidKey = AppSettings.hasValidGooglePoiApiKey(activity);
        renderingSwitch = true;
        enabledSwitch.setEnabled(hasValidKey);
        enabledSwitch.setChecked(hasValidKey && AppSettings.isGooglePoiSearchEnabled(activity));
        renderingSwitch = false;
    }

    private void setGoogleSearchEnabled(boolean enabled) {
        if (renderingSwitch) {
            return;
        }
        if (enabled && !AppSettings.hasValidGooglePoiApiKey(activity)) {
            AppSettings.setGooglePoiSearchEnabled(activity, false);
            Toast.makeText(activity, R.string.msg_google_poi_api_key_required, Toast.LENGTH_SHORT).show();
            refresh();
            return;
        }
        AppSettings.setGooglePoiSearchEnabled(activity, enabled);
        refresh();
    }

    private void showDialog() {
        EditText editText = createApiKeyEditText();
        LinearLayout content = createDialogContent(editText);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.label_google_poi_api_key)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_save, null)
                .create();
        dialog.setOnShowListener(d -> {
            content.requestFocus();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> saveOrValidateApiKey(dialog, editText));
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
    }

    @NonNull
    private LinearLayout createDialogContent(@NonNull EditText editText) {
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), dp(4));
        content.setFocusable(true);
        content.setFocusableInTouchMode(true);
        content.addView(editText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return content;
    }

    @NonNull
    private EditText createApiKeyEditText() {
        EditText editText = new EditText(activity);
        editText.setId(R.id.aboutGooglePoiApiKeyEdit);
        editText.setHint(R.string.hint_google_poi_api_key);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            editText.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editText.setMaxLines(1);
        editText.setSelectAllOnFocus(true);
        editText.setSingleLine(true);
        editText.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        editText.setText(AppSettings.getGooglePoiApiKey(activity));
        editText.setTextColor(ContextCompat.getColor(activity, R.color.white));
        editText.setHintTextColor(ContextCompat.getColor(activity, R.color.gray_700));
        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        return editText;
    }

    private void saveOrValidateApiKey(@NonNull AlertDialog dialog, @NonNull EditText editText) {
        String apiKey = editText.getText().toString().trim();
        if (apiKey.isEmpty()) {
            clearApiKey(dialog);
            return;
        }
        setDialogButtonsEnabled(dialog, false);
        Toast.makeText(activity, R.string.msg_google_poi_api_key_validating, Toast.LENGTH_SHORT).show();
        validator.validate(apiKey, result -> handleValidationResult(dialog, apiKey, result));
    }

    private void clearApiKey(@NonNull AlertDialog dialog) {
        AppSettings.setGooglePoiApiKey(activity, "");
        refresh();
        Toast.makeText(activity, R.string.msg_google_poi_api_key_cleared, Toast.LENGTH_SHORT).show();
        dialog.dismiss();
    }

    private void handleValidationResult(
            @NonNull AlertDialog dialog,
            @NonNull String apiKey,
            @NonNull GooglePoiApiKeyValidationResult result
    ) {
        setDialogButtonsEnabled(dialog, true);
        if (result == GooglePoiApiKeyValidationResult.VALID) {
            AppSettings.setValidatedGooglePoiApiKey(activity, apiKey);
            refresh();
            Toast.makeText(activity, R.string.msg_google_poi_api_key_saved, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        } else if (result == GooglePoiApiKeyValidationResult.INVALID) {
            Toast.makeText(activity, R.string.msg_google_poi_api_key_invalid, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(activity, R.string.msg_google_poi_api_key_validation_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void setDialogButtonsEnabled(@NonNull AlertDialog dialog, boolean enabled) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(enabled);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(enabled);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                activity.getResources().getDisplayMetrics()
        );
    }

    interface ApiKeyValidator {
        void validate(
                @NonNull String apiKey,
                @NonNull ValidationCallback callback
        );

        default void shutdown() {
        }
    }

    interface ValidationCallback {
        void onResult(@NonNull GooglePoiApiKeyValidationResult result);
    }

    private static final class BackgroundApiKeyValidator implements ApiKeyValidator {
        @NonNull
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        @NonNull
        private final TaskScheduler scheduler = AndroidTaskScheduler.main();

        @Override
        public void validate(
                @NonNull String apiKey,
                @NonNull ValidationCallback callback
        ) {
            executor.execute(() -> {
                GooglePoiApiKeyValidationResult result = DistributionServices.validateUserGooglePoiApiKey(apiKey);
                scheduler.post(() -> callback.onResult(result));
            });
        }

        @Override
        public void shutdown() {
            executor.shutdownNow();
        }
    }
}
