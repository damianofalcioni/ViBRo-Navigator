package vibro.navigator.nav.power;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface NavigationWakeLock {

    @Nullable
    HeldWakeLock acquire(@NonNull String wakeLockTag, long timeoutMs);

    interface HeldWakeLock extends AutoCloseable {
        @Override
        void close();
    }
}
