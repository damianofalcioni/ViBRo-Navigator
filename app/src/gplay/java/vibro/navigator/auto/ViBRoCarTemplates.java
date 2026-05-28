package vibro.navigator.auto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Header;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.core.graphics.drawable.IconCompat;

import vibro.navigator.R;
import vibro.navigator.nav.model.NavState;

final class ViBRoCarTemplates {
    interface Actions {
        void onOpenPhoneApp();

        void onBlockedRoad();

        void onStopNavigation();

        void onTogglePaused();

        void onExportRoute();
    }

    @NonNull
    private final CarContext carContext;
    @NonNull
    private final Actions actions;

    ViBRoCarTemplates(@NonNull CarContext carContext, @NonNull Actions actions) {
        this.carContext = carContext;
        this.actions = actions;
    }

    @NonNull
    Template build(@Nullable NavState state) {
        if (state == null) {
            return buildConnectingTemplate();
        }
        if (isNoActiveNavigation(state)) {
            return buildNoActiveNavigationTemplate();
        }
        return buildNavigationTemplate(state);
    }

    @NonNull
    private Template buildConnectingTemplate() {
        Pane pane = new Pane.Builder()
                .addRow(new Row.Builder()
                        .setTitle(text(R.string.auto_title))
                        .addText(text(R.string.auto_connecting))
                        .build())
                .build();
        return buildPaneTemplate(text(R.string.auto_title), pane);
    }

    @NonNull
    private Template buildNoActiveNavigationTemplate() {
        Pane.Builder pane = new Pane.Builder()
                .addRow(new Row.Builder()
                        .setTitle(text(R.string.auto_no_active_navigation_title))
                        .addText(text(R.string.auto_no_active_navigation_text))
                        .build());
        pane.addAction(new Action.Builder()
                .setTitle(text(R.string.auto_open_phone))
                .setOnClickListener(actions::onOpenPhoneApp)
                .build());
        return buildPaneTemplate(text(R.string.auto_title), pane.build());
    }

    @NonNull
    private Template buildNavigationTemplate(@NonNull NavState state) {
        return new NavigationTemplate.Builder()
                .setActionStrip(buildNavigationActionStrip(state))
                .build();
    }

    @NonNull
    private ActionStrip buildNavigationActionStrip(@NonNull NavState state) {
        return new ActionStrip.Builder()
                .addAction(buildIconAction(
                        R.string.action_blocked_road,
                        R.drawable.ic_blocked_road,
                        actions::onBlockedRoad,
                        !state.pauseStatus.paused
                ))
                .addAction(buildIconAction(
                        R.string.action_stop_navigation,
                        R.drawable.ic_stop,
                        actions::onStopNavigation,
                        true
                ))
                .addAction(buildIconAction(
                        pauseResumeTitle(state),
                        state.pauseStatus.paused ? R.drawable.ic_play : R.drawable.ic_pause,
                        actions::onTogglePaused,
                        true
                ))
                .addAction(buildIconAction(
                        R.string.action_export_route,
                        R.drawable.ic_export,
                        actions::onExportRoute,
                        true
                ))
                .build();
    }

    @NonNull
    private Action buildIconAction(int titleResId, int iconResId, @NonNull Runnable listener, boolean enabled) {
        return new Action.Builder()
                .setTitle(text(titleResId))
                .setIcon(new CarIcon.Builder(IconCompat.createWithResource(carContext, iconResId)).build())
                .setOnClickListener(listener::run)
                .setEnabled(enabled)
                .build();
    }

    @NonNull
    private Template buildPaneTemplate(@NonNull String title, @NonNull Pane pane) {
        return new PaneTemplate.Builder(pane)
                .setHeader(new Header.Builder()
                        .setTitle(title)
                        .setStartHeaderAction(Action.APP_ICON)
                        .build())
                .build();
    }

    private boolean isNoActiveNavigation(@NonNull NavState state) {
        return text(R.string.nav_no_route).equals(state.routeStatus.guidance.nextLine.trim())
                && state.routeStatus.progress.destinationLine.trim().isEmpty()
                && state.routeStatus.progress.stopProgressBlock.trim().isEmpty()
                && state.routeStatus.progress.detailBlock.trim().isEmpty();
    }

    private int pauseResumeTitle(@NonNull NavState state) {
        return state.pauseStatus.paused
                ? R.string.action_resume_navigation
                : R.string.action_pause_navigation;
    }

    @NonNull
    private String text(int resId) {
        return carContext.getString(resId);
    }
}
