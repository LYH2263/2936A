<script setup>
import { ref, onMounted, computed, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getAllQuestions, updateQuestionContent } from '@/api';
import { message } from 'ant-design-vue';
import { 
  LeftOutlined, SearchOutlined, EditOutlined, SaveOutlined, CloseOutlined,
  FilterOutlined, CheckCircleOutlined
} from '@ant-design/icons-vue';

const route = useRoute();
const router = useRouter();

const questions = ref([]);
const loading = ref(false);
const editingId = ref(null);
const saving = ref(false);

const filterSubject = ref('');
const filterType = ref(null);
const filterDifficulty = ref(null);
const filterText = ref('');

const highlightId = ref(null);
const highlightedRow = ref(null);

const typeMap = {
  SINGLE: '单选题',
  MULTI: '多选题',
  JUDGE: '判断题',
  SHORT: '简答题'
};

const typeOptions = [
  { value: 'SINGLE', label: '单选题' },
  { value: 'MULTI', label: '多选题' },
  { value: 'JUDGE', label: '判断题' },
  { value: 'SHORT', label: '简答题' }
];

const difficultyOptions = [
  { value: 1, label: '简单' },
  { value: 2, label: '较易' },
  { value: 3, label: '中等' },
  { value: 4, label: '较难' },
  { value: 5, label: '困难' }
];

const scrollToHighlight = async () => {
  if (!highlightId.value) return;
  await nextTick();
  const row = document.getElementById(`question-row-${highlightId.value}`);
  if (row) {
    row.scrollIntoView({ behavior: 'smooth', block: 'center' });
    highlightedRow.value = highlightId.value;
    setTimeout(() => { highlightedRow.value = null; }, 3000);
  }
};

const fetchQuestions = async () => {
  loading.value = true;
  try {
    const res = await getAllQuestions();
    questions.value = res.data;

    if (highlightId.value) {
      filterSubject.value = '';
      filterType.value = null;
      filterDifficulty.value = null;
      filterText.value = '';
      const target = questions.value.find(q => q.id === highlightId.value);
      if (target) {
        startEdit(target);
        await nextTick();
        await scrollToHighlight();
      }
    }
  } catch (e) {
    message.error('加载题库失败');
  } finally {
    loading.value = false;
  }
};

const filteredQuestions = computed(() => {
  return questions.value.filter(q => {
    if (filterSubject.value && !q.subject?.includes(filterSubject.value)) return false;
    if (filterType.value && q.type !== filterType.value) return false;
    if (filterDifficulty.value && q.difficulty !== filterDifficulty.value) return false;
    if (filterText.value && !q.content?.includes(filterText.value)) return false;
    return true;
  });
});

const distinctSubjects = computed(() => {
  const s = new Set(questions.value.map(q => q.subject).filter(Boolean));
  return Array.from(s);
});

const editForm = ref({
  content: '',
  type: 'SINGLE',
  options: [],
  answer: '',
  analysis: '',
  subject: '',
  knowledgePoint: '',
  difficulty: 3,
  defaultScore: 5
});

const parseOptions = (optionsStr) => {
  try { return JSON.parse(optionsStr); } catch { return []; }
};

const startEdit = (q) => {
  editingId.value = q.id;
  const opts = parseOptions(q.options);
  editForm.value = {
    content: q.content,
    type: q.type,
    options: opts.length > 0 ? opts : [{ label: 'A', text: '' }, { label: 'B', text: '' }, { label: 'C', text: '' }, { label: 'D', text: '' }],
    answer: q.answer || '',
    analysis: q.analysis || '',
    subject: q.subject || '',
    knowledgePoint: q.knowledgePoint || '',
    difficulty: q.difficulty || 3,
    defaultScore: q.defaultScore || 5
  };
};

const cancelEdit = () => {
  editingId.value = null;
};

const addOption = () => {
  const labels = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'];
  const nextLabel = labels[editForm.value.options.length] || String(editForm.value.options.length + 1);
  editForm.value.options.push({ label: nextLabel, text: '' });
};

const removeOption = (index) => {
  if (editForm.value.options.length > 2) {
    editForm.value.options.splice(index, 1);
  } else {
    message.warning('至少需要2个选项');
  }
};

