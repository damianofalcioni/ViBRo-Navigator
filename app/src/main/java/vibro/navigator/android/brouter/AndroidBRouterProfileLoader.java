package vibro.navigator.android.brouter;

import android.content.Context;
import android.net.Uri;

import androidx.core.util.Consumer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import vibro.navigator.android.storage.AndroidLegacyExternalStorageAccess;
import vibro.navigator.android.storage.AndroidPersistedUriPermissions;
import vibro.navigator.android.storage.AndroidStorageVolumes;
import vibro.navigator.brouter.BRouterFolderGrant;
import vibro.navigator.brouter.BRouterProfileParameter;
import vibro.navigator.brouter.BRouterProfilesRepository;

/** Reads APK profiles independently of potentially stalled external providers. */
public final class AndroidBRouterProfileLoader {
    public record Result(List<String> profiles, List<String> bundled, boolean complete) { }

    private final Context app;
    private final BRouterProfilesRepository repository;
    private final Consumer<Result> onResult;
    private boolean loading;
    private boolean reloadPending;
    private boolean discardRunningResult;
    private boolean disposed;
    private Access access;
    private Result published;

    public AndroidBRouterProfileLoader(Context context, BRouterProfilesRepository repository, Consumer<Result> onResult) {
        app = context.getApplicationContext();
        this.repository = repository;
        this.onResult = onResult;
    }

    public void refresh() {
        if (!disposed && !loading) {
            request(readAccess(), false);
        }
    }

    public void refreshOnResume() {
        if (!disposed) {
            request(readAccess(), false);
        }
    }

    public void refreshAfterAccessChange() {
        if (!disposed) {
            request(readAccess(), true);
        }
    }

    private void request(Access latestAccess, boolean invalidate) {
        boolean accessChanged = invalidate || !latestAccess.equals(access);
        access = latestAccess;
        if (accessChanged && published != null) {
            publish(bundledOnly(published.bundled()));
        }
        if (loading) {
            reloadPending = true;
            discardRunningResult |= accessChanged;
            return;
        }
        start();
    }

    private void start() {
        loading = true;
        AndroidBRouterFolderDiscovery.executeBundledLookup(() -> {
            List<String> bundled = repository.listBundledProfiles(app);
            AndroidBRouterFolderDiscovery.postResult(() -> loadExternal(bundled));
        });
    }

    private Access readAccess() {
        Set<Uri> grants = new HashSet<>();
        for (Uri parent : BRouterFolderGrant.parents(app)) {
            if (BRouterFolderGrant.directory(app, parent, true) != null) {
                grants.add(parent);
            }
        }
        Uri legacy = repository.getProfilesTreeUri(app);
        if (legacy != null) {
            grants.add(legacy);
        }
        AndroidPersistedUriPermissions.retainReadable(app, grants);
        return new Access(repository.isBRouterInstalled(app), AndroidLegacyExternalStorageAccess.hasReadPermission(app),
                AndroidStorageVolumes.secondaryStorageRootIds(app), grants, BRouterFolderGrant.parentFor(app, true));
    }

    private record Access(boolean installed, boolean legacyRead, List<String> volumes, Set<Uri> grants, Uri preferred) { }

    private void loadExternal(List<String> bundled) {
        if (disposed) {
            return;
        }
        Result available = bundledOnly(bundled);
        acceptScan(available);
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
        if (disposed) {
            loading = false;
            return;
        }
        acceptScan(result);
        loading = false;
        if (reloadPending) {
            reloadPending = false;
            discardRunningResult = false;
            start();
        }
    }

    private void acceptScan(Result result) {
        if (discardRunningResult) {
            return;
        }
        if (result.complete() || published == null) {
            publish(result);
        } else if (!published.complete()) {
            // Retain names that were verified under the same access even if only part of the scan completed.
            var names = new LinkedHashSet<>(published.profiles());
            names.removeAll(published.bundled());
            names.addAll(result.profiles());
            publish(new Result(new ArrayList<>(names), result.bundled(), false));
        }
    }

    private static Result bundledOnly(List<String> bundled) {
        return new Result(bundled, bundled, false);
    }

    private void publish(Result result) {
        if (!result.equals(published)) {
            published = result;
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
