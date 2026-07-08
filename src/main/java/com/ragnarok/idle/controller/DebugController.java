package com.ragnarok.idle.controller;

import com.ragnarok.idle.dto.PlayerStateResponse;
import com.ragnarok.idle.dto.SkipTimeRequest;
import com.ragnarok.idle.service.PlayerService;
import jakarta.validation.Valid;
import java.util.function.Supplier;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ТЕСТОВЫЕ ручки для плейтеста баланса — не игровой API.
 * Прокрутка времени позже переедет в игровую механику (Яблоки Идунн, GDD Яблоки-траты).
 */
@RestController
@RequestMapping("/debug")
public class DebugController {

    /**
     * Поллинг /player/me раз в 1с и /debug/skip-time делают read-modify-write по одной строке
     * Player — конкурентная запись кидает {@link ObjectOptimisticLockingFailureException}
     * (@Version). Клиент дополнительно ставит поллинг на паузу вокруг вызова, но это не закрывает
     * окно гонки полностью (уже отправленный в полёте запрос) — короткий авто-ретрай здесь не даёт
     * этому долетать до игрока ошибкой. Повтор идёт через прокси-бин playerService — каждая попытка
     * заново читает актуальную версию строки в свежей транзакции.
     */
    private static final int MAX_RETRIES = 3;

    private final PlayerService playerService;

    public DebugController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping("/skip-time")
    public PlayerStateResponse skipTime(@Valid @RequestBody SkipTimeRequest request,
                                         Authentication authentication) {
        return withOptimisticRetry(MAX_RETRIES,
                () -> playerService.skipTime(authentication.getName(), request.hours()));
    }

    /** Повторяет action до maxAttempts раз, пока не перестанет кидать конфликт версии. */
    static <T> T withOptimisticRetry(int maxAttempts, Supplier<T> action) {
        for (int attempt = 1; ; attempt++) {
            try {
                return action.get();
            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempt >= maxAttempts) {
                    throw e;
                }
            }
        }
    }
}
