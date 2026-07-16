package vibro.navigator.android.speech;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.speech.RecognitionService;
import android.speech.RecognizerIntent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.distribution.DistributionServices;

import java.util.List;

final class AndroidSpeechRecognitionAvailability {

    private static final int MAX_LOGGED_COMPONENTS = 4;
    private static final String GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox";
    private static final String GOOGLE_SPEECH_SERVICES_PACKAGE = "com.google.android.tts";
    private static final String PACKAGE_ENABLED = "enabled";
    private static final String PACKAGE_DISABLED = "disabled";
    private static final String PACKAGE_MISSING = "missing";

    @NonNull
    private final PackageManager packageManager;
    @Nullable
    private final ComponentName distributionFallbackService;

    AndroidSpeechRecognitionAvailability(@NonNull PackageManager packageManager) {
        this(packageManager, DistributionServices.createSpeechRecognitionServiceFallback());
    }

    AndroidSpeechRecognitionAvailability(
            @NonNull PackageManager packageManager,
            @Nullable ComponentName distributionFallbackService
    ) {
        this.packageManager = packageManager;
        this.distributionFallbackService = distributionFallbackService;
    }

    @Nullable
    ComponentName firstRecognitionService() {
        for (ResolveInfo info : recognitionServices()) {
            ComponentName componentName = serviceComponentName(info);
            if (componentName != null) {
                return componentName;
            }
        }
        return enabledFallbackService();
    }

    boolean hasRecognitionProvider() {
        return !recognizerActivities().isEmpty() || firstRecognitionService() != null;
    }

    @NonNull
    String describe() {
        return "recognizerActivities=" + describeComponents(recognizerActivities(), true)
                + " recognitionServices=" + describeComponents(recognitionServices(), false)
                + " fallbackRecognitionService=" + describeFallbackService()
                + " googleApp=" + packageState(GOOGLE_APP_PACKAGE)
                + " googleSpeechServices=" + packageState(GOOGLE_SPEECH_SERVICES_PACKAGE);
    }

    @Nullable
    private ComponentName enabledFallbackService() {
        if (distributionFallbackService == null) {
            return null;
        }
        return isPackageEnabled(distributionFallbackService.getPackageName())
                ? distributionFallbackService
                : null;
    }

    @NonNull
    private List<ResolveInfo> recognizerActivities() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        return packageManager.queryIntentActivities(intent, 0);
    }

    @NonNull
    private List<ResolveInfo> recognitionServices() {
        Intent intent = new Intent(RecognitionService.SERVICE_INTERFACE);
        return packageManager.queryIntentServices(intent, 0);
    }

    @NonNull
    private String describeComponents(@NonNull List<ResolveInfo> infos, boolean activities) {
        if (infos.isEmpty()) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder("[");
        appendComponentNames(builder, infos, activities);
        return builder.append(']').toString();
    }

    private void appendComponentNames(
            @NonNull StringBuilder builder,
            @NonNull List<ResolveInfo> infos,
            boolean activities
    ) {
        int count = Math.min(infos.size(), MAX_LOGGED_COMPONENTS);
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(componentName(infos.get(index), activities));
        }
        if (infos.size() > MAX_LOGGED_COMPONENTS) {
            builder.append(", ...");
        }
    }

    @NonNull
    private String componentName(@NonNull ResolveInfo info, boolean activity) {
        ComponentName componentName = activity
                ? activityComponentName(info)
                : serviceComponentName(info);
        return componentName == null ? "unknown" : componentName.flattenToShortString();
    }

    @Nullable
    private static ComponentName activityComponentName(@NonNull ResolveInfo info) {
        ActivityInfo activityInfo = info.activityInfo;
        if (activityInfo == null) {
            return null;
        }
        return new ComponentName(activityInfo.packageName, activityInfo.name);
    }

    @Nullable
    private static ComponentName serviceComponentName(@NonNull ResolveInfo info) {
        ServiceInfo serviceInfo = info.serviceInfo;
        if (serviceInfo == null) {
            return null;
        }
        return new ComponentName(serviceInfo.packageName, serviceInfo.name);
    }

    @NonNull
    private String packageState(@NonNull String packageName) {
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            return applicationInfo.enabled ? PACKAGE_ENABLED : PACKAGE_DISABLED;
        } catch (PackageManager.NameNotFoundException e) {
            return PACKAGE_MISSING;
        }
    }

    private boolean isPackageEnabled(@NonNull String packageName) {
        return PACKAGE_ENABLED.equals(packageState(packageName));
    }

    @NonNull
    private String describeFallbackService() {
        if (distributionFallbackService == null) {
            return "none";
        }
        return distributionFallbackService.flattenToShortString()
                + ":" + packageState(distributionFallbackService.getPackageName());
    }
}
