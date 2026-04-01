package com.vibenavigator.poi.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vibenavigator.R;

import java.util.ArrayList;
import java.util.List;

public final class PoiSuggestionAdapter extends BaseAdapter {

    public interface Listener {
        void onDeleteClicked(@NonNull PoiSuggestion suggestion);
    }

    private final LayoutInflater inflater;
    private final Listener listener;
    private final List<PoiSuggestion> items = new ArrayList<>();

    public PoiSuggestionAdapter(@NonNull Context context, @NonNull Listener listener) {
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
        TextView text = v.findViewById(R.id.suggestionText);
        ImageButton del = v.findViewById(R.id.deleteSuggestionButton);

        String label = v.getContext().getString(R.string.format_poi_suggestion, s.poi.name, s.poi.lat, s.poi.lon);
        text.setText(label);

        if (s.deletable) {
            del.setVisibility(View.VISIBLE);
            del.setOnClickListener(btn -> listener.onDeleteClicked(s));
        } else {
            del.setVisibility(View.GONE);
            del.setOnClickListener(null);
        }

        return v;
    }
}
