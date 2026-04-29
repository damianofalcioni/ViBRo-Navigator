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
        if (isNoRouteResponse(normalized)) {
            return new BRouterRouteException(Reason.NO_ROUTE_FOUND, sanitized);
        }
        if (isInvalidProfileResponse(normalized)) {
            return new BRouterRouteException(Reason.INVALID_PROFILE, sanitized);
        }
        return new BRouterRouteException(Reason.UNKNOWN, sanitized);
    }

    private static boolean isNoRouteResponse(@NonNull String normalizedMessage) {
        return normalizedMessage.startsWith("no track found");
    }

    private static boolean isInvalidProfileResponse(@NonNull String normalizedMessage) {
        return normalizedMessage.contains("profile")
                && hasInvalidProfileMarker(normalizedMessage);
    }

    private static boolean hasInvalidProfileMarker(@NonNull String normalizedMessage) {
        return normalizedMessage.contains("invalid")
                || normalizedMessage.contains("unknown")
                || normalizedMessage.contains("not found")
                || normalizedMessage.contains("missing");
    }

    @NonNull
    private static String sanitize(@Nullable String rawMessage) {
        return rawMessage == null ? "" : rawMessage.replace('\r', ' ').replace('\n', ' ').trim();
    }
}
