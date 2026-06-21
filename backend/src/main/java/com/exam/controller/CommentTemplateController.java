package com.exam.controller;

import com.exam.entity.CommentTemplate;
import com.exam.service.CommentTemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/comment-templates")
public class CommentTemplateController {
    private final CommentTemplateService commentTemplateService;

    public CommentTemplateController(CommentTemplateService commentTemplateService) {
        this.commentTemplateService = commentTemplateService;
    }

    @GetMapping
    public List<CommentTemplate> getVisibleTemplates(Principal principal,
                                                  @RequestParam(required = false) String subject) {
        if (subject != null && !subject.isEmpty()) {
            return commentTemplateService.getVisibleTemplatesBySubject(principal.getName(), subject);
        }
        return commentTemplateService.getVisibleTemplates(principal.getName());
    }

    @GetMapping("/my")
    public List<CommentTemplate> getMyTemplates(Principal principal) {
        return commentTemplateService.getMyTemplates(principal.getName());
    }

    @GetMapping("/public")
    public List<CommentTemplate> getPublicTemplates() {
        return commentTemplateService.getPublicTemplates();
    }

    @GetMapping("/{id}")
    public CommentTemplate getTemplate(@PathVariable Long id, Principal principal) {
        return commentTemplateService.getTemplate(principal.getName(), id);
    }

    @PostMapping
    public CommentTemplate createTemplate(@RequestBody CommentTemplate template, Principal principal) {
        return commentTemplateService.createTemplate(principal.getName(), template);
    }

    @PutMapping("/{id}")
    public CommentTemplate updateTemplate(@PathVariable Long id,
                                       @RequestBody CommentTemplate template,
                                       Principal principal) {
        return commentTemplateService.updateTemplate(principal.getName(), id, template);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTemplate(@PathVariable Long id, Principal principal) {
        commentTemplateService.deleteTemplate(principal.getName(), id);
        return ResponseEntity.ok().build();
    }
}
