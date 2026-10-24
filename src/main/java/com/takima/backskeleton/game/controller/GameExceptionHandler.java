package com.takima.backskeleton.game.controller;

import com.takima.backskeleton.exceptions.ErrorResponse;
import com.takima.backskeleton.game.exception.DraftCompletedException;
import com.takima.backskeleton.game.exception.DraftIncompleteException;
import com.takima.backskeleton.game.exception.GameCapacityReachedException;
import com.takima.backskeleton.game.exception.GameCreationRateLimitedException;
import com.takima.backskeleton.game.exception.GameNotFoundException;
import com.takima.backskeleton.game.exception.InvalidPickException;
import com.takima.backskeleton.game.exception.InvalidRequestException;
import com.takima.backskeleton.game.exception.NoRedrawAvailableException;
import com.takima.backskeleton.game.exception.UnknownFormationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GameExceptionHandler {
    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(GameNotFoundException exception) {
        return toResponse(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler(GameCapacityReachedException.class)
    public ResponseEntity<ErrorResponse> handleCapacityReached(GameCapacityReachedException exception) {
        return toResponse(HttpStatus.SERVICE_UNAVAILABLE, exception);
    }

    @ExceptionHandler(GameCreationRateLimitedException.class)
    public ResponseEntity<ErrorResponse> handleRateLimited(GameCreationRateLimitedException exception) {
        return toResponse(HttpStatus.TOO_MANY_REQUESTS, exception);
    }

    @ExceptionHandler({InvalidPickException.class, InvalidRequestException.class, UnknownFormationException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException exception) {
        return toResponse(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler({DraftCompletedException.class, DraftIncompleteException.class, NoRedrawAvailableException.class})
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException exception) {
        return toResponse(HttpStatus.CONFLICT, exception);
    }

    private ResponseEntity<ErrorResponse> toResponse(HttpStatus status, RuntimeException exception) {
        return ResponseEntity.status(status).body(new ErrorResponse(exception.getMessage()));
    }
}
