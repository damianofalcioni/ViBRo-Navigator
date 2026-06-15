package vibro.navigator.main;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vibro.navigator.R;
import vibro.navigator.brouter.BRouterProfileParameter;

final class ProfileParameterForm {

    private static final int ROW_VERTICAL_MARGIN_DP = 8;
    private static final int INFO_BUTTON_SIZE_DP = 44;

    @NonNull
    private final ScrollView view;
    @NonNull
    private final List<Row> rows;

    private ProfileParameterForm(@NonNull ScrollView view, @NonNull List<Row> rows) {
        this.view = view;
        this.rows = rows;
    }

    @NonNull
    static ProfileParameterForm create(
            @NonNull Context context,
            @NonNull List<BRouterProfileParameter> parameters,
            @NonNull Map<String, String> savedValues
    ) {
        ScrollView scrollView = new ScrollView(context);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(context, 8), dp(context, 8), dp(context, 8), 0);
        scrollView.addView(content);

        List<Row> rows = new ArrayList<>();
        for (BRouterProfileParameter parameter : parameters) {
            Row row = Row.create(context, parameter, savedValues.get(parameter.name));
            rows.add(row);
            content.addView(row.view);
        }
        return new ProfileParameterForm(scrollView, rows);
    }

    @NonNull
    View view() {
        return view;
    }

    @Nullable
    Map<String, String> collectValues(@NonNull Context context) {
        Map<String, String> values = new HashMap<>();
        for (Row row : rows) {
            if (!row.input.validate(context)) {
                return null;
            }
            values.put(row.parameter.name, row.input.value());
        }
        return values;
    }

    void resetToDefaults() {
        for (Row row : rows) {
            row.input.setValue(row.parameter.defaultValue);
        }
    }

    private static final class Row {
        @NonNull
        final BRouterProfileParameter parameter;
        @NonNull
        final LinearLayout view;
        @NonNull
        final ProfileParameterValueInput input;

        private Row(
                @NonNull BRouterProfileParameter parameter,
                @NonNull LinearLayout view,
                @NonNull ProfileParameterValueInput input
        ) {
            this.parameter = parameter;
            this.view = view;
            this.input = input;
        }

        @NonNull
        static Row create(
                @NonNull Context context,
                @NonNull BRouterProfileParameter parameter,
                @Nullable String savedValue
        ) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setLayoutParams(rowLayoutParams(context));

            TextView name = new TextView(context);
            name.setText(parameter.name);
            name.setTextColor(ContextCompat.getColor(context, R.color.white));
            name.setLayoutParams(nameLayoutParams());
            row.addView(name);

            ProfileParameterValueInput input = ProfileParameterValueInput.create(context, parameter);
            input.setValue(savedValue == null ? parameter.defaultValue : savedValue);
            row.addView(input.view());

            if (parameter.hasDescription()) {
                row.addView(infoButton(context, parameter));
            } else {
                row.addView(infoButtonSpacer(context));
            }
            return new Row(parameter, row, input);
        }

        @NonNull
        private static LinearLayout.LayoutParams rowLayoutParams(@NonNull Context context) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, dp(context, ROW_VERTICAL_MARGIN_DP), 0, dp(context, ROW_VERTICAL_MARGIN_DP));
            return params;
        }

        @NonNull
        private static LinearLayout.LayoutParams nameLayoutParams() {
            return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        }

        @NonNull
        private static View infoButton(@NonNull Context context, @NonNull BRouterProfileParameter parameter) {
            ImageButton button = new ImageButton(context);
            button.setImageResource(R.drawable.ic_info);
            button.setBackgroundResource(R.drawable.bg_button_icon);
            button.setContentDescription(context.getString(
                    R.string.format_profile_parameter_info_content_description,
                    parameter.name
            ));
            button.setLayoutParams(fixedSize(context, INFO_BUTTON_SIZE_DP));
            button.setOnClickListener(v -> new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.title_profile_parameter_description, parameter.name))
                    .setMessage(parameter.description)
                    .setPositiveButton(android.R.string.ok, null)
                    .show());
            return button;
        }

        @NonNull
        private static View infoButtonSpacer(@NonNull Context context) {
            View spacer = new View(context);
            spacer.setLayoutParams(fixedSize(context, INFO_BUTTON_SIZE_DP));
            return spacer;
        }
    }

    @NonNull
    private static LinearLayout.LayoutParams fixedSize(@NonNull Context context, int widthDp) {
        return new LinearLayout.LayoutParams(dp(context, widthDp), ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static int dp(@NonNull Context context, int value) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
