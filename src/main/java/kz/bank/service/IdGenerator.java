package kz.bank.service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple thread-safe ID generator backed by an AtomicLong counter.
 * Each entity type gets its own IdGenerator instance so IDs are
 * independent sequences (Customer IDs, Account IDs, etc. each start at 1).
 */
public class IdGenerator {

    private final AtomicLong counter = new AtomicLong(0);

    /** Returns the next unique ID. */
    public Long next() {
        return counter.incrementAndGet();
    }
}
