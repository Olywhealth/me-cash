package com.mecash.service.exchange;

import com.mecash.enums.Currency;
import com.mecash.config.MecashProperties;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/**
 * Converts money between the two currencies using today's reference rate.
 *
 * <p>Rounding policy (applied consistently everywhere):
 * <ul>
 *   <li>monetary amounts: scale 2, {@link RoundingMode#HALF_EVEN} (banker's rounding)</li>
 *   <li>exchange rates: scale 6</li>
 * </ul>
 *
 * <p>With {@code A->B = 1.3455}: converting from A multiplies by the rate, converting from B
 * divides by it, and a same-currency conversion is a no-op at rate 1.
 */
@Service
public class ExchangeRateServiceImpl implements ExchangeRateService{

    public static final int MONEY_SCALE = 2;
    public static final int RATE_SCALE = 6;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    private final BigDecimal rateAToB;

    public ExchangeRateServiceImpl(MecashProperties properties) {
        this.rateAToB = properties.getExchange().getRateAToB();
    }

    @Override
    public ConversionResult convert(BigDecimal amount, Currency from, Currency to) {
        BigDecimal source = amount.setScale(MONEY_SCALE, ROUNDING);
        BigDecimal rate = rateFor(from, to);

        BigDecimal converted;
        if (from == to) {
            converted = source;
        } else if (from == Currency.A) {
            // A -> B : multiply by the reference rate.
            converted = source.multiply(rateAToB).setScale(MONEY_SCALE, ROUNDING);
        } else {
            // B -> A : divide by the reference rate (use full precision, then round once).
            converted = source.divide(rateAToB, MathContext.DECIMAL64).setScale(MONEY_SCALE, ROUNDING);
        }
        return new ConversionResult(from, to, source, converted, rate);
    }

    /** Effective source->destination rate, scale 6. */
    private BigDecimal rateFor(Currency from, Currency to) {
        if (from == to) {
            return BigDecimal.ONE.setScale(RATE_SCALE, ROUNDING);
        }
        if (from == Currency.A) {
            return rateAToB.setScale(RATE_SCALE, ROUNDING);
        }
        return BigDecimal.ONE.divide(rateAToB, RATE_SCALE, ROUNDING);
    }
}
