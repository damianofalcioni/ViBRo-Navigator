package vibro.navigator.logging;

import android.content.Context;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

final class AppInstallSourceLogInfo {
    private static final String UNKNOWN = "unknown";
    private static final String SEPARATOR = "; ";

    private AppInstallSourceLogInfo() {
    }

    static void append(@NonNull StringBuilder out, @NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            appendInstallSourceInfo(out, context);
            return;
        }
        append(out, "appInstallerPackage", legacyInstallerPackage(context));
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private static void appendInstallSourceInfo(@NonNull StringBuilder out, @NonNull Context context) {
        try {
            InstallSourceInfo installSourceInfo = context.getPackageManager()
                    .getInstallSourceInfo(context.getPackageName());
            append(out, "appInstallingPackage", installSourceInfo.getInstallingPackageName());
            append(out, "appInitiatingPackage", installSourceInfo.getInitiatingPackageName());
            append(out, "appOriginatingPackage", installSourceInfo.getOriginatingPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            append(out, "appInstallingPackage", UNKNOWN);
            append(out, "appInitiatingPackage", UNKNOWN);
            append(out, "appOriginatingPackage", UNKNOWN);
        }
    }

    @SuppressWarnings("deprecation")
    @NonNull
    private static String legacyInstallerPackage(@NonNull Context context) {
        String installerPackage = context.getPackageManager().getInstallerPackageName(context.getPackageName());
        return installerPackage == null ? UNKNOWN : installerPackage;
    }

    private static void append(@NonNull StringBuilder out, @NonNull String key, @Nullable String value) {
        out.append(SEPARATOR)
                .append(key)
                .append("=")
                .append(safe(value));
    }

    @NonNull
    private static String safe(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return UNKNOWN;
        }
        return value.replace('\r', ' ').replace('\n', ' ');
    }
}
