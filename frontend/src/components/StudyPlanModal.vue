<script setup>
import { ref, watch, computed } from 'vue';
import { createStudyPlan, getStudyPlanByExam, checkInStudyPlanTask, addStudyPlanTask, deleteStudyPlanTask, logStudyPlanMinutes } from '@/api';
import { message } from 'ant-design-vue';
import { PlusOutlined, DeleteOutlined, CheckCircleFilled, ClockCircleOutlined, EditOutlined, SaveOutlined, StopOutlined } from '@ant-design/icons-vue';

const props = defineProps(['open', 'examId', 'examTitle']);
const emit = defineEmits(['update:open', 'created']);

const mode = ref('create');
const plan = ref(null);
const loading = ref(false);
const saving = ref(false);

const form = ref({
  dailyGoalMinutes: 120,
  taskTitles: ['复习课本重点章节', '刷历年真题', '整理错题笔记']
});
const newTaskTitle = ref('');

const logMinutes = ref(null);
const loggingMinutes = ref(false);

const fetchPlan = async () => {
  if (!props.examId) return;
  loading.value = true;
  try {
    const res = await getStudyPlanByExam(props.examId);
    if (res.status === 204 || !res.data) {
      mode.value = 'create';
      plan.value = null;
    } else {
      mode.value = res.data.isArchived ? 'archived' : 'manage';
      plan.value = res.data;
    }
  } catch (e) {
    mode.value = 'create';
  } finally {
    loading.value = false;
  }
};

watch(() => props.open, (val) => {
  if (val) fetchPlan();
});

const handleCreate = async () => {
  if (!form.value.dailyGoalMinutes || form.value.dailyGoalMinutes < 1) {
    message.error('请设置每日学习时长');
    return;
  }
  saving.value = true;
  try {
    const res = await createStudyPlan({
      examId: props.examId,
      dailyGoalMinutes: form.value.dailyGoalMinutes,
      taskTitles: form.value.taskTitles.filter(t => t.trim())
    });
    plan.value = res.data;
    mode.value = 'manage';
    message.success('备考计划创建成功');
    emit('created');
  } catch (e) {
    message.error(e.response?.data?.message || '创建失败');
  } finally {
    saving.value = false;
  }
};

const handleCheckIn = async (task) => {
  try {
    const res = await checkInStudyPlanTask(plan.value.id, {
      taskId: task.id,
      completed: !task.completedToday
    });
    plan.value = res.data;
    message.success(task.completedToday ? '取消打卡' : '打卡成功');
  } catch (e) {
    message.error(e.response?.data?.message || '打卡失败');
  }
};

const handleAddTask = async () => {
  if (!newTaskTitle.value.trim()) {
    message.error('请输入任务名称');
    return;
  }
  try {
    const res = await addStudyPlanTask(plan.value.id, { title: newTaskTitle.value.trim() });
    plan.value = res.data;
    newTaskTitle.value = '';
    message.success('任务添加成功');
  } catch (e) {
    message.error(e.response?.data?.message || '添加失败');
  }
};

const handleDeleteTask = async (taskId) => {
  try {
    const res = await deleteStudyPlanTask(plan.value.id, taskId);
    plan.value = res.data;
    message.success('任务已删除');
  } catch (e) {
    message.error(e.response?.data?.message || '删除失败');
  }
};

const handleLogMinutes = async () => {
  if (logMinutes.value === null || logMinutes.value === undefined || logMinutes.value < 0) {
    message.error('请输入有效的学习时长');
    return;
  }
  loggingMinutes.value = true;
  try {
    const res = await logStudyPlanMinutes(plan.value.id, { minutes: Number(logMinutes.value) });
    plan.value = res.data;
    logMinutes.value = null;
    message.success('学习时长已记录');
  } catch (e) {
    message.error(e.response?.data?.message || '记录失败');
  } finally {
    loggingMinutes.value = false;
  }
};

const addTaskRow = () => {
  form.value.taskTitles.push('');
};

const removeTaskRow = (index) => {
  form.value.taskTitles.splice(index, 1);
};

const todayProgress = computed(() => {
  if (!plan.value || !plan.value.tasks) return 0;
  return Math.round(plan.value.todayProgress || 0);
});

