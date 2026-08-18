package vibro.navigator.auto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.NavigationTemplate;

import vibro.navigator.R;
import vibro.navigator.nav.model.NavState;

final class ViBRoCarTemplates {
    interface Actions {
        void onOpenPhoneApp();
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
        if (!usesNavigationTemplate(state)) {
            return buildNoActiveNavigationTemplate();
        }
        return buildNavigationTemplate();
    }

    boolean requiresInvalidation(@Nullable NavState previous, @Nullable NavState next) {
        return usesNavigationTemplate(previous) != usesNavigationTemplate(next);
    }

    @NonNull
    private Template buildNoActiveNavigationTemplate() {
        Pane pane = new Pane.Builder()
                .addRow(new Row.Builder()
                        .setTitle(text(R.string.auto_no_active_navigation_title))
                        .addText(text(R.string.auto_no_active_navigation_text))
                        .build())
                .build();
        return buildPaneTemplate(text(R.string.auto_title), pane);
    }

    @NonNull
    private Template buildNavigationTemplate() {
        return new NavigationTemplate.Builder()
                .setActionStrip(buildNavigationActionStrip())
                .build();
    }

    @NonNull
    private ActionStrip buildNavigationActionStrip() {
        // NavigationTemplate requires a strip action; the surface-drawn controls stay primary.
        return new ActionStrip.Builder()
                .addAction(new Action.Builder()
                        .setIcon(CarIcon.APP_ICON)
                        .setOnClickListener(actions::onOpenPhoneApp)
                        .build())
                .build();
    }

    @NonNull
    @SuppressWarnings("deprecation")
    private Template buildPaneTemplate(@NonNull String title, @NonNull Pane pane) {
        return new PaneTemplate.Builder(pane)
                .setTitle(title)
                .setHeaderAction(Action.APP_ICON)
                .build();
    }

    private boolean isNoActiveNavigation(@NonNull NavState state) {
        return text(R.string.nav_no_route).equals(state.routeStatus.guidance.nextLine.trim())
                && state.routeStatus.progress.destinationLine.trim().isEmpty()
                && state.routeStatus.progress.stopProgressBlock.trim().isEmpty()
                && state.routeStatus.progress.detailBlock.trim().isEmpty();
    }

    private boolean usesNavigationTemplate(@Nullable NavState state) {
        return state != null && !isNoActiveNavigation(state);
    }

    @NonNull
    private String text(int resId) {
        return carContext.getString(resId);
    }
}
