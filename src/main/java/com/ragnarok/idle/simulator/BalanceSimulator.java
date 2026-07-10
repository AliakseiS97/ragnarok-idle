package com.ragnarok.idle.simulator;

import com.ragnarok.idle.economy.EconomyCurves;
import com.ragnarok.idle.math.BigNum;
import com.ragnarok.idle.service.CombatEngine;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Симулятор баланса прогрессии: «играет» в игру по реальным формулам проекта (чистая математика,
 * без Spring/БД) и печатает таймлайн прогресса, стены и итоговый отчёт.
 *
 * <p>Запуск: {@code mvn -q compile exec:java -Dexec.mainClass=com.ragnarok.idle.simulator.BalanceSimulator
 * -Dexec.args="clicksPerSecond=5 wallThresholdSeconds=600 maxSimulatedHours=24 rebirths=3"}
 */
public final class BalanceSimulator {

    private static final long BOSS_TIME_LIMIT = CombatEngine.BOSS_TIME_LIMIT_SECONDS; // 30
    private static final long[] FLOOR_MILESTONES = {10, 25, 50, 100};

    /** Параметры запуска. */
    public record Config(int clicksPerSecond, long wallThresholdSeconds, long maxSimulatedHours,
                         int rebirthsToStop, long purchaseIntervalSeconds, long randomSeed) {
        public static Config defaults() {
            return new Config(5, 600, 24, 3, 10, 42L);
        }
    }

    private enum Mode { PUSH, FARM }

    private final Config config;
    private final SimState state;
    private final WallTracker walls;
    private final List<SimEvent> events = new ArrayList<>();

    // трекинг для отчёта
    private final Map<Long, Long> firstBossKillAt = new TreeMap<>();
    private final Map<Long, Long> floorMilestoneAt = new LinkedHashMap<>();
    private final List<Long> rebirthAt = new ArrayList<>();
    private long secondsPushing;
    private long secondsFarming;

    // состояние забега
    private long runFrontier = 1;
    private long globalMaxBossKilled;
    private Mode mode = Mode.PUSH;
    private long stuckBoss;
    private long bossElapsed;

    public BalanceSimulator(Config config) {
        this.config = config;
        this.state = new SimState(config.clicksPerSecond(), new Random(config.randomSeed())::nextDouble);
        this.walls = new WallTracker(config.wallThresholdSeconds());
    }

    // ---------- Точка входа ----------

    public static void main(String[] args) {
        // кириллица в консоль/редирект независимо от платформенной кодировки (Windows cp1251)
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        Config config = parseArgs(args);
        BalanceSimulator sim = new BalanceSimulator(config);
        sim.run(true);
        sim.writeCsv(Path.of("target", "simulation-report.csv"));
        sim.printReport();
    }

    static Config parseArgs(String[] args) {
        Config c = Config.defaults();
        int clicks = c.clicksPerSecond();
        long wall = c.wallThresholdSeconds();
        long hours = c.maxSimulatedHours();
        int rebirths = c.rebirthsToStop();
        long interval = c.purchaseIntervalSeconds();
        long seed = c.randomSeed();
        for (String arg : args) {
            String[] kv = arg.split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            String v = kv[1].trim();
            switch (kv[0].trim()) {
                case "clicksPerSecond" -> clicks = Integer.parseInt(v);
                case "wallThresholdSeconds" -> wall = Long.parseLong(v);
                case "maxSimulatedHours" -> hours = Long.parseLong(v);
                case "rebirths" -> rebirths = Integer.parseInt(v);
                case "purchaseIntervalSeconds" -> interval = Long.parseLong(v);
                case "randomSeed" -> seed = Long.parseLong(v);
                default -> { }
            }
        }
        return new Config(clicks, wall, hours, rebirths, interval, seed);
    }

    // ---------- Основной цикл ----------

