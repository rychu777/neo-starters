package com.neoteric.starter.test.clock;

import com.neoteric.starter.clock.TimeZoneAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.Clock;
import java.time.Instant;
import java.util.TimeZone;

import static com.neoteric.starter.test.StarterTestProfiles.FIXED_CLOCK;

@Slf4j
@Configuration
@Profile(FIXED_CLOCK)
@AutoConfigureAfter(TimeZoneAutoConfiguration.class)
public class FixedClockAutoConfiguration {

    @Bean
    @Primary
    Clock clock() {
        return new StaticClock(Instant.parse("2010-01-10T10:00:00Z"), TimeZone.getDefault().toZoneId());
    }

}