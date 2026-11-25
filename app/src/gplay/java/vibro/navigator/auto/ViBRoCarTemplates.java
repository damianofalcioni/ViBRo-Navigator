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
import vibro.navigator.android.language.AndroidAppLanguage;
import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.settings.AppSettings;

final class ViBRoCarTemplates {
    interface Actions {
        void onStopNavigation();

        default void onTogglePaused() { }
        default void onBlockedRoad() { }
    }

    @NonNull
    private final CarContext carContext;
    @NonNull
    private final Actions actions;
    private final ViBRoCarManeuvers.Icons maneuverIcons;
    private boolean lastPresentedImperial;
    @Nullable private ViBRoCarArrivalMinutes lastPresentedArrivals;

    ViBRoCarTemplates(@NonNull CarContext carContext, @NonNull Actions actions) {
        this.carContext = carContext;
        this.actions = actions;
        maneuverIcons = ViBRoCarManeuvers.Icons.from(carContext);
        lastPresentedImperial = AppSettings.isImperialUnitsEnabled(carContext);
    }

    @NonNull
    Template build(@Nullable NavState state) {
        lastPresentedImperial = AppSettings.isImperialUnitsEnabled(carContext);
        if (!usesNavigationTemplate(state)) {
            lastPresentedArrivals = null;
            return buildNoActiveNavigationTemplate();
        }
        lastPresentedArrivals = ViBRoCarArrivalMinutes.from(state);
        var estimate = state.routeStatus.progress.travelEstimate;
        return buildNavigationTemplate(state, estimate == null ? 0 : estimate.observationWallTimeMs);
    }

    boolean requiresInvalidation(@Nullable NavState previous, @Nullable NavState next) {
        boolean active = usesNavigationTemplate(next);
        if (usesNavigationTemplate(previous) != active) {
            return true;
        }
        if (!active) { return false; }
        // Compare the arrival clocks to the last published view so small updates can accumulate.
        return lastPresentedImperial != AppSettings.isImperialUnitsEnabled(carContext)
                || hasChangedArrivalMinutes(previous, next)
                || hasChangedRelativeProgress(previous, next);
    }

    private boolean hasChangedArrivalMinutes(NavState previous, NavState next) {
        var published = lastPresentedArrivals == null ? ViBRoCarArrivalMinutes.from(previous) : lastPresentedArrivals;
        return !published.equals(ViBRoCarArrivalMinutes.from(next));
    }

    private boolean hasChangedRelativeProgress(NavState previous, NavState next) {
        return !sameTemplateInputs(previous, next)
                && !buildNavigationTemplate(previous, 0).equals(buildNavigationTemplate(next, 0));
    }

    private static boolean sameTemplateInputs(NavState previous, NavState next) {
        return previous.routeStatus.guidance == next.routeStatus.guidance
                && previous.routeStatus.progress == next.routeStatus.progress
                && previous.pauseStatus.paused == next.pauseStatus.paused
                && previous.routeStatus.blockedRoadActionAvailable == next.routeStatus.blockedRoadActionAvailable;
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
    private Template buildNavigationTemplate(@NonNull NavState state, long observationTimeMs) {
        NavigationTemplate.Builder builder = new NavigationTemplate.Builder()
                .setActionStrip(buildNavigationActionStrip(state));
        ViBRoCarGuidance.apply(builder, state, new AndroidNavigationTextResources(carContext), observationTimeMs,
                maneuverIcons);
        if (carContext.getCarAppApiLevel() >= 2) {
            // Required by the host before it delivers scroll, pinch, or double-tap zoom.
            builder.setMapActionStrip(new ActionStrip.Builder().addAction(Action.PAN).build());
        }
        return builder.build();
    }

    @NonNull
    private ActionStrip buildNavigationActionStrip(@NonNull NavState state) {
        ActionStrip.Builder strip = new ActionStrip.Builder();
        if (state.routeStatus.blockedRoadActionAvailable && !state.pauseStatus.paused) {
            strip.addAction(iconAction(R.drawable.ic_blocked_road, actions::onBlockedRoad));
        }
        return strip.addAction(iconAction(R.drawable.ic_stop, actions::onStopNavigation))
                .addAction(iconAction(state.pauseStatus.paused ? R.drawable.ic_play : R.drawable.ic_pause,
                        actions::onTogglePaused))
                .build();
    }

    private Action iconAction(int drawable, Runnable action) {
        return new Action.Builder().setIcon(new CarIcon.Builder(
                androidx.core.graphics.drawable.IconCompat.createWithResource(carContext, drawable))
                .build())
                .setOnClickListener(action::run).build();
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
        return AndroidAppLanguage.getString(carContext, resId);
    }
}
