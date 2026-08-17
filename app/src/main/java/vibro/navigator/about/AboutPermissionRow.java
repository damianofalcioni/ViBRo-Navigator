package vibro.navigator.about;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import vibro.navigator.R;
import vibro.navigator.android.startup.AndroidNavigationSettingsLauncher;

final class AboutPermissionRow {

    interface RowClickAction {
        void open();
    }

    interface SettingsIntentFactory {
        @NonNull
        Intent create(@NonNull Activity activity);
    }

    @NonNull
    private final Activity activity;
    @NonNull
    private final View rowView;
    @NonNull
    private final View markerView;
    @NonNull
    private final TextView labelView;
    @NonNull
    private final TextView statusView;

    AboutPermissionRow(
            @NonNull Activity activity,
            int rowId,
            int markerId,
            int labelId,
            int statusId,
            @NonNull SettingsIntentFactory intentFactory
    ) {
        this(activity, rowId, markerId, labelId, statusId, () -> openSettings(activity, intentFactory));
    }

    AboutPermissionRow(
            @NonNull Activity activity,
            int rowId,
            int markerId,
            int labelId,
            int statusId,
            @NonNull RowClickAction clickAction
    ) {
        this.activity = activity;
        rowView = activity.findViewById(rowId);
        markerView = activity.findViewById(markerId);
        labelView = activity.findViewById(labelId);
        statusView = activity.findViewById(statusId);
        rowView.setOnClickListener(v -> clickAction.open());
    }

    void render(boolean allowed) {
        int statusResId = allowed
                ? R.string.permission_status_ok
                : R.string.permission_status_needs_attention;
        int colorResId = allowed ? R.color.success : R.color.danger;
        int markerResId = allowed
                ? R.drawable.bg_permission_status_ok
                : R.drawable.bg_permission_status_error;

        renderStatus(statusResId, colorResId, markerResId);
    }

    void renderWarningKo() {
        renderStatus(
                R.string.permission_status_needs_attention,
                R.color.warning,
                R.drawable.bg_permission_status_warning
        );
    }

    void setVisible(boolean visible) {
        rowView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void renderStatus(int statusResId, int colorResId, int markerResId) {
        markerView.setBackgroundResource(markerResId);
        statusView.setText(statusResId);
        statusView.setTextColor(ContextCompat.getColor(activity, colorResId));
        rowView.setContentDescription(activity.getString(
                R.string.format_about_permission_status_content_description,
                labelView.getText(),
                statusView.getText()
        ));
    }

    private static void openSettings(
            @NonNull Activity activity,
            @NonNull SettingsIntentFactory intentFactory
    ) {
        Intent intent = intentFactory.create(activity);
        if (!AndroidNavigationSettingsLauncher.launch(activity, intent)) {
            Toast.makeText(activity, R.string.msg_open_settings_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
