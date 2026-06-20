<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getPendingFeedbacks, processFeedback, getQuestionExams } from '@/api';
import { message } from 'ant-design-vue';
import { LeftOutlined, CheckCircleOutlined, CloseCircleOutlined, EyeOutlined, EditOutlined } from '@ant-design/icons-vue';

const router = useRouter();
const feedbacks = ref([]);
const loading = ref(false);
const detailVisible = ref(false);
const processVisible = ref(false);
const currentFeedback = ref(null);
const processing = ref(false);
const processForm = ref({
  action: '',
  handlerComment: ''
});

const typeMap = {
  'ANSWER_ERROR': { text: '答案错误', color: 'red' },
  'QUESTION_UNCLEAR': { text: '题干不清', color: 'orange' },
  'OPTION_DUPLICATE': { text: '选项重复', color: 'purple' },
  'OTHER': { text: '其他', color: 'default' }
};

const statusMap = {
  'PENDING': { text: '待处理', color: 'orange' },
  'CONFIRMED': { text: '已确认', color: 'green' },
  'REJECTED': { text: '已驳回', color: 'red' }
};

const fetchFeedbacks = async () => {
  loading.value = true;
  try {
    const res = await getPendingFeedbacks();
    feedbacks.value = res.data;
  } catch (e) {
    message.error('获取纠错工单失败');
  } finally {
    loading.value = false;
  }
};

const showDetail = (fb) => {
  currentFeedback.value = fb;
  detailVisible.value = true;
};

const openProcess = (fb, action) => {
  currentFeedback.value = fb;
  processForm.value = {
    action: action,
    handlerComment: ''
  };
  processVisible.value = true;
};

const handleProcess = async () => {
  if (!processForm.value.handlerComment?.trim()) {
    message.warning('请填写处理说明');
    return;
  }

  processing.value = true;
  try {
    const res = await processFeedback(currentFeedback.value.id, {
      action: processForm.value.action,
      handlerComment: processForm.value.handlerComment.trim()
    });

    if (processForm.value.action === 'CONFIRM') {
      const questionId = currentFeedback.value.question?.id;
      if (questionId) {
        try {
          const examRes = await getQuestionExams(questionId);
          const exams = examRes.data;
          if (exams.length > 0) {
            message.success('已确认纠错，正在跳转至题库编辑...');
            processVisible.value = false;
            detailVisible.value = false;
            router.push(`/exam/${exams[0].examId}/assemble`);
            return;
          }
        } catch (e) { /* ignore */ }
      }
      message.success('已确认纠错');
    } else {
      message.success('已驳回纠错');
    }

    processVisible.value = false;
    detailVisible.value = false;
    fetchFeedbacks();
  } catch (e) {
    const msg = e.response?.data?.message || '处理失败';
    message.error(msg);
  } finally {
    processing.value = false;
  }
};

const handleConfirmAndEdit = async (fb) => {
  const questionId = fb.question?.id;
  if (questionId) {
    try {
      const examRes = await getQuestionExams(questionId);
      const exams = examRes.data;
      if (exams.length > 0) {
        router.push(`/exam/${exams[0].examId}/assemble`);
        return;
      }
    } catch (e) { /* ignore */ }
  }
  message.warning('未找到包含该题目的试卷');
};

const parseOptions = (optionsStr) => {
  try { return JSON.parse(optionsStr); } catch { return []; }
};

onMounted(fetchFeedbacks);
</script>

