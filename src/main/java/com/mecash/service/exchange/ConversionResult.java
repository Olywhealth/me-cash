package com.mecash.service.exchange;

import com.mecash.enums.Currency;
import java.math.BigDecimal;

/**
 * Outcome of converting an amount from one currency to another.
 *
 * @param from           source currency
 * @param to             destination currency
 * @param sourceAmount   amount in the source currency (scale 2)
 * @param convertedAmount amount in the destination currency (scale 2)
 * @param rate           effective source->destination rate applied (scale 6)
 */
public record ConversionResult(
        Currency from,
        Currency to,
        BigDecimal sourceAmount,
        BigDecimal convertedAmount,
        BigDecimal rate
) {
}
