package com.joborch.common.domain;

/**
 * The type of work a job represents.
 *
 * WHY have a JobType at all?
 * ============================
 * JobType is the discriminator that tells the worker WHICH handler to invoke.
 * In Phase 6, we build a HandlerRegistry: Map<JobType, JobHandler>.
 * Without this type marker, the worker has no way to route a job to
 * the correct business logic.
 *
 * PATTERN: This is the "type tag" in the Strategy + Registry pattern.
 * The job declares what it IS; the worker decides HOW to handle it.
 *
 * EXTENSIBILITY NOTE:
 * Adding a new job type requires:
 *   1. Adding the constant here.
 *   2. Implementing a new JobHandler (Phase 6).
 *   3. Registering it in HandlerRegistry.
 * No existing handler code changes — Open/Closed Principle.
 */
public enum JobType {

    /**
     * Send an email. Payload contains recipient, subject, body.
     */
    EMAIL,

    /**
     * Generate a report. Payload contains report parameters.
     */
    REPORT,

    /**
     * Send a push notification. Payload contains device token, message.
     */
    NOTIFICATION,

    /**
     * A generic data processing job. Used for demos and testing.
     */
    DATA_PROCESSING
}
