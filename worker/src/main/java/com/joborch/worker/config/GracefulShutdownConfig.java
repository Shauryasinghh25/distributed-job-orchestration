package com.joborch.worker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GracefulShutdownConfig implements DisposableBean {

    private final ApplicationContext applicationContext;

    @Value("${joborch.worker.shutdown.timeout-ms:30000}")
    private long shutdownTimeoutMs;

    public GracefulShutdownConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void destroy() {
        log.info("Graceful shutdown initiated. Stopping RabbitMQ listener containers...");

        applicationContext.getBeansOfType(SimpleMessageListenerContainer.class)
                .forEach((name, container) -> {
                    log.info("Stopping listener container: {}", name);
                    container.stop();
                    log.info("Listener container {} stopped", name);
                });

        log.info("All listener containers stopped. Worker shutdown complete.");
    }
}
