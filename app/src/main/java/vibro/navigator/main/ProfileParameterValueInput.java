package vibro.navigator.main;

import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.brouter.BRouterProfileParameter;
import vibro.navigator.brouter.BRouterProfileParameterOption;

interface ProfileParameterValueInput {
    int INPUT_WIDTH_DP = 164;

    @NonNull
    View view();

    @NonNull
    String value();

    void setValue(@NonNull String value);

    boolean validate(@NonNull Context context);

    @NonNull
    static ProfileParameterValueInput create(
            @NonNull Context context,
            @NonNull BRouterProfileParameter parameter
    ) {
        switch (parameter.valueType) {
            case BOOLEAN:
                return new BooleanParameterInput(context);
            case NUMBER:
                return new TextParameterInput(context, true);
            case SELECTION:
                return new SelectionParameterInput(context, parameter.options);
            case STRING:
            default:
                return new TextParameterInput(context, false);
        }
    }

    @NonNull
    static LinearLayout.LayoutParams inputLayoutParams(@NonNull Context context) {
        return new LinearLayout.LayoutParams(dp(context, INPUT_WIDTH_DP), ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    static int dp(@NonNull Context context, int value) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}

final class BooleanParameterInput implements ProfileParameterValueInput {
    @NonNull
    private final Switch input;

    BooleanParameterInput(@NonNull Context context) {
        input = new Switch(context);
        input.setShowText(false);
        input.setLayoutParams(ProfileParameterValueInput.inputLayoutParams(context));
    }

    @NonNull
    @Override
    public View view() {
        return input;
    }

    @NonNull
    @Override
    public String value() {
        return input.isChecked() ? "1" : "0";
    }

    @Override
    public void setValue(@NonNull String value) {
        input.setChecked("1".equals(value) || "true".equalsIgnoreCase(value));
    }

    @Override
    public boolean validate(@NonNull Context context) {
        return true;
    }
}

final class TextParameterInput implements ProfileParameterValueInput {
    @NonNull
    private final EditText input;
    private final boolean number;

    TextParameterInput(@NonNull Context context, boolean number) {
        this.number = number;
        input = new EditText(context);
        input.setSingleLine(true);
        input.setSelectAllOnFocus(true);
        input.setLayoutParams(ProfileParameterValueInput.inputLayoutParams(context));
        input.setInputType(number ? numberInputType() : InputType.TYPE_CLASS_TEXT);
    }

    @NonNull
    @Override
    public View view() {
        return input;
    }

    @NonNull
    @Override
    public String value() {
        return input.getText().toString().trim();
    }

    @Override
    public void setValue(@NonNull String value) {
        input.setText(value);
        input.setError(null);
    }

    @Override
    public boolean validate(@NonNull Context context) {
        String value = value();
        if (number && !isNumber(value)) {
            return reject(context, R.string.msg_profile_parameter_invalid_number);
        }
        if (!number && (value.contains("&") || value.contains("="))) {
            return reject(context, R.string.msg_profile_parameter_invalid_text);
        }
        input.setError(null);
        return true;
    }

    private boolean reject(@NonNull Context context, int messageId) {
        input.setError(context.getString(messageId));
        input.requestFocus();
        return false;
    }

    private static int numberInputType() {
        return InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED;
    }

    private static boolean isNumber(@NonNull String value) {
        if (value.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

final class SelectionParameterInput implements ProfileParameterValueInput {
    @NonNull
    private final Spinner input;
    @NonNull
    private final List<BRouterProfileParameterOption> options;

    SelectionParameterInput(
            @NonNull Context context,
            @NonNull List<BRouterProfileParameterOption> options
    ) {
        this.options = options;
        input = new Spinner(context);
        input.setLayoutParams(ProfileParameterValueInput.inputLayoutParams(context));
        input.setAdapter(adapter(context, options));
    }

    @NonNull
    @Override
    public View view() {
        return input;
    }

    @NonNull
    @Override
    public String value() {
        int position = input.getSelectedItemPosition();
        if (position < 0 || position >= options.size()) {
            return "";
        }
        return options.get(position).value;
    }

    @Override
    public void setValue(@NonNull String value) {
        input.setSelection(Math.max(0, findOptionPosition(value)), false);
    }

    @Override
    public boolean validate(@NonNull Context context) {
        return !options.isEmpty();
    }

    private int findOptionPosition(@NonNull String value) {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).value.equals(value)) {
                return i;
            }
        }
        return 0;
    }

    @NonNull
    private static ArrayAdapter<String> adapter(
            @NonNull Context context,
            @NonNull List<BRouterProfileParameterOption> options
    ) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                labels(options)
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    @NonNull
    private static List<String> labels(@NonNull List<BRouterProfileParameterOption> options) {
        List<String> labels = new ArrayList<>();
        for (BRouterProfileParameterOption option : options) {
            labels.add(option.label);
        }
        return labels;
    }
}
