package vibro.navigator.android.brouter;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

final class BRouterRoutePayloadDecoder {
    private static final String Z64_BASE64_PREFIX = "ejY0";
    private static final int Z64_MARKER_LENGTH = 3;

    interface Base64Decoder {
        @NonNull
        byte[] decode(@NonNull String encoded);
    }

    private BRouterRoutePayloadDecoder() {
    }

    @NonNull
    static String decode(@NonNull String raw, @NonNull Base64Decoder base64Decoder) throws IOException {
        String trimmed = raw.trim();
        if (!trimmed.startsWith(Z64_BASE64_PREFIX)) {
            return raw;
        }
        return decodeZ64(base64Decoder.decode(trimmed));
    }

    @NonNull
    private static String decodeZ64(@NonNull byte[] decoded) throws IOException {
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
}
