package com.ragnarok.idle.service;

import com.ragnarok.idle.domain.Player;
import com.ragnarok.idle.economy.EconomyCurves;
import com.ragnarok.idle.math.BigNum;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Общий боевой цикл для тапов и пассивного DPS героев (GDD §3.1, §3.5).
 * Модель — серия ОТДЕЛЬНЫХ ударов: каждый тап (или секунда DPS) бьёт текущего
 * моба; убил — остаток удара СГОРАЕТ, следующий моб загружается с полным HP.
 * Босс — 10-я подлокация каждого 5-го уровня, на него даётся 30 секунд;
 * не успел — босс восстанавливается, игрок падает на мобов уровня (фарм).
 */
@Component
public class CombatEngine {

    /** Босс — 10-я подлокация каждого 5-го уровня (5, 10, 15, ...). */
    public static final int BOSS_LEVEL_PERIOD = 5;

    /** Таймер боя с боссом (GDD §3.1). */
    public static final long BOSS_TIME_LIMIT_SECONDS = 30;

    /** Защита от бесконечного цикла при аномальных данных — по факту never hit. */
    private static final long MAX_HITS_PER_REQUEST = 200_000;

    /** Итог серии ударов; изменения уровня/HP уже записаны в player. */
    public record DamageResult(int mobsKilled, boolean bossDefeated, boolean leveledUp, BigNum goldGained) {
    }

    /** true, если на уровне level есть босс (каждый 5-й уровень). */
    public static boolean isBossLevel(long level) {
        return level % BOSS_LEVEL_PERIOD == 0;
    }

    /** true, если подлокация subLevel уровня level — босс. */
    public static boolean isBossSlot(long level, int subLevel) {
        return isBossLevel(level) && subLevel == EconomyCurves.MOBS_PER_LEVEL;
    }

    /** Последняя ОБЫЧНАЯ подлокация уровня: 9 на босс-уровне (10-я — босс), иначе 10. */
    private static int lastMobSlot(long level) {
        return isBossLevel(level) ? EconomyCurves.MOBS_PER_LEVEL - 1 : EconomyCurves.MOBS_PER_LEVEL;
    }

    /** Полное HP врага на подлокации subLevel уровня level. */
    public static BigNum slotHp(long level, int subLevel) {
        return isBossSlot(level, subLevel) ? EconomyCurves.bossHp(level) : EconomyCurves.mobHp(level);
    }

    /**
     * Поддержка таймера босса в реальном времени: если игрок стоит на боссе без отметки —
     * ставит её; если 30 секунд истекли — босс восстанавливается, игрок падает на 1-ю
     * подлокацию уровня (фарм мобов «перед боссом»). Вызывать перед applyHits и в getState.
     */
    public void syncBossTimer(Player player, LocalDateTime now) {
        if (!isBossSlot(player.getCurrentLevel(), player.getCurrentSubLevel())) {
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

    /** Не успел за 30с: босс восстановлен, игрок — на 1-ю подлокацию уровня. */
    private void failBossFight(Player player) {
        player.setBossStartedAt(null);
        player.setCurrentSubLevel(1);
        player.setCurrentMobHp(EconomyCurves.mobHp(player.getCurrentLevel()));
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

        if (damagePerHit.isZero() || hits <= 0) {
            return new DamageResult(0, false, false, goldGained);
        }

        long bossAttemptSeconds = 0;
        long limit = Math.min(hits, MAX_HITS_PER_REQUEST);
        for (long i = 0; i < limit; i++) {
            long level = player.getCurrentLevel();
            int subLevel = player.getCurrentSubLevel();
            boolean onBoss = isBossSlot(level, subLevel);
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
                bossDefeated = true;
                leveledUp = true;
                player.setBossStartedAt(null);
                advanceToLevel(player, level + 1);
            } else {
                mobsKilled++;
                if (subLevel < lastMobSlot(level)) {
                    player.setCurrentSubLevel(subLevel + 1);
                    player.setCurrentMobHp(EconomyCurves.mobHp(level));
                } else if (!player.isAutoAdvance()) {
                    // фарм-цикл: мобы уровня по кругу, к боссу/следующему уровню не идём
                    player.setCurrentSubLevel(1);
                    player.setCurrentMobHp(EconomyCurves.mobHp(level));
                } else if (isBossLevel(level)) {
                    player.setCurrentSubLevel(EconomyCurves.MOBS_PER_LEVEL);
                    player.setCurrentMobHp(EconomyCurves.bossHp(level));
                    player.setBossStartedAt(LocalDateTime.now());
                    bossAttemptSeconds = 0;
                } else {
                    leveledUp = true;
                    advanceToLevel(player, level + 1);
                }
            }
        }

        player.setGold(player.getGold().add(goldGained));
        return new DamageResult(mobsKilled, bossDefeated, leveledUp, goldGained);
    }

    private void advanceToLevel(Player player, long nextLevel) {
        player.setCurrentLevel(nextLevel);
        if (nextLevel > player.getMaxLevel()) {
            player.setMaxLevel(nextLevel);
        }
        player.setCurrentSubLevel(1);
        player.setCurrentMobHp(EconomyCurves.mobHp(nextLevel));
    }
}
