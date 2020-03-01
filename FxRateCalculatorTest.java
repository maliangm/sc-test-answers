import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

public class FxRateCalculatorTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private FxRateCalculator calculator;

    @Before
    public void setup() {
        calculator = new FxRateCalculator();
    }

    @Test
    public void testDirectExchange() {
        double rate = calculator.calculateRate("EUR/USD");
        assertEquals(1.2, rate, 0.005);
    }

    @Test
    public void testIndirectExchange() {
        double rate = calculator.calculateRate("EUR/AUD");
        assertEquals(1.53, rate, 0.005);
    }

    @Test
    public void testIndirectExchangeMultipleSteps() {
        double rate = calculator.calculateRate("EUR/INR");
        assertEquals(81.0, rate, 0.005);
    }

    @Test
    public void testReversExchange() {
        double rate = calculator.calculateRate("USD/EUR");
        assertEquals(1 / 1.2, rate, 0.005);
    }

    @Test
    public void testNoCurrencyPairInput() {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Invalid input: Please input the currency pair.");

        calculator.calculateRate(null);
    }

    @Test
    public void testInvalidCurrencyPairFormat() {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Invalid input: Invalid currency pair format. Should be in the format of 'FROM_CURRENCY/TO_CURRENCY', e.g. 'EUR/USD'");

        calculator.calculateRate("XXX");
    }

    @Test
    public void testInvalidSrcCurrency() {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Invalid input: 'XXX' is not a supported currency.");

        calculator.calculateRate("XXX/USD");
    }

    @Test
    public void testInvalidDescCurrency() {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Invalid input: 'XXX' is not a supported currency.");

        calculator.calculateRate("EUR/XXX");
    }
}
