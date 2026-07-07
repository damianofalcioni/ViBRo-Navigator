package vibro.navigator.nav.ui;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;

final class NavigationDetailsDialog {
    private static final float DIALOG_MAX_WIDTH_DP = 320f;
    private static final float DIALOG_MIN_WIDTH_DP = 252f;
    private static final float DIALOG_SIDE_MARGIN_DP = 40f;
    private static final float DIALOG_FALLBACK_MARGIN_DP = 24f;
    private static final float DIALOG_RADIUS_DP = 8f;
    private static final float DIALOG_HORIZONTAL_PADDING_DP = 24f;
    private static final float DIALOG_VERTICAL_PADDING_DP = 22f;
    private static final float DETAILS_TEXT_SIZE_SP = 20f;
    private static final float TITLE_TEXT_SIZE_SP = 26f;
    private static final float DETAILS_TOP_MARGIN_DP = 22f;
    private static final float BUTTON_TOP_MARGIN_DP = 18f;
    private static final float BUTTON_MIN_WIDTH_DP = 72f;
    private static final float BUTTON_MIN_HEIGHT_DP = 48f;

    @NonNull
    private final Activity activity;
    @StringRes
    private final int titleResId;
    private Dialog dialog;
    private TextView detailsText;

    NavigationDetailsDialog(@NonNull Activity activity, @StringRes int titleResId) {
        this.activity = activity;
        this.titleResId = titleResId;
    }

    void show() {
        if (dialog != null && dialog.isShowing()) {
            return;
        }
        detailsText = createDetailsText();
        Dialog createdDialog = new Dialog(activity);
        createdDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        createdDialog.setTitle(titleResId);
        createdDialog.setContentView(createDialogContent(detailsText));
        createdDialog.setOnDismissListener(ignored -> {
            dialog = null;
            detailsText = null;
        });
        dialog = createdDialog;
        createdDialog.show();
        configureWindow(createdDialog);
    }

    void update(@NonNull CharSequence details) {
        if (detailsText != null) {
            detailsText.setText(details);
        }
    }

    void dismiss() {
        Dialog activeDialog = dialog;
        if (activeDialog != null) {
            activeDialog.dismiss();
        }
    }

    @NonNull
    private TextView createDetailsText() {
        TextView text = new TextView(activity);
        text.setGravity(Gravity.START);
        text.setIncludeFontPadding(false);
        text.setTextColor(AndroidAppTheme.color(activity, R.attr.vibroTextPrimaryColor));
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, DETAILS_TEXT_SIZE_SP);
        text.setLineSpacing(dp(4f), 1f);
        return text;
    }

    @NonNull
    private LinearLayout createDialogContent(@NonNull TextView details) {
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(
                dp(DIALOG_HORIZONTAL_PADDING_DP),
                dp(DIALOG_VERTICAL_PADDING_DP),
                dp(DIALOG_HORIZONTAL_PADDING_DP),
                dp(14f)
        );
        content.setBackground(dialogBackground());
        content.addView(createTitle(), wrapContentParams());

        LinearLayout.LayoutParams detailsParams = wrapContentParams();
        detailsParams.setMargins(0, dp(DETAILS_TOP_MARGIN_DP), 0, 0);
        content.addView(details, detailsParams);

        LinearLayout.LayoutParams buttonRowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        buttonRowParams.setMargins(0, dp(BUTTON_TOP_MARGIN_DP), 0, 0);
        content.addView(createButtonRow(), buttonRowParams);
        return content;
    }

    @NonNull
    private TextView createTitle() {
        TextView title = new TextView(activity);
        title.setText(titleResId);
        title.setGravity(Gravity.CENTER);
        title.setIncludeFontPadding(false);
        title.setTextColor(AndroidAppTheme.color(activity, R.attr.vibroTextPrimaryColor));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, TITLE_TEXT_SIZE_SP);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return title;
    }

    @NonNull
    private LinearLayout createButtonRow() {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.END);
        row.addView(createOkButton(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return row;
    }

    @NonNull
    private Button createOkButton() {
        Button button = new Button(activity);
        button.setText(android.R.string.ok);
        button.setAllCaps(true);
        button.setGravity(Gravity.CENTER);
        button.setMinWidth(dp(BUTTON_MIN_WIDTH_DP));
        button.setMinHeight(dp(BUTTON_MIN_HEIGHT_DP));
        button.setPadding(dp(12f), 0, dp(12f), 0);
        button.setTextColor(AndroidAppTheme.color(activity, R.attr.vibroTextPrimaryColor));
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
        button.setBackgroundResource(resolveSelectableBorderlessBackground());
        button.setOnClickListener(v -> dismiss());
        return button;
    }

    @NonNull
    private LinearLayout.LayoutParams wrapContentParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    @NonNull
    private GradientDrawable dialogBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(AndroidAppTheme.color(activity, R.attr.vibroSurfaceStrongColor));
        background.setCornerRadius(dp(DIALOG_RADIUS_DP));
        return background;
    }

    private void configureWindow(@NonNull Dialog shownDialog) {
        Window window = shownDialog.getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(dialogWidthPx(), WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private int dialogWidthPx() {
        int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        int preferredWidth = screenWidth - dp(DIALOG_SIDE_MARGIN_DP * 2f);
        int maxWidth = Math.min(dp(DIALOG_MAX_WIDTH_DP), screenWidth - dp(DIALOG_FALLBACK_MARGIN_DP));
        int minWidth = Math.min(dp(DIALOG_MIN_WIDTH_DP), maxWidth);
        return Math.min(maxWidth, Math.max(minWidth, preferredWidth));
    }

    private int resolveSelectableBorderlessBackground() {
        TypedValue outValue = new TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
        return outValue.resourceId;
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                activity.getResources().getDisplayMetrics()
        ));
    }
}
