package vibro.navigator.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

/** Checks source resources so missing text cannot be concealed by Android's English fallback. */
public class AppTranslationsTest {
    private static final String ONE = "one";
    private static final String MANY = "many";
    private static final String OTHER = "other";
    private static final Pattern PLACEHOLDERS = Pattern.compile("%[0-9]+\\$[-+0-9.]*[a-zA-Z]|%%");
    private static final Pattern IDENTIFIERS = Pattern.compile(
            "(?:highway|railway|waterway|route)=[a-zA-Z_*]+|profiles2|segments4|\\.brf|\\.rd5");
    private static final Map<String, Set<String>> PLURAL_QUANTITIES = Map.ofEntries(
            Map.entry("ar", Set.of("zero", ONE, "two", "few", MANY, OTHER)),
            Map.entry("ca", Set.of(ONE, MANY, OTHER)),
            Map.entry("cs", Set.of(ONE, "few", MANY, OTHER)),
            Map.entry("es", Set.of(ONE, MANY, OTHER)),
            Map.entry("fr", Set.of(ONE, MANY, OTHER)),
            Map.entry("it", Set.of(ONE, MANY, OTHER)),
            Map.entry("pl", Set.of(ONE, "few", MANY, OTHER)),
            Map.entry("pt", Set.of(ONE, MANY, OTHER)),
            Map.entry("ja", Set.of(OTHER)),
            Map.entry("ko", Set.of(OTHER)),
            Map.entry("zh", Set.of(OTHER)));

    @Test
    public void everyLanguageHasCompleteSharedTranslations() throws Exception {
        Map<String, Element> english = resources("main", "values");
        for (String tag : AppLanguageSettings.availableLanguageTags()) {
            if (!"en".equals(tag)) {
                assertTranslation("main", tag, english);
            }
        }
    }

    @Test
    public void flavorOverlaysDoNotDuplicateTranslatedStrings() throws Exception {
        for (String source : List.of("fdroid", "gplay")) {
            assertTrue(source, resources(source, "values").isEmpty());
            File[] translatedDirectories = new File("src/" + source + "/res")
                    .listFiles(file -> file.isDirectory() && file.getName().startsWith("values-"));
            assertTrue(source, translatedDirectories != null);
            for (File directory : translatedDirectories) {
                assertTrue(directory.toString(), !new File(directory, "strings.xml").exists());
            }
        }
    }

    private static void assertTranslation(String source, String tag, Map<String, Element> english) throws Exception {
        Map<String, Element> translated = resources(source, "values-" + tag);
        assertEquals(source + "/" + tag, english.keySet(), translated.keySet());
        for (Map.Entry<String, Element> entry : english.entrySet()) {
            String message = source + "/" + tag + "/" + entry.getKey();
            Element local = translated.get(entry.getKey());
            assertEquals(message, entry.getValue().getTagName(), local.getTagName());
            assertResource(message, tag, entry.getValue(), local);
        }
    }

    private static void assertResource(String message, String tag, Element english, Element local) {
        if ("plurals".equals(english.getTagName())) {
            Map<String, Element> quantities = elements(local, "quantity");
            assertEquals(message, PLURAL_QUANTITIES.getOrDefault(tag, Set.of(ONE, OTHER)),
                    quantities.keySet());
            Map<String, Element> original = elements(english, "quantity");
            for (Map.Entry<String, Element> item : quantities.entrySet()) {
                Element base = original.getOrDefault(item.getKey(), original.get(OTHER));
                assertText(message + "/" + item.getKey(), base, item.getValue());
            }
        } else {
            assertText(message, english, local);
        }
    }

    private static void assertText(String message, Element english, Element local) {
        String text = local.getTextContent();
        assertTrue(message, !text.trim().isEmpty());
        assertTrue(message + " contains a translation marker", !text.contains("ZXQ") && !text.contains("ZROW"));
        assertEquals(message + " format arguments", matches(PLACEHOLDERS, english.getTextContent()),
                matches(PLACEHOLDERS, text));
        assertEquals(message + " technical identifiers", matches(IDENTIFIERS, english.getTextContent()),
                matches(IDENTIFIERS, text));
        for (String style : List.of("b", "u", "i")) {
            assertEquals(message + " styling", english.getElementsByTagName(style).getLength(),
                    local.getElementsByTagName(style).getLength());
        }
    }

    private static List<String> matches(Pattern pattern, String text) {
        List<String> matches = new ArrayList<>();
        pattern.matcher(text).results().forEach(match -> matches.add(match.group()));
        Collections.sort(matches);
        return matches;
    }

    private static Map<String, Element> resources(String source, String values) throws Exception {
        File file = new File("src/" + source + "/res/" + values + "/strings.xml");
        Element root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getDocumentElement();
        return elements(root, "name");
    }

    private static Map<String, Element> elements(Element parent, String attribute) {
        Map<String, Element> result = new LinkedHashMap<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element && !"false".equals(((Element) node).getAttribute("translatable"))) {
                Element element = (Element) node;
                String key = element.getAttribute(attribute);
                assertTrue("Unnamed resource", !key.isEmpty());
                assertTrue("Duplicate resource " + key, result.put(key, element) == null);
            }
        }
        return result;
    }
}
