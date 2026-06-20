package com.exam.service;

import com.exam.entity.CommentTemplate;
import com.exam.entity.User;
import com.exam.repository.CommentTemplateRepository;
import com.exam.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentTemplateService {
    private final CommentTemplateRepository commentTemplateRepository;
    private final UserRepository userRepository;

    public CommentTemplateService(CommentTemplateRepository commentTemplateRepository, UserRepository userRepository) {
        this.commentTemplateRepository = commentTemplateRepository;
        this.userRepository = userRepository;
    }

    public List<CommentTemplate> getVisibleTemplates(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        if ("ADMIN".equals(user.getRole())) {
            return commentTemplateRepository.findAll();
        }
        return commentTemplateRepository.findVisibleToTeacher(user);
    }

    public List<CommentTemplate> getVisibleTemplatesBySubject(String username, String subject) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        return commentTemplateRepository.findVisibleToTeacherBySubject(user, subject);
    }

    public List<CommentTemplate> getMyTemplates(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        return commentTemplateRepository.findByTeacherOrderByCreatedAtDesc(user);
    }

    public List<CommentTemplate> getPublicTemplates() {
        return commentTemplateRepository.findByIsPublicTrueOrderByCreatedAtDesc();
    }

    @Transactional
    public CommentTemplate createTemplate(String username, CommentTemplate template) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        template.setTeacher(user);
        if (!"ADMIN".equals(user.getRole())) {
            template.setIsPublic(false);
        }
        return commentTemplateRepository.save(template);
    }

    @Transactional
    public CommentTemplate updateTemplate(String username, Long id, CommentTemplate template) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        CommentTemplate existing = commentTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        boolean isOwner = existing.getTeacher() != null && existing.getTeacher().getId().equals(user.getId());
        boolean isAdmin = "ADMIN".equals(user.getRole());

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Permission denied: you can only edit your own templates");
        }

        existing.setName(template.getName());
        existing.setContent(template.getContent());
        existing.setSubject(template.getSubject());
        if (isAdmin) {
            existing.setIsPublic(template.getIsPublic());
        }
        return commentTemplateRepository.save(existing);
    }

    @Transactional
    public void deleteTemplate(String username, Long id) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        CommentTemplate existing = commentTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        boolean isOwner = existing.getTeacher() != null && existing.getTeacher().getId().equals(user.getId());
        boolean isAdmin = "ADMIN".equals(user.getRole());

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Permission denied: you can only delete your own templates");
        }

        commentTemplateRepository.delete(existing);
    }

    public CommentTemplate getTemplate(Long id) {
        return commentTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
    }
}
