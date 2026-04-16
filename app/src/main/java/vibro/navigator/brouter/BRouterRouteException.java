package vibro.navigator.brouter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public final class BRouterRouteException extends Exception {

    public enum Reason {
        NO_ROUTE_FOUND,
        SERVICE_UNAVAILABLE,
        INVALID_PROFILE,
        MALFORMED_RESPONSE,
        UNKNOWN
    }

    @NonNull
    public final Reason reason;
    @NonNull
    public final String rawMessage;

    private BRouterRouteException(@NonNull Reason reason, @NonNull String rawMessage) {
        super(rawMessage);
        this.reason = reason;
        this.rawMessage = rawMessage;
    }

    @NonNull
    public static BRouterRouteException serviceUnavailable(@NonNull String rawMessage) {
        return new BRouterRouteException(Reason.SERVICE_UNAVAILABLE, sanitize(rawMessage));
    }

    @NonNull
    public static BRouterRouteException fromTextResponse(@Nullable String rawMessage) {
        String sanitized = sanitize(rawMessage);
        String normalized = sanitized.toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return new BRouterRouteException(Reason.MALFORMED_RESPONSE, "BRouter returned an empty route response");
        }
        if (normalized.startsWith("no track found")) {
            return new BRouterRouteException(Reason.NO_ROUTE_FOUND, sanitized);
        }
        if (normalized.contains("profile")
                && (normalized.contains("invalid")
                || normalized.contains("unknown")
                || normalized.contains("not found")
                || normalized.contains("missing"))) {
            return new BRouterRouteException(Reason.INVALID_PROFILE, sanitized);
        }
        return new BRouterRouteException(Reason.UNKNOWN, sanitized);
    }

    @NonNull
    private static String sanitize(@Nullable String rawMessage) {
        return rawMessage == null ? "" : rawMessage.replace('\r', ' ').replace('\n', ' ').trim();
    }
}
