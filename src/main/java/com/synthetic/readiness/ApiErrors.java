package com.synthetic.readiness;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import org.springframework.dao.OptimisticLockingFailureException;
import jakarta.persistence.OptimisticLockException;

@RestControllerAdvice
class ApiErrors {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String,String>> badRequest(IllegalArgumentException ex) { return ResponseEntity.badRequest().body(Map.of("error",String.valueOf(ex.getMessage()))); }
    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Map<String,String>> notFound(NoSuchElementException ex) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error",String.valueOf(ex.getMessage()))); }
    @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
    ResponseEntity<Map<String,String>> optimisticConflict(RuntimeException ex) { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error","The readiness action changed during this update. Reload and retry.")); }
}

