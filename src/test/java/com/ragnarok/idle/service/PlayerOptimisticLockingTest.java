package com.ragnarok.idle.service;

import com.ragnarok.idle.domain.Player;
import com.ragnarok.idle.math.BigNum;
import com.ragnarok.idle.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Баг ребёрта (п.1): без @Version на Player конкурентный read-modify-write (напр. тап,
 * зависший в полёте, и /rebirth в тот же момент) молча теряет одно из изменений — "последний
 * записавший побеждает". Этот тест доказывает, что версионирование ловит именно такую гонку:
 * два независимо загруженных экземпляра одной строки, оба сохраняются — второй save() обязан
 * упасть с ObjectOptimisticLockingFailureException, а не тихо перезаписать первый.
 */
@SpringBootTest
class PlayerOptimisticLockingTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private PlayerRepository playerRepository;

    @Test
    void concurrentWritesToSamePlayerRowFailLoudlyInsteadOfLosingData() {
        authService.register("version_race", "password123");

        // симулируем два "одновременных" запроса: каждый загрузил свою копию строки ДО того,
        // как другой её изменил (ровно то, что происходит между /rebirth и зависшим в полёте /battle/tap)
        Player copyA = playerRepository.findByUsername("version_race").orElseThrow();
        Player copyB = playerRepository.findByUsername("version_race").orElseThrow();

        copyA.setGold(BigNum.of(111));
        playerRepository.saveAndFlush(copyA); // "выигрывает" первым — версия строки в БД бампается

        copyB.setGold(BigNum.of(999)); // всё ещё держит старую версию
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> playerRepository.saveAndFlush(copyB),
                "запись по устаревшей версии обязана упасть, а не молча перезаписать copyA");
    }
}
