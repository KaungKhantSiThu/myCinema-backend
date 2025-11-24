package com.kkst.mycinema.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@Getter
public class MetricsConfig {

    private final Counter bookingSuccessCounter;
    private final Counter bookingFailureCounter;
    private final Counter bookingCancellationCounter;
    private final Timer bookingDurationTimer;

    public MetricsConfig(MeterRegistry meterRegistry) {
        this.bookingSuccessCounter = Counter.builder("booking.success")
                .description("Number of successful bookings")
                .tag("type", "success")
                .register(meterRegistry);

        this.bookingFailureCounter = Counter.builder("booking.failure")
                .description("Number of failed bookings")
                .tag("type", "failure")
                .register(meterRegistry);

        this.bookingCancellationCounter = Counter.builder("booking.cancellation")
                .description("Number of cancelled bookings")
                .tag("type", "cancellation")
                .register(meterRegistry);

        this.bookingDurationTimer = Timer.builder("booking.duration")
                .description("Time taken to complete bookings")
                .register(meterRegistry);
    }
}

