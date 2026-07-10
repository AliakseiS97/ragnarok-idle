package com.ragnarok.idle.repository;

import com.ragnarok.idle.domain.SiegeBoss;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Интеграционный тест на сид осадных боссов (V12). БД — H2 в PostgreSQL-режиме, схему и данные
 * накатывает Flyway (как и вся тестовая среда проекта).
 */
@SpringBootTest
class SiegeBossRepositoryTest {

    @Autowired
    private SiegeBossRepository siegeBossRepository;

    @Test
    void sevenBossesWithUniqueRotationOrder() {
        assertEquals(7, siegeBossRepository.findAll().size());

        Set<Integer> orders = siegeBossRepository.findAll().stream()
                .map(SiegeBoss::getRotationOrder)
                .collect(Collectors.toSet());
        // порядок ротации 1..7 — все различны и покрывают полный цикл
        assertEquals(IntStream.rangeClosed(1, 7).boxed().collect(Collectors.toSet()), orders);
    }

    @Test
    void bossesLoadTheirDefenders() {
        // findByBossKey тянет защитников через @EntityGraph — доступ к LAZY-связи вне транзакции безопасен
        assertEquals(5, siegeBossRepository.findByBossKey("siege_fenrir").orElseThrow().getDefenders().size());
        assertEquals(9, siegeBossRepository.findByBossKey("siege_jormungandr").orElseThrow().getDefenders().size());
    }

    @Test
    void defendersAreOrderedByPosition() {
        SiegeBoss fenrir = siegeBossRepository.findByBossKey("siege_fenrir").orElseThrow();
        // @OrderBy("position ASC") — первый защитник Фенрира это Скёлль (позиция 1)
        assertEquals("Скёлль", fenrir.getDefenders().get(0).getNameRu());
    }

    @Test
    void findByRotationOrderReturnsExpectedBoss() {
        // порядок 4 = Сурт (четвёртый в недельной ротации)
        assertEquals("siege_surtr", siegeBossRepository.findByRotationOrder(4).orElseThrow().getBossKey());
    }
}
