package com.ragnarok.idle.simulator;

import java.util.ArrayList;
import java.util.List;

/**
 * Детекция «стен» прогресса: если frontier (макс. пройденный этаж) не растёт дольше
 * {@code thresholdSeconds}, фиксируется стена. Длительность стены = всё время застоя на этаже.
 * Вынесен отдельным классом, чтобы тестировать детекцию независимо от полной симуляции.
 */
public class WallTracker {

    /** Зафиксированная стена: этаж, когда игрок на него попал, когда пробил (или -1 — ещё стоит). */
    public static final class Wall {
        public final long floor;
        public final long startSeconds;
        long endSeconds = -1;

        Wall(long floor, long startSeconds) {
            this.floor = floor;
            this.startSeconds = startSeconds;
        }

        /** Длительность застоя; для непробитой стены считается до {@code asOfSeconds}. */
        public long durationSeconds(long asOfSeconds) {
            return (endSeconds < 0 ? asOfSeconds : endSeconds) - startSeconds;
        }
    }

    private final long thresholdSeconds;
    private final List<Wall> walls = new ArrayList<>();

    private long currentFloor = 0;
    private long floorEnteredAt = 0;
    private Wall openWall;

    public WallTracker(long thresholdSeconds) {
        this.thresholdSeconds = thresholdSeconds;
    }

    /**
     * Вызывать каждый игровой тик. Возвращает только что зафиксированную стену (если застой этого
     * этажа впервые превысил порог) — вызывающий пишет событие; иначе {@code null}.
     */
    public Wall onTick(long gameTimeSeconds, long maxFloor) {
        if (maxFloor > currentFloor) {
            if (openWall != null) {           // пробили стену — закрываем её длительность
                openWall.endSeconds = gameTimeSeconds;
                openWall = null;
            }
            currentFloor = maxFloor;
            floorEnteredAt = gameTimeSeconds;
            return null;
        }
        if (openWall == null && gameTimeSeconds - floorEnteredAt > thresholdSeconds) {
            openWall = new Wall(currentFloor, floorEnteredAt);
            walls.add(openWall);
            return openWall;
        }
        return null;
    }

    /** Сброс базы отсчёта на новый забег (после ребёрта frontier считается заново); стены сохраняются. */
    public void reset(long gameTimeSeconds) {
        currentFloor = 0;
        floorEnteredAt = gameTimeSeconds;
        openWall = null;
    }

    public List<Wall> walls() {
        return walls;
    }
}
