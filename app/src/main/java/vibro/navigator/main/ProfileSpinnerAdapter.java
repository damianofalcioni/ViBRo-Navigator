package vibro.navigator.main;

import vibro.navigator.R;

import android.app.AlertDialog;
import android.content.Context;
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

import java.util.ArrayList;

final class ProfileSpinnerAdapter extends ArrayAdapter<ProfileSpinnerOption> {

    @NonNull
    private final Context context;
    @NonNull
    private final LayoutInflater inflater;

    ProfileSpinnerAdapter(@NonNull Context context) {
        super(context, R.layout.item_profile_spinner, new ArrayList<>());
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
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
        bindDropdown(holder, getItem(position));
        return view;
    }

    private void bindDropdown(@NonNull DropDownHolder holder, @Nullable ProfileSpinnerOption option) {
        holder.label.setText(option == null ? "" : option.toString());
        BundledProfileInfo info = profileInfo(option);
        if (info == null) {
            hideProfileActions(holder);
            return;
        }
        bindProfileActions(holder, info);
    }

    @Nullable
    private static BundledProfileInfo profileInfo(@Nullable ProfileSpinnerOption option) {
        if (option == null || option.isCustom()) {
            return null;
        }
        return BundledProfileInfo.forProfile(option.profileName());
    }

    private static void hideProfileActions(@NonNull DropDownHolder holder) {
        holder.attentionIcon.setVisibility(View.GONE);
        holder.attentionIcon.setContentDescription(null);
        holder.infoButton.setVisibility(View.GONE);
        configurePassiveInfoButton(holder.infoButton);
        holder.infoButton.setContentDescription(null);
        holder.row.setOnTouchListener(null);
    }

    private void bindProfileActions(@NonNull DropDownHolder holder, @NonNull BundledProfileInfo info) {
        holder.attentionIcon.setVisibility(info.hasAttentionIcon() ? View.VISIBLE : View.GONE);
        holder.attentionIcon.setContentDescription(context.getString(R.string.label_profile_attention_icon));
        holder.infoButton.setVisibility(View.VISIBLE);
        configurePassiveInfoButton(holder.infoButton);
        holder.infoButton.setContentDescription(context.getString(
                R.string.format_profile_info_content_description,
                context.getString(info.titleRes())
        ));
        holder.row.setOnTouchListener((view, event) -> handleRowTouch(holder, info, event));
    }

    private static void configurePassiveInfoButton(@NonNull ImageButton infoButton) {
        infoButton.setOnClickListener(null);
        infoButton.setClickable(false);
        infoButton.setFocusable(false);
        infoButton.setFocusableInTouchMode(false);
    }

    private boolean handleRowTouch(
            @NonNull DropDownHolder holder,
            @NonNull BundledProfileInfo info,
            @Nullable MotionEvent event
    ) {
        if (event == null) {
            return false;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            holder.infoTouchActive = isInsideInfoButton(holder, event);
            return holder.infoTouchActive;
        }
        if (!holder.infoTouchActive) {
            return false;
        }
        return finishInfoTouch(holder, info, event);
    }

    private boolean finishInfoTouch(
            @NonNull DropDownHolder holder,
            @NonNull BundledProfileInfo info,
            @NonNull MotionEvent event
    ) {
        if (event.getAction() == MotionEvent.ACTION_UP && isInsideInfoButton(holder, event)) {
            showInfoDialog(info);
        }
        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            holder.infoTouchActive = false;
        }
        return true;
    }

    private static boolean isInsideInfoButton(
            @NonNull DropDownHolder holder,
            @NonNull MotionEvent event
    ) {
        return holder.infoButton.getVisibility() == View.VISIBLE
                && event.getX() >= holder.infoButton.getLeft()
                && event.getX() <= holder.infoButton.getRight()
                && event.getY() >= holder.infoButton.getTop()
                && event.getY() <= holder.infoButton.getBottom();
    }

    private void showInfoDialog(@NonNull BundledProfileInfo info) {
        new AlertDialog.Builder(context)
                .setTitle(info.titleRes())
                .setMessage(info.descriptionRes())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private static final class DropDownHolder {
        final View row;
        final TextView label;
        final ImageView attentionIcon;
        final ImageButton infoButton;
        boolean infoTouchActive;

        DropDownHolder(@NonNull View view) {
            row = view;
            label = view.findViewById(android.R.id.text1);
            attentionIcon = view.findViewById(R.id.profileAttentionIcon);
            infoButton = view.findViewById(R.id.profileInfoButton);
        }
    }
}
