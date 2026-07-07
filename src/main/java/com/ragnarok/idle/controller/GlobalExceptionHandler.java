package com.ragnarok.idle.controller;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Player/Avatar версионируются ({@code @Version}) — конкурентная запись (напр. тап, зависший в полёте,
 * и ребёрт) кидает {@link ObjectOptimisticLockingFailureException} вместо тихой потери чужих изменений.
 * Отдаём клиенту понятный 409, а не голый 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleConcurrentModification(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Состояние изменилось параллельным запросом, повтори попытку"));
    }
}
