package vibro.navigator.android.brouter;

import android.content.Context;
import android.os.CancellationSignal;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import vibro.navigator.brouter.BRouterProfileParameter;
import vibro.navigator.brouter.BRouterProfileParameterParser;
import vibro.navigator.brouter.BRouterProfileParameterValues;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.brouter.BRouterRouteRequest;

final class AndroidBRouterRouteProfileParameters {
    private static final long LOOKUP_TIMEOUT_MS = 1_000L;

    private AndroidBRouterRouteProfileParameters() { }

    @Nullable
    static String resolve(Context context, BRouterProfilesRepository repository, BRouterRouteRequest request,
                          @Nullable String remoteProfile) throws InterruptedException {
        String encoded = request.profileParameters;
        if (encoded == null) {
            return null;
        }
        List<BRouterProfileParameter> known = remoteProfile == null
                ? readNamed(context, repository, request.profile) : new BRouterProfileParameterParser().parse(remoteProfile);
        return known == null ? encoded : BRouterProfileParameterValues.filterExtraParams(encoded, known);
    }

    @Nullable
    private static List<BRouterProfileParameter> readNamed(Context context, BRouterProfilesRepository repository,
                                                           String name) throws InterruptedException {
        CancellationSignal signal = new CancellationSignal();
        var task = new FutureTask<>(() -> AndroidBRouterProfilesRepositoryFactory.withCancellation(signal,
                () -> repository.readResolvedProfileParameters(context, name, false)));
        try {
            AndroidBRouterFolderDiscovery.executeProfileLookup(task);
            return task.get(LOOKUP_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException | RejectedExecutionException e) {
            return null;
        } finally {
            AndroidBRouterFolderDiscovery.cancelTask(task, signal);
        }
    }
}
