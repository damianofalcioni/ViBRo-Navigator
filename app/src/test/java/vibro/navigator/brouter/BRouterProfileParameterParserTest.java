package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.List;

public class BRouterProfileParameterParserTest {

    @Test
    public void parse_readsSupportedProfileParameterTypesAndSkipsTurnInstructionMode() {
        String profile = ""
                + "assign avoid_path = false # %avoid_path% | Avoid paths | boolean\n"
                + "assign uphillcost 80 # %uphillcost% | Uphill cost | number\n"
                + "assign route_pref = 1 # %route_pref% | Route preference | [0=short,1=balanced,2=fast]\n"
                + "assign custom_tag value # %custom_tag% | Custom text | text\n"
                + "assign turnInstructionMode 1 # %turnInstructionMode% | Turn mode | number\n";

        List<BRouterProfileParameter> parameters = new BRouterProfileParameterParser().parse(profile);

        assertEquals(4, parameters.size());
        assertEquals("avoid_path", parameters.get(0).name);
        assertEquals("false", parameters.get(0).defaultValue);
        assertEquals(BRouterProfileParameter.ValueType.BOOLEAN, parameters.get(0).valueType);
        assertEquals("uphillcost", parameters.get(1).name);
        assertEquals("80", parameters.get(1).defaultValue);
        assertEquals(BRouterProfileParameter.ValueType.NUMBER, parameters.get(1).valueType);
        assertEquals("route_pref", parameters.get(2).name);
        assertEquals(BRouterProfileParameter.ValueType.SELECTION, parameters.get(2).valueType);
        assertEquals(3, parameters.get(2).options.size());
        assertEquals("1=balanced", parameters.get(2).options.get(1).label);
        assertEquals("custom_tag", parameters.get(3).name);
        assertEquals(BRouterProfileParameter.ValueType.STRING, parameters.get(3).valueType);
    }
}
