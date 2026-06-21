package vibro.navigator.main;

import vibro.navigator.R;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

final class MainActivityRouteModeAdapter extends ArrayAdapter<String> {
    private static final float ENABLED_ALPHA = 1.0f;
    private static final float DISABLED_ALPHA = 0.38f;

    private final boolean brouterInstalled;

    MainActivityRouteModeAdapter(@NonNull Context context, boolean brouterInstalled) {
        super(
                context,
                R.layout.item_profile_spinner,
                android.R.id.text1,
                MainActivityRouteModeOption.labels(context)
        );
        this.brouterInstalled = brouterInstalled;
        setDropDownViewResource(R.layout.item_profile_spinner_bundled_dropdown);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return brouterInstalled;
    }

    @Override
    public boolean isEnabled(int position) {
        return MainActivityRouteModeOption.isEnabled(position, brouterInstalled);
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        renderAvailability(view, position);
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        renderAvailability(view, position);
        return view;
    }

    private void renderAvailability(@NonNull View view, int position) {
        boolean enabled = isEnabled(position);
        view.setEnabled(enabled);
        TextView text = view.findViewById(android.R.id.text1);
        if (text != null) {
            text.setEnabled(enabled);
            text.setAlpha(enabled ? ENABLED_ALPHA : DISABLED_ALPHA);
        }
    }
}
