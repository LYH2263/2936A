package com.exam.service;

import com.exam.entity.Question;
import com.exam.entity.QuestionRating;
import com.exam.entity.User;
import com.exam.repository.QuestionRatingRepository;
import com.exam.repository.QuestionRepository;
import com.exam.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class QuestionRatingService {

    private final QuestionRatingRepository ratingRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    public QuestionRatingService(QuestionRatingRepository ratingRepository,
                                  QuestionRepository questionRepository,
                                  UserRepository userRepository) {
        this.ratingRepository = ratingRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public QuestionRating submitRating(Long questionId, Integer rating, String username) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new RuntimeException("评分必须在 1-5 星之间");
        }

        User student = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("题目不存在"));

        Optional<QuestionRating> existingOpt = ratingRepository.findByQuestionIdAndStudentId(questionId, student.getId());
        QuestionRating ratingEntity;
        if (existingOpt.isPresent()) {
            ratingEntity = existingOpt.get();
            ratingEntity.setRating(rating);
        } else {
            ratingEntity = new QuestionRating();
            ratingEntity.setQuestion(question);
            ratingEntity.setStudent(student);
            ratingEntity.setRating(rating);
        }

        return ratingRepository.save(ratingEntity);
    }

    public List<QuestionRating> getMyRatings(String username) {
        return ratingRepository.findByStudentUsernameOrderByUpdatedAtDesc(username);
    }

    public QuestionRating getMyRatingForQuestion(Long questionId, String username) {
        return ratingRepository.findByQuestionIdAndStudentUsername(questionId, username).orElse(null);
    }

    public Double getAverageRatingForQuestion(Long questionId) {
        return ratingRepository.findAverageRatingByQuestionId(questionId);
    }

    public Long getRatingCountForQuestion(Long questionId) {
        return ratingRepository.countByQuestionId(questionId);
    }
}
