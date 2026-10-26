package com.synthetic.readiness;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestControllerAdvice
class ApiErrors {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String,String>> badRequest(IllegalArgumentException ex) { return ResponseEntity.badRequest().body(Map.of("error",String.valueOf(ex.getMessage()))); }
    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Map<String,String>> notFound(NoSuchElementException ex) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error",String.valueOf(ex.getMessage()))); }
}

