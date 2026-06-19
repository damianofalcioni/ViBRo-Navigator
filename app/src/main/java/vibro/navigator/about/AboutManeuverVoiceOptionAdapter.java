package vibro.navigator.about;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.List;

import vibro.navigator.R;
import vibro.navigator.android.theme.AndroidAppTheme;
import vibro.navigator.nav.voice.NavigationVoiceOption;

final class AboutManeuverVoiceOptionAdapter
        extends ArrayAdapter<NavigationVoiceOption> {
    @NonNull
    private final Context context;
    @NonNull
    private String selectedVoiceName = "";

    AboutManeuverVoiceOptionAdapter(
            @NonNull Context context,
            @NonNull List<NavigationVoiceOption> options
    ) {
        super(context, R.layout.item_profile_spinner, options);
        this.context = context;
        setDropDownViewResource(R.layout.item_profile_spinner_dropdown);
    }

    void setSelectedVoiceName(@NonNull String selectedVoiceName) {
        this.selectedVoiceName = selectedVoiceName;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        view.setActivated(false);
        view.setSelected(false);
        applyTextStyle(view, false);
        return view;
    }

    @NonNull
    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        boolean selected = isSelectedVoice(getItem(position));
        view.setBackgroundResource(R.drawable.bg_spinner_dropdown_item);
        view.setActivated(selected);
        view.setSelected(selected);
        applyTextStyle(view, selected);
        return view;
    }

    private boolean isSelectedVoice(@Nullable NavigationVoiceOption option) {
        return option != null && option.voiceName.equals(selectedVoiceName);
    }

    private void applyTextStyle(@NonNull View view, boolean selected) {
        if (!(view instanceof TextView)) {
            return;
        }
        TextView textView = (TextView) view;
        textView.setTextColor(selected
                ? ContextCompat.getColor(context, R.color.success)
                : AndroidAppTheme.color(context, R.attr.vibroTextPrimaryColor));
        textView.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
    }
}
