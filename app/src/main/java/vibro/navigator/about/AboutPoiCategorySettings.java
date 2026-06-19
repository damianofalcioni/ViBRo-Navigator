package vibro.navigator.about;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.settings.AppPoiCategorySetting;
import vibro.navigator.settings.AppSettings;

final class AboutPoiCategorySettings {
    @NonNull
    private final Activity activity;
    @NonNull
    private final View listButton;
    @NonNull
    private final Switch enabledSwitch;

    AboutPoiCategorySettings(
            @NonNull Activity activity,
            @NonNull View listButton,
            @NonNull Switch enabledSwitch
    ) {
        this.activity = activity;
        this.listButton = listButton;
        this.enabledSwitch = enabledSwitch;
    }

    void configure() {
        refresh();
        enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                AppSettings.setMapPoiCategoryFilterEnabled(activity, isChecked));
        AboutDeferredDialogAction.configure(activity, listButton, this::showDialog);
    }

    void refresh() {
        enabledSwitch.setChecked(AppSettings.isMapPoiCategoryFilterEnabled(activity));
    }

    private void showDialog() {
        List<CategoryRow> rows = new ArrayList<>();
        LinearLayout categoryList = createCategoryList();
        populateRows(categoryList, rows);

        LinearLayout content = createDialogContent();
        content.addView(createCategoryScrollView(categoryList));
        content.addView(createAddButtonRow(categoryList, rows));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.label_poi_categories)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_save_poi_categories, (d, which) -> save(rows))
                .create();
        dialog.setOnShowListener(d -> content.requestFocus());
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
    }

    private void populateRows(
            @NonNull LinearLayout categoryList,
            @NonNull List<CategoryRow> rows
    ) {
        List<AppPoiCategorySetting> settings = AppSettings.getMapPoiCategorySettings(activity);
        if (settings.isEmpty()) {
            addCategoryRow(categoryList, rows, "", true);
            return;
        }
        for (AppPoiCategorySetting setting : settings) {
            addCategoryRow(categoryList, rows, setting.name, setting.enabled);
        }
    }

    @NonNull
    private LinearLayout createDialogContent() {
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), dp(4));
        content.setFocusable(true);
        content.setFocusableInTouchMode(true);
        return content;
    }

    @NonNull
    private LinearLayout createCategoryList() {
        LinearLayout categoryList = new LinearLayout(activity);
        categoryList.setId(R.id.aboutPoiCategoryDialogList);
        categoryList.setOrientation(LinearLayout.VERTICAL);
        return categoryList;
    }

    @NonNull
    private View createCategoryScrollView(@NonNull LinearLayout categoryList) {
        ScrollView scrollView = new MaxHeightScrollView(dp(360));
        scrollView.setFillViewport(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        scrollView.addView(categoryList, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));
        return scrollView;
    }

    @NonNull
    private View createAddButtonRow(
            @NonNull LinearLayout categoryList,
            @NonNull List<CategoryRow> rows
    ) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, dp(8), 0, 0);

        ImageButton addButton = createIconButton(R.drawable.ic_plus, R.string.action_add_poi_category);
        addButton.setId(R.id.aboutPoiCategoryAddButton);
        addButton.setOnClickListener(v -> addCategoryRow(categoryList, rows, "", true));
        row.addView(addButton, new LinearLayout.LayoutParams(dp(44), dp(44)));
        return row;
    }

    private void addCategoryRow(
            @NonNull LinearLayout categoryList,
            @NonNull List<CategoryRow> rows,
            @NonNull String value,
            boolean checked
    ) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(4));

        EditText editText = createCategoryEditText(value);
        row.addView(editText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Switch rowSwitch = createCategorySwitch(checked);
        LinearLayout.LayoutParams switchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        switchParams.setMarginStart(dp(8));
        row.addView(rowSwitch, switchParams);

        CategoryRow categoryRow = new CategoryRow(editText, rowSwitch);
        rows.add(categoryRow);
        ImageButton removeButton = createIconButton(R.drawable.ic_close, R.string.action_remove_poi_category);
        removeButton.setOnClickListener(v -> {
            categoryList.removeView(row);
            rows.remove(categoryRow);
        });
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        buttonParams.setMarginStart(dp(8));
        row.addView(removeButton, buttonParams);

        categoryList.addView(row);
    }

    @NonNull
    private EditText createCategoryEditText(@NonNull String value) {
        EditText editText = new EditText(activity);
        editText.setHint(R.string.hint_poi_category_name);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            editText.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editText.setMaxLines(1);
        editText.setSingleLine(true);
        editText.setText(value);
        editText.setTextColor(AndroidAppTheme.color(activity, R.attr.vibroTextPrimaryColor));
        editText.setHintTextColor(AndroidAppTheme.color(activity, R.attr.vibroTextHintColor));
        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        return editText;
    }

    @NonNull
    private Switch createCategorySwitch(boolean checked) {
        Switch rowSwitch = new Switch(activity);
        rowSwitch.setChecked(checked);
        rowSwitch.setContentDescription(activity.getString(R.string.action_enable_poi_category));
        rowSwitch.setMinHeight(dp(44));
        rowSwitch.setShowText(false);
        return rowSwitch;
    }

    @NonNull
    private ImageButton createIconButton(int iconResId, int contentDescriptionResId) {
        ImageButton button = new ImageButton(activity);
        button.setBackgroundResource(R.drawable.bg_button_icon);
        button.setContentDescription(activity.getString(contentDescriptionResId));
        button.setImageResource(iconResId);
        button.setPadding(dp(10), dp(10), dp(10), dp(10));
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        return button;
    }

    private void save(@NonNull List<CategoryRow> rows) {
        List<AppPoiCategorySetting> settings = new ArrayList<>();
        for (CategoryRow row : rows) {
            settings.add(new AppPoiCategorySetting(row.name(), row.enabled()));
        }
        AppSettings.setMapPoiCategorySettings(activity, settings);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                activity.getResources().getDisplayMetrics()
        );
    }

    private static final class CategoryRow {
        @NonNull
        private final EditText nameField;
        @NonNull
        private final Switch enabledSwitch;

        private CategoryRow(@NonNull EditText nameField, @NonNull Switch enabledSwitch) {
            this.nameField = nameField;
            this.enabledSwitch = enabledSwitch;
        }

        @NonNull
        String name() {
            return nameField.getText().toString();
        }

        boolean enabled() {
            return enabledSwitch.isChecked();
        }
    }

    private final class MaxHeightScrollView extends ScrollView {
        private final int maxHeight;

        private MaxHeightScrollView(int maxHeight) {
            super(activity);
            this.maxHeight = maxHeight;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int cappedHeight = View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST);
            super.onMeasure(widthMeasureSpec, cappedHeight);
        }
    }
}
