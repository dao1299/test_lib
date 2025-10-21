package com.vtnet.netat.db.exceptions.transaction;

import com.vtnet.netat.db.exceptions.ErrorSeverity;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown when a deadlock is detected by the database.
 * The database automatically chooses a victim transaction to abort.
 *
 * <p>A deadlock occurs when two or more transactions are waiting for
 * each other to release locks, creating a circular dependency.
 *
 * <p>This exception is retryable - the victim transaction can be
 * retried and will likely succeed if the conflicting transaction completes.
 *
 * @author NETAT Team
 * @since 1.1.0
 */
public class DeadlockException extends TransactionException {

    private static final long serialVersionUID = 1L;

    private final List<String> locksHeld;
    private final List<String> locksWaiting;
    private final String victimInfo;
    private final String deadlockGraph;

    protected DeadlockException(Builder builder) {
        super(builder);
        this.locksHeld = new ArrayList<>(builder.locksHeld);
        this.locksWaiting = new ArrayList<>(builder.locksWaiting);
        this.victimInfo = builder.victimInfo;
        this.deadlockGraph = builder.deadlockGraph;
    }

    /**
     * Gets the locks held by this transaction when deadlock occurred.
     *
     * @return list of lock descriptions
     */
    public List<String> getLocksHeld() {
        return new ArrayList<>(locksHeld);
    }

    /**
     * Gets the locks this transaction was waiting for.
     *
     * @return list of lock descriptions
     */
    public List<String> getLocksWaiting() {
        return new ArrayList<>(locksWaiting);
    }

    /**
     * Gets information about the victim transaction.
     *
     * @return victim info, or null if not available
     */
    public String getVictimInfo() {
        return victimInfo;
    }

    /**
     * Gets the deadlock graph (if available from database).
     *
     * @return deadlock graph description, or null if not available
     */
    public String getDeadlockGraph() {
        return deadlockGraph;
    }

    @Override
    public String getSuggestion() {
        StringBuilder suggestion = new StringBuilder();

        suggestion.append("Deadlock Detected - Transaction was chosen as victim and rolled back.\n\n");

        if (victimInfo != null) {
            suggestion.append("Victim Info: ").append(victimInfo).append("\n\n");
        }

        if (!locksHeld.isEmpty()) {
            suggestion.append("Locks held by this transaction:\n");
            locksHeld.forEach(lock -> suggestion.append("  - ").append(lock).append("\n"));
            suggestion.append("\n");
        }

        if (!locksWaiting.isEmpty()) {
            suggestion.append("Locks this transaction was waiting for:\n");
            locksWaiting.forEach(lock -> suggestion.append("  - ").append(lock).append("\n"));
            suggestion.append("\n");
        }

        if (deadlockGraph != null) {
            suggestion.append("Deadlock Graph:\n");
            suggestion.append(deadlockGraph).append("\n\n");
        }

        suggestion.append("Understanding Deadlocks:\n");
        suggestion.append("A deadlock occurs when transactions wait for each other's locks.\n");
        suggestion.append("Example: Transaction A locks row 1, Transaction B locks row 2.\n");
        suggestion.append("         Then A tries to lock row 2 while B tries to lock row 1.\n");
        suggestion.append("         Neither can proceed - deadlock!\n\n");

        suggestion.append("Prevention Strategies:\n\n");

        suggestion.append("1. CONSISTENT LOCK ORDERING:\n");
        suggestion.append("   - Always access tables in same order\n");
        suggestion.append("   - Always lock rows in same order (e.g., by ID ascending)\n");
        suggestion.append("   - Example: Always lock UserTable before OrderTable\n\n");

        suggestion.append("2. MINIMIZE LOCK DURATION:\n");
        suggestion.append("   - Keep transactions short\n");
        suggestion.append("   - Fetch data before starting transaction\n");
        suggestion.append("   - Release locks as soon as possible\n\n");

        suggestion.append("3. USE APPROPRIATE ISOLATION LEVELS:\n");
        suggestion.append("   - Consider READ COMMITTED instead of SERIALIZABLE\n");
        suggestion.append("   - Use NOLOCK hints carefully (SQL Server)\n");
        suggestion.append("   - Review if REPEATABLE READ is necessary\n\n");

        suggestion.append("4. REDUCE CONTENTION:\n");
        suggestion.append("   - Partition hot tables\n");
        suggestion.append("   - Use optimistic locking where appropriate\n");
        suggestion.append("   - Defer updates to less critical data\n\n");

        suggestion.append("5. IMPLEMENT RETRY LOGIC:\n");
        suggestion.append("   - This exception is retryable\n");
        suggestion.append("   - Use exponential backoff (e.g., 100ms, 200ms, 400ms)\n");
        suggestion.append("   - Limit retry attempts (e.g., max 3 retries)\n");

        return suggestion.toString();
    }

    /**
     * Creates a new builder for DeadlockException.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for DeadlockException.
     */
    public static class Builder extends TransactionException.Builder {
        private List<String> locksHeld = new ArrayList<>();
        private List<String> locksWaiting = new ArrayList<>();
        private String victimInfo;
        private String deadlockGraph;

        public Builder() {
            severity(ErrorSeverity.WARNING);
            retryable(true); // Deadlock victims should retry
        }

        public Builder locksHeld(List<String> locksHeld) {
            this.locksHeld = new ArrayList<>(locksHeld);
            return this;
        }

        public Builder addLockHeld(String lock) {
            this.locksHeld.add(lock);
            return this;
        }

        public Builder locksWaiting(List<String> locksWaiting) {
            this.locksWaiting = new ArrayList<>(locksWaiting);
            return this;
        }

        public Builder addLockWaiting(String lock) {
            this.locksWaiting.add(lock);
            return this;
        }

        public Builder victimInfo(String victimInfo) {
            this.victimInfo = victimInfo;
            if (victimInfo != null) {
                addContext("victimInfo", victimInfo);
            }
            return this;
        }

        public Builder deadlockGraph(String deadlockGraph) {
            this.deadlockGraph = deadlockGraph;
            return this;
        }

        @Override
        public DeadlockException build() {
            return new DeadlockException(this);
        }
    }
}