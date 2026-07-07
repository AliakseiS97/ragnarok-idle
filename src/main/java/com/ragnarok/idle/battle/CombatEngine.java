package com.ragnarok.idle.battle;

import com.ragnarok.idle.economy.EconomyCurves;
import com.ragnarok.idle.math.BigNum;
import com.ragnarok.idle.player.Player;
import org.springframework.stereotype.Component;

/**
 * Общий боевой цикл: применяет урон (тапы ИЛИ пассивный DPS героев) к текущему
 * мобу/боссу игрока, убивая мобов и продвигая уровни (GDD §3.1, §3.5).
 * Один источник истины для тапов и idle-прогресса — формулы не дублируются.
 */
@Component
public class CombatEngine {

    /** Босс сидит на 11-й позиции: 10 обычных мобов (GDD §3.1) + таймер-босс. */
    public static final int BOSS_SUB_LEVEL = EconomyCurves.MOBS_PER_LEVEL + 1;

    /** Защита от бесконечного цикла при аномальных данных — по факту never hit. */
    private static final int MAX_KILLS_PER_REQUEST = 100_000;

    /** Итог применения урона; изменения уровня/HP уже записаны в player. */
    public record DamageResult(int mobsKilled, boolean bossDefeated, boolean leveledUp, BigNum goldGained) {
    }

    /** Наносит damage текущему мобу игрока, перенося излишек на следующих. Мутирует player, не сохраняет его. */
    public DamageResult applyDamage(Player player, BigNum damage) {
        BigNum remainingDamage = damage;

        int mobsKilled = 0;
        boolean bossDefeated = false;
        boolean leveledUp = false;
        BigNum goldGained = BigNum.ZERO;

        int killsThisRequest = 0;
        while (remainingDamage.gte(player.getCurrentMobHp())) {
            if (++killsThisRequest > MAX_KILLS_PER_REQUEST) break;

            remainingDamage = remainingDamage.subtract(player.getCurrentMobHp());
            boolean isBoss = player.getCurrentSubLevel() == BOSS_SUB_LEVEL;

            goldGained = goldGained.add(isBoss
                    ? EconomyCurves.bossGold(player.getCurrentLevel())
                    : EconomyCurves.goldPerMob(player.getCurrentLevel()));

            if (isBoss) {
                bossDefeated = true;
                leveledUp = true;
                long nextLevel = player.getCurrentLevel() + 1;
                player.setCurrentLevel(nextLevel);
                if (nextLevel > player.getMaxLevel()) {
                    player.setMaxLevel(nextLevel);
                }
                player.setCurrentSubLevel(1);
                player.setCurrentMobHp(EconomyCurves.mobHp(nextLevel));
            } else {
                mobsKilled++;
                int nextSubLevel = player.getCurrentSubLevel() + 1;
                player.setCurrentSubLevel(nextSubLevel);
                player.setCurrentMobHp(nextSubLevel == BOSS_SUB_LEVEL
                        ? EconomyCurves.bossHp(player.getCurrentLevel())
                        : EconomyCurves.mobHp(player.getCurrentLevel()));
            }
        }

        if (!remainingDamage.isZero()) {
            player.setCurrentMobHp(player.getCurrentMobHp().subtract(remainingDamage));
        }

        player.setGold(player.getGold().add(goldGained));
        return new DamageResult(mobsKilled, bossDefeated, leveledUp, goldGained);
    }
}
