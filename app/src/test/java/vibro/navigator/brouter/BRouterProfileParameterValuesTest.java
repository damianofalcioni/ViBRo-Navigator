package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class BRouterProfileParameterValuesTest {

    @Test
    public void toExtraParams_sortsValuesAndSkipsAmbiguousPairs() {
        Map<String, String> values = new HashMap<>();
        values.put("zparam", "2");
        values.put("avoid_path", "1");
        values.put("bad", "a&b");
        values.put("badEquals", "a=b");
        values.put("badQuestion", "a?b");
        values.put("bad?key", "3");

        assertEquals("avoid_path=1&zparam=2", BRouterProfileParameterValues.toExtraParams(values));
    }

    @Test
    public void toExtraParams_preservesScientificNotationThroughBRouterDecoding() throws Exception {
        Map<String, String> values = Collections.singletonMap("uphillcost", "1e+3");

        assertEquals(values, decodeAsBRouter(BRouterProfileParameterValues.toExtraParams(values)));
    }

    @Test
    public void toExtraParams_preservesLiteralPercentEscapesWithoutInjectingParameters() throws Exception {
        Map<String, String> values = new HashMap<>();
        values.put("percent", "50%");
        values.put("literal", "%26avoid_path%3D1%3F");

        assertEquals(values, decodeAsBRouter(BRouterProfileParameterValues.toExtraParams(values)));
    }

    @Test
    public void toExtraParams_preservesNamesSpacesAndUtf8ThroughBRouterDecoding() throws Exception {
        Map<String, String> values = new HashMap<>();
        values.put("custom+name%", "caf\u00e9 + 50%");
        values.put("avoid_path", "1");

        assertEquals(values, decodeAsBRouter(BRouterProfileParameterValues.toExtraParams(values)));
    }

    @Test
    public void toExtraParams_omitsMissingEmptyAndUnusableValues() {
        Map<String, String> values = new HashMap<>();
        values.put("missing", null);
        values.put("empty", "");
        values.put("blank", " ");
        values.put("separator", "?");

        assertNull(BRouterProfileParameterValues.toExtraParams(null));
        assertNull(BRouterProfileParameterValues.toExtraParams(Collections.emptyMap()));
        assertNull(BRouterProfileParameterValues.toExtraParams(values));
    }

    private static Map<String, String> decodeAsBRouter(String extraParams) throws UnsupportedEncodingException {
        // RoutingParamCollector.getUrlParams decodes the whole string before tokenizing it.
        Map<String, String> values = new HashMap<>();
        StringTokenizer pairs = new StringTokenizer(URLDecoder.decode(extraParams, "UTF-8"), "?&");
        while (pairs.hasMoreTokens()) {
            StringTokenizer parts = new StringTokenizer(pairs.nextToken(), "=");
            String key = parts.nextToken();
            if (parts.hasMoreTokens()) {
                values.put(key, parts.nextToken());
            }
        }
        return values;
    }
}
