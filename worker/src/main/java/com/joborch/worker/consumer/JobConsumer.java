package com.joborch.worker.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joborch.common.messaging.JobMessage;
import com.joborch.worker.service.ExecutionResult;
import com.joborch.worker.service.JobExecutionService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JobConsumer {

    private final JobExecutionService jobExecutionService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @RabbitListener(
        queues = "${joborch.rabbitmq.queue.jobs:jobs.queue}",
        containerFactory = "rabbitListenerContainerFactory",
        ackMode = "MANUAL"
    )
    public void onJobReceived(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        JobMessage jobMessage = null;

        try {
            jobMessage = objectMapper.readValue(message.getBody(), JobMessage.class);
            log.info("Received: jobId={} type={} attempt={}",
                    jobMessage.getJobId(), jobMessage.getJobType(), jobMessage.getAttemptNumber());

            ExecutionResult result = jobExecutionService.execute(jobMessage);

            if (result instanceof ExecutionResult.Success) {
                channel.basicAck(deliveryTag, false);
                log.debug("ACK'd job {}", jobMessage.getJobId());

            } else if (result instanceof ExecutionResult.Retry retry) {

                publishToRetryQueue(jobMessage, retry.delayMs());
                channel.basicAck(deliveryTag, false); 
                log.info("Job {} moved to retry queue (delay={}ms)", jobMessage.getJobId(), retry.delayMs());

            } else if (result instanceof ExecutionResult.DeadLetter dl) {

                channel.basicNack(deliveryTag, false, false);
                log.error("Job {} dead-lettered: {}", jobMessage.getJobId(), dl.reason());
            }

        } catch (Exception e) {
            log.error("Unexpected error processing message: {}", e.getMessage(), e);

            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void publishToRetryQueue(JobMessage original, long delayMs) {
        JobMessage retryMessage = original.forRetry();

        rabbitTemplate.convertAndSend(
            com.joborch.worker.config.RabbitMQConfig.JOBS_EXCHANGE,
            com.joborch.worker.config.RabbitMQConfig.ROUTING_RETRY,
            retryMessage,
            msg -> {
                msg.getMessageProperties().setExpiration(String.valueOf(delayMs));
                msg.getMessageProperties().setPriority(
                    original.getPriority() != null ? original.getPriority().getValue() : 5);
                return msg;
            }
        );
    }
}
