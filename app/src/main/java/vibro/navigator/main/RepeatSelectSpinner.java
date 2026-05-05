package vibro.navigator.main;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class RepeatSelectSpinner extends Spinner {

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
        boolean sameSelection = position == getSelectedItemPosition();
        super.setSelection(position);
        notifyIfSameSelection(position, sameSelection);
    }

    @Override
    public void setSelection(int position, boolean animate) {
        boolean sameSelection = position == getSelectedItemPosition();
        super.setSelection(position, animate);
        notifyIfSameSelection(position, sameSelection);
    }

    private void notifyIfSameSelection(int position, boolean sameSelection) {
        if (!sameSelection) {
            return;
        }
        AdapterView.OnItemSelectedListener listener = getOnItemSelectedListener();
        if (listener == null) {
            return;
        }
        View selectedView = getSelectedView();
        listener.onItemSelected(this, selectedView, position, getSelectedItemId());
    }
}

