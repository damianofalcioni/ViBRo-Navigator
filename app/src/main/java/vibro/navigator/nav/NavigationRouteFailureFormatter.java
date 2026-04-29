package vibro.navigator.nav;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.brouter.BRouterRouteException;

final class NavigationRouteFailureFormatter {

    private NavigationRouteFailureFormatter() {
    }

    @NonNull
    static String format(@NonNull Context context, @NonNull Throwable throwable, boolean keepCurrentRoute) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof BRouterRouteException) {
                return formatBRouterFailure(context, (BRouterRouteException) current, keepCurrentRoute);
            }
            current = current.getCause();
        }

        String sanitized = firstMessage(throwable);
        if (!sanitized.isEmpty()) {
            return sanitized;
        }
        return context.getString(R.string.nav_route_unavailable_generic);
    }

    @NonNull
    private static String formatBRouterFailure(
            @NonNull Context context,
            @NonNull BRouterRouteException error,
            boolean keepCurrentRoute
    ) {
        switch (error.reason) {
            case NO_ROUTE_FOUND:
                return context.getString(keepCurrentRoute
                        ? R.string.nav_route_notice_no_alternative_keep_current
                        : R.string.nav_route_notice_no_route_found);
            case SERVICE_UNAVAILABLE:
                return context.getString(keepCurrentRoute
                        ? R.string.nav_route_notice_service_unavailable_keep_current
                        : R.string.nav_route_notice_service_unavailable);
            case INVALID_PROFILE:
                return context.getString(R.string.nav_route_notice_invalid_profile);
            case MALFORMED_RESPONSE:
            case UNKNOWN:
            default:
                return context.getString(keepCurrentRoute
                        ? R.string.nav_route_notice_update_failed_keep_current
                        : R.string.nav_route_notice_unavailable);
        }
    }

    @NonNull
    private static String firstMessage(@Nullable Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String sanitized = sanitizeMessage(current.getMessage());
            if (!sanitized.isEmpty()) {
                return truncateMessage(sanitized);
            }
            current = current.getCause();
        }
        return "";
    }

    @NonNull
    private static String sanitizeMessage(@Nullable String message) {
        if (message == null) {
            return "";
        }
        return message.replace('\r', ' ').replace('\n', ' ').trim();
    }

    @NonNull
    private static String truncateMessage(@NonNull String message) {
        return message.length() > 120 ? message.substring(0, 117) + "..." : message;
    }
}
