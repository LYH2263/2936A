<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { useConfigStore } from '@/stores/config';
import {
  getFlashSubjects,
  getFlashKnowledgePoints,
  getFlashTodayStats,
  startFlashSession,
  getFlashNextQuestion,
  submitFlashAnswer,
  endFlashSession
} from '@/api';
import { message } from 'ant-design-vue';
import {
  ArrowLeftOutlined,
  ThunderboltOutlined,
  CheckOutlined,
  CloseOutlined,
  RightOutlined,
  FireOutlined,
  TrophyOutlined,
  BulbOutlined,
  BookOutlined,
  HomeOutlined
} from '@ant-design/icons-vue';

const router = useRouter();
const configStore = useConfigStore();

const PHASE = {
  SETUP: 'setup',
  PRACTICING: 'practicing',
  RESULT: 'result'
};

const phase = ref(PHASE.SETUP);
const loading = ref(false);
const todayStats = ref({ totalQuestions: 0, correctCount: 0, accuracy: 0 });

const setupForm = ref({
  subject: null,
  difficulty: null,
  knowledgePoint: null
});

const subjects = ref([]);
const knowledgePoints = ref([]);
const difficultyOptions = [
  { value: 1, label: '入门', color: 'green' },
  { value: 2, label: '简单', color: 'cyan' },
  { value: 3, label: '中等', color: 'blue' },
  { value: 4, label: '困难', color: 'orange' },
  { value: 5, label: '地狱', color: 'red' }
];

const sessionId = ref(null);
const currentQuestion = ref(null);
const userAnswer = ref(null);
const multiAnswer = ref([]);
const showResult = ref(false);
const lastResult = ref(null);

const sessionStats = ref({
  totalQuestions: 0,
  correctCount: 0,
  currentStreak: 0,
  maxStreak: 0,
  accuracy: 0
});

const fetchSubjects = async () => {
  try {
    const res = await getFlashSubjects();
    subjects.value = res.data || [];
  } catch (e) {
    console.error('Failed to fetch subjects', e);
  }
};

const fetchKnowledgePoints = async (subject) => {
  try {
    const res = await getFlashKnowledgePoints(subject);
    knowledgePoints.value = res.data || [];
  } catch (e) {
    console.error('Failed to fetch knowledge points', e);
  }
};

const fetchTodayStats = async () => {
  try {
    const res = await getFlashTodayStats();
    todayStats.value = res.data || { totalQuestions: 0, correctCount: 0, accuracy: 0 };
  } catch (e) { /* ignore */ }
};

const onSubjectChange = (val) => {
  setupForm.value.knowledgePoint = null;
  if (val) {
    fetchKnowledgePoints(val);
  } else {
    knowledgePoints.value = [];
  }
};

const startPractice = async () => {
  if (!setupForm.value.subject) {
    message.warning('请选择科目');
    return;
  }
  loading.value = true;
  let createdSessionId = null;
  try {
    const res = await startFlashSession({
      subject: setupForm.value.subject,
      difficulty: setupForm.value.difficulty,
      knowledgePoint: setupForm.value.knowledgePoint
    });
    createdSessionId = res.data.id;
    sessionId.value = createdSessionId;
    sessionStats.value = {
      totalQuestions: 0,
      correctCount: 0,
      currentStreak: 0,
      maxStreak: 0,
      accuracy: 0
    };
    phase.value = PHASE.PRACTICING;
    currentQuestion.value = null;
    showResult.value = false;
    userAnswer.value = null;
    multiAnswer.value = [];
    lastResult.value = null;
    const qRes = await getFlashNextQuestion(createdSessionId);
    currentQuestion.value = qRes.data;
  } catch (e) {
    const errMsg = e?.response?.data?.message || e?.message || '开始闪练失败';
    message.error(errMsg);
    sessionId.value = null;
    currentQuestion.value = null;
    phase.value = PHASE.SETUP;
  } finally {
    loading.value = false;
  }
};

