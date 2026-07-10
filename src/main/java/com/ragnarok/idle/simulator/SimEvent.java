package com.ragnarok.idle.simulator;

import com.ragnarok.idle.math.BigNum;

/**
 * Строка журнала симуляции: игровое_время | событие | этаж | золото | DPS.
 * {@code kind} — машинный тип для отчёта, {@code text} — человекочитаемое описание.
 */
public record SimEvent(long gameTimeSeconds, SimEvent.Kind kind, String text, long floor, BigNum gold, BigNum dps) {

    public enum Kind {
        BUY_HERO, UPGRADE_HERO, UPGRADE_TAP, SAGA_RANK, BOSS_KILL, WALL, REBIRTH, MILESTONE
    }

    /** ЧЧ:ММ:СС игрового времени. */
    public String clock() {
        long h = gameTimeSeconds / 3600;
        long m = (gameTimeSeconds % 3600) / 60;
        long s = gameTimeSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    /** Строка CSV: game_time_seconds,event,floor,gold,dps. */
    public String toCsvRow() {
        return gameTimeSeconds + ",\"" + text.replace("\"", "'") + "\"," + floor + ","
                + gold.toDisplayString() + "," + dps.toDisplayString();
    }
}
