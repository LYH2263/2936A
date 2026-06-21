<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getPendingAppeals, processAppeal, getExamQuestions } from '@/api';
import { message } from 'ant-design-vue';
import { LeftOutlined, CheckCircleOutlined, CloseCircleOutlined, EyeOutlined } from '@ant-design/icons-vue';

const router = useRouter();
const appeals = ref([]);
const loading = ref(false);
const detailVisible = ref(false);
const processVisible = ref(false);
const currentAppeal = ref(null);
const processing = ref(false);
const processForm = ref({
  action: '',
  handlerComment: '',
  newScore: null
});
const maxScore = ref(100);

const statusMap = {
  'PENDING': { text: '待处理', color: 'orange' },
  'APPROVED': { text: '已通过', color: 'green' },
  'REJECTED': { text: '已驳回', color: 'red' }
};

const fetchAppeals = async () => {
  loading.value = true;
  try {
    const res = await getPendingAppeals();
    appeals.value = res.data;
  } catch (e) {
    message.error('获取申诉列表失败');
  } finally {
    loading.value = false;
  }
};

const showDetail = (appeal) => {
  currentAppeal.value = appeal;
  detailVisible.value = true;
};

const openProcess = async (appeal, action) => {
  currentAppeal.value = appeal;
  processForm.value = {
    action: action,
    handlerComment: '',
    newScore: null
  };
  if (action === 'APPROVE') {
    processForm.value.newScore = appeal.answer?.score || 0;
    try {
      const examId = appeal.submission?.exam?.id;
      const questionId = appeal.answer?.question?.id;
      const qRes = await getExamQuestions(examId);
      const eq = qRes.data.find(q => q.question.id === questionId);
      maxScore.value = eq ? eq.score : 100;
    } catch (e) {
      maxScore.value = 100;
    }
  }
  processVisible.value = true;
};

const handleProcess = async () => {
  if (processForm.value.action === 'APPROVE' && processForm.value.newScore === null) {
    message.warning('改分时必须提供新分数');
    return;
  }
  if (!processForm.value.handlerComment?.trim()) {
    message.warning('请填写处理说明');
    return;
  }

  processing.value = true;
  try {
    await processAppeal(currentAppeal.value.id, {
      action: processForm.value.action,
      handlerComment: processForm.value.handlerComment.trim(),
      newScore: processForm.value.action === 'APPROVE' ? processForm.value.newScore : null
    });
    message.success(processForm.value.action === 'APPROVE' ? '已通过申诉并改分' : '已驳回申诉');
    processVisible.value = false;
    detailVisible.value = false;
    fetchAppeals();
  } catch (e) {
    const msg = e.response?.data?.message || '处理失败';
    message.error(msg);
  } finally {
    processing.value = false;
  }
};

const parseOptions = (optionsStr) => {
  try { return JSON.parse(optionsStr); } catch { return []; }
};

onMounted(fetchAppeals);
</script>

