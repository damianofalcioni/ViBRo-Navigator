package vibro.navigator.poi.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;

import java.util.ArrayList;
import java.util.List;

public final class PoiSuggestionAdapter extends BaseAdapter {

    public interface Listener {
        void onSuggestionClicked(@NonNull PoiSuggestion suggestion);
        void onInfoClicked(@NonNull PoiSuggestion suggestion);
        void onEditClicked(@NonNull PoiSuggestion suggestion);
        void onDeleteClicked(@NonNull PoiSuggestion suggestion);
    }

    private final Context context;
    private final LayoutInflater inflater;
    private final Listener listener;
    private final List<PoiSuggestion> items = new ArrayList<>();

    public PoiSuggestionAdapter(@NonNull Context context, @NonNull Listener listener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    public void setItems(@Nullable List<PoiSuggestion> next) {
        items.clear();
        if (next != null) {
            items.addAll(next);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = inflater.inflate(R.layout.item_poi_suggestion, parent, false);
        }

        PoiSuggestion s = items.get(position);
        ImageButton info = v.findViewById(R.id.poiSuggestionInfoButton);
        TextView text = v.findViewById(R.id.suggestionText);
        ImageButton edit = v.findViewById(R.id.editSuggestionButton);
        ImageButton del = v.findViewById(R.id.deleteSuggestionButton);

        String label = s.displayLabel(context);
        text.setText(label);
        v.setOnClickListener(row -> listener.onSuggestionClicked(s));
        bindInfoButton(info, s, label);

        if (s.deletable) {
            edit.setVisibility(View.VISIBLE);
            edit.setOnClickListener(btn -> listener.onEditClicked(s));
            del.setVisibility(View.VISIBLE);
            del.setOnClickListener(btn -> listener.onDeleteClicked(s));
        } else {
            edit.setVisibility(View.GONE);
            edit.setOnClickListener(null);
            del.setVisibility(View.GONE);
            del.setOnClickListener(null);
        }

        return v;
    }

    private void bindInfoButton(
            @NonNull ImageButton info,
            @NonNull PoiSuggestion suggestion,
            @NonNull String label
    ) {
        if (!suggestion.hasDetails()) {
            info.setVisibility(View.GONE);
            info.setOnClickListener(null);
            info.setContentDescription(null);
            return;
        }

        info.setVisibility(View.VISIBLE);
        info.setContentDescription(context.getString(
                R.string.format_poi_details_content_description,
                label
        ));
        info.setOnClickListener(btn -> listener.onInfoClicked(suggestion));
    }
}
