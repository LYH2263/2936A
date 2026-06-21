import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import LoginView from '@/views/LoginView.vue'
import DashboardView from '@/views/DashboardView.vue'
import ExamView from '@/views/ExamView.vue'
import ExamDetailView from '@/views/ExamDetailView.vue'
import ScoreDetailView from '@/views/ScoreDetailView.vue'
import ExamAssembleView from '@/views/ExamAssembleView.vue'
import ExamPublishView from '@/views/ExamPublishView.vue'
import ExamAnalysisView from '@/views/ExamAnalysisView.vue'
import AppealStudentView from '@/views/AppealStudentView.vue'
import AppealTeacherView from '@/views/AppealTeacherView.vue'
import FeedbackTeacherView from '@/views/FeedbackTeacherView.vue'
import QuestionBankView from '@/views/QuestionBankView.vue'
import FlashPracticeView from '@/views/FlashPracticeView.vue'
import ProctorView from '@/views/ProctorView.vue'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/login',
            name: 'login',
            component: LoginView
        },
        {
            path: '/',
            redirect: '/dashboard'
        },
        {
            path: '/dashboard',
            name: 'dashboard',
            component: DashboardView,
            meta: { requiresAuth: true }
        },
        {
            path: '/exam/:id',
            name: 'exam',
            component: ExamView,
            meta: { requiresAuth: true }
        },
        {
            path: '/exam/:id/detail',
            name: 'exam-detail',
            component: ExamDetailView,
            meta: { requiresAuth: true }
        },
        {
            path: '/score/:id',
            name: 'score-detail',
            component: ScoreDetailView,
            meta: { requiresAuth: true }
        },
        {
            path: '/exam/:id/assemble',
            name: 'exam-assemble',
            component: ExamAssembleView,
            meta: { requiresAuth: true }
        },
        {
            path: '/exam/:id/publish',
            name: 'exam-publish',
            component: ExamPublishView,
            meta: { requiresAuth: true }
        },
        {
            path: '/exam/:id/analysis',
            name: 'exam-analysis',
            component: ExamAnalysisView,
            meta: { requiresAuth: true }
        },
        {
            path: '/appeals/student',
            name: 'appeal-student',
            component: AppealStudentView,
            meta: { requiresAuth: true, role: 'STUDENT' }
        },
        {
            path: '/appeals/teacher',
            name: 'appeal-teacher',
            component: AppealTeacherView,
            meta: { requiresAuth: true, role: 'TEACHER' }
        },
        {
            path: '/feedbacks/teacher',
            name: 'feedback-teacher',
            component: FeedbackTeacherView,
            meta: { requiresAuth: true, role: 'TEACHER' }
        },
        {
            path: '/question-bank',
            name: 'question-bank',
            component: QuestionBankView,
            meta: { requiresAuth: true, role: 'TEACHER' }
        },
        {
            path: '/flash-practice',
            name: 'flash-practice',
            component: FlashPracticeView,
            meta: { requiresAuth: true, role: 'STUDENT' }
        },
        {
            path: '/proctor/:examId',
            name: 'proctor',
            component: ProctorView,
            meta: { requiresAuth: true, role: 'TEACHER' }
        }
    ]
})

router.beforeEach((to, from, next) => {
    const authStore = useAuthStore()
    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
        next('/login')
    } else if (to.meta.role) {
        const requiredRole = to.meta.role
        if (requiredRole === 'TEACHER' && !authStore.isTeacher) {
            next('/dashboard')
        } else if (requiredRole === 'STUDENT' && authStore.user?.role !== 'STUDENT') {
            next('/dashboard')
        } else if (requiredRole === 'ADMIN' && !authStore.isAdmin) {
            next('/dashboard')
        } else {
            next()
        }
    } else {
        next()
    }
})

export default router
