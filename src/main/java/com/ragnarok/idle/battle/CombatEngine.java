package com.ragnarok.idle.battle;

import com.ragnarok.idle.economy.EconomyCurves;
import com.ragnarok.idle.math.BigNum;
import com.ragnarok.idle.player.Player;
import org.springframework.stereotype.Component;

/**
 * Общий боевой цикл для тапов и пассивного DPS героев (GDD §3.1, §3.5).
 * Модель — серия ОТДЕЛЬНЫХ ударов: каждый тап (или секунда DPS) бьёт текущего
 * моба; убил — остаток удара сгорает, следующий моб загружается с полным HP.
 * Переноса урона между мобами нет.
 */
@Component
public class CombatEngine {

    /** Босс — 10-я подлокация каждого 5-го уровня (5, 10, 15, ...). */
    public static final int BOSS_LEVEL_PERIOD = 5;

    /** Защита от бесконечного цикла при аномальных данных — по факту never hit. */
    private static final long MAX_HITS_PER_REQUEST = 200_000;

    /** Итог серии ударов; изменения уровня/HP уже записаны в player. */
    public record DamageResult(int mobsKilled, boolean bossDefeated, boolean leveledUp, BigNum goldGained) {
    }

    /** true, если подлокация subLevel уровня level — босс. */
    public static boolean isBossSlot(long level, int subLevel) {
        return level % BOSS_LEVEL_PERIOD == 0 && subLevel == EconomyCurves.MOBS_PER_LEVEL;
    }

    /** Полное HP врага на подлокации subLevel уровня level. */
    public static BigNum slotHp(long level, int subLevel) {
        return isBossSlot(level, subLevel) ? EconomyCurves.bossHp(level) : EconomyCurves.mobHp(level);
    }

    /** Наносит hits отдельных ударов по damagePerHit каждый. Мутирует player, не сохраняет его. */
    public DamageResult applyHits(Player player, BigNum damagePerHit, long hits) {
        int mobsKilled = 0;
        boolean bossDefeated = false;
        boolean leveledUp = false;
        BigNum goldGained = BigNum.ZERO;

        if (damagePerHit.isZero() || hits <= 0) {
            return new DamageResult(0, false, false, goldGained);
        }

        long limit = Math.min(hits, MAX_HITS_PER_REQUEST);
        for (long i = 0; i < limit; i++) {
            BigNum currentHp = player.getCurrentMobHp();
            if (damagePerHit.lt(currentHp)) {
                player.setCurrentMobHp(currentHp.subtract(damagePerHit));
                continue;
            }

            // враг убит; излишек урона этого удара сгорает
            boolean isBoss = isBossSlot(player.getCurrentLevel(), player.getCurrentSubLevel());
            goldGained = goldGained.add(isBoss
                    ? EconomyCurves.bossGold(player.getCurrentLevel())
                    : EconomyCurves.goldPerMob(player.getCurrentLevel()));
            if (isBoss) {
                bossDefeated = true;
            } else {
                mobsKilled++;
            }

            if (player.getCurrentSubLevel() >= EconomyCurves.MOBS_PER_LEVEL) {
                leveledUp = true;
                long nextLevel = player.getCurrentLevel() + 1;
                player.setCurrentLevel(nextLevel);
                if (nextLevel > player.getMaxLevel()) {
                    player.setMaxLevel(nextLevel);
                }
                player.setCurrentSubLevel(1);
                player.setCurrentMobHp(EconomyCurves.mobHp(nextLevel));
            } else {
                int nextSubLevel = player.getCurrentSubLevel() + 1;
                player.setCurrentSubLevel(nextSubLevel);
                player.setCurrentMobHp(slotHp(player.getCurrentLevel(), nextSubLevel));
            }
        }

        player.setGold(player.getGold().add(goldGained));
        return new DamageResult(mobsKilled, bossDefeated, leveledUp, goldGained);
    }
}
