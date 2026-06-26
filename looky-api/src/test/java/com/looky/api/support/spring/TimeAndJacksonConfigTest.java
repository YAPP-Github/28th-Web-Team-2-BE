package com.looky.api.support.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Clock;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {TimeConfig.class, JacksonConfig.class})
class TimeAndJacksonConfigTest {

    @Autowired
    private Clock clock;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void clockAndObjectMapperUseKst() {
        assertEquals(ZoneId.of("Asia/Seoul"), clock.getZone());
        assertEquals("Asia/Seoul", objectMapper.getSerializationConfig().getTimeZone().getID());
    }
}
