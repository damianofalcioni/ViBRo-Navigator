package vibro.navigator.nav.foreground;

public interface NavigationScreenInteractivityMonitor {

    interface Listener {
        void onScreenInteractiveChanged(boolean interactive);
    }

    boolean start();

    void stop();
}
