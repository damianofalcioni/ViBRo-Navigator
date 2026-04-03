package com.vibenavigator.nav;

import androidx.annotation.NonNull;

public final class NavigationLifecyclePolicy {

    public enum BackPressAction {
        MOVE_TASK_TO_BACKGROUND
    }

    public enum ForegroundAction {
        NONE,
        PROMOTE_TO_FOREGROUND,
        STOP_NAVIGATION
    }

    public enum TaskRemovedAction {
        STOP_NAVIGATION
    }

    @NonNull
    public BackPressAction onNavigationBackPressed() {
        return BackPressAction.MOVE_TASK_TO_BACKGROUND;
    }

    @NonNull
    public ForegroundAction onNavigationUiConnected(boolean foregroundNotificationVisible) {
        return ForegroundAction.PROMOTE_TO_FOREGROUND;
    }

    @NonNull
    public ForegroundAction onForegroundNotificationCheck(boolean foregroundNotificationVisible) {
        return foregroundNotificationVisible ? ForegroundAction.NONE : ForegroundAction.STOP_NAVIGATION;
    }

    @NonNull
    public TaskRemovedAction onTaskRemoved() {
        return TaskRemovedAction.STOP_NAVIGATION;
    }
}
