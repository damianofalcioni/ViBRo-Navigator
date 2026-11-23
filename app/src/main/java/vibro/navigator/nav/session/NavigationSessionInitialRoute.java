package vibro.navigator.nav.session;

import androidx.annotation.Nullable;

import vibro.navigator.brouter.BRouterRouteException;

/** Keeps the caller waiting through GPS acquisition and transient route failures. */
public final class NavigationSessionInitialRoute {
    public interface Callback {
        void finished(boolean missingRoutingData);
    }

    @Nullable private Callback result;
    private boolean missingDataPending;

    public void replace(@Nullable Callback callback) {
        finish(false);
        result = callback;
    }

    public boolean detectMissingData(NavigationSession session) {
        if (result == null) { return false; }
        if (!session.isStarted() || session.hasActiveRoute()) {
            finish(false);
            return false;
        }
        if (missingDataPending) { return false; }
        missingDataPending = BRouterRouteException.isMissingRoutingData(
                session.components.routeRequestManager.getLastRouteFailure());
        return missingDataPending;
    }

    public void finish(boolean missingRoutingData) {
        Callback pending = result;
        result = null;
        missingDataPending = false;
        if (pending != null) {
            pending.finished(missingRoutingData);
        }
    }
}
