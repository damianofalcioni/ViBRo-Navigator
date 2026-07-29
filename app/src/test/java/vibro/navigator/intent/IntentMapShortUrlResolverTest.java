package vibro.navigator.intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class IntentMapShortUrlResolverTest {

    private static final String SHORT_URL = "https://maps.app.goo.gl/abc123";
    private static final String EXPANDED_URL = "https://www.google.com/maps/place/Margaretenstra%C3%9Fe+25,"
            + "+1040+Wien/@48.1960405,16.3640609,18z/data=!4m6!3m5"
            + "!1s0x476d078150b0f74b:0x5cb88e606b2bdaa2!8m2"
            + "!3d48.1958755!4d16.3645947!16s%2Fg%2F11c29xktv0";

    @Test
    public void normalizeShortMapUrl_bareGoogleMapsShortLinkAddsHttpsScheme() {
        String normalized = IntentMapShortUrlResolver.normalizeShortMapUrl("maps.app.goo.gl/abc123");

        assertEquals(SHORT_URL, normalized);
    }

    @Test
    public void normalizeShortMapUrl_nonShortUrlReturnsNull() {
        String normalized = IntentMapShortUrlResolver.normalizeShortMapUrl("https://www.google.com/maps");

        assertNull(normalized);
    }

    @Test
    public void expand_followsHttpRedirectsToExpandedMapUrl() throws IOException {
        FakeConnectionFactory factory = new FakeConnectionFactory(
                FakeResponse.redirect(EXPANDED_URL),
                FakeResponse.ok()
        );

        String expanded = IntentMapShortUrlResolver.expand(SHORT_URL, factory);

        assertEquals(EXPANDED_URL, expanded);
        assertEquals(SHORT_URL, factory.requestedUrls.get(0));
        assertEquals(EXPANDED_URL, factory.requestedUrls.get(1));
    }

    @Test
    public void expand_ignoresNonShortUrls() throws IOException {
        String expanded = IntentMapShortUrlResolver.expand("https://example.com/maps", url -> {
            throw new AssertionError("Connection should not be opened");
        });

        assertNull(expanded);
    }

    private static final class FakeConnectionFactory implements IntentMapShortUrlResolver.ConnectionFactory {
        private final Queue<FakeResponse> responses = new ArrayDeque<>();
        private final List<String> requestedUrls = new ArrayList<>();

        FakeConnectionFactory(@NonNull FakeResponse... responses) {
            for (FakeResponse response : responses) {
                this.responses.add(response);
            }
        }

        @NonNull
        @Override
        public HttpURLConnection open(@NonNull URL url) {
            requestedUrls.add(url.toExternalForm());
            return new FakeHttpURLConnection(url, responses.remove());
        }
    }

    private static final class FakeResponse {
        private final int responseCode;
        @Nullable
        private final String location;

        private FakeResponse(int responseCode, @Nullable String location) {
            this.responseCode = responseCode;
            this.location = location;
        }

        @NonNull
        static FakeResponse redirect(@NonNull String location) {
            return new FakeResponse(HttpURLConnection.HTTP_MOVED_TEMP, location);
        }

        @NonNull
        static FakeResponse ok() {
            return new FakeResponse(HttpURLConnection.HTTP_OK, null);
        }
    }

    private static final class FakeHttpURLConnection extends HttpURLConnection {
        @NonNull
        private final FakeResponse response;

        FakeHttpURLConnection(@NonNull URL url, @NonNull FakeResponse response) {
            super(url);
            this.response = response;
        }

        @Override
        public int getResponseCode() {
            return response.responseCode;
        }

        @Override
        public String getHeaderField(String name) {
            if ("Location".equalsIgnoreCase(name)) {
                return response.location;
            }
            return null;
        }

        @Override
        public void disconnect() {
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() {
        }
    }
}
