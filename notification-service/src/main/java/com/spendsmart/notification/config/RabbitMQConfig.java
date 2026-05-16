package com.spendsmart.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.listener.FatalExceptionStrategy;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ErrorHandler;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE = "notification.queue";
    public static final String EXCHANGE = "notification.exchange";
    public static final String ROUTING_KEY = "notification.routing.key";

    @Bean
    public Queue queue() {
        // durable=true so the queue survives broker restarts
        return new Queue(QUEUE, true);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate template(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }

    /**
     * Custom error handler: non-fatal exceptions are logged but do NOT crash
     * the application.  Only truly unrecoverable errors (e.g. bad message
     * format declared fatal by FatalExceptionStrategy) will propagate.
     */
    @Bean
    public ErrorHandler rabbitErrorHandler() {
        return new ConditionalRejectingErrorHandler(new CustomFatalExceptionStrategy());
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter());
        factory.setErrorHandler(rabbitErrorHandler());
        // Do NOT treat missing queues as fatal — allows app to start even when
        // RabbitMQ is temporarily unavailable or credentials are being set up.
        factory.setMissingQueuesFatal(false);
        return factory;
    }

    /**
     * Strategy that marks only listener-execution failures as fatal.
     * Connection / auth errors are treated as non-fatal so the container
     * keeps retrying instead of crashing the Spring context.
     */
    static class CustomFatalExceptionStrategy implements FatalExceptionStrategy {
        @Override
        public boolean isFatal(Throwable t) {
            if (t instanceof ListenerExecutionFailedException ex) {
                Throwable cause = ex.getCause();
                return cause instanceof ClassCastException
                        || cause instanceof ClassNotFoundException;
            }
            return false;
        }
    }
}