    /** Прогоняет симуляцию до maxSimulatedHours или заданного числа перерождений. */
    public void run(boolean logToConsole) {
        long maxSeconds = config.maxSimulatedHours() * 3600;
        long prevLevel = 1;
        for (long t = 0; t < maxSeconds && rebirthAt.size() < config.rebirthsToStop(); t++) {
            if (t % config.purchaseIntervalSeconds() == 0) {
                for (PurchaseStrategy.Action a : PurchaseStrategy.runCycle(state)) {
                    log(logToConsole, t, a.kind(), a.text());
                }
            }

            if (state.rebirthReady()) {
                BigNum ashGained = state.rebirth();
                rebirthAt.add(t);
                log(logToConsole, t, SimEvent.Kind.REBIRTH,
                        "ПЕРЕРОЖДЕНИЕ #%d: +Слава %s (всего %s)"
                                .formatted(rebirthAt.size(), ashGained.toDisplayString(), state.ash().toDisplayString()));
                runFrontier = 1;
                prevLevel = 1;
                mode = Mode.PUSH;
                bossElapsed = 0;
                walls.reset(t);
                continue;
            }

            combatSecond();

            long cur = state.currentLevel();
            if (CombatEngine.isBossLevel(prevLevel) && cur > prevLevel && !firstBossKillAt.containsKey(prevLevel)) {
                firstBossKillAt.put(prevLevel, t);
            }
            if (CombatEngine.isBossLevel(prevLevel) && cur > prevLevel && prevLevel > globalMaxBossKilled) {
                globalMaxBossKilled = prevLevel;
                log(logToConsole, t, SimEvent.Kind.BOSS_KILL, "Убит босс этажа " + prevLevel);
            }
            if (cur > runFrontier) {
                runFrontier = cur;
                recordFloorMilestones(logToConsole, t);
            }
            WallTracker.Wall newWall = walls.onTick(t, runFrontier);
            if (newWall != null) {
                log(logToConsole, t, SimEvent.Kind.WALL, wallText(newWall, t));
            }
            if (logToConsole && t > 0 && t % 3600 == 0) {   // почасовой heartbeat прогресса
                System.out.printf("%s | ПРОГРЕСС     | этаж %d, золото %s, DPS %s, Слава %s%n",
                        hms(t), cur, state.gold().toDisplayString(), state.totalDps().toDisplayString(),
                        state.ash().toDisplayString());
            }
            prevLevel = cur;
        }
    }

    /** Одна игровая секунда боя + логика «застрял на боссе → отступил фармить → вернулся». */
    private void combatSecond() {
        long level = state.currentLevel();
        state.stepOneSecond();
        if (mode == Mode.PUSH && CombatEngine.isBossLevel(level)) {
            if (state.currentLevel() > level) {
                bossElapsed = 0;                                   // босс убит
            } else if (++bossElapsed >= BOSS_TIME_LIMIT) {
                state.resetBoss(level);                            // не успел за 30с — босс на полное HP
                bossElapsed = 0;
                stuckBoss = level;
                state.enterLevel(level - 1, false);                // отступаем фармить пред. обычный этаж
                mode = Mode.FARM;
            }
        } else if (mode == Mode.FARM && bossBeatable(stuckBoss)) {
            state.enterLevel(stuckBoss, true);                     // накопили силу — возвращаемся к боссу
            mode = Mode.PUSH;
            bossElapsed = 0;
        }
        if (mode == Mode.PUSH) {
            secondsPushing++;
        } else {
            secondsFarming++;
        }
    }

    /** Босс убиваем, если за 30с суммарного урона хватает на его HP. */
    private boolean bossBeatable(long bossLevel) {
        return state.perSecondDamage().multiply(BOSS_TIME_LIMIT).gte(EconomyCurves.bossHp(bossLevel));
    }

    private void recordFloorMilestones(boolean logToConsole, long t) {
        for (long m : FLOOR_MILESTONES) {
            if (runFrontier >= m && !floorMilestoneAt.containsKey(m)) {
                floorMilestoneAt.put(m, t);
                log(logToConsole, t, SimEvent.Kind.MILESTONE, "Достигнут этаж " + m);
            }
        }
    }

