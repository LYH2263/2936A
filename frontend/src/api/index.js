import axios from 'axios';
import { message } from 'ant-design-vue';

const api = axios.create({
    baseURL: '/api',
    timeout: 5000,
    withCredentials: true,
    xsrfCookieName: 'XSRF-TOKEN',
    xsrfHeaderName: 'X-XSRF-TOKEN',
});

// Request interceptor
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Response interceptor
api.interceptors.response.use(
    (response) => {
        return response;
    },
    (error) => {
        if (error.response) {
            const { status, data } = error.response;
            if (status === 401) {
                message.error('登录已过期，请重新登录');
                localStorage.removeItem('token');
                window.location.href = '/login';
            } else if (status === 403) {
                message.error('权限不足，拒绝访问');
            } else if (status === 404) {
                message.error('未找到请求的资源');
            } else if (status === 500) {
                message.error('服务器内部错误，请稍后再试');
            } else {
                message.error(data.message || '请求执行失败');
            }
        } else if (error.request) {
            message.error('网络连接超时或服务器无响应');
        } else {
            message.error('请求配置异常');
        }
        return Promise.reject(error);
    }
);

export const login = (data) => api.post('/auth/login', data);
export const register = (data) => api.post('/auth/register', data);
export const updateProfile = (data) => api.put('/auth/profile', data);

// System Config
export const getSystemConfig = () => api.get('/config');
export const updateSystemConfig = (data) => api.post('/config', data);

// Exams
export const getExams = () => api.get('/exams');
export const getMySubmissions = () => api.get('/submissions/my');
export const createExam = (data) => api.post('/exams', data);
export const createQuestion = (data) => api.post('/exams/questions', data);
export const addQuestionToExam = (examId, data) => api.post(`/exams/${examId}/questions`, data);
export const getExam = (id) => api.get(`/exams/${id}`);
export const submitExam = (examId, data) => api.post(`/submissions/${examId}`, data);
export const getStudentStats = () => api.get('/submissions/stats');
export const getTeacherStats = () => api.get('/submissions/teacher-stats');
export const getExamSubmissions = (examId) => api.get(`/submissions/exam/${examId}`);
export const getSubmission = (id) => api.get(`/submissions/${id}`);
export const getExamQuestions = (examId) => api.get(`/exams/${examId}/questions`);
export const getAllQuestions = () => api.get('/exams/questions');
export const publishExam = (examId, data) => api.post(`/exams/${examId}/publish`, data);
export const deleteExam = (examId) => api.delete(`/exams/${examId}`);
export const getStatistics = (examId) => api.get(`/exams/${examId}/statistics`);
export const exportExamStatistics = (examId) => api.get(`/exams/${examId}/export`, { responseType: 'blob' });
export const gradeSubmission = (id, data) => api.post(`/submissions/${id}/grade`, data);
export const autoGenerateExam = (examId, strategy) => api.post(`/exams/${examId}/auto-generate`, strategy);
export const updateExamQuestion = (examId, questionId, data) => api.put(`/exams/${examId}/questions/${questionId}`, data);
export const removeQuestionFromExam = (examId, questionId) => api.delete(`/exams/${examId}/questions/${questionId}`);
export const updateQuestionContent = (questionId, data) => api.put(`/exams/questions/${questionId}`, data);
export const getAllQuestions = () => api.get('/exams/questions');
export const recordCheating = (examId, data) => api.post(`/exams/${examId}/record-cheating`, data);

export const getNotifications = () => api.get('/notifications');
export const markNotificationRead = (id) => api.post(`/notifications/${id}/read`);

export const submitAppeal = (data) => api.post('/appeals', data);
export const getMyAppeals = () => api.get('/appeals/my');
export const getPendingAppeals = () => api.get('/appeals/pending');
export const getPendingAppealCount = () => api.get('/appeals/pending-count');
export const processAppeal = (id, data) => api.post(`/appeals/${id}/process`, data);

export const submitFeedback = (data) => api.post('/feedbacks', data);
export const getMyFeedbacks = () => api.get('/feedbacks/my');
export const getPendingFeedbacks = () => api.get('/feedbacks/pending');
export const getPendingFeedbackCount = () => api.get('/feedbacks/pending-count');
export const processFeedback = (id, data) => api.post(`/feedbacks/${id}/process`, data);
export const getQuestionExams = (questionId) => api.get(`/feedbacks/question-exams/${questionId}`);

