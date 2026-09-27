package com.joborch.producer.repository;

import com.joborch.common.domain.JobStatus;
import com.joborch.producer.domain.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    List<Job> findByStatus(JobStatus status);

    Optional<Job> findByIdempotencyKey(String idempotencyKey);

    @Query("""
        SELECT j FROM Job j
        WHERE j.status = 'PENDING'
        AND (j.scheduledAt IS NULL OR j.scheduledAt <= :now)
        """)
    List<Job> findReadyToQueue(@Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Job j SET j.status = :status WHERE j.id = :jobId")
    int updateStatus(@Param("jobId") UUID jobId, @Param("status") JobStatus status);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Job j
        SET j.status = :status,
            j.attemptCount = j.attemptCount + 1,
            j.errorMessage = :errorMessage
        WHERE j.id = :jobId
        """)
    int updateStatusWithError(@Param("jobId") UUID jobId,
                              @Param("status") JobStatus status,
                              @Param("errorMessage") String errorMessage);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Job j SET j.status = :status, j.attemptCount = j.attemptCount + 1 WHERE j.id = :jobId")
    int updateStatusAndIncrementAttempt(@Param("jobId") UUID jobId, @Param("status") JobStatus status);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
