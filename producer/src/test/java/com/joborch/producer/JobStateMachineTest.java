package com.joborch.producer;

import com.joborch.common.domain.JobStatus;
import com.joborch.common.domain.JobStateMachine;
import com.joborch.common.domain.IllegalStateTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit test for the JobStateMachine — Phase 7.
 * No Spring context needed — pure Java logic.
 */
class JobStateMachineTest {

    @ParameterizedTest(name = "{0} → {1} should be VALID")
    @CsvSource({
        "PENDING,    QUEUED",
        "QUEUED,     PROCESSING",
        "PROCESSING, COMPLETED",
        "PROCESSING, FAILED",
        "FAILED,     RETRYING",
        "FAILED,     DEAD_LETTERED",
        "RETRYING,   QUEUED",
        "RETRYING,   DEAD_LETTERED"
    })
    @DisplayName("Valid transitions are accepted")
    void validTransitions(JobStatus from, JobStatus to) {
        assertThatNoException()
                .isThrownBy(() -> JobStateMachine.transition(from, to));
        assertThat(JobStateMachine.transition(from, to)).isEqualTo(to);
    }

    @ParameterizedTest(name = "{0} → {1} should be INVALID")
    @CsvSource({
        "PENDING,      PROCESSING",
        "PENDING,      COMPLETED",
        "QUEUED,       COMPLETED",
        "QUEUED,       FAILED",
        "COMPLETED,    PROCESSING",
        "COMPLETED,    QUEUED",
        "DEAD_LETTERED,QUEUED",
        "DEAD_LETTERED,PROCESSING"
    })
    @DisplayName("Invalid transitions throw IllegalStateTransitionException")
    void invalidTransitions(JobStatus from, JobStatus to) {
        assertThatThrownBy(() -> JobStateMachine.transition(from, to))
                .isInstanceOf(IllegalStateTransitionException.class)
                .hasMessageContaining(from.name())
                .hasMessageContaining(to.name());
    }

    @Test
    @DisplayName("COMPLETED is a terminal state")
    void completedIsTerminal() {
        assertThat(JobStateMachine.isTerminal(JobStatus.COMPLETED)).isTrue();
    }

    @Test
    @DisplayName("DEAD_LETTERED is a terminal state")
    void deadLetteredIsTerminal() {
        assertThat(JobStateMachine.isTerminal(JobStatus.DEAD_LETTERED)).isTrue();
    }

    @Test
    @DisplayName("PENDING is NOT a terminal state")
    void pendingIsNotTerminal() {
        assertThat(JobStateMachine.isTerminal(JobStatus.PENDING)).isFalse();
    }

    @Test
    @DisplayName("allowedTransitions returns correct set for PROCESSING")
    void allowedTransitionsForProcessing() {
        var allowed = JobStateMachine.allowedTransitions(JobStatus.PROCESSING);
        assertThat(allowed).containsExactlyInAnyOrder(JobStatus.COMPLETED, JobStatus.FAILED);
    }
}