<template>
  <div class="appeal-teacher-wrapper">
    <div class="appeal-header">
      <div class="header-content">
        <a-button type="link" @click="router.push('/dashboard')" class="back-btn">
          <LeftOutlined /> 返回
        </a-button>
        <span class="page-title">申诉处理台</span>
        <a-badge :count="appeals.length" :overflow-count="99">
          <span style="color: #999; font-size: 14px;">待处理申诉</span>
        </a-badge>
      </div>
    </div>

    <div class="appeal-content">
      <a-empty v-if="!loading && appeals.length === 0" description="暂无待处理申诉" />
      <a-table v-else :loading="loading" :dataSource="appeals" :columns="[
        { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
        { title: '学生', key: 'student', width: 100 },
        { title: '考试名称', key: 'exam', width: 160 },
        { title: '申诉题目', key: 'question', ellipsis: true },
        { title: '系统得分', key: 'score', width: 100 },
        { title: '申诉理由', dataIndex: 'reason', key: 'reason', ellipsis: true, width: 200 },
        { title: '申诉时间', dataIndex: 'createdAt', key: 'createdAt', width: 160, customRender: ({text}) => text ? new Date(text).toLocaleString() : '-' },
        { title: '操作', key: 'action', width: 220 }
      ]">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'student'">
            {{ record.student?.fullName || record.student?.username }}
          </template>
          <template v-if="column.key === 'exam'">
            {{ record.submission?.exam?.title }}
          </template>
          <template v-if="column.key === 'question'">
            <span v-html="(record.answer?.question?.content || '').substring(0, 60)"></span>
          </template>
          <template v-if="column.key === 'score'">
            <a-tag color="blue">{{ record.answer?.score }} 分</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="showDetail(record)">
                <EyeOutlined /> 查看
              </a-button>
              <a-button type="link" size="small" style="color: #52c41a" @click="openProcess(record, 'APPROVE')">
                <CheckCircleOutlined /> 改分
              </a-button>
              <a-button type="link" size="small" danger @click="openProcess(record, 'REJECT')">
                <CloseCircleOutlined /> 维持
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal v-model:open="detailVisible" title="申诉详情" :footer="null" width="720px">
      <template v-if="currentAppeal">
        <a-descriptions bordered :column="2" size="small">
          <a-descriptions-item label="学生">{{ currentAppeal.student?.fullName }}</a-descriptions-item>
          <a-descriptions-item label="考试">{{ currentAppeal.submission?.exam?.title }}</a-descriptions-item>
          <a-descriptions-item label="系统得分">
            <a-tag color="blue">{{ currentAppeal.answer?.score }} 分</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="申诉时间">
            {{ currentAppeal.createdAt ? new Date(currentAppeal.createdAt).toLocaleString() : '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="申诉理由" :span="2">
            {{ currentAppeal.reason }}
          </a-descriptions-item>
        </a-descriptions>

        <a-divider>原题信息</a-divider>
        <div v-if="currentAppeal.answer?.question" class="question-detail">
          <div class="q-content" v-html="currentAppeal.answer.question.content"></div>
          <div v-if="['SINGLE', 'MULTI'].includes(currentAppeal.answer.question.type)" class="options-list">
            <div v-for="opt in parseOptions(currentAppeal.answer.question.options)" :key="opt.label" class="option-row" :class="{ 'is-correct': currentAppeal.answer.question.answer?.split(',').includes(opt.label) }">
              <span class="opt-key">{{ opt.label }}.</span>
              <span class="opt-text">{{ opt.text }}</span>
            </div>
          </div>
          <a-descriptions bordered :column="2" size="small" style="margin-top: 16px;">
            <a-descriptions-item label="正确答案">
              <span style="color: #1890ff; font-weight: 600;">{{ currentAppeal.answer.question.answer }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="学生答案">
              <span :style="{ color: currentAppeal.answer.studentAnswer === currentAppeal.answer.question.answer ? '#52c41a' : '#f5222d', fontWeight: 600 }">
                {{ currentAppeal.answer.studentAnswer || '未作答' }}
              </span>
            </a-descriptions-item>
          </a-descriptions>
        </div>

        <div style="margin-top: 16px; text-align: right;">
          <a-space>
            <a-button type="primary" @click="openProcess(currentAppeal, 'APPROVE')">
              <CheckCircleOutlined /> 改分
            </a-button>
            <a-button danger @click="openProcess(currentAppeal, 'REJECT')">
              <CloseCircleOutlined /> 维持原判
            </a-button>
          </a-space>
        </div>
      </template>
    </a-modal>

    <a-modal v-model:open="processVisible" :title="processForm.action === 'APPROVE' ? '改分处理' : '维持原判'" :confirm-loading="processing" @ok="handleProcess" ok-text="确认" cancel-text="取消" width="500px">
      <a-form layout="vertical">
        <a-form-item v-if="processForm.action === 'APPROVE'" :label="'新分数（满分 ' + maxScore + '）'" required>
          <a-input-number v-model:value="processForm.newScore" :min="0" :max="maxScore" style="width: 100%;" />
        </a-form-item>
        <a-form-item label="处理说明" required>
          <a-textarea v-model:value="processForm.handlerComment" placeholder="请填写处理说明" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.appeal-teacher-wrapper {
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