const fetchNextQuestion = async () => {
  loading.value = true;
  showResult.value = false;
  userAnswer.value = null;
  multiAnswer.value = [];
  lastResult.value = null;
  try {
    const res = await getFlashNextQuestion(sessionId.value);
    currentQuestion.value = res.data;
  } catch (e) {
    const errMsg = e?.response?.data?.message || e?.message || '获取题目失败';
    message.warning(errMsg);
    sessionId.value = null;
    currentQuestion.value = null;
    phase.value = PHASE.SETUP;
  } finally {
    loading.value = false;
  }
};

const parsedOptions = computed(() => {
  if (!currentQuestion.value?.options) return [];
  try {
    return JSON.parse(currentQuestion.value.options);
  } catch (e) {
    return [];
  }
});

const isJudgeType = computed(() => currentQuestion.value?.type === 'JUDGE');
const isSingleType = computed(() => currentQuestion.value?.type === 'SINGLE');
const isMultiType = computed(() => currentQuestion.value?.type === 'MULTI');
const isShortType = computed(() => currentQuestion.value?.type === 'SHORT');

const canSubmit = computed(() => {
  if (!currentQuestion.value) return false;
  if (showResult.value) return false;
  if (isSingleType.value || isJudgeType.value) return userAnswer.value !== null && userAnswer.value !== '';
  if (isMultiType.value) return multiAnswer.value.length > 0;
  if (isShortType.value) return userAnswer.value && userAnswer.value.trim().length > 0;
  return false;
});

const submitAnswer = async () => {
  let answer;
  if (isMultiType.value) {
    answer = [...multiAnswer.value].sort().join(',');
  } else {
    answer = userAnswer.value;
  }
  loading.value = true;
  try {
    const res = await submitFlashAnswer(sessionId.value, {
      questionId: currentQuestion.value.id,
      answer: answer
    });
    lastResult.value = res.data;
    sessionStats.value = {
      totalQuestions: res.data.totalQuestions,
      correctCount: res.data.correctCount,
      currentStreak: res.data.currentStreak,
      maxStreak: res.data.maxStreak,
      accuracy: res.data.accuracy
    };
    showResult.value = true;
    await fetchTodayStats();
  } catch (e) {
    message.error('提交答案失败');
  } finally {
    loading.value = false;
  }
};

const nextQuestion = () => {
  fetchNextQuestion();
};

const quitPractice = async () => {
  if (sessionId.value) {
    try {
      await endFlashSession(sessionId.value);
    } catch (e) { /* ignore */ }
  }
  router.push('/dashboard?tab=overview');
};

const goBackToSetup = () => {
  phase.value = PHASE.SETUP;
  currentQuestion.value = null;
  showResult.value = false;
  sessionId.value = null;
};

const getDifficultyLabel = (val) => {
  const opt = difficultyOptions.find(o => o.value === val);
  return opt ? opt.label : '-';
};

const getDifficultyColor = (val) => {
  const opt = difficultyOptions.find(o => o.value === val);
  return opt ? opt.color : 'default';
};

const streakColor = computed(() => {
  const s = sessionStats.value.currentStreak;
  if (s >= 10) return '#f5222d';
  if (s >= 5) return '#fa8c16';
  if (s >= 3) return '#faad14';
  return '#52c41a';
});

const formatJudgeAnswer = (val) => {
  if (!val) return '-';
  const v = (val + '').trim().toUpperCase();
  if (v === 'TRUE' || v === 'T' || v === '正确' || v === '对') return 'TRUE (正确)';
  if (v === 'FALSE' || v === 'F' || v === '错误' || v === '错') return 'FALSE (错误)';
  return val;
};

const formattedCorrectAnswer = computed(() => {
  if (!lastResult.value?.correctAnswer) return '-';
  const ans = lastResult.value.correctAnswer;
  if (isJudgeType.value) return formatJudgeAnswer(ans);
  if (isMultiType.value) {
    return ans.split(/[, ]+/).filter(s => s).sort().join(', ');
  }
  return ans;
});

