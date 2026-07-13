package vibro.navigator.android.export;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class AndroidRouteGpxProviderMetadata {
    private AndroidRouteGpxProviderMetadata() {
    }

    @NonNull
    static Cursor cursor(@NonNull File file, @Nullable String[] projection) {
        String[] requested = projection == null
                ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}
                : projection;
        List<String> columns = new ArrayList<>(requested.length);
        List<Object> values = new ArrayList<>(requested.length);
        for (String column : requested) {
            addColumn(file, column, columns, values);
        }
        MatrixCursor cursor = new MatrixCursor(columns.toArray(new String[0]), 1);
        cursor.addRow(values.toArray(new Object[0]));
        return cursor;
    }

    private static void addColumn(
            @NonNull File file,
            @Nullable String column,
            @NonNull List<String> columns,
            @NonNull List<Object> values
    ) {
        if (OpenableColumns.DISPLAY_NAME.equals(column)) {
            columns.add(OpenableColumns.DISPLAY_NAME);
            values.add(file.getName());
        } else if (OpenableColumns.SIZE.equals(column)) {
            columns.add(OpenableColumns.SIZE);
            values.add(file.length());
        }
    }
}
