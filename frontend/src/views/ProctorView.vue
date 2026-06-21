<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getProctorData, exportProctorSnapshot } from '@/api';
import { message } from 'ant-design-vue';
import { 
  UserOutlined, TeamOutlined, CheckCircleOutlined, WarningOutlined,
  DownloadOutlined, ReloadOutlined, ArrowLeftOutlined,
  ClockCircleOutlined, DesktopOutlined, ExclamationCircleOutlined
} from '@ant-design/icons-vue';

const route = useRoute();
const router = useRouter();
const examId = route.params.examId;

const proctorData = ref(null);
const loading = ref(true);
const pollingInterval = ref(null);
const lastUpdateTime = ref(null);

const formatDateTime = (date) => {
  const d = new Date(date);
  const pad = (n) => n.toString().padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

const formatTime = (date) => {
  const d = new Date(date);
  const pad = (n) => n.toString().padStart(2, '0');
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

const formatFileTimestamp = () => {
  const d = new Date();
  const pad = (n) => n.toString().padStart(2, '0');
  return `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}_${pad(d.getHours())}${pad(d.getMinutes())}${pad(d.getSeconds())}`;
};

const fetchProctorData = async () => {
  try {
    const res = await getProctorData(examId);
    proctorData.value = res.data;
    lastUpdateTime.value = formatDateTime(new Date());
  } catch (e) {
    console.error('Failed to fetch proctor data', e);
    if (e.response?.status === 403) {
      const errMsg = e.response?.data?.message || '无权访问此监考页面，仅考试创建者或ADMIN可查看';
      message.error(errMsg);
      if (pollingInterval.value) {
        clearInterval(pollingInterval.value);
      }
      setTimeout(() => router.push('/dashboard'), 1500);
    }
  } finally {
    loading.value = false;
  }
};

const handleExport = async () => {
  try {
    const res = await exportProctorSnapshot(examId);
    const blob = new Blob([res.data], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', `proctor_snapshot_${examId}_${formatFileTimestamp()}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    message.success('导出成功');
  } catch (e) {
    console.error('Export failed', e);
    message.error('导出失败');
  }
};

const formatLastActive = (time) => {
  if (!time) return '暂无数据';
  const now = new Date();
  const activeTime = new Date(time);
  const diffMinutes = Math.floor((now - activeTime) / (1000 * 60));
  if (diffMinutes < 1) return '刚刚';
  if (diffMinutes < 60) return `${diffMinutes} 分钟前`;
  return formatTime(time);
};

const getAnomalyClass = (anomalyType) => {
  if (anomalyType === 'EXCESS_TAB_SWITCH') return 'anomaly-red';
  if (anomalyType === 'INACTIVITY') return 'anomaly-yellow';
  return '';
};

const getAnomalyText = (anomalyType) => {
  if (anomalyType === 'EXCESS_TAB_SWITCH') return '切屏超标';
  if (anomalyType === 'INACTIVITY') return '长时间无操作';
  return '正常';
};

const getAnomalyIcon = (anomalyType) => {
  if (anomalyType === 'EXCESS_TAB_SWITCH' || anomalyType === 'INACTIVITY') {
    return ExclamationCircleOutlined;
  }
  return null;
};

const onlineCount = computed(() => proctorData.value?.onlineCount || 0);
const submittedCount = computed(() => proctorData.value?.submittedCount || 0);
const anomalyCount = computed(() => proctorData.value?.anomalyCount || 0);

const isTabSwitchExcess = (student) => {
  if (student.anomalyType === 'EXCESS_TAB_SWITCH') return true;
  if (proctorData.value?.allowTabSwitch !== false) return false;
  const limit = proctorData.value?.tabSwitchLimit;
  if (limit == null) return false;
  return (student.tabSwitchCount || 0) >= limit;
};

onMounted(() => {
  fetchProctorData();
  pollingInterval.value = setInterval(fetchProctorData, 5000);
});

onUnmounted(() => {
  if (pollingInterval.value) {
    clearInterval(pollingInterval.value);
  }
});
</script>

<template>
  <div class="proctor-container">
    <div class="proctor-header">
      <div class="header-left">
        <a-button type="text" class="back-btn" @click="router.push('/dashboard')">
          <ArrowLeftOutlined /> 返回
        </a-button>
        <h1 class="exam-title">
          <DesktopOutlined class="title-icon" />
          {{ proctorData?.examTitle || '监考大屏' }}
        </h1>
      </div>
      <div class="header-right">
        <span class="update-time">
          <ReloadOutlined spin /> 最后更新: {{ lastUpdateTime }}
        </span>
        <a-button type="primary" class="export-btn" @click="handleExport">
          <DownloadOutlined /> 导出快照
        </a-button>
      </div>
    </div>

    <div class="stats-cards">
      <div class="stat-card online-card">
        <div class="stat-icon">
          <TeamOutlined />
        </div>
        <div class="stat-content">
          <div class="stat-label">在线人数</div>
          <div class="stat-value">{{ onlineCount }}</div>
        </div>
      </div>
      <div class="stat-card submitted-card">
        <div class="stat-icon">
          <CheckCircleOutlined />
        </div>
        <div class="stat-content">
          <div class="stat-label">已交卷</div>
          <div class="stat-value">{{ submittedCount }}</div>
        </div>
      </div>
      <div class="stat-card anomaly-card" :class="{ 'has-anomaly': anomalyCount > 0 }">
        <div class="stat-icon">
          <WarningOutlined />
        </div>
        <div class="stat-content">
          <div class="stat-label">异常人数</div>
          <div class="stat-value">{{ anomalyCount }}</div>
        </div>
      </div>
    </div>

    <div class="student-list-container">
      <div class="list-header">
        <h2 class="list-title">
          <UserOutlined /> 在线学生列表
        </h2>
        <div class="legend">
          <span class="legend-item">
            <span class="legend-dot normal"></span> 正常
          </span>
          <span class="legend-item">
            <span class="legend-dot yellow"></span> 长时间无操作
          </span>
          <span class="legend-item">
            <span class="legend-dot red"></span> 切屏超标
          </span>
        </div>
      </div>

      <a-spin size="large" v-if="loading" class="loading-spin">
        <div class="loading-placeholder"></div>
      </a-spin>

      <div v-else class="student-table">
        <div class="table-header">
          <div class="col-name">姓名</div>
          <div class="col-info">学号 / 班级</div>
          <div class="col-progress">答题进度</div>
          <div class="col-time">
            <ClockCircleOutlined /> 最近活跃
          </div>
          <div class="col-tab">
            <DesktopOutlined /> 切屏次数
          </div>
          <div class="col-status">状态</div>
        </div>

        <div v-if="!proctorData?.students?.length" class="empty-state">
          <UserOutlined class="empty-icon" />
          <p>暂无在线学生</p>
        </div>

        <div 
          v-for="student in proctorData?.students" 
          :key="student.submissionId" 
          class="table-row"
          :class="getAnomalyClass(student.anomalyType)"
        >
          <div class="col-name">
            <span class="name-text">{{ student.studentName }}</span>
            <component 
              v-if="getAnomalyIcon(student.anomalyType)" 
              :is="getAnomalyIcon(student.anomalyType)" 
              class="anomaly-icon"
            />
          </div>
          <div class="col-info">
            <span class="username">{{ student.studentUsername }}</span>
            <span class="clazz" v-if="student.studentClazz">{{ student.studentClazz }}</span>
          </div>
          <div class="col-progress">
            <div class="progress-bar-container">
              <div class="progress-bar-fill" :style="{ width: student.progress + '%' }"></div>
            </div>
            <span class="progress-text">{{ student.progress.toFixed(1) }}%</span>
          </div>
          <div class="col-time">
            {{ formatLastActive(student.lastActiveTime) }}
          </div>
          <div class="col-tab">
            <span :class="{ 'tab-excess': isTabSwitchExcess(student) }">
              {{ student.tabSwitchCount }}
            </span>
          </div>
          <div class="col-status">
            <a-tag 
              :color="student.anomalyType === 'EXCESS_TAB_SWITCH' ? 'red' : 
                     student.anomalyType === 'INACTIVITY' ? 'gold' : 'green'"
            >
              {{ getAnomalyText(student.anomalyType) }}
            </a-tag>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.proctor-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
  padding: 24px 32px;
  color: #e2e8f0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.proctor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  color: #94a3b8;
  font-size: 14px;
}

.back-btn:hover {
  color: #3b82f6;
}

.exam-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #f1f5f9;
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-icon {
  color: #3b82f6;
  font-size: 28px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.update-time {
  color: #64748b;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.export-btn {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  height: 40px;
  padding: 0 24px;
  border-radius: 8px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.export-btn:hover {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 28px;
}

.stat-card {
  background: rgba(30, 41, 59, 0.8);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 24px 28px;
  display: flex;
  align-items: center;
  gap: 20px;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.online-card .stat-icon {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.2) 0%, rgba(22, 163, 74, 0.1) 100%);
  color: #22c55e;
}

.submitted-card .stat-icon {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.2) 0%, rgba(37, 99, 235, 0.1) 100%);
  color: #3b82f6;
}

.anomaly-card .stat-icon {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.2) 0%, rgba(220, 38, 38, 0.1) 100%);
  color: #64748b;
}

