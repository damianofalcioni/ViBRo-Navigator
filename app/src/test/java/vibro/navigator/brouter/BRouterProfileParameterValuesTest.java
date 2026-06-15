package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class BRouterProfileParameterValuesTest {

    @Test
    public void toExtraParams_sortsValuesAndSkipsAmbiguousPairs() {
        Map<String, String> values = new HashMap<>();
        values.put("zparam", "2");
        values.put("avoid_path", "1");
        values.put("bad", "a&b");

        assertEquals("avoid_path=1&zparam=2", BRouterProfileParameterValues.toExtraParams(values));
    }
}
