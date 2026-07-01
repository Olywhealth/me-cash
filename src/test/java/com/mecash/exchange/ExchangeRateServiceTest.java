package com.mecash.exchange;
import com.mecash.service.exchange.ConversionResult;
import com.mecash.service.exchange.ExchangeRateService;

import static org.assertj.core.api.Assertions.assertThat;

import com.mecash.enums.Currency;
import com.mecash.config.MecashProperties;
import java.math.BigDecimal;

import com.mecash.service.exchange.ExchangeRateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the FX core. BigDecimal equality is asserted with isEqualByComparingTo so
 * scale differences (e.g. 134.55 vs 134.550) don't cause false failures.
 */
class ExchangeRateServiceTest {

    private ExchangeRateService service;

    @BeforeEach
    void setUp() {
        MecashProperties properties = new MecashProperties();
        properties.getExchange().setRateAToB(new BigDecimal("1.3455"));
        service = new ExchangeRateServiceImpl(properties);
    }

    @Test
    void convertsAToBByMultiplyingByTheRate() {
        ConversionResult result = service.convert(new BigDecimal("100.00"), Currency.A, Currency.B);

        assertThat(result.sourceAmount()).isEqualByComparingTo("100.00");
        assertThat(result.convertedAmount()).isEqualByComparingTo("134.55"); // 100 * 1.3455
        assertThat(result.rate()).isEqualByComparingTo("1.3455");
    }

    @Test
    void convertsBToAByDividingByTheRate() {
        ConversionResult result = service.convert(new BigDecimal("100.00"), Currency.B, Currency.A);

//         100 / 1.3455 = 74.32181... -> 74.32 (HALF_EVEN, scale 2)
        assertThat(result.convertedAmount()).isEqualByComparingTo("74.32");
        // Effective rate is the inverse of the reference rate (1 / 1.3455 = 0.7432181...).
        assertThat(result.rate()).isEqualByComparingTo("0.743218");
    }

    @Test
    void sameCurrencyConversionIsANoOpAtRateOne() {
        ConversionResult result = service.convert(new BigDecimal("50.00"), Currency.A, Currency.A);

        assertThat(result.convertedAmount()).isEqualByComparingTo("50.00");
        assertThat(result.rate()).isEqualByComparingTo("1");
    }

}