.anomaly-card.has-anomaly .stat-icon {
  color: #ef4444;
}

.anomaly-card.has-anomaly {
  border-color: rgba(239, 68, 68, 0.5);
  animation: pulse-red 2s infinite;
}

@keyframes pulse-red {
  0%, 100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.4); }
  50% { box-shadow: 0 0 0 10px rgba(239, 68, 68, 0); }
}

.stat-label {
  font-size: 14px;
  color: #94a3b8;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 36px;
  font-weight: 800;
  color: #f1f5f9;
  line-height: 1;
}

.student-list-container {
  background: rgba(30, 41, 59, 0.6);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 24px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.list-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #f1f5f9;
  display: flex;
  align-items: center;
  gap: 10px;
}

.legend {
  display: flex;
  gap: 20px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #94a3b8;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.legend-dot.normal { background: #22c55e; }
.legend-dot.yellow { background: #eab308; }
.legend-dot.red { background: #ef4444; }

.loading-spin {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.loading-placeholder {
  width: 100px;
  height: 100px;
}

.table-header {
  display: grid;
  grid-template-columns: 1.2fr 1.2fr 2fr 1.2fr 1fr 1fr;
  gap: 16px;
  padding: 14px 20px;
  background: rgba(15, 23, 42, 0.8);
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

.table-row {
  display: grid;
  grid-template-columns: 1.2fr 1.2fr 2fr 1.2fr 1fr 1fr;
  gap: 16px;
  padding: 18px 20px;
  background: rgba(15, 23, 42, 0.4);
  border-radius: 10px;
  margin-bottom: 8px;
  align-items: center;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.table-row:hover {
  background: rgba(15, 23, 42, 0.6);
  border-color: rgba(59, 130, 246, 0.3);
}

.table-row.anomaly-yellow {
  background: rgba(234, 179, 8, 0.1);
  border-color: rgba(234, 179, 8, 0.4);
}

.table-row.anomaly-red {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.4);
}

.col-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #f1f5f9;
  font-size: 15px;
}

.anomaly-icon {
  font-size: 16px;
}

.anomaly-yellow .anomaly-icon { color: #eab308; }
.anomaly-red .anomaly-icon { color: #ef4444; }

.col-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.username {
  color: #cbd5e1;
  font-size: 14px;
  font-family: 'JetBrains Mono', monospace;
}

.clazz {
  color: #64748b;
  font-size: 12px;
}

.col-progress {
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-bar-container {
  flex: 1;
  height: 10px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 5px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #22c55e 0%, #16a34a 100%);
  border-radius: 5px;
  transition: width 0.5s ease;
}

.anomaly-yellow .progress-bar-fill {
  background: linear-gradient(90deg, #eab308 0%, #ca8a04 100%);
}

.anomaly-red .progress-bar-fill {
  background: linear-gradient(90deg, #ef4444 0%, #dc2626 100%);
}

.progress-text {
  font-weight: 700;
  font-size: 14px;
  color: #22c55e;
  min-width: 55px;
  text-align: right;
}

.anomaly-yellow .progress-text { color: #eab308; }
.anomaly-red .progress-text { color: #ef4444; }

.col-time {
  color: #94a3b8;
  font-size: 14px;
}

.col-tab {
  font-size: 14px;
  font-weight: 600;
  color: #94a3b8;
}

.tab-excess {
  color: #ef4444;
  font-weight: 700;
}

.col-status {
  display: flex;
  justify-content: flex-start;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #64748b;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state p {
  margin: 0;
  font-size: 16px;
}

@media (max-width: 1600px) {
  .stat-value {
    font-size: 32px;
  }
  
  .exam-title {
    font-size: 24px;
  }
}
</style>