    private String wallText(WallTracker.Wall wall, long t) {
        long waitMin = (t - wall.startSeconds) / 60;
        String missing;
        if (CombatEngine.isBossLevel(wall.floor)) {
            BigNum need = EconomyCurves.bossHp(wall.floor).divide(BigNum.of(BOSS_TIME_LIMIT));
            BigNum have = state.perSecondDamage();
            missing = "нужно ~%s урона/сек (есть %s), золото %s"
                    .formatted(need.toDisplayString(), have.toDisplayString(), state.gold().toDisplayString());
        } else {
            missing = "золота %s, DPS %s".formatted(state.gold().toDisplayString(), state.totalDps().toDisplayString());
        }
        return "СТЕНА: этаж %d, ожидание %d мин, не хватает: %s".formatted(wall.floor, waitMin, missing);
    }

    // ---------- Журнал ----------

    private void log(boolean toConsole, long t, SimEvent.Kind kind, String text) {
        SimEvent e = new SimEvent(t, kind, text, state.currentLevel(), state.gold(), state.totalDps(), state.ash());
        events.add(e);   // в CSV попадают ВСЕ события
        if (toConsole && consoleWorthy(kind, e.floor())) {   // в консоль — только ключевые (детали в CSV)
            System.out.printf("%s | %-11s | %-58s | этаж %-5d | %s зол | %s DPS%n",
                    e.clock(), kind, text, e.floor(), e.gold().toDisplayString(), e.dps().toDisplayString());
        }
    }

    /** Консоль показывает только ключевые события; поуровневые апгрейды и рядовых боссов — только в CSV. */
    private static boolean consoleWorthy(SimEvent.Kind kind, long floor) {
        return switch (kind) {
            case UPGRADE_HERO, UPGRADE_TAP -> false;
            case BOSS_KILL -> floor - 1 <= 50 || (floor - 1) % 100 == 0;  // ранние боссы + каждый 100-й
            default -> true;                                              // BUY_HERO, SAGA_RANK, WALL, REBIRTH, MILESTONE
        };
    }

