package vibro.navigator.main;

import vibro.navigator.R;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class MainActivityRouteModeAdapter extends ArrayAdapter<String> {
    private static final float ENABLED_ALPHA = 1.0f;
    private static final float DISABLED_ALPHA = 0.38f;

    @NonNull
    private final Context context;
    @NonNull
    private final LayoutInflater inflater;
    private final boolean brouterInstalled;

    MainActivityRouteModeAdapter(@NonNull Context context, boolean brouterInstalled) {
        super(
                context,
                R.layout.item_profile_spinner,
                android.R.id.text1,
                MainActivityRouteModeOption.labels(context)
        );
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.brouterInstalled = brouterInstalled;
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
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        DropDownHolder holder;
        if (view == null || !(view.getTag() instanceof DropDownHolder)) {
            view = inflater.inflate(R.layout.item_profile_spinner_bundled_dropdown, parent, false);
            holder = new DropDownHolder(view);
            view.setTag(holder);
        } else {
            holder = (DropDownHolder) view.getTag();
        }
        bindDropdown(holder, position);
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

    private void bindDropdown(@NonNull DropDownHolder holder, int position) {
        holder.label.setText(MainActivityRouteModeOption.labelResAt(position));
        holder.attentionIcon.setVisibility(View.GONE);
        holder.attentionIcon.setContentDescription(null);
        holder.infoButton.setVisibility(View.VISIBLE);
        configurePassiveInfoButton(holder.infoButton);
        holder.infoButton.setContentDescription(context.getString(
                R.string.format_route_mode_info_content_description,
                context.getString(MainActivityRouteModeOption.labelResAt(position))
        ));
        holder.row.setOnTouchListener(new RouteModeInfoTouchListener(holder, position));
        renderDropdownAvailability(holder, position);
    }

    private void renderDropdownAvailability(@NonNull DropDownHolder holder, int position) {
        boolean enabled = isEnabled(position);
        holder.row.setEnabled(true);
        holder.label.setEnabled(enabled);
        holder.label.setAlpha(enabled ? ENABLED_ALPHA : DISABLED_ALPHA);
        holder.infoButton.setEnabled(true);
        holder.infoButton.setAlpha(ENABLED_ALPHA);
    }

    private static void configurePassiveInfoButton(@NonNull ImageButton infoButton) {
        infoButton.setOnClickListener(null);
        infoButton.setClickable(false);
        infoButton.setFocusable(false);
        infoButton.setFocusableInTouchMode(false);
    }

    private static boolean isInsideInfoButton(
            @NonNull DropDownHolder holder,
            @NonNull MotionEvent event
    ) {
        Rect bounds = new Rect();
        bounds.set(0, 0, holder.infoButton.getWidth(), holder.infoButton.getHeight());
        holder.row.offsetDescendantRectToMyCoords(holder.infoButton, bounds);
        return holder.infoButton.getVisibility() == View.VISIBLE
                && bounds.contains((int) event.getX(), (int) event.getY());
    }

    private void showInfoDialog(int position) {
        new AlertDialog.Builder(context)
                .setTitle(MainActivityRouteModeOption.labelResAt(position))
                .setMessage(MainActivityRouteModeOption.descriptionResAt(position))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private static final class DropDownHolder {
        final ViewGroup row;
        final TextView label;
        final ImageView attentionIcon;
        final ImageButton infoButton;
        boolean infoTouchActive;

        DropDownHolder(@NonNull View view) {
            row = (ViewGroup) view;
            label = view.findViewById(android.R.id.text1);
            attentionIcon = view.findViewById(R.id.profileAttentionIcon);
            infoButton = view.findViewById(R.id.profileInfoButton);
        }
    }

    private final class RouteModeInfoTouchListener implements View.OnTouchListener {
        @NonNull
        private final DropDownHolder holder;
        private final int position;

        private RouteModeInfoTouchListener(@NonNull DropDownHolder holder, int position) {
            this.holder = holder;
            this.position = position;
        }

        @Override
        public boolean onTouch(@NonNull View view, @Nullable MotionEvent event) {
            if (event == null) {
                return false;
            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    holder.infoTouchActive = isInsideInfoButton(holder, event);
                    return holder.infoTouchActive;
                case MotionEvent.ACTION_UP:
                    if (!holder.infoTouchActive) {
                        return false;
                    }
                    if (isInsideInfoButton(holder, event)) {
                        showInfoDialog(position);
                        view.performClick();
                    }
                    holder.infoTouchActive = false;
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    return clearInfoTouch();
                default:
                    return holder.infoTouchActive;
            }
        }

        private boolean clearInfoTouch() {
            boolean wasActive = holder.infoTouchActive;
            holder.infoTouchActive = false;
            return wasActive;
        }
    }
}
