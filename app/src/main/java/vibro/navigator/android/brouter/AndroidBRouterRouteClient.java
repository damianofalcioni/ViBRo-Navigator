package vibro.navigator.android.brouter;

import android.content.Context;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.RemoteException;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import btools.routingapp.IBRouterService;
import vibro.navigator.brouter.BRouterRouteClient;
import vibro.navigator.brouter.BRouterRouteException;
import vibro.navigator.brouter.BRouterRouteRequest;
import vibro.navigator.brouter.BRouterProfilesRepository;
import vibro.navigator.logging.AppLogger;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public final class AndroidBRouterRouteClient implements BRouterRouteClient {

    private static final String TAG = "AndroidBRouterRouteClient";
    private static final long CONNECT_RETRY_DELAY_MS = 250L;
    private static final int MAX_REQUEST_ATTEMPTS = 2;
    private static final String Z64_BASE64_PREFIX = "ejY0";
    private static final int Z64_MARKER_LENGTH = 3;

    private final Context context;
    private final AndroidBRouterConnectionController connectionController;
    private final BRouterProfilesRepository profilesRepository;

    public AndroidBRouterRouteClient(@NonNull Context context) {
        this(context, AndroidBRouterProfilesRepositoryFactory.create());
    }

    AndroidBRouterRouteClient(
            @NonNull Context context,
            @NonNull BRouterProfilesRepository profilesRepository
    ) {
        this.context = context.getApplicationContext();
        connectionController = new AndroidBRouterConnectionController(context);
        this.profilesRepository = profilesRepository;
    }

    public boolean connect() {
        return connectionController.connect();
    }

    @Nullable
    @Override
    public String requestRoutePayload(@NonNull BRouterRouteRequest request) throws Exception {
        for (int attempt = 1; attempt <= MAX_REQUEST_ATTEMPTS; attempt++) {
            IBRouterService service = requireConnectedService();
            if (service == null) {
                return null;
            }
            RoutePayloadAttemptResult result = requestRoutePayloadAttempt(service, request, attempt);
            if (!result.retry) {
                return result.payload;
            }
        }
        return null;
    }

    @NonNull
    private RoutePayloadAttemptResult requestRoutePayloadAttempt(
            @NonNull IBRouterService service,
            @NonNull BRouterRouteRequest request,
            int attempt
    ) throws Exception {
        try {
            AppLogger.d(TAG, "Requesting track from BRouter service attempt="
                    + attempt + "/" + MAX_REQUEST_ATTEMPTS);
            String raw = service.getTrackFromParams(buildRouteParamsForRequest(request));
            return RoutePayloadAttemptResult.done(raw == null ? null : decodeRoutePayload(raw));
        } catch (RemoteException e) {
            if (!recoverFromRouteRequestFailure(attempt, e)) {
                throw BRouterRouteException.serviceUnavailable(routeRequestFailureLogMessage(e), e);
            }
            return RoutePayloadAttemptResult.retry();
        }
    }

    private boolean recoverFromRouteRequestFailure(
            int attempt,
            @NonNull RemoteException error
    ) {
        AppLogger.w(TAG, routeRequestFailureLogMessage(error) + " attempt="
                + attempt + "/" + MAX_REQUEST_ATTEMPTS, error);
        return retryAfterDisconnect(attempt, MAX_REQUEST_ATTEMPTS, routeRequestRecoveryAction(error));
    }

    @NonNull
    private static String routeRequestFailureLogMessage(@NonNull RemoteException error) {
        return error instanceof DeadObjectException
                ? "BRouter binder died during route request"
                : "BRouter remote call failed during route request";
    }

    @NonNull
    private static String routeRequestRecoveryAction(@NonNull RemoteException error) {
        return error instanceof DeadObjectException
                ? "recovering from BRouter binder death"
                : "recovering from BRouter remote failure";
    }

    @Nullable
    private IBRouterService requireConnectedService() {
        if (!connect()) {
            AppLogger.w(TAG, "Cannot request track because BRouter is not connected");
            return null;
        }
        IBRouterService svc = connectedService();
        if (svc != null) {
            return svc;
        }
        AppLogger.w(TAG, "BRouter service became unavailable before route request");
        connectionController.disconnect();
        if (!connect()) {
            return null;
        }
        svc = connectedService();
        if (svc == null) {
            AppLogger.w(TAG, "BRouter service is still unavailable after reconnect");
            return null;
        }
        return svc;
    }

    @Nullable
    private IBRouterService connectedService() {
        return connectionController.connectedService();
    }

    private boolean retryAfterDisconnect(int attempt, int maxAttempts, @NonNull String action) {
        connectionController.disconnect();
        if (attempt >= maxAttempts) {
            return false;
        }
        try {
            Thread.sleep(CONNECT_RETRY_DELAY_MS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            AppLogger.w(TAG, "Interrupted while " + action, e);
            return false;
        }
    }

    @Override
    public void close() {
        connectionController.close();
    }

    @NonNull
    static Bundle buildRouteParams(@NonNull BRouterRouteRequest request) {
        return AndroidBRouterRouteParams.build(request);
    }

    @NonNull
    private Bundle buildRouteParamsForRequest(@NonNull BRouterRouteRequest request) throws BRouterRouteException {
        return AndroidBRouterRouteParams.build(request, readRemoteProfile(request));
    }

    @Nullable
    private String readRemoteProfile(@NonNull BRouterRouteRequest request) throws BRouterRouteException {
        if (!request.customProfile) {
            return null;
        }
        String profileText = profilesRepository.getCustomProfileText(context, request.profile);
        if (profileText == null || profileText.trim().isEmpty()) {
            throw BRouterRouteException.fromTextResponse(
                    "Profile " + request.profile + " missing custom profile document"
            );
        }
        return profileText;
    }

    @NonNull
    static String decodeRoutePayload(@NonNull String raw) throws IOException {
        String trimmed = raw.trim();
        if (!trimmed.startsWith(Z64_BASE64_PREFIX)) {
            return raw;
        }
        return decodeZ64(trimmed);
    }

    @NonNull
    private static String decodeZ64(@NonNull String encoded) throws IOException {
        byte[] decoded = Base64.decode(encoded, Base64.DEFAULT);
        ByteArrayInputStream compressedPayload = new ByteArrayInputStream(decoded);
        compressedPayload.skip(Z64_MARKER_LENGTH);
        try (GZIPInputStream gzipInput = new GZIPInputStream(compressedPayload);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzipInput, StandardCharsets.UTF_8))) {
            StringBuilder result = new StringBuilder();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) >= 0) {
                result.append(buffer, 0, read);
            }
            return result.toString();
        }
    }

    private static final class RoutePayloadAttemptResult {
        @Nullable
        private final String payload;
        private final boolean retry;

        private RoutePayloadAttemptResult(@Nullable String payload, boolean retry) {
            this.payload = payload;
            this.retry = retry;
        }

        @NonNull
        private static RoutePayloadAttemptResult done(@Nullable String payload) {
            return new RoutePayloadAttemptResult(payload, false);
        }

        @NonNull
        private static RoutePayloadAttemptResult retry() {
            return new RoutePayloadAttemptResult(null, true);
        }
    }
}
