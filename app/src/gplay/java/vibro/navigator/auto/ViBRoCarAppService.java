package vibro.navigator.auto;

import android.content.pm.ApplicationInfo;

import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.Session;
import androidx.car.app.validation.HostValidator;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import vibro.navigator.R;

public final class ViBRoCarAppService extends CarAppService {
    private static boolean activeSessionForTest;
    private static int activeSessionCount;

    @Override
    @NonNull
    public HostValidator createHostValidator() {
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
        }
        return new HostValidator.Builder(getApplicationContext())
                .addAllowedHosts(R.array.auto_hosts_allowlist)
                .build();
    }

    @Override
    @NonNull
    public Session onCreateSession() {
        Session session = new ViBRoCarSession();
        activeSessionCount++;
        session.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                activeSessionCount = Math.max(0, activeSessionCount - 1);
                ViBRoCarAppComponent.onSessionDestroyed(getApplicationContext());
            }
        });
        return session;
    }

    static boolean hasActiveSession() {
        return activeSessionForTest || activeSessionCount > 0;
    }

    static void setActiveSessionForTest(boolean active) {
        activeSessionForTest = active;
    }

    static void clearActiveSessionsForTest() {
        activeSessionForTest = false;
        activeSessionCount = 0;
    }
}