<template>
  <div class="feedback-teacher-wrapper">
    <div class="feedback-header">
      <div class="header-content">
        <a-button type="link" @click="router.push('/dashboard')" class="back-btn">
          <LeftOutlined /> 返回
        </a-button>
        <span class="page-title">纠错工单</span>
        <a-badge :count="feedbacks.length" :overflow-count="99">
          <span style="color: #999; font-size: 14px;">待处理纠错</span>
        </a-badge>
      </div>
    </div>

    <div class="feedback-content">
      <a-empty v-if="!loading && feedbacks.length === 0" description="暂无待处理纠错工单" />
      <a-table v-else :loading="loading" :dataSource="feedbacks" :columns="[
        { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
        { title: '学生', key: 'student', width: 100 },
        { title: '问题类型', key: 'type', width: 110 },
        { title: '题目预览', key: 'question', ellipsis: true },
        { title: '补充说明', dataIndex: 'description', key: 'description', ellipsis: true, width: 180 },
        { title: '提交时间', dataIndex: 'createdAt', key: 'createdAt', width: 160, customRender: ({text}) => text ? new Date(text).toLocaleString() : '-' },
        { title: '操作', key: 'action', width: 240 }
      ]">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'student'">
            {{ record.student?.fullName || record.student?.username }}
          </template>
          <template v-if="column.key === 'type'">
            <a-tag :color="typeMap[record.type]?.color || 'default'">
              {{ typeMap[record.type]?.text || record.type }}
            </a-tag>
          </template>
          <template v-if="column.key === 'question'">
            <span v-html="(record.question?.content || '').substring(0, 60)"></span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="showDetail(record)">
                <EyeOutlined /> 查看
              </a-button>
              <a-button type="link" size="small" style="color: #52c41a" @click="openProcess(record, 'CONFIRM')">
                <CheckCircleOutlined /> 确认
              </a-button>
              <a-button type="link" size="small" danger @click="openProcess(record, 'REJECT')">
                <CloseCircleOutlined /> 驳回
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal v-model:open="detailVisible" title="纠错详情" :footer="null" width="720px">
      <template v-if="currentFeedback">
        <a-descriptions bordered :column="2" size="small">
          <a-descriptions-item label="学生">{{ currentFeedback.student?.fullName }}</a-descriptions-item>
          <a-descriptions-item label="问题类型">
            <a-tag :color="typeMap[currentFeedback.type]?.color || 'default'">
              {{ typeMap[currentFeedback.type]?.text || currentFeedback.type }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="提交时间">
            {{ currentFeedback.createdAt ? new Date(currentFeedback.createdAt).toLocaleString() : '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="补充说明" :span="2">
            {{ currentFeedback.description || '无' }}
          </a-descriptions-item>
        </a-descriptions>

        <a-divider>题目信息</a-divider>
        <div v-if="currentFeedback.question" class="question-detail">
          <div class="q-content" v-html="currentFeedback.question.content"></div>
          <div v-if="['SINGLE', 'MULTI'].includes(currentFeedback.question.type)" class="options-list">
            <div v-for="opt in parseOptions(currentFeedback.question.options)" :key="opt.label" class="option-row" :class="{ 'is-correct': currentFeedback.question.answer?.split(',').includes(opt.label) }">
              <span class="opt-key">{{ opt.label }}.</span>
              <span class="opt-text">{{ opt.text }}</span>
            </div>
          </div>
          <a-descriptions bordered :column="2" size="small" style="margin-top: 16px;">
            <a-descriptions-item label="正确答案">
              <span style="color: #1890ff; font-weight: 600;">{{ currentFeedback.question.answer }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="题目类型">
              {{ { 'SINGLE': '单选题', 'MULTI': '多选题', 'JUDGE': '判断题', 'SHORT': '简答题' }[currentFeedback.question.type] }}
            </a-descriptions-item>
          </a-descriptions>
        </div>

        <div style="margin-top: 16px; text-align: right;">
          <a-space>
            <a-button type="primary" @click="openProcess(currentFeedback, 'CONFIRM')">
              <EditOutlined /> 确认并跳转编辑
            </a-button>
            <a-button danger @click="openProcess(currentFeedback, 'REJECT')">
              <CloseCircleOutlined /> 驳回
            </a-button>
          </a-space>
        </div>
      </template>
    </a-modal>

    <a-modal v-model:open="processVisible" :title="processForm.action === 'CONFIRM' ? '确认纠错' : '驳回纠错'" :confirm-loading="processing" @ok="handleProcess" ok-text="确认" cancel-text="取消" width="500px">
      <a-form layout="vertical">
        <a-form-item v-if="processForm.action === 'CONFIRM'" label="操作说明">
          <p style="color: #666;">确认后系统将自动跳转至题库编辑页面，您可以修改该题目。</p>
        </a-form-item>
        <a-form-item label="处理说明" required>
          <a-textarea v-model:value="processForm.handlerComment" placeholder="请填写处理说明" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.feedback-teacher-wrapper {
  background-color: #f0f2f5;
  min-height: 100vh;
  padding-top: 80px;
  padding-bottom: 40px;
}
.feedback-header {
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
.feedback-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
}
.question-detail {
  background: #fafafa;
  padding: 16px;
  border-radius: 8px;
}
.q-content {
  font-size: 16px;
  line-height: 1.8;
  margin-bottom: 16px;
  color: #262626;
}
.options-list { margin-left: 10px; }
.option-row {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  border-radius: 6px;
  margin-bottom: 6px;
  border: 1px solid #f0f0f0;
}
.option-row.is-correct {
  background: #f6ffed;
  border-color: #b7eb8f;
}
.opt-key { font-weight: 700; width: 30px; color: #1890ff; }
.opt-text { flex: 1; }
</style>
