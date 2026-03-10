package com.travel.travelapp.config;

import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.context.event.ContextClosedEvent;

@Configuration
public class ApplicationAvailabilityConfig {

    private final ApplicationContext applicationContext;
    private final AtomicReference<ReadinessState> readinessState = new AtomicReference<>(ReadinessState.REFUSING_TRAFFIC);

    public ApplicationAvailabilityConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean(name = "readinessStateHealthIndicator")
    public HealthIndicator readinessStateHealthIndicator() {
        return () -> readinessState.get() == ReadinessState.ACCEPTING_TRAFFIC
                ? Health.up().withDetail("state", ReadinessState.ACCEPTING_TRAFFIC.name()).build()
                : Health.outOfService().withDetail("state", ReadinessState.REFUSING_TRAFFIC.name()).build();
    }

    @Bean
    public ApplicationRunner readinessStateInitializer() {
        return args -> {
            readinessState.set(ReadinessState.ACCEPTING_TRAFFIC);
            AvailabilityChangeEvent.publish(applicationContext, ReadinessState.ACCEPTING_TRAFFIC);
        };
    }

    @Bean
    public ApplicationListener<ContextClosedEvent> contextClosedListener() {
        return new ApplicationListener<>() {
            @Override
            public void onApplicationEvent(ContextClosedEvent event) {
                readinessState.set(ReadinessState.REFUSING_TRAFFIC);
                AvailabilityChangeEvent.publish(applicationContext, ReadinessState.REFUSING_TRAFFIC);
            }
        };
    }
}
