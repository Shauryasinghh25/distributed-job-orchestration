package com.joborch.worker.handler;

import com.joborch.common.domain.JobType;
import com.joborch.common.messaging.JobMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class JobHandlerRegistry {

    private final List<JobHandler> handlers;
    private final Map<JobType, JobHandler> registry = new EnumMap<>(JobType.class);

    @PostConstruct
    public void init() {
        for (JobHandler handler : handlers) {
            JobType type = getType(handler);
            registry.put(type, handler);
            log.info("Registered handler {} for type {}", handler.getClass().getSimpleName(), type);
        }
        log.info("Handler registry initialized with {} handlers", registry.size());
    }

    private JobType getType(JobHandler handler) {

        if (handler instanceof EmailJobHandler h)        return h.getType();
        if (handler instanceof ReportJobHandler h)       return h.getType();
        if (handler instanceof NotificationJobHandler h) return h.getType();
        if (handler instanceof DataProcessingJobHandler h) return h.getType();
        throw new IllegalStateException("Unknown handler type: " + handler.getClass());
    }

    public void handle(JobMessage message) throws Exception {
        JobHandler handler = registry.get(message.getJobType());
        if (handler == null) {
            throw new IllegalArgumentException("No handler registered for job type: " + message.getJobType());
        }
        handler.handle(message);
    }

    public boolean hasHandler(JobType type) {
        return registry.containsKey(type);
    }
}
