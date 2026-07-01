package com.mecash.config;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed configuration, bound from the {@code mecash.*} namespace.
 */
@Getter
@ConfigurationProperties(prefix = "mecash")
public class MecashProperties {

    private final Jwt jwt = new Jwt();
    private final Account account = new Account();
    private final Exchange exchange = new Exchange();

    @Setter
    @Getter
    public static class Jwt {
        /** Base64 or raw secret used to sign HS256 tokens. Override in every real environment. */
        private String secret;
        /** Token lifetime in milliseconds (default 24h). */
        private long expirationMs = 86_400_000L;

    }

    @Setter
    @Getter
    public static class Account {
        /**
         * Balance credited to a newly created account. Must be > 0, otherwise new users
         * could never initiate a transfer (the seeded accounts have no owner / login).
         */
        private BigDecimal openingBalance = new BigDecimal("1000.00");

    }

    @Setter
    @Getter
    public static class Exchange {
        /** Today's reference rate: 1 unit of currency A buys this many units of currency B. */
        private BigDecimal rateAToB = new BigDecimal("1.3455");

    }
}
