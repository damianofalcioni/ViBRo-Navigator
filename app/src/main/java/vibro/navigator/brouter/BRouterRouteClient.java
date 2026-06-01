package vibro.navigator.brouter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface BRouterRouteClient extends AutoCloseable {
    @Nullable
    String requestRoutePayload(@NonNull BRouterRouteRequest request) throws Exception;

    @Override
    default void close() throws Exception {
    }
}
