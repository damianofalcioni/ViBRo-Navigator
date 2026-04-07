package com.vibenavigator.nav;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vibenavigator.R;
import com.vibenavigator.brouter.BRouterRouteException;

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
            String message = current.getMessage();
            if (message != null) {
                String sanitized = message.replace('\r', ' ').replace('\n', ' ').trim();
                if (!sanitized.isEmpty()) {
                    return sanitized.length() > 120 ? sanitized.substring(0, 117) + "..." : sanitized;
                }
            }
            current = current.getCause();
        }
        return "";
    }
}
