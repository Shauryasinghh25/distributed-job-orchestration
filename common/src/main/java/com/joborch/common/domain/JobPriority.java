package com.joborch.common.domain;

/**
 * Priority levels for job scheduling.
 *
 * WHY ordinal matters here:
 * ===========================
 * RabbitMQ priority queues use integer values (0–255).
 * We map our semantic priorities to numeric values explicitly
 * rather than relying on enum.ordinal(), because ordinal() is
 * fragile — if someone inserts a new enum constant between existing ones,
 * all ordinals shift, silently corrupting priority semantics.
 *
 * Each constant stores its own numeric value via a constructor.
 * Phase 5 will use getValue() when setting AMQP message priority.
 *
 * INTERVIEW TRAP: "Why not just use integer priorities in your API?"
 * Because integers (0–10) have no business meaning. An engineer joining
 * the team must understand what "priority 7" means. CRITICAL vs HIGH
 * is self-documenting.
 */
public enum JobPriority {

    LOW(1),
    NORMAL(5),
    HIGH(8),
    CRITICAL(10);

    private final int value;

    JobPriority(int value) {
        this.value = value;
    }

    /**
     * Returns the numeric priority value for use with RabbitMQ message headers.
     */
    public int getValue() {
        return value;
    }
}
