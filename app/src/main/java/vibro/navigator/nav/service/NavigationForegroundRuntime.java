package vibro.navigator.nav.service;

import androidx.annotation.NonNull;

import vibro.navigator.android.foreground.AndroidNavigationForegroundController;
import vibro.navigator.android.foreground.AndroidScreenInteractivityMonitor;
import vibro.navigator.nav.foreground.NavigationForegroundController;
import vibro.navigator.nav.foreground.NavigationScreenInteractivityMonitor;

final class NavigationForegroundRuntime {
    @NonNull
    final NavigationForegroundController controller;
    @NonNull
    final NavigationScreenInteractivityMonitor screenInteractivityMonitor;
    final boolean screenInteractive;

    private NavigationForegroundRuntime(
            @NonNull NavigationForegroundController controller,
            @NonNull NavigationScreenInteractivityMonitor screenInteractivityMonitor,
            boolean screenInteractive
    ) {
        this.controller = controller;
        this.screenInteractivityMonitor = screenInteractivityMonitor;
        this.screenInteractive = screenInteractive;
    }

    @NonNull
    static NavigationForegroundRuntime create(
            @NonNull NavigationService service,
            @NonNull NavigationServiceUiVisibility uiVisibility
    ) {
        NavigationForegroundController controller = new AndroidNavigationForegroundController(service);
        NavigationScreenInteractivityMonitor screenInteractivityMonitor =
                new AndroidScreenInteractivityMonitor(service, uiVisibility::onScreenInteractiveChanged);
        controller.ensureChannels();
        return new NavigationForegroundRuntime(
                controller,
                screenInteractivityMonitor,
                screenInteractivityMonitor.start()
        );
    }
}
