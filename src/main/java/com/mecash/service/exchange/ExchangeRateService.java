package com.mecash.service.exchange;

import com.mecash.enums.Currency;

import java.math.BigDecimal;

public interface ExchangeRateService {
    ConversionResult convert(BigDecimal amount, Currency from, Currency to);
}
