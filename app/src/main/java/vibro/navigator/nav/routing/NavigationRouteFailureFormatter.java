package vibro.navigator.nav.routing;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.brouter.BRouterRouteException;
import vibro.navigator.nav.format.AndroidNavigationTextResources;
import vibro.navigator.nav.format.NavigationTextResources;

public final class NavigationRouteFailureFormatter {

    private NavigationRouteFailureFormatter() {
    }

    @NonNull
    public static String format(@NonNull Context context, @NonNull Throwable throwable, boolean keepCurrentRoute) {
        return format(new AndroidNavigationTextResources(context), throwable, keepCurrentRoute);
    }

    @NonNull
    public static String format(
            @NonNull NavigationTextResources textResources,
            @NonNull Throwable throwable,
            boolean keepCurrentRoute
    ) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof BRouterRouteException) {
                return formatBRouterFailure(textResources, (BRouterRouteException) current, keepCurrentRoute);
            }
            current = current.getCause();
        }

        String sanitized = firstMessage(throwable);
        if (!sanitized.isEmpty()) {
            return sanitized;
        }
        return textResources.getString(R.string.nav_route_unavailable_generic);
    }

    @NonNull
    private static String formatBRouterFailure(
            @NonNull NavigationTextResources textResources,
            @NonNull BRouterRouteException error,
            boolean keepCurrentRoute
    ) {
        return textResources.getString(bRouterFailureMessageId(error, keepCurrentRoute));
    }

    private static int bRouterFailureMessageId(
            @NonNull BRouterRouteException error,
            boolean keepCurrentRoute
    ) {
        if (keepCurrentRoute) {
            return retainedRouteFailureMessageId(error);
        }
        return unavailableRouteFailureMessageId(error);
    }

    private static int retainedRouteFailureMessageId(@NonNull BRouterRouteException error) {
        switch (error.reason) {
            case NO_ROUTE_FOUND:
                return R.string.nav_route_notice_no_alternative_keep_current;
            case SERVICE_UNAVAILABLE:
                return R.string.nav_route_notice_service_unavailable_keep_current;
            case INVALID_PROFILE:
                return R.string.nav_route_notice_invalid_profile;
            case MALFORMED_RESPONSE:
            case UNKNOWN:
            default:
                return R.string.nav_route_notice_update_failed_keep_current;
        }
    }

    private static int unavailableRouteFailureMessageId(@NonNull BRouterRouteException error) {
        switch (error.reason) {
            case NO_ROUTE_FOUND:
                return R.string.nav_route_notice_no_route_found;
            case SERVICE_UNAVAILABLE:
                return R.string.nav_route_notice_service_unavailable;
            case INVALID_PROFILE:
                return R.string.nav_route_notice_invalid_profile;
            case MALFORMED_RESPONSE:
            case UNKNOWN:
            default:
                return R.string.nav_route_notice_unavailable;
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
