package com.exam.controller;

import com.exam.service.BadgeService;
import com.exam.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/badges")
public class BadgeController {

    private final BadgeService badgeService;
    private final UserRepository userRepository;

    public BadgeController(BadgeService badgeService, UserRepository userRepository) {
        this.badgeService = badgeService;
        this.userRepository = userRepository;
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<Map<String, Object>>> getMyBadges(Principal principal) {
        Long userId = userRepository.findByUsername(principal.getName())
                .orElseThrow().getId();
        return ResponseEntity.ok(badgeService.getMyBadges(userId));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<Map<String, Object>>> getAllBadges(Principal principal) {
        Long userId = userRepository.findByUsername(principal.getName())
                .orElseThrow().getId();
        return ResponseEntity.ok(badgeService.getMyBadges(userId));
    }
}
