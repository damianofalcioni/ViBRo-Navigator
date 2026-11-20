package vibro.navigator.android.brouter;

import android.content.Context;

import androidx.core.util.Consumer;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.brouter.BRouterProfileParameter;

/** Reads APK profiles independently of potentially stalled external providers. */
public final class AndroidBRouterProfileLoader {
    public record Result(List<String> profiles, List<String> bundled, boolean complete) { }

    private final Context app;
    private final BRouterProfilesRepository repository;
    private final Consumer<Result> onResult;
    private boolean loading;
    private boolean reloadPending;
    private boolean disposed;

    public AndroidBRouterProfileLoader(Context context, BRouterProfilesRepository repository, Consumer<Result> onResult) {
        app = context.getApplicationContext();
        this.repository = repository;
        this.onResult = onResult;
    }

    public void refresh() {
        if (disposed || loading) {
            return;
        }
        loading = true;
        AndroidBRouterFolderDiscovery.executeBundledLookup(() -> {
            List<String> bundled = repository.listBundledProfiles(app);
            AndroidBRouterFolderDiscovery.postResult(() -> loadExternal(bundled));
        });
    }

    public void refreshAfterAccessChange() {
        reloadPending = loading;
        refresh();
    }

    private void loadExternal(List<String> bundled) {
        if (disposed) {
            return;
        }
        Result available = new Result(bundled, bundled, false);
        onResult.accept(available);
        AtomicReference<Result> partial = new AtomicReference<>(available);
        AndroidBRouterFolderDiscovery.listProfiles(
                signal -> {
                    AtomicBoolean incomplete = new AtomicBoolean();
                    List<String> profiles = AndroidBRouterProfilesRepositoryFactory.withCancellation(signal,
                            () -> repository.listAvailableProfiles(app, bundled,
                                    found -> partial.set(new Result(found, bundled, false))),
                            () -> incomplete.set(true));
                    return new Result(profiles, bundled, !incomplete.get());
                },
                partial::get, this::apply);
    }

    private void apply(Result result) {
        loading = false;
        if (disposed) {
            return;
        }
        if (reloadPending) {
            reloadPending = false;
            refresh();
        } else {
            onResult.accept(result);
        }
    }

    public void dispose() {
        disposed = true;
    }

    public static void loadParameters(Context context, BRouterProfilesRepository repository, String name,
                                      boolean custom, Consumer<List<BRouterProfileParameter>> result) {
        Context app = context.getApplicationContext();
        AndroidBRouterFolderDiscovery.find(signal -> AndroidBRouterProfilesRepositoryFactory.withCancellation(
                signal, () -> repository.getProfileParameters(app, name, custom)), null, result);
    }
}
