package com.example.inflace.global.util;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class UuidV7Generator {

    private static final long TIMESTAMP_MASK = 0xFFFFFFFFFFFFL;
    private static final long VERSION_7_BITS = 0x0000000000007000L;
    private static final long VARIANT_BITS = 0x8000000000000000L;
    private static final long RANDOM_62_BIT_MASK = 0x3FFFFFFFFFFFFFFFL;

    private UuidV7Generator() {
    }

    public static UUID next() {
        return next(Instant.now());
    }

    public static UUID next(Instant instant) {
        long unixTsMs = instant.toEpochMilli();
        int randA = ThreadLocalRandom.current().nextInt(1 << 12);
        long randB = ThreadLocalRandom.current().nextLong() & RANDOM_62_BIT_MASK;

        long mostSignificantBits =
                ((unixTsMs & TIMESTAMP_MASK) << 16)
                        | VERSION_7_BITS
                        | randA;

        long leastSignificantBits = VARIANT_BITS | randB;

        return new UUID(mostSignificantBits, leastSignificantBits);
    }
}
