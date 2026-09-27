package com.joborch.worker.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String JOBS_EXCHANGE    = "jobs.exchange";
    public static final String JOBS_QUEUE       = "jobs.queue";
    public static final String JOBS_RETRY_QUEUE = "jobs.retry.queue";
    public static final String JOBS_DLQ         = "jobs.dlq.queue";
    public static final String ROUTING_JOBS     = "jobs.queue";
    public static final String ROUTING_RETRY    = "jobs.retry";
    public static final String ROUTING_DLQ      = "jobs.dlq";

    @Bean
    public DirectExchange jobsExchange() {
        return ExchangeBuilder.directExchange(JOBS_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue jobsQueue() {
        return QueueBuilder.durable(JOBS_QUEUE)
                .withArgument("x-max-priority", 10)
                .withArgument("x-dead-letter-exchange", JOBS_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ROUTING_DLQ)
                .build();
    }

    @Bean
    public Queue jobsRetryQueue() {
        return QueueBuilder.durable(JOBS_RETRY_QUEUE)
                .withArgument("x-dead-letter-exchange", JOBS_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ROUTING_JOBS)
                .withArgument("x-message-ttl", 600_000)
                .build();
    }

    @Bean
    public Queue jobsDlq() {
        return QueueBuilder.durable(JOBS_DLQ).build();
    }

    @Bean public Binding jobsBinding()  { return BindingBuilder.bind(jobsQueue()).to(jobsExchange()).with(ROUTING_JOBS); }
    @Bean public Binding retryBinding() { return BindingBuilder.bind(jobsRetryQueue()).to(jobsExchange()).with(ROUTING_RETRY); }
    @Bean public Binding dlqBinding()   { return BindingBuilder.bind(jobsDlq()).to(jobsExchange()).with(ROUTING_DLQ); }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate t = new RabbitTemplate(connectionFactory);
        t.setMessageConverter(jsonMessageConverter());
        t.setExchange(JOBS_EXCHANGE);
        return t;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(connectionFactory);
        f.setMessageConverter(jsonMessageConverter());
        f.setAcknowledgeMode(AcknowledgeMode.MANUAL);  
        f.setPrefetchCount(1);
        f.setConcurrentConsumers(1);
        f.setMaxConcurrentConsumers(5);

        f.setDefaultRequeueRejected(false);  
        return f;
    }
}
