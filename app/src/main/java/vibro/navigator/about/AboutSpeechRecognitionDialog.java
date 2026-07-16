package vibro.navigator.about;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import vibro.navigator.R;
import vibro.navigator.android.speech.AndroidSpeechRecognitionSettingsLauncher;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.android.theme.AndroidThemedButtons;

final class AboutSpeechRecognitionDialog {
    private AboutSpeechRecognitionDialog() {
    }

    static void show(
            @NonNull Activity activity,
            @NonNull LanguageSpinnerHandler spinnerHandler
    ) {
        LinearLayout content = createDialogContent(activity);
        content.addView(createLanguageRow(activity, spinnerHandler), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(activity, 52)
        );
        buttonParams.gravity = Gravity.END;
        buttonParams.setMargins(0, dp(activity, 12), 0, 0);
        content.addView(createAndroidSettingsButton(activity), buttonParams);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.label_speech_recognition)
                .setView(content)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        dialog.setOnDismissListener(d -> spinnerHandler.clear());
        dialog.setOnShowListener(d -> content.requestFocus());
        dialog.show();
    }

    @NonNull
    private static LinearLayout createDialogContent(@NonNull Activity activity) {
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(activity, 20), dp(activity, 8), dp(activity, 20), dp(activity, 4));
        content.setFocusable(true);
        content.setFocusableInTouchMode(true);
        return content;
    }

    @NonNull
    private static View createLanguageRow(
            @NonNull Activity activity,
            @NonNull LanguageSpinnerHandler spinnerHandler
    ) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);

        row.addView(createLanguageLabel(activity));

        Spinner spinner = createLanguageSpinner(activity);
        spinnerHandler.configure(spinner);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        spinnerParams.setMarginStart(dp(activity, 12));
        row.addView(spinner, spinnerParams);
        return row;
    }

    @NonNull
    private static TextView createLanguageLabel(@NonNull Activity activity) {
        TextView label = new TextView(activity);
        label.setId(R.id.aboutSpeechRecognitionLanguageLabel);
        label.setText(R.string.label_speech_recognition_language);
        label.setTextColor(AndroidAppTheme.color(activity, R.attr.vibroTextPrimaryColor));
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        return label;
    }

    @NonNull
    private static Spinner createLanguageSpinner(@NonNull Activity activity) {
        Spinner spinner = new Spinner(activity);
        spinner.setId(R.id.aboutSpeechRecognitionLanguageSpinner);
        spinner.setBackgroundResource(R.drawable.bg_spinner_field);
        spinner.setPopupBackgroundResource(R.drawable.bg_spinner_popup);
        spinner.setMinimumHeight(dp(activity, 48));
        return spinner;
    }

    @NonNull
    private static Button createAndroidSettingsButton(@NonNull Activity activity) {
        Button button = new Button(activity);
        button.setId(R.id.aboutSpeechRecognitionAndroidSettingsButton);
        button.setText(R.string.action_android_stt_settings_short);
        button.setContentDescription(activity.getString(R.string.action_open_android_stt_settings));
        button.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_settings, 0);
        button.setCompoundDrawablePadding(dp(activity, 8));
        button.setAllCaps(false);
        AndroidThemedButtons.applySecondaryButtonBackground(button);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(activity, 52));
        button.setMinWidth(dp(activity, 96));
        button.setPadding(dp(activity, 18), 0, dp(activity, 16), 0);
        button.setTextColor(AndroidAppTheme.color(activity, R.attr.vibroTextPrimaryColor));
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        button.setOnClickListener(v -> AndroidSpeechRecognitionSettingsLauncher.open(activity));
        return button;
    }

    private static int dp(@NonNull Activity activity, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                activity.getResources().getDisplayMetrics()
        );
    }

    interface LanguageSpinnerHandler {
        void configure(@NonNull Spinner spinner);

        void clear();
    }
}
