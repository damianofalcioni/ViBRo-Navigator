package vibro.navigator;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.logging.AppLogger;

public final class ViBRoNavigatorApp extends Application {

    private static final String TAG = "App";

    @Nullable
    private Thread.UncaughtExceptionHandler previousUncaughtExceptionHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        AppLogger.init(this);
        AppLogger.i(TAG, "Application started version=" + BuildConfig.VERSION_NAME
                + " logFile=" + AppLogger.getLogFilePath(this));
        installActivityLifecycleLogging();
        installCrashLogging();
    }

    @Override
    public void onLowMemory() {
        AppLogger.w(TAG, "Application onLowMemory");
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        AppLogger.i(TAG, "Application onTrimMemory level=" + level);
        super.onTrimMemory(level);
    }

    private void installCrashLogging() {
        previousUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            AppLogger.e(TAG, "Uncaught exception on thread=" + thread.getName(), throwable);
            if (previousUncaughtExceptionHandler != null) {
                previousUncaughtExceptionHandler.uncaughtException(thread, throwable);
            } else {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }
        });
    }

    private void installActivityLifecycleLogging() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                AppLogger.i(TAG, activityName(activity) + " created savedState=" + (savedInstanceState != null));
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                AppLogger.i(TAG, activityName(activity) + " started");
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                AppLogger.i(TAG, activityName(activity) + " resumed");
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                AppLogger.i(TAG, activityName(activity) + " paused");
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                AppLogger.i(TAG, activityName(activity) + " stopped");
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
                AppLogger.d(TAG, activityName(activity) + " saved instance state");
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                AppLogger.i(TAG, activityName(activity) + " destroyed");
            }
        });
    }

    @NonNull
    private static String activityName(@NonNull Activity activity) {
        return activity.getClass().getSimpleName();
    }
}
