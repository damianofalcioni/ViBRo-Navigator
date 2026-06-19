package vibro.navigator.android.theme;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

public final class AndroidThemedButtons {
    private AndroidThemedButtons() {
    }

    public static void applyIconButtonBackground(@NonNull ImageButton button) {
        applyBackground(button);
    }

    public static void applySecondaryButtonBackground(@NonNull TextView button) {
        applyBackground(button);
    }

    private static void applyBackground(@NonNull View view) {
        view.setBackgroundResource(borderlessSelectableBackground(view.getContext()));
        view.setBackgroundTintList(null);
    }

    private static int borderlessSelectableBackground(@NonNull Context context) {
        TypedValue value = new TypedValue();
        if (!context.getTheme().resolveAttribute(
                android.R.attr.selectableItemBackgroundBorderless,
                value,
                true
        ) || value.resourceId == 0) {
            throw new IllegalStateException("Borderless selectable background is unavailable");
        }
        return value.resourceId;
    }
}
