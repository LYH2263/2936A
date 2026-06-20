<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { getMyAppeals, getMySubmissions, submitAppeal, getExamQuestions, getSubmission } from '@/api';
import { message } from 'ant-design-vue';
import { LeftOutlined, FileTextOutlined, SendOutlined, CheckCircleOutlined, CloseCircleOutlined, ClockCircleOutlined } from '@ant-design/icons-vue';

const router = useRouter();
const authStore = useAuthStore();
const appeals = ref([]);
const submissions = ref([]);
const loading = ref(false);
const submitModalVisible = ref(false);
const submitting = ref(false);

const selectedSubmission = ref(null);
const selectedAnswer = ref(null);
const appealReason = ref('');
const availableAnswers = ref([]);

const statusMap = {
  'PENDING': { text: '待处理', color: 'orange', icon: ClockCircleOutlined },
  'APPROVED': { text: '已通过', color: 'green', icon: CheckCircleOutlined },
  'REJECTED': { text: '已驳回', color: 'red', icon: CloseCircleOutlined }
};

const fetchAppeals = async () => {
  loading.value = true;
  try {
    const res = await getMyAppeals();
    appeals.value = res.data;
  } catch (e) {
    message.error('获取申诉列表失败');
  } finally {
    loading.value = false;
  }
};

const fetchSubmissions = async () => {
  try {
    const res = await getMySubmissions();
    submissions.value = res.data.filter(s => s.state === 'SUBMITTED');
  } catch (e) {
    message.error('获取考试记录失败');
  }
};

const onSubmissionSelect = async (submissionId) => {
  selectedAnswer.value = null;
  availableAnswers.value = [];
  if (!submissionId) return;

  try {
    const subRes = await getSubmission(submissionId);
    const sub = subRes.data;
    const qRes = await getExamQuestions(sub.exam.id);
    const questions = qRes.data;

    availableAnswers.value = (sub.answers || []).map(sa => {
      const eq = questions.find(q => q.question.id === sa.question.id);
      return {
        answerId: sa.id,
        questionId: sa.question.id,
        questionContent: eq ? eq.question.content : '',
        questionType: sa.question.type || (eq && eq.question.type),
        studentScore: sa.score,
        maxScore: eq ? eq.score : 0,
        studentAnswer: sa.studentAnswer
      };
    });
  } catch (e) {
    message.error('获取答案详情失败');
  }
};

const openSubmitModal = () => {
  selectedSubmission.value = null;
  selectedAnswer.value = null;
  appealReason.value = '';
  availableAnswers.value = [];
  submitModalVisible.value = true;
  fetchSubmissions();
};

const handleSubmitAppeal = async () => {
  if (!selectedSubmission.value) {
    message.warning('请选择考试');
    return;
  }
  if (!selectedAnswer.value) {
    message.warning('请选择申诉题目');
    return;
  }
  if (!appealReason.value.trim()) {
    message.warning('请填写申诉理由');
    return;
  }
  if (appealReason.value.length > 500) {
    message.warning('申诉理由不能超过500字');
    return;
  }

  submitting.value = true;
  try {
    await submitAppeal({
      submissionId: selectedSubmission.value,
      answerId: selectedAnswer.value,
      reason: appealReason.value.trim()
    });
    message.success('申诉已提交');
    submitModalVisible.value = false;
    fetchAppeals();
  } catch (e) {
    const msg = e.response?.data?.message || '提交申诉失败';
    message.error(msg);
  } finally {
    submitting.value = false;
  }
};

const viewScoreDetail = (submissionId) => {
  router.push(`/score/${submissionId}`);
};

onMounted(() => {
  fetchAppeals();
});
</script>

<template>
  <div class="appeal-student-wrapper">
    <div class="appeal-header">
      <div class="header-content">
        <a-button type="link" @click="router.push('/dashboard')" class="back-btn">
          <LeftOutlined /> 返回
        </a-button>
        <span class="page-title">我的申诉</span>
        <a-button type="primary" @click="openSubmitModal">
          <SendOutlined /> 发起申诉
        </a-button>
      </div>
    </div>

    <div class="appeal-content">
      <a-table :loading="loading" :dataSource="appeals" :columns="[
        { title: '考试名称', dataIndex: ['submission', 'exam', 'title'], key: 'exam' },
        { title: '申诉题目', key: 'question', width: 200 },
        { title: '申诉理由', dataIndex: 'reason', key: 'reason', ellipsis: true },
        { title: '申诉时间', dataIndex: 'createdAt', key: 'createdAt', customRender: ({text}) => text ? new Date(text).toLocaleString() : '-' },
        { title: '状态', key: 'status', width: 120 },
        { title: '处理说明', dataIndex: 'handlerComment', key: 'handlerComment', ellipsis: true },
        { title: '操作', key: 'action', width: 120 }
      ]">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'question'">
            <span v-html="record.answer?.question?.content?.substring(0, 50) || '题目' + record.answer?.id"></span>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="statusMap[record.status]?.color || 'default'">
              <component :is="statusMap[record.status]?.icon" style="margin-right: 4px;" v-if="statusMap[record.status]?.icon" />
              {{ statusMap[record.status]?.text || record.status }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click="viewScoreDetail(record.submission?.id)">
              查看成绩
            </a-button>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal v-model:open="submitModalVisible" title="发起成绩申诉" :confirm-loading="submitting" @ok="handleSubmitAppeal" ok-text="提交申诉" cancel-text="取消" width="640px">
      <a-form layout="vertical">
        <a-form-item label="选择考试" required>
          <a-select v-model:value="selectedSubmission" placeholder="请选择已提交的考试" @change="onSubmissionSelect" show-search option-filter-prop="label">
            <a-select-option v-for="sub in submissions" :key="sub.id" :value="sub.id" :label="sub.exam.title">
              {{ sub.exam.title }} (得分: {{ sub.score }}分)
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="选择申诉题目" required>
          <a-select v-model:value="selectedAnswer" placeholder="请选择要申诉的题目" :disabled="!selectedSubmission">
            <a-select-option v-for="ans in availableAnswers" :key="ans.answerId" :value="ans.answerId">
              <span v-html="(ans.questionContent || '').substring(0, 60) || '题目'"></span>
              (得分: {{ ans.studentScore }}/{{ ans.maxScore }})
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="申诉理由" required>
          <a-textarea v-model:value="appealReason" placeholder="请详细说明申诉理由，最多500字" :maxlength="500" :rows="4" show-count />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.appeal-student-wrapper {
  background-color: #f0f2f5;
  min-height: 100vh;
  padding-top: 80px;
  padding-bottom: 40px;
}
.appeal-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 64px;
  background: white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  z-index: 1000;
  display: flex;
  align-items: center;
}
.header-content {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}
.back-btn { font-size: 16px; color: #666; }
.page-title { font-size: 18px; font-weight: 600; }
.appeal-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
}
</style>