const formattedUserAnswer = computed(() => {
  if (!lastResult.value) return '-';
  if (isJudgeType.value) {
    if (!userAnswer.value) return '(未作答)';
    return formatJudgeAnswer(userAnswer.value);
  }
  if (isMultiType.value) {
    return multiAnswer.value.length > 0 ? [...multiAnswer.value].sort().join(', ') : '(未作答)';
  }
  return userAnswer.value && userAnswer.value.trim() !== '' ? userAnswer.value : '(未作答)';
});

onMounted(() => {
  fetchSubjects();
  fetchTodayStats();
});

onBeforeUnmount(() => {
  if (sessionId.value && phase.value === PHASE.PRACTICING) {
    endFlashSession(sessionId.value).catch(() => {});
  }
});
</script>

<template>
  <div class="flash-practice-container">
    <div class="flash-header">
      <div class="flash-header-inner">
        <a-button type="text" class="back-btn" @click="quitPractice">
          <HomeOutlined /> 返回主页
        </a-button>
        <div class="flash-title-wrap">
          <ThunderboltOutlined class="flash-title-icon" />
          <span class="flash-title">轻量速练 · 闪练</span>
        </div>
        <div class="today-mini-stats">
          <span class="mini-stat"><BookOutlined /> 今日 {{ todayStats.totalQuestions }} 题</span>
          <span class="mini-stat" style="color: #52c41a;"><TrophyOutlined /> {{ todayStats.accuracy || 0 }}%</span>
        </div>
      </div>
    </div>

    <div class="flash-body">
      <!-- SETUP PHASE -->
      <div v-if="phase === PHASE.SETUP" class="setup-wrapper">
        <a-card class="setup-card" :bordered="false">
          <div class="setup-header">
            <div class="setup-icon-wrap">
              <BulbOutlined class="setup-icon" />
            </div>
            <h2 class="setup-title">开始闪练前，选择你的练习范围</h2>
            <p class="setup-subtitle">系统将根据条件随机抽取单题，答完即判分并显示解析</p>
          </div>

          <a-form :model="setupForm" layout="vertical" class="setup-form">
            <a-form-item label="科目" required>
              <a-select
                v-model:value="setupForm.subject"
                placeholder="请选择科目"
                size="large"
                show-search
                option-filter-prop="children"
                @change="onSubjectChange"
              >
                <a-select-option v-for="s in subjects" :key="s" :value="s">{{ s }}</a-select-option>
              </a-select>
            </a-form-item>

            <a-form-item label="难度">
              <a-radio-group v-model:value="setupForm.difficulty" button-style="solid" size="large">
                <a-radio-button :value="null">全部</a-radio-button>
                <a-radio-button
                  v-for="d in difficultyOptions"
                  :key="d.value"
                  :value="d.value"
                >
                  {{ d.label }}
                </a-radio-button>
              </a-radio-group>
            </a-form-item>

            <a-form-item label="知识点">
              <a-select
                v-model:value="setupForm.knowledgePoint"
                placeholder="全部知识点（可选）"
                size="large"
                allowClear
                show-search
                option-filter-prop="children"
                :disabled="!setupForm.subject"
              >
                <a-select-option v-for="k in knowledgePoints" :key="k" :value="k">{{ k }}</a-select-option>
              </a-select>
            </a-form-item>

            <div class="setup-summary" v-if="setupForm.subject">
              <div class="summary-item">
                <span class="summary-label">已选科目</span>
                <a-tag color="purple">{{ setupForm.subject }}</a-tag>
              </div>
              <div class="summary-item">
                <span class="summary-label">难度</span>
                <a-tag :color="setupForm.difficulty ? getDifficultyColor(setupForm.difficulty) : 'default'">
                  {{ setupForm.difficulty ? getDifficultyLabel(setupForm.difficulty) : '全部' }}
                </a-tag>
              </div>
              <div class="summary-item">
                <span class="summary-label">知识点</span>
                <a-tag color="cyan">{{ setupForm.knowledgePoint || '全部' }}</a-tag>
              </div>
            </div>

            <a-button
              type="primary"
              size="large"
              block
              class="start-btn"
              :loading="loading"
              @click="startPractice"
            >
              <ThunderboltOutlined /> 开始闪练
            </a-button>
          </a-form>
        </a-card>
      </div>

      <!-- PRACTICING PHASE -->
      <div v-else-if="phase === PHASE.PRACTICING" class="practice-wrapper">
        <div class="progress-bar-section">
          <div class="progress-top">
            <div class="streak-display" :style="{ color: streakColor }">
              <FireOutlined class="fire-icon" />
              <span class="streak-label">连对</span>
              <span class="streak-num">{{ sessionStats.currentStreak }}</span>
            </div>
            <div class="stats-display">
              <span class="stat-chip">
                <BookOutlined /> {{ sessionStats.totalQuestions }} 题
              </span>
              <span class="stat-chip correct-chip">
                <CheckOutlined /> {{ sessionStats.correctCount }}
              </span>
              <span class="stat-chip accuracy-chip">
                <TrophyOutlined /> {{ sessionStats.accuracy || 0 }}%
              </span>
            </div>
          </div>
          <a-progress
            :percent="sessionStats.accuracy || 0"
            :show-info="false"
            :stroke-color="sessionStats.accuracy >= 60 ? '#52c41a' : '#1890ff'"
            :trail-color="'rgba(255,255,255,0.1)'"
            size="small"
          />
        </div>

        <div class="question-area">
          <a-skeleton :loading="loading && !currentQuestion" active :paragraph="{ rows: 6 }">
            <a-card v-if="currentQuestion" class="question-card" :bordered="false">
              <div class="question-meta">
                <a-tag color="purple">{{ currentQuestion.subject || '-' }}</a-tag>
                <a-tag :color="getDifficultyColor(currentQuestion.difficulty)">
                  难度: {{ getDifficultyLabel(currentQuestion.difficulty) }}
                </a-tag>
                <a-tag v-if="currentQuestion.knowledgePoint" color="cyan">
                  {{ currentQuestion.knowledgePoint }}
                </a-tag>
                <a-tag color="orange">
                  {{ { SINGLE: '单选题', MULTI: '多选题', JUDGE: '判断题', SHORT: '简答题' }[currentQuestion.type] }}
                </a-tag>
              </div>

              <div class="question-content">
                <span class="q-mark">Q.</span>
                <div class="q-text" v-html="currentQuestion.content"></div>
              </div>

              <!-- Options: SINGLE / MULTI -->
              <div v-if="isSingleType || isMultiType" class="options-list">
                <div
                  v-for="opt in parsedOptions"
                  :key="opt.label"
                  :class="[
                    'option-item',
                    isSingleType ? 'single' : 'multi',
                    {
                      'selected': isSingleType ? userAnswer === opt.label : multiAnswer.includes(opt.label),
                      'correct-highlight': showResult && lastResult?.correct && isSingleType ? userAnswer === opt.label : (showResult && lastResult?.correctAnswer?.includes(opt.label)),
                      'wrong-highlight': showResult && !lastResult?.correct && isSingleType && userAnswer === opt.label
                    }
                  ]"
                  @click="!showResult && (isSingleType ? (userAnswer = opt.label) : (multiAnswer.includes(opt.label) ? multiAnswer = multiAnswer.filter(x => x !== opt.label) : multiAnswer.push(opt.label)))"
                >
                  <span class="opt-label">{{ opt.label }}.</span>
                  <span class="opt-text" v-html="opt.text"></span>
                  <CheckOutlined
                    v-if="showResult && lastResult?.correctAnswer?.includes(opt.label)"
                    class="opt-indicator correct-indicator"
                  />
                  <CloseOutlined
                    v-else-if="showResult && !lastResult?.correct && (isSingleType ? userAnswer === opt.label : multiAnswer.includes(opt.label)) && !lastResult?.correctAnswer?.includes(opt.label)"
                    class="opt-indicator wrong-indicator"
                  />
                </div>
              </div>

              <!-- JUDGE -->
              <div v-else-if="isJudgeType" class="judge-options">
                <div
                  :class="['judge-btn', { selected: userAnswer === 'TRUE', 'correct-highlight': showResult && lastResult?.correctAnswer === 'TRUE', 'wrong-highlight': showResult && !lastResult?.correct && userAnswer === 'TRUE' }]"
                  @click="!showResult && (userAnswer = 'TRUE')"
                >
                  <CheckOutlined class="judge-icon" />
                  <span>正确 (TRUE)</span>
                </div>
                <div
                  :class="['judge-btn', { selected: userAnswer === 'FALSE', 'correct-highlight': showResult && lastResult?.correctAnswer === 'FALSE', 'wrong-highlight': showResult && !lastResult?.correct && userAnswer === 'FALSE' }]"
                  @click="!showResult && (userAnswer = 'FALSE')"
                >
                  <CloseOutlined class="judge-icon" />
                  <span>错误 (FALSE)</span>
                </div>
              </div>

              <!-- SHORT -->
              <div v-else-if="isShortType" class="short-answer-area">
                <a-textarea
                  v-model:value="userAnswer"
                  placeholder="请输入你的答案..."
                  :rows="4"
                  :disabled="showResult"
                  :maxlength="1000"
                  show-count
                />
              </div>

              <!-- Result display -->
              <div v-if="showResult && lastResult" class="result-section">
                <a-divider />
                <div :class="['result-header', lastResult.correct ? 'result-correct' : 'result-wrong']">
                  <CheckOutlined v-if="lastResult.correct" class="result-icon" />
                  <CloseOutlined v-else class="result-icon" />
                  <span class="result-text">{{ lastResult.correct ? '回答正确！' : '回答错误' }}</span>
                  <span v-if="lastResult.currentStreak >= 3 && lastResult.correct" class="streak-toast">
                    <FireOutlined /> 连对 {{ lastResult.currentStreak }} 题！
                  </span>
                </div>

                <div class="result-compare">
                  <div class="compare-row">
                    <span class="compare-label">你的答案:</span>
                    <span :class="['compare-value', lastResult.correct ? 'text-correct' : 'text-wrong']">
                      {{ formattedUserAnswer }}
                    </span>
                  </div>
                  <div class="compare-row">
                    <span class="compare-label">正确答案:</span>
                    <span class="compare-value text-correct">{{ formattedCorrectAnswer }}</span>
                  </div>
                </div>

                <div v-if="lastResult.analysis" class="analysis-section">
                  <div class="analysis-title">
                    <BulbOutlined /> 答案解析
                  </div>
                  <div class="analysis-content" v-html="lastResult.analysis"></div>
                </div>

                <div class="session-summary-row">
                  <div class="summary-pill">
                    <TrophyOutlined /> 本次正确率: <b>{{ lastResult.accuracy }}%</b>
                  </div>
                  <div class="summary-pill" style="color: streakColor">
                    <FireOutlined /> 最高连对: <b>{{ lastResult.maxStreak }}</b>
                  </div>
                </div>
              </div>

              <div class="action-area">
                <a-button
                  v-if="!showResult"
                  type="primary"
                  size="large"
                  :disabled="!canSubmit"
                  :loading="loading"
                  @click="submitAnswer"
                  block
                  class="submit-btn"
                >
                  提交答案
                </a-button>
                <a-button
                  v-else
                  type="primary"
                  size="large"
                  @click="nextQuestion"
                  block
                  class="next-btn"
                >
                  下一题 <RightOutlined />
                </a-button>
              </div>
            </a-card>
          </a-skeleton>
        </div>

        <div class="bottom-actions">
          <a-button @click="goBackToSetup">
            <ArrowLeftOutlined /> 重新选择条件
          </a-button>
          <a-button danger @click="quitPractice">
            结束练习
          </a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.flash-practice-container {
  min-height: 100vh;
  background: linear-gradient(180deg, #1e1b4b 0%, #312e81 40%, #4338ca 100%);
  display: flex;
  flex-direction: column;
  color: white;
  position: relative;
  overflow-x: hidden;
}
.flash-practice-container::before {
  content: '';
  position: absolute;
  top: -200px;
  right: -200px;
  width: 500px;
  height: 500px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(250, 204, 21, 0.15) 0%, transparent 70%);
  pointer-events: none;
}
.flash-practice-container::after {
  content: '';
  position: absolute;
  bottom: -150px;
  left: -150px;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.2) 0%, transparent 70%);
  pointer-events: none;
}

