package vibro.navigator.android.brouter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;

import androidx.core.util.Consumer;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import vibro.navigator.brouter.BRouterProfileTestDependencies;
import vibro.navigator.brouter.BRouterProfilesRepository;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = AndroidBRouterDiscoveryRecoveryTest.RealDiscovery.class)
public class AndroidBRouterProfileLoaderTest {
    @Test
    public void profileEnumerationRunsOffTheMainThread() throws InterruptedException {
        try (var activity = Robolectric.buildActivity(Activity.class).setup()) {
            var repository = new RecordingRepository();
            var loader = new AndroidBRouterProfileLoader(activity.get(), repository, ignored -> { });
            try {
                loader.refresh();

                assertTrue(repository.enumerated.await(5, TimeUnit.SECONDS));
                assertFalse(repository.onMainThread.get());
            } finally {
                loader.dispose();
            }
        }
    }

    private static final class RecordingRepository extends BRouterProfilesRepository {
        final CountDownLatch enumerated = new CountDownLatch(1);
        final AtomicBoolean onMainThread = new AtomicBoolean();

        RecordingRepository() {
            super(BRouterProfileTestDependencies.create());
        }

        @Override
        public List<String> listProfiles(Context context, Consumer<List<String>> onBundledProfiles) {
            onMainThread.set(Looper.getMainLooper().isCurrentThread());
            enumerated.countDown();
            return Collections.emptyList();
        }
    }
}
