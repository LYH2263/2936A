package com.exam.service;

import com.exam.dto.StudyPlanDTO;
import com.exam.entity.Exam;
import com.exam.entity.StudyPlan;
import com.exam.entity.StudyPlanLog;
import com.exam.entity.StudyPlanTask;
import com.exam.entity.User;
import com.exam.repository.ExamRepository;
import com.exam.repository.StudyPlanLogRepository;
import com.exam.repository.StudyPlanRepository;
import com.exam.repository.StudyPlanTaskRepository;
import com.exam.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudyPlanService {

    private final StudyPlanRepository studyPlanRepository;
    private final StudyPlanTaskRepository studyPlanTaskRepository;
    private final StudyPlanLogRepository studyPlanLogRepository;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;

    public StudyPlanService(StudyPlanRepository studyPlanRepository,
                            StudyPlanTaskRepository studyPlanTaskRepository,
                            StudyPlanLogRepository studyPlanLogRepository,
                            ExamRepository examRepository,
                            UserRepository userRepository) {
        this.studyPlanRepository = studyPlanRepository;
        this.studyPlanTaskRepository = studyPlanTaskRepository;
        this.studyPlanLogRepository = studyPlanLogRepository;
        this.examRepository = examRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public StudyPlanDTO createPlan(String username, StudyPlanDTO.CreateRequest request) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Exam exam = examRepository.findById(request.getExamId()).orElseThrow();

        studyPlanRepository.findByUserIdAndExamId(user.getId(), exam.getId()).ifPresent(p -> {
            throw new RuntimeException("该考试已有备考计划");
        });

        StudyPlan plan = new StudyPlan();
        plan.setUser(user);
        plan.setExam(exam);
        plan.setDailyGoalMinutes(request.getDailyGoalMinutes());
        plan.setStatus("ACTIVE");
        plan = studyPlanRepository.save(plan);

        if (request.getTaskTitles() != null) {
            int order = 0;
            for (String title : request.getTaskTitles()) {
                if (title == null || title.trim().isEmpty()) continue;
                StudyPlanTask task = new StudyPlanTask();
                task.setPlan(plan);
                task.setTitle(title.trim());
                task.setSortOrder(order++);
                studyPlanTaskRepository.save(task);
            }
        }

        return toDTO(plan);
    }

    public StudyPlanDTO getPlanByExam(String username, Long examId) {
        User user = userRepository.findByUsername(username).orElseThrow();
        StudyPlan plan = studyPlanRepository.findByUserIdAndExamId(user.getId(), examId).orElse(null);
        if (plan == null) return null;
        archiveIfNeeded(plan);
        return toDTO(plan);
    }

    public List<StudyPlanDTO> getMyPlans(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        List<StudyPlan> plans = studyPlanRepository.findByUserId(user.getId());
        plans.forEach(this::archiveIfNeeded);
        return plans.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<StudyPlanDTO.DashboardCard> getDashboardCards(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        List<StudyPlan> plans = studyPlanRepository.findByUserIdAndStatus(user.getId(), "ACTIVE");
        plans.forEach(this::archiveIfNeeded);
        List<StudyPlan> activePlans = studyPlanRepository.findByUserIdAndStatus(user.getId(), "ACTIVE");

        List<StudyPlanDTO.DashboardCard> cards = new ArrayList<>();
        for (StudyPlan plan : activePlans) {
            StudyPlanDTO.DashboardCard card = new StudyPlanDTO.DashboardCard();
            card.setPlanId(plan.getId());
            card.setExamId(plan.getExam().getId());
            card.setExamTitle(plan.getExam().getTitle());

            if (plan.getExam().getStartTime() != null) {
                long days = ChronoUnit.DAYS.between(LocalDate.now(), plan.getExam().getStartTime().toLocalDate());
                card.setDaysUntilExam(Math.max(0, days));
            } else {
                card.setDaysUntilExam(null);
            }

            List<StudyPlanTask> tasks = studyPlanTaskRepository.findByPlanIdOrderBySortOrderAsc(plan.getId());
            String today = LocalDate.now().toString();
            int completed = 0;
            for (StudyPlanTask t : tasks) {
                if (t.isCompletedOn(today)) completed++;
            }
            int total = tasks.size();
            card.setTotalTasksToday(total);
            card.setCompletedTasksToday(completed);
            card.setTodayProgress(total > 0 ? (double) completed / total * 100 : 0);
            card.setStreakDays(calculateStreak(plan.getId(), tasks));

            card.setDailyGoalMinutes(plan.getDailyGoalMinutes());
            StudyPlanLog log = studyPlanLogRepository.findByPlanIdAndLogDate(plan.getId(), LocalDate.now()).orElse(null);
            int studied = log != null ? (log.getStudiedMinutes() != null ? log.getStudiedMinutes() : 0) : 0;
            card.setTodayStudiedMinutes(studied);
            int goal = plan.getDailyGoalMinutes() != null ? plan.getDailyGoalMinutes() : 0;
            card.setMinutesProgress(goal > 0 ? Math.min(100.0, (double) studied / goal * 100) : 0);

            cards.add(card);
        }
        return cards;
    }

    @Transactional
    public StudyPlanDTO checkIn(String username, Long planId, StudyPlanDTO.CheckInRequest request) {
        User user = userRepository.findByUsername(username).orElseThrow();
        StudyPlan plan = studyPlanRepository.findById(planId).orElseThrow(() -> new RuntimeException("计划不存在"));
        if (!plan.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("无权操作该备考计划");
        }
        if ("ARCHIVED".equals(plan.getStatus())) {
            throw new RuntimeException("备考计划已归档，无法打卡");
        }

        StudyPlanTask task = studyPlanTaskRepository.findById(request.getTaskId()).orElseThrow(() -> new RuntimeException("任务不存在"));
        if (!task.getPlan().getId().equals(planId)) {
            throw new RuntimeException("任务不属于该备考计划");
        }

        String today = LocalDate.now().toString();
        task.toggleDate(today, Boolean.TRUE.equals(request.getCompleted()));
        studyPlanTaskRepository.save(task);

        return toDTO(plan);
    }

    @Transactional
    public StudyPlanDTO addTask(String username, Long planId, StudyPlanDTO.AddTaskRequest request) {
        User user = userRepository.findByUsername(username).orElseThrow();
        StudyPlan plan = studyPlanRepository.findById(planId).orElseThrow(() -> new RuntimeException("计划不存在"));
        if (!plan.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("无权操作该备考计划");
        }
        if ("ARCHIVED".equals(plan.getStatus())) {
            throw new RuntimeException("备考计划已归档，无法添加任务");
        }

        List<StudyPlanTask> existing = studyPlanTaskRepository.findByPlanIdOrderBySortOrderAsc(planId);
        StudyPlanTask task = new StudyPlanTask();
        task.setPlan(plan);
        task.setTitle(request.getTitle());
        task.setSortOrder(existing.size());
        studyPlanTaskRepository.save(task);

        return toDTO(plan);
    }

    @Transactional
    public StudyPlanDTO deleteTask(String username, Long planId, Long taskId) {
        User user = userRepository.findByUsername(username).orElseThrow();
        StudyPlan plan = studyPlanRepository.findById(planId).orElseThrow(() -> new RuntimeException("计划不存在"));
        if (!plan.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("无权操作该备考计划");
        }
        if ("ARCHIVED".equals(plan.getStatus())) {
            throw new RuntimeException("备考计划已归档，无法删除任务");
        }

        StudyPlanTask task = studyPlanTaskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("任务不存在"));
        if (!task.getPlan().getId().equals(planId)) {
            throw new RuntimeException("任务不属于该备考计划");
        }

        studyPlanTaskRepository.delete(task);

        return toDTO(plan);
    }

    @Transactional
    public StudyPlanDTO logMinutes(String username, Long planId, StudyPlanDTO.LogMinutesRequest request) {
        User user = userRepository.findByUsername(username).orElseThrow();
        StudyPlan plan = studyPlanRepository.findById(planId).orElseThrow(() -> new RuntimeException("计划不存在"));
        if (!plan.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("无权操作该备考计划");
        }
        if ("ARCHIVED".equals(plan.getStatus())) {
            throw new RuntimeException("备考计划已归档，无法记录学习时长");
        }

        if (request.getMinutes() == null || request.getMinutes() < 0) {
            throw new RuntimeException("无效的学习时长");
        }

        LocalDate today = LocalDate.now();
        StudyPlanLog log = studyPlanLogRepository.findByPlanIdAndLogDate(planId, today).orElse(null);
        if (log == null) {
            log = new StudyPlanLog();
            log.setPlan(plan);
            log.setLogDate(today);
            log.setStudiedMinutes(Math.min(request.getMinutes(), plan.getDailyGoalMinutes() != null ? plan.getDailyGoalMinutes() * 2 : request.getMinutes()));
        } else {
            int capped = plan.getDailyGoalMinutes() != null ? Math.min(plan.getDailyGoalMinutes() * 2, request.getMinutes()) : request.getMinutes();
            log.setStudiedMinutes(capped);
        }
        studyPlanLogRepository.save(log);

        return toDTO(plan);
    }

    private void archiveIfNeeded(StudyPlan plan) {
        Exam exam = plan.getExam();
        if (!"ARCHIVED".equals(plan.getStatus()) && exam.getStartTime() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (!now.isBefore(exam.getStartTime())) {
                plan.setStatus("ARCHIVED");
                plan.setArchivedAt(now);
                studyPlanRepository.save(plan);
            }
        }
    }

    private int calculateStreak(Long planId, List<StudyPlanTask> tasks) {
        if (tasks.isEmpty()) return 0;
        int streak = 0;
        LocalDate checkDate = LocalDate.now();
        String today = checkDate.toString();

        boolean todayAllDone = !tasks.isEmpty() && tasks.stream().allMatch(t -> t.isCompletedOn(today));
        StudyPlan todayLog = studyPlanRepository.findById(planId).orElse(null);
        boolean todayMinutesMet = false;
        if (todayLog != null && todayLog.getDailyGoalMinutes() != null) {
            StudyPlanLog log = studyPlanLogRepository.findByPlanIdAndLogDate(planId, LocalDate.now()).orElse(null);
            int studied = log != null && log.getStudiedMinutes() != null ? log.getStudiedMinutes() : 0;
            todayMinutesMet = studied >= todayLog.getDailyGoalMinutes();
        }
        boolean todayQualified = todayAllDone && todayMinutesMet;

        if (!todayQualified) {
            checkDate = checkDate.minusDays(1);
        }

        while (true) {
            String dateStr = checkDate.toString();
            StudyPlan plan = studyPlanRepository.findById(planId).orElse(null);
            if (plan != null && plan.getCreatedAt() != null && checkDate.isBefore(plan.getCreatedAt().toLocalDate())) {
                break;
            }
            boolean allTasksDone = !tasks.isEmpty();
            for (StudyPlanTask t : tasks) {
                if (!t.isCompletedOn(dateStr)) {
                    allTasksDone = false;
                    break;
                }
            }

            boolean minutesMet = true;
            if (plan != null && plan.getDailyGoalMinutes() != null) {
                StudyPlanLog log = studyPlanLogRepository.findByPlanIdAndLogDate(planId, checkDate).orElse(null);
                int studied = log != null && log.getStudiedMinutes() != null ? log.getStudiedMinutes() : 0;
                minutesMet = studied >= plan.getDailyGoalMinutes();
            }

            if (allTasksDone && minutesMet) {
                streak++;
                checkDate = checkDate.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }

    private StudyPlanDTO toDTO(StudyPlan plan) {
        StudyPlanDTO dto = new StudyPlanDTO();
        dto.setId(plan.getId());
        dto.setExamId(plan.getExam().getId());
        dto.setExamTitle(plan.getExam().getTitle());
        dto.setDailyGoalMinutes(plan.getDailyGoalMinutes());
        dto.setStatus(plan.getStatus());
        dto.setCreatedAt(plan.getCreatedAt() != null ? plan.getCreatedAt().toString() : null);
        dto.setArchivedAt(plan.getArchivedAt() != null ? plan.getArchivedAt().toString() : null);
        dto.setIsArchived("ARCHIVED".equals(plan.getStatus()));

        if (plan.getExam().getStartTime() != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), plan.getExam().getStartTime().toLocalDate());
            dto.setDaysUntilExam(Math.max(0, days));
        }

        List<StudyPlanTask> tasks = studyPlanTaskRepository.findByPlanIdOrderBySortOrderAsc(plan.getId());
        String today = LocalDate.now().toString();
        List<StudyPlanDTO.TaskItem> taskItems = new ArrayList<>();
        int completedToday = 0;
        for (StudyPlanTask t : tasks) {
            StudyPlanDTO.TaskItem item = new StudyPlanDTO.TaskItem();
            item.setId(t.getId());
            item.setTitle(t.getTitle());
            item.setSortOrder(t.getSortOrder());
            item.setCompletedToday(t.isCompletedOn(today));
            item.setCompletedDates(t.getCompletedDates());
            if (Boolean.TRUE.equals(item.getCompletedToday())) completedToday++;
            taskItems.add(item);
        }
        dto.setTasks(taskItems);
        dto.setTodayProgress(tasks.isEmpty() ? 0 : (double) completedToday / tasks.size() * 100);

        StudyPlanLog log = studyPlanLogRepository.findByPlanIdAndLogDate(plan.getId(), LocalDate.now()).orElse(null);
        int studied = log != null && log.getStudiedMinutes() != null ? log.getStudiedMinutes() : 0;
        dto.setTodayStudiedMinutes(studied);
        int goal = plan.getDailyGoalMinutes() != null ? plan.getDailyGoalMinutes() : 0;
        dto.setMinutesProgress(goal > 0 ? Math.min(100.0, (double) studied / goal * 100) : 0);

        dto.setStreakDays(calculateStreak(plan.getId(), tasks));

        return dto;
    }
}
