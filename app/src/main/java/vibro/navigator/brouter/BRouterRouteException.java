package vibro.navigator.brouter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public final class BRouterRouteException extends Exception {

    public enum Reason {
        NO_ROUTE_FOUND,
        SERVICE_UNAVAILABLE,
        INVALID_PROFILE,
        MISSING_ROUTING_DATA,
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

    private BRouterRouteException(
            @NonNull Reason reason,
            @NonNull String rawMessage,
            @NonNull Throwable cause
    ) {
        super(rawMessage, cause);
        this.reason = reason;
        this.rawMessage = rawMessage;
    }

    @NonNull
    public static BRouterRouteException serviceUnavailable(@NonNull String rawMessage) {
        return new BRouterRouteException(Reason.SERVICE_UNAVAILABLE, sanitize(rawMessage));
    }

    @NonNull
    public static BRouterRouteException serviceUnavailable(
            @NonNull String rawMessage,
            @NonNull Throwable cause
    ) {
        return new BRouterRouteException(Reason.SERVICE_UNAVAILABLE, sanitize(rawMessage), cause);
    }

    @NonNull
    public static BRouterRouteException fromTextResponse(@Nullable String rawMessage) {
        String sanitized = sanitize(rawMessage);
        String normalized = sanitized.toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return new BRouterRouteException(Reason.MALFORMED_RESPONSE, "BRouter returned an empty route response");
        }
        if (isMissingRoutingDataResponse(normalized)) {
            return new BRouterRouteException(Reason.MISSING_ROUTING_DATA, sanitized);
        }
        if (isNoRouteResponse(normalized)) {
            return new BRouterRouteException(Reason.NO_ROUTE_FOUND, sanitized);
        }
        if (isInvalidProfileResponse(normalized)) {
            return new BRouterRouteException(Reason.INVALID_PROFILE, sanitized);
        }
        return new BRouterRouteException(Reason.UNKNOWN, sanitized);
    }

    public static boolean isMissingRoutingData(@Nullable Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof BRouterRouteException) {
                return ((BRouterRouteException) cause).reason == Reason.MISSING_ROUTING_DATA;
            }
        }
        return false;
    }

    private static boolean isMissingRoutingDataResponse(String message) {
        return (message.contains("datafile ") && message.contains(".rd5 not found"))
                || (message.contains("segment directory ") && message.endsWith(" does not exist"))
                || message.contains("no routing data files");
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
                || normalizedMessage.contains("does not exist")
                || normalizedMessage.contains("does not exists")
                || normalizedMessage.contains("not found")
                || normalizedMessage.contains("missing");
    }

    @NonNull
    private static String sanitize(@Nullable String rawMessage) {
        return rawMessage == null ? "" : rawMessage.replace('\r', ' ').replace('\n', ' ').trim();
    }
}
