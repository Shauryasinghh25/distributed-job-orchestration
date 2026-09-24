package com.joborch.common.domain;

/**
 * Represents the lifecycle state of a job.
 *
 * WHY an enum and not String constants?
 * ========================================
 * 1. Type safety: You can't accidentally set status = "COMPLTED" (typo).
 * 2. Exhaustive switch: The compiler warns if you add a new state and forget to handle it.
 * 3. JPA @Enumerated(EnumType.STRING) stores the name as a VARCHAR — readable in DB.
 *
 * PHASE 1 subset: PENDING, QUEUED, PROCESSING, COMPLETED, FAILED
 * PHASE 2 will add: RETRYING, DEAD_LETTERED
 * We define all states now to avoid a DB migration mid-project.
 *
 * Valid transitions (Phase 1):
 *   PENDING → QUEUED      (producer publishes to RabbitMQ)
 *   QUEUED → PROCESSING   (worker picks up message)
 *   PROCESSING → COMPLETED (worker finishes successfully)
 *   PROCESSING → FAILED    (worker throws an exception)
 */
public enum JobStatus {

    /**
     * Job has been accepted and persisted. Not yet on the queue.
     * This is the initial state on DB insert.
     */
    PENDING,

    /**
     * Job has been published to RabbitMQ. Waiting for a worker to pick it up.
     * Producer sets this after successful publish.
     */
    QUEUED,

    /**
     * A worker has dequeued the message and is actively executing it.
     * Worker sets this immediately before executing business logic.
     */
    PROCESSING,

    /**
     * Job finished successfully.
     * Worker sets this after execution completes without exception.
     */
    COMPLETED,

    /**
     * Job execution threw an exception.
     * Phase 1: terminal state.
     * Phase 2: will transition to RETRYING.
     */
    FAILED,

    /**
     * Phase 2+: Job is waiting in the retry queue before being re-queued.
     */
    RETRYING,

    /**
     * Phase 3+: Job has exhausted all retry attempts and moved to DLQ.
     * Terminal state — requires manual intervention.
     */
    DEAD_LETTERED
}
