package vibro.navigator.poi.ui;

import android.view.View;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class PoiInputClearButtonController {

    private PoiInputClearButtonController() {
    }

    public static void bind(@NonNull EditText editText, @NonNull View clearButton) {
        clearButton.setOnClickListener(v -> clearText(editText));
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateVisibility(clearButton, s);
            }
        });
        updateVisibility(clearButton, editText.getText());
    }

    private static void clearText(@NonNull EditText editText) {
        editText.requestFocus();
        editText.setText("");
        editText.setSelection(0);
    }

    private static void updateVisibility(@NonNull View clearButton, @Nullable CharSequence text) {
        clearButton.setVisibility(text == null || text.length() == 0 ? View.GONE : View.VISIBLE);
    }
}
