package vibro.navigator.android.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.atomic.AtomicReference;

import vibro.navigator.android.brouter.AndroidBRouterPickerRequest.Completion;

@RunWith(RobolectricTestRunner.class)
public class AndroidBRouterPickerRequestTest {
    private static final String KEY = "test_picker";

    @Test
    public void completionBelongsOnlyToTheRequestThatReturned() {
        Context context = ApplicationProvider.getApplicationContext();
        var first = new AndroidBRouterPickerRequest(context, KEY);
        var second = new AndroidBRouterPickerRequest(context, KEY);
        first.begin(42);
        second.begin(42);
        Bundle firstState = new Bundle();
        Bundle secondState = new Bundle();
        first.save(firstState);
        second.save(secondState);

        first.complete(Completion.RETRY);

        var restored = new AndroidBRouterPickerRequest(context, KEY);
        assertNull(restored.restore(secondState));
        assertTrue(restored.matches(42));
        restored.complete(Completion.ADVANCE);
        assertEquals(Completion.RETRY, restored.restore(firstState));
        assertEquals(Completion.ADVANCE, restored.restore(secondState));
        assertFalse(restored.isPending());
    }

    @Test
    public void restoredPickerReceivesValidationAlreadyInProgress() {
        Context context = ApplicationProvider.getApplicationContext();
        var original = new AndroidBRouterPickerRequest(context, KEY);
        original.begin(42);
        Bundle snapshot = new Bundle();
        original.save(snapshot);
        original.beginValidation();

        var replacement = new AndroidBRouterPickerRequest(context, KEY);
        assertNull(replacement.restore(snapshot));
        var completion = new AtomicReference<Completion>();
        replacement.watch(completion::set);
        original.complete(Completion.ADVANCE);
        assertEquals(Completion.ADVANCE, completion.get());
        assertFalse(replacement.isPending());
    }
}
