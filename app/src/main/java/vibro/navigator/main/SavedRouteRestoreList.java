package vibro.navigator.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.R;

final class SavedRouteRestoreList {
    @NonNull
    private final Activity activity;
    @NonNull
    private final SavedRouteStore routeStore;
    @NonNull
    private final LinearLayout content;
    @NonNull
    private final LinearLayout routeList;
    @NonNull
    private final TextView emptyText;
    @NonNull
    private final List<Row> rows = new ArrayList<>();

    @Nullable
    private String selectedRouteId;

    SavedRouteRestoreList(
            @NonNull Activity activity,
            @NonNull SavedRouteStore routeStore,
            @NonNull List<SavedRoute> routes
    ) {
        this.activity = activity;
        this.routeStore = routeStore;
        content = SavedRouteDialogViews.paddedContent(activity);
        routeList = createRouteList();
        emptyText = createEmptyText();
        content.addView(createRouteScrollView());
        populateRows(routes);
    }

    @NonNull
    View view() {
        return content;
    }

    @Nullable
    SavedRoute selectedRoute() {
        for (Row row : rows) {
            if (row.route.id.equals(selectedRouteId)) {
                return row.route;
            }
        }
        return null;
    }

    private void populateRows(@NonNull List<SavedRoute> routes) {
        for (SavedRoute route : routes) {
            addRouteRow(route);
        }
        selectFirstAvailable();
        updateEmptyState();
    }

    @NonNull
    private LinearLayout createRouteList() {
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        return list;
    }

    @NonNull
    private View createRouteScrollView() {
        ScrollView scrollView = new MaxHeightScrollView(activity, SavedRouteDialogViews.dp(activity, 360));
        scrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        scrollView.addView(routeList, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));
        return scrollView;
    }

    @NonNull
    private TextView createEmptyText() {
        TextView textView = new TextView(activity);
        textView.setText(R.string.msg_saved_routes_empty);
        textView.setTextColor(Color.WHITE);
        textView.setGravity(Gravity.CENTER);
        textView.setMinHeight(SavedRouteDialogViews.dp(activity, 64));
        return textView;
    }

    private void addRouteRow(@NonNull SavedRoute route) {
        Row row = createRow(route);
        rows.add(row);
        routeList.addView(row.view);
    }

    @NonNull
    private Row createRow(@NonNull SavedRoute route) {
        LinearLayout rowView = createRowView();
        RadioButton radioButton = new RadioButton(activity);
        TextView title = createRouteTitle(route.name);
        ImageButton editButton = SavedRouteDialogViews.iconButton(activity, R.drawable.ic_edit, R.string.action_edit);
        ImageButton deleteButton = SavedRouteDialogViews.iconButton(
                activity,
                R.drawable.ic_delete,
                R.string.action_delete
        );
        Row row = new Row(route, rowView, radioButton, title);
        rowView.setOnClickListener(v -> select(row));
        radioButton.setOnClickListener(v -> select(row));
        editButton.setOnClickListener(v -> promptRename(row));
        deleteButton.setOnClickListener(v -> remove(row));
        addRowViews(rowView, radioButton, title, editButton, deleteButton);
        return row;
    }

    @NonNull
    private LinearLayout createRowView() {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, SavedRouteDialogViews.dp(activity, 4), 0, SavedRouteDialogViews.dp(activity, 4));
        return row;
    }

    @NonNull
    private TextView createRouteTitle(@NonNull String title) {
        TextView textView = new TextView(activity);
        textView.setText(title);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(15f);
        return textView;
    }

    private void addRowViews(
            @NonNull LinearLayout rowView,
            @NonNull RadioButton radioButton,
            @NonNull TextView title,
            @NonNull ImageButton editButton,
            @NonNull ImageButton deleteButton
    ) {
        rowView.addView(radioButton, new LinearLayout.LayoutParams(
                SavedRouteDialogViews.dp(activity, 44),
                SavedRouteDialogViews.dp(activity, 44)
        ));
        rowView.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        rowView.addView(editButton, new LinearLayout.LayoutParams(
                SavedRouteDialogViews.dp(activity, 40),
                SavedRouteDialogViews.dp(activity, 40)
        ));
        rowView.addView(deleteButton, new LinearLayout.LayoutParams(
                SavedRouteDialogViews.dp(activity, 40),
                SavedRouteDialogViews.dp(activity, 40)
        ));
    }

    private void select(@NonNull Row selectedRow) {
        selectedRouteId = selectedRow.route.id;
        for (Row row : rows) {
            row.radioButton.setChecked(row.equals(selectedRow));
        }
    }

    private void selectFirstAvailable() {
        if (!rows.isEmpty()) {
            select(rows.get(0));
        }
    }

    private void promptRename(@NonNull Row row) {
        EditText input = SavedRouteDialogViews.routeNameEditText(activity, row.route.name);
        LinearLayout dialogContent = SavedRouteDialogViews.paddedContent(activity);
        dialogContent.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.title_edit_route_name)
                .setView(dialogContent)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_save_route, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> saveRename(dialog, input, row)));
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        input.requestFocus();
    }

    private void saveRename(
            @NonNull AlertDialog dialog,
            @NonNull EditText input,
            @NonNull Row row
    ) {
        String updatedName = input.getText().toString().trim();
        if (updatedName.isEmpty()) {
            input.setError(activity.getString(R.string.msg_invalid_route_name));
            return;
        }
        if (routeStore.rename(row.route.id, updatedName)) {
            row.route = row.route.renamed(updatedName);
            row.title.setText(updatedName);
        }
        dialog.dismiss();
    }

    private void remove(@NonNull Row row) {
        routeStore.remove(row.route.id);
        rows.remove(row);
        routeList.removeView(row.view);
        if (row.route.id.equals(selectedRouteId)) {
            selectedRouteId = null;
            selectFirstAvailable();
        }
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (rows.isEmpty() && emptyText.getParent() == null) {
            routeList.addView(emptyText);
        } else if (!rows.isEmpty() && emptyText.getParent() != null) {
            routeList.removeView(emptyText);
        }
    }

    private static final class Row {
        @NonNull
        private SavedRoute route;
        @NonNull
        private final View view;
        @NonNull
        private final RadioButton radioButton;
        @NonNull
        private final TextView title;

        private Row(
                @NonNull SavedRoute route,
                @NonNull View view,
                @NonNull RadioButton radioButton,
                @NonNull TextView title
        ) {
            this.route = route;
            this.view = view;
            this.radioButton = radioButton;
            this.title = title;
        }
    }

    private static final class MaxHeightScrollView extends ScrollView {
        private final int maxHeight;

        private MaxHeightScrollView(@NonNull Activity activity, int maxHeight) {
            super(activity);
            this.maxHeight = maxHeight;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int cappedHeight = View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST);
            super.onMeasure(widthMeasureSpec, cappedHeight);
        }
    }
}
