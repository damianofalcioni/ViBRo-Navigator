package vibro.navigator.nav.service;

import androidx.annotation.NonNull;

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
        NavigationForegroundController controller = new NavigationForegroundController(service);
        NavigationScreenInteractivityMonitor screenInteractivityMonitor =
                new NavigationScreenInteractivityMonitor(service, uiVisibility::onScreenInteractiveChanged);
        controller.ensureChannels();
        return new NavigationForegroundRuntime(
                controller,
                screenInteractivityMonitor,
                screenInteractivityMonitor.start()
        );
    }
}
