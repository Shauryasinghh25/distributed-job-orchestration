package com.joborch.worker.handler;

import com.joborch.common.domain.JobType;
import com.joborch.common.messaging.JobMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationJobHandler implements JobHandler {
    @Override
    public void handle(JobMessage message) throws Exception {
        String msg = message.getPayload().getOrDefault("message", "");
        String device = message.getPayload().getOrDefault("deviceToken", "unknown");
        log.info("[NOTIFICATION] Sending push to device={} msg='{}' jobId={}", device, msg, message.getJobId());
        Thread.sleep(100);
        log.info("[NOTIFICATION] Push delivered to {}", device);
    }
    public JobType getType() { return JobType.NOTIFICATION; }
}
