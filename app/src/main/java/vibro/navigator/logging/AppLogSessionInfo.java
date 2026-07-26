package vibro.navigator.logging;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;

import vibro.navigator.BuildConfig;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.distribution.DistributionServices;
import vibro.navigator.settings.AppAndroidAutoSettings;

final class AppLogSessionInfo {

    private static final String UNKNOWN = "unknown";
    private static final String NOT_SUPPORTED = "not_supported";
    private static final String SEPARATOR = "; ";
    private static final String GOOGLE_PLAY_SERVICES_PACKAGE = "com.google.android.gms";
    private static final String ANDROID_AUTO_PHONE_HOST_PACKAGE = "com.google.android.projection.gearhead";
    private static final String ANDROID_AUTO_AUTOMOTIVE_HOST_PACKAGE =
            "com.google.android.apps.automotive.templates.host";

    private AppLogSessionInfo() {
    }

    @NonNull
    static String format(@NonNull Context context, @Nullable File logFile) {
        Context appContext = context.getApplicationContext();
        Resources resources = appContext.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        Configuration configuration = resources.getConfiguration();
        StringBuilder out = new StringBuilder("Log session system info");
        appendAndroidInfo(out);
        appendAppInfo(out, appContext);
        appendIntegrationInfo(out, appContext);
        appendDeviceInfo(out);
        appendRuntimeInfo(out, metrics, configuration, logFile);
        return out.toString();
    }

    private static void appendAndroidInfo(@NonNull StringBuilder out) {
        append(out, "androidVersion", Build.VERSION.RELEASE);
        append(out, "androidSdk", Build.VERSION.SDK_INT);
        append(out, "androidCodename", Build.VERSION.CODENAME);
        append(out, "androidSecurityPatch", Build.VERSION.SECURITY_PATCH);
    }

    private static void appendAppInfo(@NonNull StringBuilder out, @NonNull Context context) {
        append(out, "appVersion", BuildConfig.VERSION_NAME);
        append(out, "versionCode", BuildConfig.VERSION_CODE);
        append(out, "applicationId", BuildConfig.APPLICATION_ID);
        append(out, "package", context.getPackageName());
        append(out, "flavor", BuildConfig.FLAVOR);
        append(out, "buildType", BuildConfig.BUILD_TYPE);
        append(out, "targetSdk", context.getApplicationInfo().targetSdkVersion);
        append(out, "minSdk", minSdk(context));
    }

    private static void appendIntegrationInfo(@NonNull StringBuilder out, @NonNull Context context) {
        appendInstalledPackage(out, "brouter", context, BRouterProfilesRepository.BROUTER_PACKAGE_NAME);
        appendAndroidAutoInfo(out, context);
        appendGooglePlayServicesInfo(out, context);
    }

    private static void appendAndroidAutoInfo(@NonNull StringBuilder out, @NonNull Context context) {
        boolean supported = DistributionServices.supportsAndroidAutoIntegration();
        append(out, "androidAutoSupported", supported);
        append(out, "androidAutoIntegrationEnabled",
                supported ? String.valueOf(AppAndroidAutoSettings.isIntegrationEnabled(context)) : NOT_SUPPORTED);
        append(out, "androidAutoServiceState", DistributionServices.androidAutoIntegrationComponentState(context));
        if (supported) {
            appendInstalledPackage(out, "androidAutoPhoneHost", context, ANDROID_AUTO_PHONE_HOST_PACKAGE);
            appendInstalledPackage(out, "androidAutoAutomotiveHost", context, ANDROID_AUTO_AUTOMOTIVE_HOST_PACKAGE);
        }
    }

    private static void appendGooglePlayServicesInfo(@NonNull StringBuilder out, @NonNull Context context) {
        boolean supported = DistributionServices.supportsGooglePlayServicesDiagnostics();
        append(out, "googlePlayServicesFlavorSupported", supported);
        append(out, "googlePlayServicesStatus", DistributionServices.googlePlayServicesRuntimeStatus(context));
        if (supported) {
            appendInstalledPackage(out, "googlePlayServices", context, GOOGLE_PLAY_SERVICES_PACKAGE);
        }
    }

    private static void appendDeviceInfo(@NonNull StringBuilder out) {
        append(out, "manufacturer", Build.MANUFACTURER);
        append(out, "brand", Build.BRAND);
        append(out, "model", Build.MODEL);
        append(out, "device", Build.DEVICE);
        append(out, "product", Build.PRODUCT);
        append(out, "hardware", Build.HARDWARE);
        append(out, "supportedAbis", Arrays.toString(Build.SUPPORTED_ABIS));
    }

    private static void appendRuntimeInfo(
            @NonNull StringBuilder out,
            @NonNull DisplayMetrics metrics,
            @NonNull Configuration configuration,
            @Nullable File logFile
    ) {
        append(out, "locale", Locale.getDefault().toLanguageTag());
        append(out, "timeZone", TimeZone.getDefault().getID());
        append(out, "screen", metrics.widthPixels + "x" + metrics.heightPixels);
        append(out, "densityDpi", metrics.densityDpi);
        append(out, "fontScale", String.valueOf(configuration.fontScale));
        append(out, "uiModeNight", uiModeNight(configuration));
        append(out, "logFile", logFile == null ? UNKNOWN : logFile.getAbsolutePath());
    }

    private static void appendInstalledPackage(
            @NonNull StringBuilder out,
            @NonNull String prefix,
            @NonNull Context context,
            @NonNull String packageName
    ) {
        append(out, prefix + "Package", packageName);
        PackageInfo packageInfo = packageInfo(context, packageName);
        append(out, prefix + "Installed", packageInfo != null);
        if (packageInfo == null) {
            return;
        }
        ApplicationInfo applicationInfo = applicationInfo(context, packageName);
        append(out, prefix + "VersionName", packageInfo.versionName);
        append(out, prefix + "VersionCode", versionCode(packageInfo));
        append(out, prefix + "Enabled", applicationInfo == null ? UNKNOWN : String.valueOf(applicationInfo.enabled));
        append(out, prefix + "SourceDir", applicationInfo == null ? UNKNOWN : applicationInfo.sourceDir);
    }

    @Nullable
    private static PackageInfo packageInfo(@NonNull Context context, @NonNull String packageName) {
        try {
            return context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Nullable
    private static ApplicationInfo applicationInfo(@NonNull Context context, @NonNull String packageName) {
        try {
            return context.getPackageManager().getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private static long versionCode(@NonNull PackageInfo packageInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return packageInfo.getLongVersionCode();
        }
        return packageInfo.versionCode;
    }

    @NonNull
    private static String minSdk(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return String.valueOf(context.getApplicationInfo().minSdkVersion);
        }
        return UNKNOWN;
    }

    @NonNull
    private static String uiModeNight(@NonNull Configuration configuration) {
        int nightMode = configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            return "yes";
        }
        if (nightMode == Configuration.UI_MODE_NIGHT_NO) {
            return "no";
        }
        return UNKNOWN;
    }

    private static void append(@NonNull StringBuilder out, @NonNull String key, int value) {
        append(out, key, String.valueOf(value));
    }

    private static void append(@NonNull StringBuilder out, @NonNull String key, long value) {
        append(out, key, String.valueOf(value));
    }

    private static void append(@NonNull StringBuilder out, @NonNull String key, boolean value) {
        append(out, key, String.valueOf(value));
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