.flash-header {
  padding: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(8px);
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgba(30, 27, 75, 0.6);
}
.flash-header-inner {
  max-width: 960px;
  margin: 0 auto;
  padding: 14px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.back-btn {
  color: rgba(255, 255, 255, 0.75) !important;
  font-size: 14px;
}
.back-btn:hover {
  color: white !important;
}
.flash-title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}
.flash-title-icon {
  font-size: 22px;
  color: #facc15;
  text-shadow: 0 0 16px rgba(250, 204, 21, 0.5);
}
.flash-title {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
.today-mini-stats {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
}
.mini-stat {
  display: flex;
  align-items: center;
  gap: 4px;
}

.flash-body {
  flex: 1;
  padding: 32px 24px;
  max-width: 960px;
  margin: 0 auto;
  width: 100%;
  position: relative;
  z-index: 1;
  box-sizing: border-box;
}

/* SETUP */
.setup-wrapper {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding-top: 40px;
}
.setup-card {
  width: 100%;
  max-width: 560px;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  padding: 40px 36px;
  color: #1a1a1a;
}
.setup-header {
  text-align: center;
  margin-bottom: 32px;
}
.setup-icon-wrap {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.35);
}
.setup-icon {
  font-size: 28px;
  color: white;
}
.setup-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 8px;
  color: #1a1a1a;
}
.setup-subtitle {
  font-size: 14px;
  color: #666;
  margin: 0;
}
.setup-form {
  margin-top: 8px;
}
:deep(.setup-form .ant-form-item-label > label) {
  font-weight: 600;
  color: #333;
  font-size: 14px;
}
.setup-summary {
  background: #f6f8ff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 24px;
  border: 1px solid #e0e7ff;
}
.summary-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 0;
}
.summary-label {
  font-size: 13px;
  color: #666;
}
.start-btn {
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}
.start-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.5);
}

