package com.ragnarok.idle.service;

import com.ragnarok.idle.domain.Player;
import com.ragnarok.idle.economy.EconomyCurves;
import com.ragnarok.idle.math.BigNum;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.DoubleSupplier;

/**
 * Общий боевой цикл для тапов и пассивного DPS героев (GDD §3.1, §3.5).
 * Модель — серия ОТДЕЛЬНЫХ ударов: каждый тап (или секунда DPS) бьёт текущую цель;
 * убил — остаток удара СГОРАЕТ, следующая цель загружается с полным HP.
 * Уровни, кратные 5, — ЦЕЛИКОМ боссовые (один бой, без обычных мобов); остальные —
 * 10 подлокаций-мобов, как обычно. На босса даётся 30 секунд; не успел — босс
 * восстанавливается на полное HP, игрок остаётся на этом же (боссовом) уровне —
 * откатываться некуда, обычных мобов там нет.
 */
@Component
public class CombatEngine {

    /** Уровни, кратные этому числу, — целиком боссовые (5, 10, 15, ...). */
    public static final int BOSS_LEVEL_PERIOD = 5;

    /** Таймер боя с боссом (GDD §3.1). */
    public static final long BOSS_TIME_LIMIT_SECONDS = 30;

    /** Защита от бесконечного цикла при аномальных данных — по факту never hit. */
    private static final long MAX_HITS_PER_REQUEST = 200_000;

    /**
     * Шанс дропа Славы с ОБЫЧНОГО (не боссового) моба, доступен только после 1-го перерождения
     * ({@code player.rebirthCount >= 1}). Синхронизировано с economy_constants.md ("Шанс дропа Славы с моба").
     */
    static final double ASH_DROP_CHANCE = 0.027;

    /**
     * Плоское количество Славы за один дроп (баланс по симулятору): раньше росло с уровнем
     * {@code floor(1 + level/100)} и на глубине переполняло экономику. Теперь фикс. {@value} —
     * за 100 уровней капает ~40-45 Славы на любой глубине (8 обычных этажей из 10 × 10 мобов ×
     * 0.027 × 2 ≈ 43; боссовые этажи мобов не дают). К 230 уровню ~98 (< 100).
     */
    static final int ASH_PER_DROP = 2;

    private final DoubleSupplier ashDropRoll;

    public CombatEngine(DoubleSupplier ashDropRoll) {
        this.ashDropRoll = ashDropRoll;
    }

    /** Итог серии ударов; изменения уровня/HP уже записаны в player. */
    public record DamageResult(int mobsKilled, boolean bossDefeated, boolean leveledUp, BigNum goldGained,
                                BigNum ashGained) {
    }

    /** true, если уровень level — целиком боссовый (каждый 5-й). */
    public static boolean isBossLevel(long level) {
        return level % BOSS_LEVEL_PERIOD == 0;
    }

    /**
     * Поддержка таймера босса в реальном времени: если игрок стоит на боссовом уровне без отметки —
     * ставит её; если 30 секунд истекли — босс восстанавливается на полное HP (игрок остаётся на
     * этом же уровне, откатываться некуда — обычных мобов на боссовом уровне нет).
     * Вызывать перед applyHits и в getState.
     */
    public void syncBossTimer(Player player, LocalDateTime now) {
        if (!isBossLevel(player.getCurrentLevel())) {
            return;
        }
        if (player.getBossStartedAt() == null) {
            player.setBossStartedAt(now);
            return;
        }
        if (Duration.between(player.getBossStartedAt(), now).getSeconds() >= BOSS_TIME_LIMIT_SECONDS) {
            failBossFight(player);
        }
    }

    /** Не успел за 30с: босс восстановлен на полное HP, уровень тот же. */
    private void failBossFight(Player player) {
        player.setBossStartedAt(null);
        player.setCurrentMobHp(EconomyCurves.bossHp(player.getCurrentLevel()));
    }