    private void writeCsv(Path path) {
        StringBuilder sb = new StringBuilder("game_time_seconds,event,floor,gold,dps,ash\n");
        for (SimEvent e : events) {
            sb.append(e.toCsvRow()).append('\n');
        }
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, sb.toString());
            System.out.println("\nCSV-отчёт: " + path.toAbsolutePath());
        } catch (IOException ex) {
            System.err.println("Не удалось записать CSV: " + ex.getMessage());
        }
    }

    // ---------- Итоговый отчёт ----------

    private void printReport() {
        long total = secondsPushing + secondsFarming;
        System.out.println("\n" + "=".repeat(70));
        System.out.println("ИТОГОВЫЙ ОТЧЁТ СИМУЛЯЦИИ");
        System.out.println("=".repeat(70));
        System.out.printf("Параметры: клики/сек=%d, порог стены=%dс, лимит=%dч, ребёртов до стопа=%d%n",
                config.clicksPerSecond(), config.wallThresholdSeconds(), config.maxSimulatedHours(),
                config.rebirthsToStop());
        System.out.printf("Достигнуто: этаж %d, ребёртов %d, Слава %s, симулировано %s игрового времени%n",
                Math.max(runFrontier, state.maxLevel()), rebirthAt.size(), state.ash().toDisplayString(), hms(total));

        System.out.println("\n-- Время до ключевых рубежей --");
        for (long m : FLOOR_MILESTONES) {
            System.out.printf("  этаж %-4d : %s%n", m, floorMilestoneAt.containsKey(m) ? hms(floorMilestoneAt.get(m)) : "не достигнут");
        }
        for (int i = 0; i < rebirthAt.size(); i++) {
            System.out.printf("  ребёрт #%d : %s%n", i + 1, hms(rebirthAt.get(i)));
        }

        System.out.println("\n-- Время до боссов (первое убийство; ранние + каждый 100-й, полный список в CSV) --");
        if (firstBossKillAt.isEmpty()) {
            System.out.println("  (ни одного босса не убито)");
        } else {
            long deepest = firstBossKillAt.keySet().stream().mapToLong(Long::longValue).max().orElse(0);
            firstBossKillAt.forEach((floor, t) -> {
                if (floor <= 50 || floor % 100 == 0 || floor == deepest) {
                    System.out.printf("  босс %-5d : %s%n", floor, hms(t));
                }
            });
        }

        System.out.println("\n-- Топ-5 самых длинных стен --");
        List<WallTracker.Wall> top = new ArrayList<>(walls.walls());
        top.sort(Comparator.comparingLong((WallTracker.Wall w) -> w.durationSeconds(total)).reversed());
        if (top.isEmpty()) {
            System.out.println("  (стен не зафиксировано)");
        } else {
            for (int i = 0; i < Math.min(5, top.size()); i++) {
                WallTracker.Wall w = top.get(i);
                String reason = CombatEngine.isBossLevel(w.floor) ? "босс не пробивается за 30с" : "мало золота/DPS";
                System.out.printf("  %d) этаж %-4d длительность %-9s причина: %s%n",
                        i + 1, w.floor, hms(w.durationSeconds(total)), reason);
            }
        }

        System.out.println("\n-- Разбивка времени --");
        System.out.printf("  активный прогресс : %s (%.1f%%)%n", hms(secondsPushing), pct(secondsPushing, total));
        System.out.printf("  ожидание золота   : %s (%.1f%%)%n", hms(secondsFarming), pct(secondsFarming, total));

        System.out.println("\n-- Сравнение прохождений (ускорение от Славы) --");
        if (rebirthAt.size() >= 2) {
            long run1 = rebirthAt.get(0);
            long run2 = rebirthAt.get(1) - rebirthAt.get(0);
            System.out.printf("  1-е прохождение (старт → ребёрт #1) : %s%n", hms(run1));
            System.out.printf("  2-е прохождение (ребёрт #1 → #2)    : %s%n", hms(run2));
            System.out.printf("  ускорение: 2-е быстрее 1-го в %.2f раза%n", run2 == 0 ? 0.0 : (double) run1 / run2);
        } else {
            System.out.println("  недостаточно перерождений для сравнения (нужно ≥2)");
        }
        System.out.println("=".repeat(70));
    }

    // ---------- Хелперы ----------

    private static String hms(long seconds) {
        return "%02d:%02d:%02d".formatted(seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

    private static double pct(long part, long total) {
        return total == 0 ? 0.0 : 100.0 * part / total;
    }

    public List<SimEvent> events() {
        return events;
    }

    public List<Long> rebirthTimes() {
        return rebirthAt;
    }

    public List<WallTracker.Wall> walls() {
        return walls.walls();
    }

    // ---------- Хелперы для тестов ----------

    /**
     * Игровое время (сек) до достижения этажа {@code floor} с заданным стартовым Славой; -1 если не
     * достигнут за {@code maxSeconds}. Используется, чтобы показать, что Слава ускоряет прохождение.
     */
    public static long secondsToReachFloor(int clicksPerSecond, BigNum startingAsh, long floor, long maxSeconds) {
        Config cfg = new Config(clicksPerSecond, 600, 1000, 99, 10, 42L);
        BalanceSimulator sim = new BalanceSimulator(cfg);
        sim.state.injectAsh(startingAsh);
        Mode m = Mode.PUSH;
        long stuck = 0;
        long bElapsed = 0;
        for (long t = 0; t < maxSeconds; t++) {
            if (t % cfg.purchaseIntervalSeconds() == 0) {
                PurchaseStrategy.runCycle(sim.state);
            }
            long level = sim.state.currentLevel();
            sim.state.stepOneSecond();
            if (m == Mode.PUSH && CombatEngine.isBossLevel(level)) {
                if (sim.state.currentLevel() > level) {
                    bElapsed = 0;
                } else if (++bElapsed >= BOSS_TIME_LIMIT) {
                    sim.state.resetBoss(level);
                    bElapsed = 0;
                    stuck = level;
                    sim.state.enterLevel(level - 1, false);
                    m = Mode.FARM;
                }
            } else if (m == Mode.FARM && sim.bossBeatable(stuck)) {
                sim.state.enterLevel(stuck, true);
                m = Mode.PUSH;
                bElapsed = 0;
            }
            if (sim.state.currentLevel() >= floor) {
                return t;
            }
        }
        return -1;
    }
}
