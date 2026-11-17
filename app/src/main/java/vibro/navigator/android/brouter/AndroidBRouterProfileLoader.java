package vibro.navigator.android.brouter;

import android.content.Context;

import androidx.core.util.Consumer;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import vibro.navigator.brouter.BRouterProfilesRepository;

/** Enumerates profiles away from the activity and keeps bundled profiles available if a provider stalls. */
public final class AndroidBRouterProfileLoader {
    public record Result(List<String> profiles, boolean complete) { }

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
        if (disposed) {
            return;
        }
        if (loading) {
            reloadPending = true;
            return;
        }
        loading = true;
        AtomicReference<Result> bundled = new AtomicReference<>(new Result(Collections.emptyList(), false));
        AndroidBRouterFolderDiscovery.listProfiles(signal -> {
            List<String> profiles = repository.listProfiles(app, names -> bundled.set(new Result(names, false)));
            return new Result(profiles, true);
        }, bundled::get, this::apply);
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
}
