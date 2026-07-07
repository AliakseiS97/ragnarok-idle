package com.ragnarok.idle.config;

import java.util.function.DoubleSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Источник случайности для игровых бросков (напр. дроп Пепла с мобов, {@link com.ragnarok.idle.service.CombatEngine}).
 * Вынесен в бин, чтобы тесты могли подменить его через {@code @MockBean} и получить детерминированный бросок.
 */
@Configuration
public class RandomnessConfig {

    @Bean
    public DoubleSupplier ashDropRoll() {
        return Math::random;
    }
}
