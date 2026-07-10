package com.ragnarok.idle.repository;

import com.ragnarok.idle.domain.SiegeBoss;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiegeBossRepository extends JpaRepository<SiegeBoss, Long> {

    /** Активный босс недели по порядку ротации; защитники — одним запросом (без N+1). */
    @EntityGraph(attributePaths = "defenders")
    Optional<SiegeBoss> findByRotationOrder(int rotationOrder);

    /** Детали босса по бизнес-ключу; защитники — одним запросом (без N+1). */
    @EntityGraph(attributePaths = "defenders")
    Optional<SiegeBoss> findByBossKey(String bossKey);

    /** Всё расписание по порядку ротации (защитники не нужны — LAZY не трогаем). */
    List<SiegeBoss> findAllByOrderByRotationOrderAsc();
}
