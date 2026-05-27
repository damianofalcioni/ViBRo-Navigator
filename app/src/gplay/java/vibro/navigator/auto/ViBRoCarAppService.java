package vibro.navigator.auto;

import android.content.pm.ApplicationInfo;

import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.Session;
import androidx.car.app.validation.HostValidator;

import vibro.navigator.R;

public final class ViBRoCarAppService extends CarAppService {

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
        return new ViBRoCarSession();
    }
}
