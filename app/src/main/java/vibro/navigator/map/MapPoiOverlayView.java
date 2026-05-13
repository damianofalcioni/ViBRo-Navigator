package vibro.navigator.map;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.List;
import java.util.Set;

import vibro.navigator.R;

final class MapPoiOverlayView {
    private static final int CATEGORY_TEXT_COLOR = Color.WHITE;
    private static final int CATEGORY_SELECTED_TEXT_COLOR = 0xFF32D074;
    private static final int CATEGORY_SELECTED_BACKGROUND = 0xCC2C3744;
    private static final int CATEGORY_UNSELECTED_BACKGROUND = Color.TRANSPARENT;

    interface Listener {
        void onCategoryChecked(@NonNull MapPoiCategory category, boolean checked);
    }

    interface PanelListener {
        void onPanelOpened();
    }

    @NonNull
    private final Activity activity;
    @NonNull
    private final View panel;
    @NonNull
    private final LinearLayout categoryList;
    @NonNull
    private final TextView status;
    @Nullable
    private PanelListener panelListener;

    private MapPoiOverlayView(
            @NonNull Activity activity,
            @NonNull ImageButton button,
            @NonNull View panel,
            @NonNull LinearLayout categoryList,
            @NonNull TextView status
    ) {
        this.activity = activity;
        this.panel = panel;
        this.categoryList = categoryList;
        this.status = status;
        button.setOnClickListener(v -> {
            if (togglePanel() && panelListener != null) {
                panelListener.onPanelOpened();
            }
        });
    }

    @NonNull
    static MapPoiOverlayView bind(@NonNull Activity activity) {
        return new MapPoiOverlayView(
                activity,
                activity.findViewById(R.id.mapPoiButton),
                activity.findViewById(R.id.mapPoiPanel),
                activity.findViewById(R.id.mapPoiCategoryList),
                activity.findViewById(R.id.mapPoiStatus)
        );
    }

    void populateCategories(
            @NonNull List<MapPoiCategory> categories,
            @NonNull Set<String> enabledCategoryIds,
            @NonNull Listener listener
    ) {
        categoryList.removeAllViews();
        for (MapPoiCategory category : categories) {
            categoryList.addView(createCategoryRow(category, enabledCategoryIds.contains(category.id), listener));
        }
    }

    void setPanelListener(@NonNull PanelListener listener) {
        panelListener = listener;
    }

    void hideStatus() {
        status.setVisibility(View.GONE);
    }

    void hidePanel() {
        panel.setVisibility(View.GONE);
    }

    void showStatus(@StringRes int messageResId) {
        status.setText(messageResId);
        status.setVisibility(View.VISIBLE);
    }

    @NonNull
    String labelFor(@NonNull MapPoiCategory category) {
        return category.count > 0 ? category.label + " (" + category.count + ")" : category.label;
    }

    private boolean togglePanel() {
        boolean show = panel.getVisibility() != View.VISIBLE;
        panel.setVisibility(show ? View.VISIBLE : View.GONE);
        return show;
    }

    @NonNull
    private View createCategoryRow(
            @NonNull MapPoiCategory category,
            boolean checked,
            @NonNull Listener listener
    ) {
        TextView row = new TextView(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinHeight(dp(38));
        row.setPadding(dp(12), dp(6), dp(12), dp(6));
        row.setText(labelFor(category));
        row.setTextColor(checked ? CATEGORY_SELECTED_TEXT_COLOR : CATEGORY_TEXT_COLOR);
        row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        row.setTypeface(Typeface.DEFAULT, checked ? Typeface.BOLD : Typeface.NORMAL);
        row.setBackgroundColor(checked ? CATEGORY_SELECTED_BACKGROUND : CATEGORY_UNSELECTED_BACKGROUND);
        row.setSelected(checked);
        row.setOnClickListener(v -> listener.onCategoryChecked(category, !checked));
        return row;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                activity.getResources().getDisplayMetrics()
        );
    }
}