    /**
     * Переход на уровень level (автопрогресс, ручная навигация или «К боссу») — выставляет HP и
     * таймер под тип уровня: боссовый — полное HP босса и сразу стартующий 30-сек таймер; обычный —
     * HP 1-го моба, таймер выключен. Обновляет maxLevel, если level — новый рекорд.
     */
    public void enterLevel(Player player, long level) {
        player.setCurrentLevel(level);
        if (level > player.getMaxLevel()) {
            player.setMaxLevel(level);
        }
        player.setCurrentSubLevel(1);
        if (isBossLevel(level)) {
            player.setCurrentMobHp(EconomyCurves.bossHp(level));
            player.setBossStartedAt(LocalDateTime.now());
        } else {
            player.setCurrentMobHp(EconomyCurves.mobHp(level));
            player.setBossStartedAt(null);
        }
    }

    /**
     * Наносит hits отдельных ударов по damagePerHit каждый. Мутирует player, не сохраняет его.
     *
     * @param hitsAreSeconds true для пассивного DPS (тик/прокрутка): удар = секунда игрового
     *                       времени, и таймер босса отсчитывается прямо в симуляции;
     *                       false для тапов (таймер босса — только реальное время, syncBossTimer).
     */
    public DamageResult applyHits(Player player, BigNum damagePerHit, long hits, boolean hitsAreSeconds) {
        int mobsKilled = 0;
        boolean bossDefeated = false;
        boolean leveledUp = false;
        BigNum goldGained = BigNum.ZERO;
        BigNum ashGained = BigNum.ZERO;

        if (damagePerHit.isZero() || hits <= 0) {
            return new DamageResult(0, false, false, goldGained, ashGained);
        }

        long bossAttemptSeconds = 0;
        long limit = Math.min(hits, MAX_HITS_PER_REQUEST);
        for (long i = 0; i < limit; i++) {
            long level = player.getCurrentLevel();
            int subLevel = player.getCurrentSubLevel();
            boolean onBoss = isBossLevel(level);
            BigNum currentHp = player.getCurrentMobHp();

            if (damagePerHit.lt(currentHp)) {
                player.setCurrentMobHp(currentHp.subtract(damagePerHit));
                if (onBoss && hitsAreSeconds && ++bossAttemptSeconds >= BOSS_TIME_LIMIT_SECONDS) {
                    failBossFight(player);
                    bossAttemptSeconds = 0;
                }
                continue;
            }

            // враг убит; излишек урона этого удара сгорает
            goldGained = goldGained.add(onBoss
                    ? EconomyCurves.bossGold(level)
                    : EconomyCurves.goldPerMob(level));

            if (onBoss) {
                // босс — единственная цель целиком боссового уровня
                bossDefeated = true;
                leveledUp = true;
                bossAttemptSeconds = 0;
                enterLevel(player, level + 1);
            } else {
                mobsKilled++;
                if (player.getRebirthCount() >= 1 && ashDropRoll.getAsDouble() < ASH_DROP_CHANCE) {
                    ashGained = ashGained.add(BigNum.of(ASH_PER_DROP));
                }
                if (subLevel < EconomyCurves.MOBS_PER_LEVEL) {
                    player.setCurrentSubLevel(subLevel + 1);
                    player.setCurrentMobHp(EconomyCurves.mobHp(level));
                } else if (!player.isAutoAdvance()) {
                    // фарм-цикл: мобы уровня по кругу, на следующий уровень (в т.ч. боссовый) не идём
                    player.setCurrentSubLevel(1);
                    player.setCurrentMobHp(EconomyCurves.mobHp(level));
                } else {
                    // 10-й моб обычного уровня добит — дальше либо обычный, либо целиком боссовый
                    leveledUp = true;
                    enterLevel(player, level + 1);
                }
            }
        }

        player.setGold(player.getGold().add(goldGained));
        player.setAsh(player.getAsh().add(ashGained));
        return new DamageResult(mobsKilled, bossDefeated, leveledUp, goldGained, ashGained);
    }
}
