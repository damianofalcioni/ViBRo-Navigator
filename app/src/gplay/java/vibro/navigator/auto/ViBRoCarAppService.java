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
    private static boolean activeSession;

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
        activeSession = true;
        session.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                activeSession = false;
                ViBRoCarAppComponent.onSessionDestroyed(getApplicationContext());
            }
        });
        return session;
    }

    static boolean hasActiveSession() {
        return activeSession;
    }

    static void setActiveSessionForTest(boolean active) {
        activeSession = active;
    }
}
