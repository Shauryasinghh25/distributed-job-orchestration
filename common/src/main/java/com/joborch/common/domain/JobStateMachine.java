package com.joborch.common.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Explicit finite-state-machine for Job lifecycle transitions.
 *
 * WHY a state machine instead of ad-hoc setStatus() calls?
 *   Without it, any code can set any status at any time.
 *   Invalid sequences (e.g., COMPLETED → PROCESSING) go undetected.
 *   This class is the single authority on what transitions are legal.
 *
 * VALID TRANSITIONS:
 *   PENDING    → QUEUED
 *   QUEUED     → PROCESSING
 *   PROCESSING → COMPLETED
 *   PROCESSING → FAILED
 *   FAILED     → RETRYING
 *   RETRYING   → QUEUED      (re-enqueue for next attempt)
 *   FAILED     → DEAD_LETTERED
 *   RETRYING   → DEAD_LETTERED (exhausted max attempts)
 */
public final class JobStateMachine {

    private static final Map<JobStatus, Set<JobStatus>> TRANSITIONS =
            new EnumMap<>(JobStatus.class);

    static {
        TRANSITIONS.put(JobStatus.PENDING,       EnumSet.of(JobStatus.QUEUED));
        TRANSITIONS.put(JobStatus.QUEUED,        EnumSet.of(JobStatus.PROCESSING));
        TRANSITIONS.put(JobStatus.PROCESSING,    EnumSet.of(JobStatus.COMPLETED, JobStatus.FAILED));
        TRANSITIONS.put(JobStatus.FAILED,        EnumSet.of(JobStatus.RETRYING, JobStatus.DEAD_LETTERED));
        TRANSITIONS.put(JobStatus.RETRYING,      EnumSet.of(JobStatus.QUEUED, JobStatus.DEAD_LETTERED));
        TRANSITIONS.put(JobStatus.COMPLETED,     EnumSet.noneOf(JobStatus.class));
        TRANSITIONS.put(JobStatus.DEAD_LETTERED, EnumSet.noneOf(JobStatus.class));
    }

    private JobStateMachine() {}

    /**
     * Validates and returns the target status if the transition is legal.
     *
     * @throws IllegalStateTransitionException if the transition is invalid.
     */
    public static JobStatus transition(JobStatus current, JobStatus target) {
        Set<JobStatus> allowed = TRANSITIONS.getOrDefault(current, EnumSet.noneOf(JobStatus.class));
        if (!allowed.contains(target)) {
            throw new IllegalStateTransitionException(
                "Invalid job state transition: " + current + " → " + target +
                ". Allowed from " + current + ": " + allowed
            );
        }
        return target;
    }

    /** Returns true if a job in this status can still be transitioned. */
    public static boolean isTerminal(JobStatus status) {
        return TRANSITIONS.getOrDefault(status, EnumSet.noneOf(JobStatus.class)).isEmpty();
    }

    public static Set<JobStatus> allowedTransitions(JobStatus from) {
        return TRANSITIONS.getOrDefault(from, EnumSet.noneOf(JobStatus.class));
    }
}
