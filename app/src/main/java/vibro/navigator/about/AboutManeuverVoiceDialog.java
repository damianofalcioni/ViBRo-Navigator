package vibro.navigator.about;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.android.theme.AndroidThemedButtons;
import vibro.navigator.nav.voice.NavigationTextToSpeechSettingsLauncher;

final class AboutManeuverVoiceDialog {
    private AboutManeuverVoiceDialog() {
    }

    static void show(
            @NonNull Activity activity,
            @NonNull VoiceSpinnerHandler spinnerHandler
    ) {
        LinearLayout content = createDialogContent(activity);
        content.addView(createVoiceRow(activity, spinnerHandler), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(activity, 52)
        );
        buttonParams.gravity = Gravity.END;
        buttonParams.setMargins(0, dp(activity, 12), 0, 0);
        content.addView(createTtsSettingsButton(activity), buttonParams);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.label_maneuver_voice)
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
    private static View createVoiceRow(
            @NonNull Activity activity,
            @NonNull VoiceSpinnerHandler spinnerHandler
    ) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);

        row.addView(createVoiceLabel(activity));

        Spinner spinner = createVoiceSpinner(activity);
        spinnerHandler.configure(spinner);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        spinnerParams.setMarginStart(dp(activity, 12));
        row.addView(spinner, spinnerParams);

        ImageButton playButton = createPlayButton(activity, spinnerHandler);
        LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(
                dp(activity, 44),
                dp(activity, 44)
        );
        playParams.setMarginStart(dp(activity, 8));
        row.addView(playButton, playParams);
        return row;
    }

    @NonNull
    private static TextView createVoiceLabel(@NonNull Activity activity) {
        TextView label = new TextView(activity);
        label.setId(R.id.aboutManeuverVoiceDialogVoiceLabel);
        label.setText(R.string.label_maneuver_voice_spinner);
        label.setTextColor(AndroidAppTheme.color(activity, R.attr.vibroTextPrimaryColor));
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        return label;
    }

    @NonNull
    private static Spinner createVoiceSpinner(@NonNull Activity activity) {
        Spinner spinner = new Spinner(activity);
        spinner.setId(R.id.aboutManeuverVoiceSpinner);
        spinner.setBackgroundResource(R.drawable.bg_spinner_field);
        spinner.setPopupBackgroundResource(R.drawable.bg_spinner_popup);
        spinner.setMinimumHeight(dp(activity, 48));
        return spinner;
    }

    @NonNull
    private static ImageButton createPlayButton(
            @NonNull Activity activity,
            @NonNull VoiceSpinnerHandler spinnerHandler
    ) {
        ImageButton button = new ImageButton(activity);
        button.setId(R.id.aboutManeuverVoicePlayButton);
        AndroidThemedButtons.applyIconButtonBackground(button);
        button.setContentDescription(activity.getString(R.string.action_test_maneuver_voice));
        button.setImageResource(R.drawable.ic_play);
        button.setPadding(dp(activity, 10), dp(activity, 10), dp(activity, 10), dp(activity, 10));
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setOnClickListener(v -> spinnerHandler.playSelectedVoice());
        return button;
    }

    @NonNull
    private static Button createTtsSettingsButton(@NonNull Activity activity) {
        Button button = new Button(activity);
        button.setId(R.id.aboutTtsSettingsButton);
        button.setText(R.string.action_android_tts_settings_short);
        button.setContentDescription(activity.getString(R.string.action_open_tts_settings));
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
        button.setOnClickListener(v -> NavigationTextToSpeechSettingsLauncher.open(activity));
        return button;
    }

    private static int dp(@NonNull Activity activity, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                activity.getResources().getDisplayMetrics()
        );
    }

    interface VoiceSpinnerHandler {
        void configure(@NonNull Spinner spinner);

        void playSelectedVoice();

        void clear();
    }
}
