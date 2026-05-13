package vibro.navigator.map;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

final class MapAssetTextReader {
    private MapAssetTextReader() {
    }

    @NonNull
    static String read(@NonNull Context context, @NonNull String assetName) throws IOException {
        try (InputStream is = context.getAssets().open(assetName)) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = br.read(buf)) >= 0) {
                sb.append(buf, 0, n);
            }
            return sb.toString();
        }
    }
}
