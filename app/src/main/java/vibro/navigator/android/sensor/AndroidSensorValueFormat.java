package vibro.navigator.android.sensor;

import androidx.annotation.NonNull;

import java.util.Locale;

import vibro.navigator.nav.orientation.HeadingAccuracyStatus;

final class AndroidSensorValueFormat {
    private AndroidSensorValueFormat() {
    }

    @NonNull
    static String accuracyLabel(int accuracy) {
        switch (accuracy) {
            case HeadingAccuracyStatus.LOW:
                return "low";
            case HeadingAccuracyStatus.MEDIUM:
                return "medium";
            case HeadingAccuracyStatus.HIGH:
                return "high";
            case HeadingAccuracyStatus.UNRELIABLE:
            default:
                return "unreliable";
        }
    }

    @NonNull
    static String formatVector(@NonNull float[] values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(String.format(Locale.US, "%.3f", values[i]));
        }
        sb.append("]");
        return sb.toString();
    }
}
