package com.exam.controller;

import com.exam.entity.QuestionRating;
import com.exam.service.QuestionRatingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/question-ratings")
public class QuestionRatingController {

    private final QuestionRatingService ratingService;

    public QuestionRatingController(QuestionRatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> submitRating(@RequestBody Map<String, Object> body, Principal principal) {
        try {
            Long questionId = Long.valueOf(body.get("questionId").toString());
            Integer rating = Integer.valueOf(body.get("rating").toString());

            QuestionRating result = ratingService.submitRating(questionId, rating, principal.getName());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public List<QuestionRating> getMyRatings(Principal principal) {
        return ratingService.getMyRatings(principal.getName());
    }

    @GetMapping("/question/{questionId}/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getMyRatingForQuestion(@PathVariable Long questionId, Principal principal) {
        QuestionRating rating = ratingService.getMyRatingForQuestion(questionId, principal.getName());
        if (rating == null) {
            return ResponseEntity.ok(Map.of("rating", 0));
        }
        return ResponseEntity.ok(rating);
    }

    @GetMapping("/question/{questionId}/average")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
    public ResponseEntity<?> getAverageRating(@PathVariable Long questionId) {
        Double avg = ratingService.getAverageRatingForQuestion(questionId);
        Long count = ratingService.getRatingCountForQuestion(questionId);
        return ResponseEntity.ok(Map.of(
                "average", avg != null ? avg : 0.0,
                "count", count != null ? count : 0L
        ));
    }
}
