package iq.haji.alhagebb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class UtilTest {
    @Test
    public void localNumberKeepsDigitsOnly() {
        assertEquals("07701234567", Util.normalizePhone("0770 123 4567"));
    }

    @Test
    public void internationalIraqiNumberBecomesLocal() {
        assertEquals("07701234567", Util.normalizePhone("+9647701234567"));
        assertEquals("07701234567", Util.normalizePhone("009647701234567"));
    }

    @Test
    public void hiddenOrInvalidNumbersReturnNull() {
        assertNull(Util.normalizePhone(null));
        assertNull(Util.normalizePhone(""));
        assertNull(Util.normalizePhone("Unknown"));
        assertNull(Util.normalizePhone("123"));
        assertNull(Util.normalizePhone("12+34567"));
    }

    @Test
    public void csvEscapesQuotesAndFormulas() {
        assertEquals("\"a\"\"b\"", Util.csvText("a\"b"));
        assertEquals("\"'=1+1\"", Util.csvText("=1+1"));
        assertEquals("\"\"", Util.csvText(null));
        assertEquals("\"+9647701234567\"", Util.csvRaw("+9647701234567"));
    }
}
