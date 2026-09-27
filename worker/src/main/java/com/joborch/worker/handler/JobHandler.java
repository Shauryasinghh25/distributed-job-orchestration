package com.joborch.worker.handler;

import com.joborch.common.messaging.JobMessage;

public interface JobHandler {
    void handle(JobMessage message) throws Exception;
}