/* PRACTICE */
.practice-wrapper {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.progress-bar-section {
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  padding: 16px 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}
.progress-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.streak-display {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 700;
}
.fire-icon {
  font-size: 20px;
  animation: firePulse 1.5s ease-in-out infinite;
}
@keyframes firePulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.15); }
}
.streak-label {
  font-size: 14px;
  opacity: 0.85;
}
.streak-num {
  font-size: 26px;
  font-weight: 800;
  text-shadow: 0 2px 8px currentColor;
}
.stats-display {
  display: flex;
  gap: 8px;
}
.stat-chip {
  background: rgba(255, 255, 255, 0.12);
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}
.correct-chip { color: #52c41a; }
.accuracy-chip { color: #1890ff; font-weight: 600; }

.question-area {
  flex: 1;
}
.question-card {
  border-radius: 20px;
  padding: 32px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.25);
  color: #1a1a1a;
}
.question-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 24px;
}
.question-content {
  display: flex;
  gap: 12px;
  margin-bottom: 28px;
  padding: 20px;
  background: #fafbff;
  border-radius: 14px;
  border-left: 4px solid #667eea;
}
.q-mark {
  font-size: 24px;
  font-weight: 800;
  color: #667eea;
  flex-shrink: 0;
  line-height: 1.5;
}
.q-text {
  flex: 1;
  font-size: 16px;
  line-height: 1.8;
  color: #1a1a1a;
  font-weight: 500;
}
.q-text :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
}
.option-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 18px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  background: white;
}
.option-item:hover {
  border-color: #667eea;
  background: #f5f6ff;
}
.option-item.selected {
  border-color: #667eea;
  background: #eef0ff;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.15);
}
.option-item.correct-highlight {
  border-color: #52c41a !important;
  background: #f6ffed !important;
}
.option-item.wrong-highlight {
  border-color: #ff4d4f !important;
  background: #fff1f0 !important;
}
.opt-label {
  font-weight: 700;
  color: #667eea;
  font-size: 15px;
  min-width: 22px;
}
.opt-text {
  flex: 1;
  font-size: 14px;
  line-height: 1.6;
  color: #333;
}
.opt-indicator {
  font-size: 16px;
  flex-shrink: 0;
  margin-top: 2px;
}
.correct-indicator { color: #52c41a; }
.wrong-indicator { color: #ff4d4f; }

.judge-options {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}
.judge-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px;
  border: 2px solid #e5e7eb;
  border-radius: 14px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  transition: all 0.2s;
  color: #555;
}
.judge-btn:hover {
  border-color: #667eea;
  background: #f5f6ff;
}
.judge-btn.selected {
  border-color: #667eea;
  background: #eef0ff;
  color: #667eea;
}
.judge-btn.correct-highlight {
  border-color: #52c41a !important;
  background: #f6ffed !important;
  color: #52c41a !important;
}
.judge-btn.wrong-highlight {
  border-color: #ff4d4f !important;
  background: #fff1f0 !important;
  color: #ff4d4f !important;
}
.judge-icon {
  font-size: 20px;
}

.short-answer-area {
  margin-bottom: 24px;
}

.result-section {
  margin-top: 8px;
}
.result-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
  border-radius: 12px;
  font-weight: 700;
  font-size: 16px;
  margin-bottom: 16px;
}
.result-correct {
  background: #f6ffed;
  color: #389e0d;
  border: 1px solid #b7eb8f;
}
.result-wrong {
  background: #fff1f0;
  color: #cf1322;
  border: 1px solid #ffa39e;
}
.result-icon {
  font-size: 20px;
}
.result-text {
  flex: 1;
}
.streak-toast {
  background: #fff7e6;
  color: #d46b08;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 13px;
  border: 1px solid #ffd591;
  font-weight: 600;
  animation: streakPop 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55);
}
@keyframes streakPop {
  0% { transform: scale(0.5); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}

.result-compare {
  background: #fafafa;
  border-radius: 12px;
  padding: 14px 18px;
  margin-bottom: 16px;
}
.compare-row {
  display: flex;
  align-items: flex-start;
  padding: 6px 0;
  gap: 12px;
}
.compare-label {
  font-size: 13px;
  color: #888;
  min-width: 72px;
  flex-shrink: 0;
}
.compare-value {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  word-break: break-word;
}
.text-correct { color: #389e0d; }
.text-wrong { color: #cf1322; }

.analysis-section {
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 12px;
  padding: 16px 18px;
  margin-bottom: 16px;
}
.analysis-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 700;
  color: #d46b08;
  font-size: 14px;
  margin-bottom: 10px;
}
.analysis-content {
  font-size: 14px;
  line-height: 1.8;
  color: #613400;
}
.analysis-content :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

.session-summary-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.summary-pill {
  background: #f0f5ff;
  color: #1d39c4;
  padding: 8px 14px;
  border-radius: 20px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #d6e4ff;
}

.action-area {
  margin-top: 24px;
}
.submit-btn, .next-btn {
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
}
.submit-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.35);
}
.next-btn {
  background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(82, 196, 26, 0.35);
}
.next-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(82, 196, 26, 0.45) !important;
}

.bottom-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 4px;
}
:deep(.bottom-actions .ant-btn) {
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.1);
  color: white !important;
  border-color: rgba(255, 255, 255, 0.2) !important;
  backdrop-filter: blur(4px);
}
:deep(.bottom-actions .ant-btn:hover) {
  background: rgba(255, 255, 255, 0.18) !important;
}
:deep(.bottom-actions .ant-btn-dangerous) {
  background: rgba(255, 77, 79, 0.15) !important;
  border-color: rgba(255, 77, 79, 0.4) !important;
}
:deep(.bottom-actions .ant-btn-dangerous:hover) {
  background: rgba(255, 77, 79, 0.25) !important;
}
</style>