const saveEdit = async (questionId) => {
  if (!editForm.value.content?.trim()) {
    message.warning('题干不能为空');
    return;
  }
  if (!editForm.value.answer?.trim()) {
    message.warning('请填写正确答案');
    return;
  }
  if (['SINGLE', 'MULTI', 'JUDGE'].includes(editForm.value.type)) {
    const validOptions = editForm.value.options.filter(o => o.text?.trim());
    if (validOptions.length < 2) {
      message.warning('至少需要2个有效选项');
      return;
    }
    const answer = editForm.value.answer.toUpperCase();
    const labels = editForm.value.options.map(o => o.label);
    if (editForm.value.type === 'MULTI') {
      const answerParts = answer.split(',');
      for (const a of answerParts) {
        if (!labels.includes(a.trim())) {
          message.warning(`答案 ${a.trim()} 不在选项中`);
          return;
        }
      }
    } else if (!labels.includes(answer)) {
      message.warning(`答案 ${answer} 不在选项中`);
      return;
    }
  }

  saving.value = true;
  try {
    const payload = {
      content: editForm.value.content.trim(),
      type: editForm.value.type,
      options: JSON.stringify(editForm.value.options),
      answer: editForm.value.answer.trim().toUpperCase(),
      analysis: editForm.value.analysis?.trim() || '',
      subject: editForm.value.subject?.trim() || '',
      knowledgePoint: editForm.value.knowledgePoint?.trim() || '',
      difficulty: editForm.value.difficulty,
      defaultScore: editForm.value.defaultScore
    };

    await updateQuestionContent(questionId, payload);
    message.success('题目已更新');
    editingId.value = null;
    fetchQuestions();
  } catch (e) {
    const msg = e.response?.data?.message || '保存失败';
    message.error(msg);
  } finally {
    saving.value = false;
  }
};

const getDifficultyColor = (d) => {
  if (d <= 2) return 'green';
  if (d <= 4) return 'orange';
  return 'red';
};

onMounted(() => {
  if (route.query.highlight) {
    highlightId.value = parseInt(route.query.highlight);
  }
  fetchQuestions();
});
</script>

