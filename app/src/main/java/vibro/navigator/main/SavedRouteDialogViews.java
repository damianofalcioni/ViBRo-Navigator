package vibro.navigator.main;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;

final class SavedRouteDialogViews {
    private SavedRouteDialogViews() {
    }

    @NonNull
    static LinearLayout paddedContent(@NonNull Activity activity) {
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(activity, 20), dp(activity, 8), dp(activity, 20), dp(activity, 4));
        return content;
    }

    @NonNull
    static EditText routeNameEditText(@NonNull Activity activity, @NonNull String value) {
        EditText editText = new EditText(activity);
        editText.setHint(R.string.hint_saved_route_name);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            editText.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editText.setMaxLines(1);
        editText.setSelectAllOnFocus(true);
        editText.setSingleLine(true);
        editText.setText(value);
        editText.setSelection(editText.getText().length());
        editText.setTextColor(AndroidAppTheme.color(activity, R.attr.vibroTextPrimaryColor));
        editText.setHintTextColor(AndroidAppTheme.color(activity, R.attr.vibroTextHintColor));
        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        return editText;
    }

    @NonNull
    static ImageButton iconButton(@NonNull Activity activity, int iconResId, int contentDescriptionResId) {
        ImageButton button = new ImageButton(activity);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setContentDescription(activity.getString(contentDescriptionResId));
        button.setImageResource(iconResId);
        button.setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 8), dp(activity, 8));
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        return button;
    }

    static int dp(@NonNull Activity activity, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                activity.getResources().getDisplayMetrics()
        );
    }
}
