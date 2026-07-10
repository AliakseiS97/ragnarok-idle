package com.ragnarok.idle.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Источник времени приложения. Вынесен в бин, чтобы сервисы (напр. ротация осадных боссов,
 * {@link com.ragnarok.idle.service.SiegeScheduleService}) не звали {@code LocalDate.now()} напрямую —
 * тесты подменяют {@link Clock#fixed} и получают детерминированную «текущую неделю».
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
