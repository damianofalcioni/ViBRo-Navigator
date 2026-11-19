package vibro.navigator.main;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.core.util.Consumer;

public final class RepeatSelectSpinner extends Spinner {
    private Consumer<Integer> onDropdownSelection;

    public RepeatSelectSpinner(@NonNull Context context) {
        super(context);
    }

    public RepeatSelectSpinner(@NonNull Context context, int mode) {
        super(context, mode);
    }

    public RepeatSelectSpinner(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RepeatSelectSpinner(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RepeatSelectSpinner(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr,
            int mode
    ) {
        super(context, attrs, defStyleAttr, mode);
    }

    @Override
    public void setSelection(int position) {
        // Spinner's dropdown calls this overload for item activation, even for the current row.
        super.setSelection(position);
        if (onDropdownSelection != null) {
            onDropdownSelection.accept(position);
        }
    }

    void onDropdownSelection(Consumer<Integer> listener) {
        onDropdownSelection = listener;
    }

    void renderSelection(int position) {
        super.setSelection(position);
    }
}

