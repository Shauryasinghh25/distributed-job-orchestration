package com.joborch.worker.handler;

import com.joborch.common.domain.JobType;
import com.joborch.common.messaging.JobMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailJobHandler implements JobHandler {
    @Override
    public void handle(JobMessage message) throws Exception {
        String recipient = message.getPayload().getOrDefault("recipient", "unknown");
        String subject   = message.getPayload().getOrDefault("subject", "(no subject)");
        log.info("[EMAIL] Sending email to={} subject='{}' jobId={}", recipient, subject, message.getJobId());
        Thread.sleep(300); 
        log.info("[EMAIL] Sent successfully to {}", recipient);
    }
    public JobType getType() { return JobType.EMAIL; }
}
