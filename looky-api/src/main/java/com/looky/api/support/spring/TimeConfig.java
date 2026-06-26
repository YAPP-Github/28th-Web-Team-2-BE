package com.looky.api.support.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class TimeConfig {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @PostConstruct
    void applyDefaultTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone(KST));
    }

    @Bean
    public Clock clock() {
        return Clock.system(KST);
    }
}
