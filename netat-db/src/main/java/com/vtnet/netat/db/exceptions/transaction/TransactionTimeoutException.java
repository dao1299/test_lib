package com.vtnet.netat.db.exceptions.transaction;

import com.vtnet.netat.db.exceptions.ErrorSeverity;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown when a transaction exceeds its configured timeout.
 * Long-running transactions can cause lock contention and resource issues.
 *
 * <p>Common causes:
 * <ul>
 *   <li>Too many operations in single transaction</li>
 *   <li>Slow queries within transaction</li>
 *   <li>Lock waiting times accumulating</li>
 *   <li>External API calls within transaction</li>
 * </ul>
 *
 * @author NETAT Team
 * @since 1.1.0
 */
public class TransactionTimeoutException extends TransactionException {

    private static final long serialVersionUID = 1L;

    private final long timeoutMillis;
    private final long actualDurationMillis;
    private final int operationCount;
    private final List<String> operations;

    protected TransactionTimeoutException(Builder builder) {
        super(builder);
        this.timeoutMillis = builder.timeoutMillis;
        this.actualDurationMillis = builder.actualDurationMillis;
        this.operationCount = builder.operationCount;
        this.operations = new ArrayList<>(builder.operations);
    }

    /**
     * Gets the configured transaction timeout.
     *
     * @return timeout in milliseconds
     */
    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    /**
     * Gets the actual duration before timeout.
     *
     * @return duration in milliseconds
     */
    public long getActualDurationMillis() {
        return actualDurationMillis;
    }

    /**
     * Gets the number of operations performed in the transaction.
     *
     * @return operation count
     */
    public int getOperationCount() {
        return operationCount;
    }

    /**
     * Gets the list of operations performed (if tracked).
     *
     * @return list of operation descriptions
     */
    public List<String> getOperations() {
        return new ArrayList<>(operations);
    }

    @Override
    public String getSuggestion() {
        StringBuilder suggestion = new StringBuilder();

        suggestion.append(String.format(
                "Transaction timed out after %,d ms (timeout: %,d ms)\n\n",
                actualDurationMillis, timeoutMillis));

        suggestion.append("Transaction Statistics:\n");
        suggestion.append(String.format("  - Duration: %,d ms\n", actualDurationMillis));
        suggestion.append(String.format("  - Operations: %d\n", operationCount));
        suggestion.append(String.format("  - Average per operation: %.2f ms\n\n",
                operationCount > 0 ? (double) actualDurationMillis / operationCount : 0));

        if (!operations.isEmpty()) {
            suggestion.append("Operations in transaction:\n");
            for (int i = 0; i < Math.min(operations.size(), 10); i++) {
                suggestion.append(String.format("  %d. %s\n", i + 1, operations.get(i)));
            }
            if (operations.size() > 10) {
                suggestion.append(String.format("  ... and %d more operations\n", operations.size() - 10));
            }
            suggestion.append("\n");
        }

        suggestion.append("Resolution Strategies:\n\n");

        suggestion.append("1. BREAK INTO SMALLER TRANSACTIONS:\n");
        suggestion.append("   - Split into multiple independent transactions\n");
        suggestion.append("   - Commit after logical units of work\n");
        suggestion.append("   - Consider eventual consistency patterns\n\n");

        suggestion.append("2. OPTIMIZE QUERIES:\n");
        suggestion.append("   - Review slow queries within transaction\n");
        suggestion.append("   - Add indexes to speed up operations\n");
        suggestion.append("   - Use batch operations where possible\n\n");

        suggestion.append("3. REDUCE LOCK CONTENTION:\n");
        suggestion.append("   - Order operations to minimize lock conflicts\n");
        suggestion.append("   - Use lower isolation levels if acceptable\n");
        suggestion.append("   - Avoid row locks on frequently accessed data\n\n");

        suggestion.append("4. REMOVE NON-CRITICAL OPERATIONS:\n");
        suggestion.append("   - Move logging outside transaction\n");
        suggestion.append("   - Move external API calls outside transaction\n");
        suggestion.append("   - Defer non-essential updates\n\n");

        suggestion.append("5. INCREASE TIMEOUT (last resort):\n");
        suggestion.append(String.format("   - Current: %,d ms\n", timeoutMillis));
        suggestion.append(String.format("   - Suggested: %,d ms\n", Math.min(timeoutMillis * 2, 600000)));
        suggestion.append("   - Note: Long transactions can cause system-wide issues\n");

        return suggestion.toString();
    }

    /**
     * Creates a new builder for TransactionTimeoutException.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for TransactionTimeoutException.
     */
    public static class Builder extends TransactionException.Builder {
        private long timeoutMillis;
        private long actualDurationMillis;
        private int operationCount;
        private List<String> operations = new ArrayList<>();

        public Builder() {
            severity(ErrorSeverity.WARNING);
            retryable(true); // May succeed with less contention
        }

        public Builder timeoutMillis(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            addContext("timeoutMillis", timeoutMillis);
            return this;
        }

        public Builder actualDurationMillis(long actualDurationMillis) {
            this.actualDurationMillis = actualDurationMillis;
            addContext("actualDurationMillis", actualDurationMillis);
            return this;
        }

        public Builder operationCount(int operationCount) {
            this.operationCount = operationCount;
            addContext("operationCount", operationCount);
            return this;
        }

        public Builder operations(List<String> operations) {
            this.operations = new ArrayList<>(operations);
            return this;
        }

        public Builder addOperation(String operation) {
            this.operations.add(operation);
            return this;
        }

        @Override
        public TransactionTimeoutException build() {
            return new TransactionTimeoutException(this);
        }
    }
}