<template>
  <div class="question-bank-wrapper">
    <div class="bank-header">
      <div class="header-content">
        <a-button type="link" @click="router.push('/dashboard')" class="back-btn">
          <LeftOutlined /> 返回
        </a-button>
        <span class="page-title">题库管理</span>
        <span class="page-subtitle">共 {{ questions.length }} 道题</span>
      </div>
    </div>

    <div class="bank-content">
      <a-card :bordered="false" class="filter-card">
        <a-space size="large" wrap>
          <a-input-search
            v-model:value="filterText"
            placeholder="搜索题干..."
            style="width: 300px"
            allow-clear
          >
            <template #prefix><SearchOutlined /></template>
          </a-input-search>
          <a-select
            v-model:value="filterSubject"
            placeholder="选择学科"
            allow-clear
            style="width: 160px"
          >
            <a-select-option v-for="s in distinctSubjects" :key="s" :value="s">{{ s }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="filterType"
            placeholder="选择题型"
            allow-clear
            style="width: 140px"
          >
            <a-select-option v-for="opt in typeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="filterDifficulty"
            placeholder="选择难度"
            allow-clear
            style="width: 140px"
          >
            <a-select-option v-for="opt in difficultyOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</a-select-option>
          </a-select>
        </a-space>
      </a-card>

      <a-table
        :loading="loading"
        :dataSource="filteredQuestions"
        rowKey="id"
        size="middle"
        :scroll="{ x: 900 }"
        :rowClassName="(record) => highlightedRow === record.id ? 'highlighted-row' : ''"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'id'">
            <span :id="`question-row-${record.id}`">{{ record.id }}</span>
          </template>
          <template v-if="column.key === 'type'">
            <a-tag color="blue">{{ typeMap[record.type] || record.type }}</a-tag>
          </template>
          <template v-if="column.key === 'content'">
            <span v-if="editingId !== record.id" v-html="(record.content || '').substring(0, 80)"></span>
            <span v-else style="color: #1890ff;">正在编辑...</span>
          </template>
          <template v-if="column.key === 'difficulty'">
            <a-tag :color="getDifficultyColor(record.difficulty)">
              {{ difficultyOptions.find(d => d.value === record.difficulty)?.label || record.difficulty }}
            </a-tag>
          </template>
          <template v-if="column.key === 'answer'">
            <span style="font-weight: 600; color: #52c41a;">{{ record.answer || '-' }}</span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button
                v-if="editingId !== record.id"
                type="link"
                size="small"
                @click="startEdit(record)"
              >
                <EditOutlined /> 编辑
              </a-button>
              <template v-else>
                <a-button type="link" size="small" :loading="saving" style="color: #52c41a" @click="saveEdit(record.id)">
                  <SaveOutlined /> 保存
                </a-button>
                <a-button type="link" size="small" danger @click="cancelEdit">
                  <CloseOutlined /> 取消
                </a-button>
              </template>
            </a-space>
          </template>
        </template>

        <template #expandedRowRender="{ record }">
          <div v-if="editingId === record.id" class="edit-form">
            <a-form layout="vertical">
              <a-row :gutter="24">
                <a-col :span="16">
                  <a-form-item label="题干" required>
                    <a-textarea v-model:value="editForm.content" :rows="3" placeholder="请输入题干" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-row :gutter="12">
                    <a-col :span="12">
                      <a-form-item label="题型">
                        <a-select v-model:value="editForm.type">
                          <a-select-option v-for="opt in typeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</a-select-option>
                        </a-select>
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="正确答案" required>
                        <a-input v-model:value="editForm.answer" placeholder="如: A 或 A,B,C" />
                      </a-form-item>
                    </a-col>
                  </a-row>
                </a-col>
              </a-row>

              <div v-if="['SINGLE', 'MULTI', 'JUDGE'].includes(editForm.type)">
                <a-form-item label="选项">
                  <div class="options-editor">
                    <div v-for="(opt, idx) in editForm.options" :key="opt.label" class="option-row">
                      <span class="opt-label">{{ opt.label }}.</span>
                      <a-input v-model:value="opt.text" placeholder="请输入选项内容" style="flex: 1;" />
                      <a-button
                        v-if="editForm.options.length > 2"
                        type="link"
                        danger
                        size="small"
                        @click="removeOption(idx)"
                      >
                        删除
                      </a-button>
                    </div>
                    <a-button type="dashed" block @click="addOption" style="margin-top: 8px;">
                      + 添加选项
                    </a-button>
                  </div>
                </a-form-item>
              </div>

              <a-form-item label="答案解析">
                <a-textarea v-model:value="editForm.analysis" :rows="2" placeholder="请输入答案解析（可选）" />
              </a-form-item>

              <a-row :gutter="24">
                <a-col :span="8">
                  <a-form-item label="学科">
                    <a-input v-model:value="editForm.subject" placeholder="如: 数学" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="知识点">
                    <a-input v-model:value="editForm.knowledgePoint" placeholder="如: 一元二次方程" />
                  </a-form-item>
                </a-col>
                <a-col :span="4">
                  <a-form-item label="难度">
                    <a-select v-model:value="editForm.difficulty">
                      <a-select-option v-for="opt in difficultyOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :span="4">
                  <a-form-item label="默认分值">
                    <a-input-number v-model:value="editForm.defaultScore" :min="1" :max="100" style="width: 100%;" />
                  </a-form-item>
                </a-col>
              </a-row>
            </a-form>
          </div>
          <div v-else class="question-preview">
            <div class="q-content" v-html="record.content"></div>
            <div v-if="['SINGLE', 'MULTI'].includes(record.type)" class="q-options">
              <div v-for="opt in parseOptions(record.options)" :key="opt.label" class="opt-item" :class="{ 'correct': record.answer?.split(',').includes(opt.label) }">
                <span class="opt-key">{{ opt.label }}.</span>
                <span class="opt-text">{{ opt.text }}</span>
                <CheckCircleOutlined v-if="record.answer?.split(',').includes(opt.label)" class="correct-icon" />
              </div>
            </div>
            <div v-if="record.analysis" class="q-analysis">
              <strong>解析：</strong>{{ record.analysis }}
            </div>
          </div>
        </template>

        <a-table-column title="ID" dataIndex="id" key="id" width="60" />
        <a-table-column title="题型" key="type" width="100" />
        <a-table-column title="题目内容" key="content" ellipsis />
        <a-table-column title="难度" key="difficulty" width="100" />
        <a-table-column title="答案" key="answer" width="80" />
        <a-table-column title="操作" key="action" width="160" />
      </a-table>

      <a-empty v-if="!loading && filteredQuestions.length === 0 && questions.length > 0" description="没有匹配的题目" />
    </div>
  </div>
</template>

<style scoped>
.question-bank-wrapper {
  background-color: #f0f2f5;
  min-height: 100vh;
  padding-top: 80px;
  padding-bottom: 40px;
}
.bank-header {
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
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 20px;
}
.back-btn { font-size: 16px; color: #666; }
.page-title { font-size: 18px; font-weight: 600; }
.page-subtitle { color: #999; font-size: 14px; }
.bank-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}
.filter-card {
  margin-bottom: 16px;
  border-radius: 8px;
}
.highlighted-row {
  background-color: #fffbe6 !important;
  animation: highlight-pulse 1.5s ease-in-out 2;
}
@keyframes highlight-pulse {
  0%, 100% { background-color: #fffbe6; }
  50% { background-color: #fff1b8; }
}
.question-preview {
  background: #fafafa;
  padding: 20px;
  border-radius: 8px;
}
.q-content {
  font-size: 16px;
  line-height: 1.8;
  margin-bottom: 16px;
  color: #262626;
}
.q-options { margin-left: 10px; }
.opt-item {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  border-radius: 6px;
  margin-bottom: 6px;
  border: 1px solid #f0f0f0;
}
.opt-item.correct {
  background: #f6ffed;
  border-color: #b7eb8f;
}
.opt-key { font-weight: 700; width: 30px; color: #1890ff; }
.opt-text { flex: 1; }
.correct-icon { color: #52c41a; margin-left: 8px; }
.q-analysis {
  margin-top: 16px;
  padding: 12px;
  background: #e6f7ff;
  border-radius: 6px;
  color: #1890ff;
}
.edit-form {
  background: #e6f7ff;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #91d5ff;
}
.option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.opt-label {
  font-weight: 600;
  width: 30px;
  color: #1890ff;
}
</style>