// User Management
export const getUsers = (params) => api.get('/users', { params });
export const createUser = (data) => api.post('/users', data);
export const updateUser = (id, data) => api.put(`/users/${id}`, data);
export const deleteUser = (id) => api.delete(`/users/${id}`);
export const importUsers = (formData) => api.post('/users/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
});
export const exportUsers = () => api.get('/users/export', { responseType: 'blob' });
export const resetUserPassword = (id, password) => api.post(`/users/${id}/reset-password`, { password });

export const checkCertEligibility = (examId) => api.get(`/certificates/check/${examId}`);
export const downloadCertificate = (examId, submissionId) => api.get(`/certificates/download/${examId}/${submissionId}`, { responseType: 'blob' });
export const previewCertificate = (examId) => api.get(`/certificates/preview/${examId}`, { responseType: 'blob' });

// Exam Announcements
export const getTeacherAnnouncements = (examId) => api.get(`/exams/${examId}/announcements`);
export const createAnnouncement = (examId, data) => api.post(`/exams/${examId}/announcements`, data);
export const updateAnnouncement = (announcementId, data) => api.put(`/exams/announcements/${announcementId}`, data);
export const toggleAnnouncementPin = (announcementId) => api.post(`/exams/announcements/${announcementId}/toggle-pin`);
export const deleteAnnouncement = (announcementId) => api.delete(`/exams/announcements/${announcementId}`);
export const getStudentAnnouncements = (examId) => api.get(`/exams/${examId}/announcements/student`);
export const getAnnouncementUnreadCount = (examId) => api.get(`/exams/${examId}/announcements/unread-count`);
export const markAnnouncementRead = (announcementId) => api.post(`/exams/announcements/${announcementId}/read`);
export const markAllAnnouncementsRead = (examId) => api.post(`/exams/${examId}/announcements/read-all`);

// Study Plans
export const createStudyPlan = (data) => api.post('/study-plans', data);
export const getStudyPlanByExam = (examId) => api.get(`/study-plans/exam/${examId}`);
export const getMyStudyPlans = () => api.get('/study-plans/my');
export const getStudyPlanDashboard = () => api.get('/study-plans/dashboard');
export const checkInStudyPlanTask = (planId, data) => api.post(`/study-plans/${planId}/check-in`, data);
export const addStudyPlanTask = (planId, data) => api.post(`/study-plans/${planId}/tasks`, data);
export const deleteStudyPlanTask = (planId, taskId) => api.delete(`/study-plans/${planId}/tasks/${taskId}`);

// Comment Templates
export const getCommentTemplates = (subject) => api.get('/comment-templates', { params: subject ? { subject } : {} });
export const getMyCommentTemplates = () => api.get('/comment-templates/my');
export const getPublicCommentTemplates = () => api.get('/comment-templates/public');
export const getCommentTemplate = (id) => api.get(`/comment-templates/${id}`);
export const createCommentTemplate = (data) => api.post('/comment-templates', data);
export const updateCommentTemplate = (id, data) => api.put(`/comment-templates/${id}`, data);
export const deleteCommentTemplate = (id) => api.delete(`/comment-templates/${id}`);

export const getMyBadges = () => api.get('/badges/my');

// Flash Practice
export const getFlashSubjects = () => api.get('/flash-practice/subjects');
export const getFlashKnowledgePoints = (subject) => api.get('/flash-practice/knowledge-points', { params: subject ? { subject } : {} });
export const getFlashTodayStats = () => api.get('/flash-practice/today-stats');
export const startFlashSession = (data) => api.post('/flash-practice/start', data);
export const getFlashNextQuestion = (sessionId) => api.get(`/flash-practice/${sessionId}/next-question`);
export const submitFlashAnswer = (sessionId, data) => api.post(`/flash-practice/${sessionId}/submit`, data);
export const endFlashSession = (sessionId) => api.post(`/flash-practice/${sessionId}/end`);
export const getFlashSession = (sessionId) => api.get(`/flash-practice/${sessionId}`);

export const startExam = (examId) => api.post(`/submissions/${examId}/start`);
export const heartbeat = (examId) => api.post(`/exams/${examId}/heartbeat`);

export const getProctorData = (examId) => api.get(`/proctor/${examId}`);
export const exportProctorSnapshot = (examId) => api.get(`/proctor/${examId}/export`, { responseType: 'blob' });

// Question Ratings
export const submitQuestionRating = (data) => api.post('/question-ratings', data);
export const getMyQuestionRatings = () => api.get('/question-ratings/my');
export const getMyRatingForQuestion = (questionId) => api.get(`/question-ratings/question/${questionId}/my`);
export const getQuestionRatingAverage = (questionId) => api.get(`/question-ratings/question/${questionId}/average`);

export default api;