const minutesProgress = computed(() => {
  if (!plan.value) return 0;
  return Math.round(plan.value.minutesProgress || 0);
});

const handleCancel = () => {
  emit('update:open', false);
};

const formatGoal = (minutes) => {
  if (!minutes) return '0分钟';
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h > 0 && m > 0) return `${h}小时${m}分钟`;
  if (h > 0) return `${h}小时`;
  return `${m}分钟`;
};
</script>

<template>
  <a-modal
    :open="open"
    :title="mode === 'create' ? '制定备考计划' : (mode === 'archived' ? '备考计划（已归档）' : '我的备考计划')"
    @cancel="handleCancel"
    :footer="null"
    width="680px"
    :destroyOnClose="true"
  >
    <a-spin :spinning="loading">
      <div v-if="mode === 'create'" class="create-form">
        <a-form layout="vertical">
          <a-form-item label="考试名称">
            <a-input :value="examTitle" disabled />
          </a-form-item>
          <a-form-item label="每日学习时长目标" required>
            <a-input-number
              v-model:value="form.dailyGoalMinutes"
              :min="10"
              :max="720"
              :step="15"
              style="width: 200px"
              addon-after="分钟"
            />
            <span style="margin-left: 12px; color: #999;">≈ {{ formatGoal(form.dailyGoalMinutes) }}</span>
          </a-form-item>
          <a-form-item label="自定义任务清单">
            <div v-for="(t, i) in form.taskTitles" :key="i" style="display: flex; gap: 8px; margin-bottom: 8px;">
              <a-input v-model:value="form.taskTitles[i]" :placeholder="`任务 ${i + 1}`" />
              <a-button danger @click="removeTaskRow(i)" :disabled="form.taskTitles.length <= 1">
                <DeleteOutlined />
              </a-button>
            </div>
            <a-button type="dashed" block @click="addTaskRow">
              <PlusOutlined /> 添加任务
            </a-button>
          </a-form-item>
          <a-form-item>
            <a-button type="primary" block size="large" :loading="saving" @click="handleCreate">
              确认制定计划
            </a-button>
          </a-form-item>
        </a-form>
      </div>

      <div v-else-if="mode === 'manage'" class="manage-section">
        <div class="plan-header">
          <div class="plan-meta">
            <div class="meta-item">
              <ClockCircleOutlined />
              <span>每日目标: <b>{{ formatGoal(plan.dailyGoalMinutes) }}</b></span>
            </div>
            <div class="meta-item" v-if="plan.daysUntilExam !== null">
              <span>距考试: <b style="color: #f5222d;">{{ plan.daysUntilExam }}天</b></span>
            </div>
            <div class="meta-item">
              <span>连续打卡: <b style="color: #52c41a;">{{ plan.streakDays }}天</b></span>
            </div>
          </div>
          <div class="progress-rings">
            <div class="ring-item">
              <a-progress
                type="circle"
                :percent="todayProgress"
                :size="72"
                :stroke-color="todayProgress === 100 ? '#52c41a' : '#1890ff'"
              >
                <template #format="{ percent }">
                  <span style="font-size: 12px; font-weight: 600;">{{ percent }}%</span>
                </template>
              </a-progress>
              <div class="ring-label">任务完成</div>
            </div>
            <div class="ring-item">
              <a-progress
                type="circle"
                :percent="minutesProgress"
                :size="72"
                :stroke-color="minutesProgress >= 100 ? '#722ed1' : '#fa8c16'"
              >
                <template #format="{ percent }">
                  <span style="font-size: 12px; font-weight: 600;">{{ percent }}%</span>
                </template>
              </a-progress>
              <div class="ring-label">学习时长</div>
            </div>
          </div>
        </div>

        <div class="minutes-log-box">
          <span class="log-title">今日学习: <b style="color: #722ed1;">{{ plan.todayStudiedMinutes || 0 }}分钟</b> / {{ formatGoal(plan.dailyGoalMinutes) }}</span>
          <div class="log-input-row">
            <a-input-number
              v-model:value="logMinutes"
              :min="0"
              :max="plan.dailyGoalMinutes * 2 || 1440"
              placeholder="今日累计学习"
              :addon-after="'分钟'"
              size="small"
              style="width: 180px;"
            />
            <a-button type="primary" size="small" :loading="loggingMinutes" @click="handleLogMinutes">
              <SaveOutlined /> 记录
            </a-button>
          </div>
        </div>

        <a-divider>今日任务打卡</a-divider>

        <div class="task-list">
          <div v-for="task in plan.tasks" :key="task.id" class="task-item">
            <a-checkbox
              :checked="task.completedToday"
              @change="handleCheckIn(task)"
            >
              <span :class="{ 'task-done': task.completedToday }">{{ task.title }}</span>
            </a-checkbox>
            <a-button type="text" danger size="small" @click="handleDeleteTask(task.id)">
              <DeleteOutlined />
            </a-button>
          </div>
        </div>

        <div class="add-task-row">
          <a-input
            v-model:value="newTaskTitle"
            placeholder="添加新任务..."
            @pressEnter="handleAddTask"
            style="flex: 1;"
          />
          <a-button type="primary" @click="handleAddTask">
            <PlusOutlined /> 添加
          </a-button>
        </div>
      </div>

      <div v-else-if="mode === 'archived'" class="archived-section">
        <a-alert
          message="该考试已开始或结束，备考计划已归档为只读"
          type="info"
          show-icon
          style="margin-bottom: 16px;"
        />
        <div class="plan-header">
          <div class="plan-meta">
            <div class="meta-item">
              <ClockCircleOutlined />
              <span>每日目标: <b>{{ formatGoal(plan.dailyGoalMinutes) }}</b></span>
            </div>
            <div class="meta-item">
              <span>今日已学: <b>{{ plan.todayStudiedMinutes || 0 }}分钟</b></span>
            </div>
            <div class="meta-item">
              <span>连续打卡: <b>{{ plan.streakDays }}天</b></span>
            </div>
          </div>
          <div class="progress-rings">
            <div class="ring-item">
              <a-progress
                type="circle"
                :percent="todayProgress"
                :size="72"
                :stroke-color="todayProgress === 100 ? '#52c41a' : '#1890ff'"
              >
                <template #format="{ percent }">
                  <span style="font-size: 12px; font-weight: 600;">{{ percent }}%</span>
                </template>
              </a-progress>
              <div class="ring-label">任务完成</div>
            </div>
            <div class="ring-item">
              <a-progress
                type="circle"
                :percent="minutesProgress"
                :size="72"
                :stroke-color="minutesProgress >= 100 ? '#722ed1' : '#fa8c16'"
              >
                <template #format="{ percent }">
                  <span style="font-size: 12px; font-weight: 600;">{{ percent }}%</span>
                </template>
              </a-progress>
              <div class="ring-label">学习时长</div>
            </div>
          </div>
        </div>
        <a-divider>任务列表（只读）</a-divider>
        <div class="task-list">
          <div v-for="task in plan.tasks" :key="task.id" class="task-item">
            <a-checkbox :checked="task.completedToday" disabled>
              <span>{{ task.title }}</span>
            </a-checkbox>
          </div>
        </div>
      </div>
    </a-spin>
  </a-modal>
</template>

<style scoped>
.create-form {
  padding: 8px 0;
}
.plan-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f6ffed 0%, #e6f7ff 100%);
  border-radius: 8px;
  margin-bottom: 12px;
  border: 1px solid #b7eb8f;
}
.plan-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #555;
}
.progress-rings {
  display: flex;
  gap: 24px;
}
.ring-item {
  text-align: center;
}
.ring-label {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}
.minutes-log-box {
  background: #f9f0ff;
  border: 1px solid #d3adf7;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}
.log-title {
  font-size: 14px;
  color: #531dab;
}
.log-input-row {
  display: flex;
  gap: 8px;
}
.task-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}
.task-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  transition: all 0.2s;
}
.task-item:hover {
  border-color: #bae7ff;
  background: #fafafa;
}
.task-done {
  text-decoration: line-through;
  color: #999;
}
.add-task-row {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.archived-section {
  padding: 8px 0;
}
</style>
