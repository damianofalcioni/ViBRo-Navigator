package vibro.navigator.nav.routing;

import android.os.DeadObjectException;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import java.util.Locale;

import vibro.navigator.brouter.BRouterRouteException;

public final class BRouterTransientFailureClassifier {

    private BRouterTransientFailureClassifier() {
    }

    public static boolean isTransient(@NonNull Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (isTransientExceptionType(current)
                    || isTransientBRouterRouteException(current)
                    || hasTransientBRouterMessage(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean isTransientExceptionType(@NonNull Throwable error) {
        return error instanceof DeadObjectException || error instanceof RemoteException;
    }

    private static boolean isTransientBRouterRouteException(@NonNull Throwable error) {
        return error instanceof BRouterRouteException
                && ((BRouterRouteException) error).reason == BRouterRouteException.Reason.SERVICE_UNAVAILABLE;
    }

    private static boolean hasTransientBRouterMessage(@NonNull Throwable error) {
        String message = error.getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("brouter service not available")
                || normalized.contains("brouter is not connected")
                || normalized.contains("brouter binding died")
                || normalized.contains("null binding");
    }

}
