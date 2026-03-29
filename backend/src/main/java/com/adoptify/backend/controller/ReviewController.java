package com.adoptify.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptions() {
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<?> submitReview(@RequestBody Map<String, Object> payload) {
        // MVP: Returns a success message without saving since Review data model was omitted in initial build
        return ResponseEntity.ok(Map.of("message", "Review submitted successfully"));
    }
}
