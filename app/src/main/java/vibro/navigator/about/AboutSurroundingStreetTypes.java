package vibro.navigator.about;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.EnumSet;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.nav.compass.CompassStreetCategory;
import vibro.navigator.nav.compass.CompassStreetType;
import vibro.navigator.nav.compass.CompassStreetVisibility;
import vibro.navigator.settings.AppCompassStreetTypeSettings;

/** Four color groups with independent per-tag visibility and reversible group masters. */
final class AboutSurroundingStreetTypes {
    private final Activity activity;

    AboutSurroundingStreetTypes(@NonNull Activity activity) {
        this.activity = activity;
    }

    void configure(@NonNull View button) {
        AboutDeferredDialogAction.configure(activity, button, this::showDialog);
    }

    private void showDialog() {
        CompassStreetVisibility saved = AppCompassStreetTypeSettings.get(activity);
        Switch[] groups = new Switch[CompassStreetCategory.values().length];
        Switch[] types = new Switch[CompassStreetType.values().length];
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), dp(8));
        TextView hint = new TextView(activity);
        hint.setText(R.string.hint_surrounding_street_types);
        hint.setTextColor(AndroidAppTheme.color(activity, R.attr.vibroTextPrimaryColor));
        content.addView(hint);
        for (CompassStreetCategory category : CompassStreetCategory.values()) {
            addCategory(content, category, saved, groups, types);
        }

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        scroll.addView(content);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.title_surrounding_street_types)
                .setView(scroll)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_save_poi_categories, (dialog, which) -> save(groups, types))
                .show();
    }

    private void addCategory(
            @NonNull LinearLayout content,
            @NonNull CompassStreetCategory category,
            @NonNull CompassStreetVisibility saved,
            @NonNull Switch[] groups,
            @NonNull Switch[] types
    ) {
        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setMinimumHeight(dp(56));
        header.setPadding(0, dp(18), 0, dp(4));
        String categoryLabel = activity.getString(AboutStreetTypeLabels.categoryLabel(category));
        TextView heading = new TextView(activity);
        heading.setText(categoryLabel);
        heading.setTextColor(AndroidAppTheme.color(activity, R.attr.vibroTextPrimaryColor));
        heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        heading.setTag(category.name() + "_HEADING");
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView swatch = new TextView(activity);
        swatch.setText(R.string.street_color_swatch);
        swatch.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        swatch.setTextColor(AndroidAppTheme.color(activity, AboutStreetTypeLabels.colorAttribute(category)));
        swatch.setTag(category.name() + "_SWATCH");
        LinearLayout.LayoutParams swatchParams = new LinearLayout.LayoutParams(-2, -2);
        swatchParams.setMarginStart(dp(8));
        swatchParams.setMarginEnd(dp(8));
        header.addView(swatch, swatchParams);
        Switch group = new Switch(activity);
        group.setChecked(saved.isCategoryEnabled(category));
        group.setShowText(false);
        group.setContentDescription(categoryLabel);
        group.setTag(category.name());
        header.addView(group, new LinearLayout.LayoutParams(-2, dp(48)));
        groups[category.ordinal()] = group;
        content.addView(header);
        addTypes(content, category, saved, types);
    }

    private void addTypes(
            @NonNull LinearLayout content,
            @NonNull CompassStreetCategory category,
            @NonNull CompassStreetVisibility saved,
            @NonNull Switch[] types
    ) {
        for (int rank = 0; rank <= 3; rank++) {
            for (CompassStreetType type : CompassStreetType.values()) {
                if (type.category() != category || !selectable(type) || displayRank(type) != rank) {
                    continue;
                }
                Switch leaf = streetSwitch(AboutStreetTypeLabels.typeLabel(activity, type), saved.isTypeEnabled(type));
                leaf.setTag(type.name());
                leaf.setPadding(dp(24), 0, 0, 0);
                content.addView(leaf, new LinearLayout.LayoutParams(-1, dp(44)));
                types[type.ordinal()] = leaf;
            }
        }
    }

    private static int displayRank(@NonNull CompassStreetType type) {
        if (type.name().startsWith("ROUTE_")) {
            return 0;
        }
        if (type == CompassStreetType.RAILWAY || type == CompassStreetType.WATERWAY) {
            return 1;
        }
        return type == CompassStreetType.OTHER ? 3 : 2;
    }

    private Switch streetSwitch(@NonNull String label, boolean checked) {
        Switch control = new Switch(activity);
        control.setText(label);
        control.setTextColor(AndroidAppTheme.color(activity, R.attr.vibroTextPrimaryColor));
        control.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        control.setChecked(checked);
        control.setShowText(false);
        return control;
    }

    private void save(
            @NonNull Switch[] groups,
            @NonNull Switch[] types
    ) {
        EnumSet<CompassStreetCategory> disabledGroups = EnumSet.noneOf(CompassStreetCategory.class);
        EnumSet<CompassStreetType> disabledTypes = EnumSet.noneOf(CompassStreetType.class);
        for (CompassStreetCategory category : CompassStreetCategory.values()) {
            if (!groups[category.ordinal()].isChecked()) {
                disabledGroups.add(category);
            }
        }
        for (CompassStreetType type : CompassStreetType.values()) {
            Switch control = types[type.ordinal()];
            if (control != null && !control.isChecked()) {
                disabledTypes.add(type);
            }
        }
        AppCompassStreetTypeSettings.set(activity, new CompassStreetVisibility(disabledGroups, disabledTypes));
    }

    private static boolean selectable(@NonNull CompassStreetType type) {
        return type != CompassStreetType.ROUTE_WALKING_CYCLING
                && type != CompassStreetType.SPECIAL_ROUTE;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                activity.getResources().getDisplayMetrics()
        );
    }
}
