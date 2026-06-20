<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getExam, getExamQuestions, getStudentAnnouncements, getAnnouncementUnreadCount, markAnnouncementRead, markAllAnnouncementsRead } from '@/api';
import { 
  ClockCircleOutlined, CalendarOutlined, BookOutlined, 
  LeftOutlined, SafetyCertificateOutlined, EyeOutlined,
  CloudOutlined, InfoCircleOutlined, NotificationOutlined,
  PushpinFilled, CheckOutlined, BellFilled
} from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';

const route = useRoute();
const router = useRouter();
const examId = route.params.id;
const exam = ref(null);
const loading = ref(true);
const questions = ref([]);

const announcements = ref([]);
const announcementsLoading = ref(false);
const unreadCount = ref(0);
const panelActiveKeys = ref([]);

const fetchAnnouncements = async () => {
  if (!examId) return;
  announcementsLoading.value = true;
  try {
    const [listRes, countRes] = await Promise.all([
      getStudentAnnouncements(examId),
      getAnnouncementUnreadCount(examId)
    ]);
    announcements.value = listRes.data;
    unreadCount.value = countRes.data.count || 0;
    if (unreadCount.value > 0) {
      panelActiveKeys.value = ['announcements'];
    }
  } catch (e) {
    console.error('加载公告失败', e);
  } finally {
    announcementsLoading.value = false;
  }
};

const fetchData = async () => {
  try {
    const [eRes, qRes] = await Promise.all([
      getExam(examId),
      getExamQuestions(examId)
    ]);
    exam.value = eRes.data;
    questions.value = qRes.data;
    fetchAnnouncements();
  } catch (e) {
    message.error('加载考试信息失败');
    router.push('/dashboard');
  } finally {
    loading.value = false;
  }
};

const handlePanelChange = (keys) => {
  panelActiveKeys.value = keys;
  if (keys?.includes('announcements') && unreadCount.value > 0) {
    handleMarkAllRead();
  }
};

const handleItemOpen = async (item) => {
  if (!item.isRead) {
    try {
      await markAnnouncementRead(item.id);
      item.isRead = true;
      unreadCount.value = Math.max(0, unreadCount.value - 1);
    } catch (e) {
      console.error('标记已读失败', e);
    }
  }
};

const handleMarkAllRead = async () => {
  try {
    await markAllAnnouncementsRead(examId);
    announcements.value.forEach(a => a.isRead = true);
    unreadCount.value = 0;
  } catch (e) {
    console.error('全部标已读失败', e);
  }
};

const status = computed(() => {
  if (!exam.value) return {};
  const now = new Date();
  const start = exam.value.startTime ? new Date(exam.value.startTime) : null;
  const end = exam.value.endTime ? new Date(exam.value.endTime) : null;

  if (start && now < start) return { text: '未开始', color: 'blue', allow: false };
  if (end && now > end) return { text: '已结束', color: 'red', allow: false };
  return { text: '进行中', color: 'green', allow: true };
});

const canStart = computed(() => status.value.allow);

const totalScore = computed(() => questions.value.reduce((sum, q) => sum + (q.score || 0), 0));

const formatTime = (t) => t ? new Date(t).toLocaleString() : '-';

onMounted(fetchData);

const startExam = () => {
  router.push(`/exam/${examId}`);
};

const goBack = () => {
  router.back();
};
</script>

<template>
  <div class="detail-page-wrapper">
    <div class="background-accent"></div>
    <div class="content-container" v-if="exam">
      <div class="back-link" @click="goBack">
        <LeftOutlined /> 返回列表
      </div>

      <a-collapse
        v-if="announcements.length > 0"
        v-model:activeKey="panelActiveKeys"
        class="announcement-collapse"
        ghost
        @change="handlePanelChange"
      >
        <a-collapse-panel key="announcements">
          <template #header>
            <div class="panel-header">
              <BellFilled class="panel-icon" />
              <span class="panel-title">考试公告</span>
              <a-badge
                v-if="unreadCount > 0"
                :count="unreadCount"
                :overflow-count="99"
                class="unread-badge"
              />
              <span class="panel-count">({{ announcements.length }}条)</span>
            </div>
          </template>
          <template #extra>
            <a-button
              v-if="unreadCount > 0"
              type="link"
              size="small"
              @click.stop="handleMarkAllRead"
            >
              <CheckOutlined /> 全部标为已读
            </a-button>
          </template>

          <a-spin :spinning="announcementsLoading">
            <div class="announcement-list">
              <div
                v-for="item in announcements"
                :key="item.id"
                class="announcement-card"
                :class="{ pinned: item.isPinned, unread: !item.isRead }"
                @click="handleItemOpen(item)"
              >
                <div class="ann-header">
                  <div class="ann-title-row">
                    <span v-if="item.isPinned" class="pin-tag">
                      <PushpinFilled /> 置顶
                    </span>
                    <span class="ann-title">{{ item.title }}</span>
                    <span v-if="!item.isRead" class="unread-dot"></span>
                  </div>
                  <span class="ann-time">{{ formatTime(item.createdAt) }}</span>
                </div>
                <div class="ann-content" v-html="item.content"></div>
              </div>
              <a-empty v-if="announcements.length === 0" description="暂无公告" />
            </div>
          </a-spin>
        </a-collapse-panel>
      </a-collapse>

      <div class="hero-section">
        <div class="hero-left">
           <img :src="exam.coverUrl || 'https://gw.alipayobjects.com/zos/rmsportal/JiqGstEfoWAOHiTxclqi.png'" alt="cover" class="exam-cover"/>
        </div>
        <div class="hero-right">
           <div class="title-row">
             <h1 class="exam-title">{{ exam.title }}</h1>
             <a-tag :color="status.color" class="status-tag">{{ status.text }}</a-tag>
           </div>
           <p class="course-name">{{ exam.course || '通识课程' }}</p>
           <div class="quick-stats">
              <div class="stat-item">
                 <div class="stat-val">{{ exam.duration }}</div>
                 <div class="stat-label">考试时长(分)</div>
              </div>
              <div class="stat-item">
                 <div class="stat-val">{{ questions.length }}</div>
                 <div class="stat-label">题目总数</div>
              </div>
              <div class="stat-item">
                 <div class="stat-val">{{ totalScore }}</div>
                 <div class="stat-label">总分</div>
              </div>
           </div>
        </div>
      </div>

      <a-row :gutter="24" class="main-body">
        <a-col :span="16">
           <a-card title="考试说明" class="info-card">
              <div class="description-box">
                {{ exam.description || '暂无详细考试说明' }}
              </div>
              <div class="rules-section">
                <h3><InfoCircleOutlined /> 考生规则</h3>
                <ul>
                  <li>请确保网络连接稳定，建议使用 Chrome 浏览器。</li>
                  <li>考试过程中系统将实时检测切屏行为，达到上限将自动交卷。</li>
                  <li>一旦进入考试，计时器将立即启动，请注意时间。</li>
                </ul>
              </div>
           </a-card>
        </a-col>
        <a-col :span="8">
           <a-card title="配置信息" class="info-card">
              <a-list size="small">
                <a-list-item>
                  <template #extra><EyeOutlined /></template>
                  <a-list-item-meta title="防作弊设置">
                    <template #description>
                      <a-tag v-if="!exam.allowTabSwitch" color="warning">切屏统计 (上限 {{ exam.tabSwitchLimit }} 次)</a-tag>
                      <a-tag v-else color="success">自由切屏</a-tag>
                    </template>
                  </a-list-item-meta>
                </a-list-item>
                <a-list-item>
                  <template #extra><CloudOutlined /></template>
                  <a-list-item-meta title="实时监考">
                    <template #description>
                      <span v-if="exam.enableCamera">开启系统录像/抓拍监控</span>
                      <span v-else>未开启远程监控</span>
                    </template>
                  </a-list-item-meta>
                </a-list-item>
                <a-list-item>
                  <template #extra><SafetyCertificateOutlined /></template>
                  <a-list-item-meta title="成绩发布">
                    <template #description>
                      <span v-if="exam.publicScores">交卷后立即显示分数</span>
                      <span v-else>老师阅卷后统一公布</span>
                    </template>
                  </a-list-item-meta>
                </a-list-item>
              </a-list>
              
              <div class="start-action">
                <div v-if="status.text === '未开始'" class="countdown-hint">
                  <ClockCircleOutlined /> 距离开考还有 24:00:00
                </div>
                <a-button type="primary" block size="large" :disabled="!canStart" @click="startExam" class="primary-start-btn">
                   {{ canStart ? '正式进入考试' : '尚未开始或已结束' }}
                </a-button>
              </div>
           </a-card>
        </a-col>
      </a-row>
    </div>
    <div v-else class="loading-state">
       <a-spin size="large" />
    </div>
  </div>
</template>

<style scoped>
.detail-page-wrapper {
  min-height: 100vh;
  background-color: #f0f2f5;
  padding-bottom: 60px;
  position: relative;
}
.background-accent {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 280px;
  background: linear-gradient(135deg, #1890ff 0%, #3a76f0 100%);
  z-index: 0;
}
.content-container {
  position: relative;
  z-index: 1;
  max-width: 1100px;
  margin: 0 auto;
  padding-top: 24px;
}
.back-link {
  color: rgba(255,255,255,0.8);
  cursor: pointer;
  margin-bottom: 24px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;
}
.back-link:hover {
  color: white;
  transform: translateX(-4px);
}

.announcement-collapse {
  margin-bottom: 24px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  overflow: hidden;
}
.announcement-collapse :deep(.ant-collapse-header) {
  padding: 16px 24px !important;
  align-items: center !important;
}
.announcement-collapse :deep(.ant-collapse-content-box) {
  padding: 0 24px 24px !important;
}
.panel-header {
  display: flex;
  align-items: center;
  gap: 10px;
}
.panel-icon {
  color: #faad14;
  font-size: 18px;
}
.panel-title {
  font-weight: 600;
  font-size: 16px;
  color: #1a1a1a;
}
.unread-badge {
  margin-left: 4px;
}
.panel-count {
  color: #999;
  font-size: 13px;
  margin-left: 4px;
}

.announcement-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.announcement-card {
  padding: 16px 20px;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}
.announcement-card:hover {
  border-color: #bae7ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
}
.announcement-card.pinned {
  background: linear-gradient(135deg, #fffbe6 0%, #fff 100%);
  border-color: #ffe58f;
}
.announcement-card.unread {
  border-left: 4px solid #f5222d;
  padding-left: 16px;
}
.ann-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}
.ann-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}
.pin-tag {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 2px 8px;
  background: #faad14;
  color: white;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
}
.ann-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}
.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f5222d;
  flex-shrink: 0;
  animation: pulse-dot 1.5s infinite;
}
.ann-time {
  font-size: 12px;
  color: #999;
  flex-shrink: 0;
  margin-left: 12px;
}
.ann-content {
  line-height: 1.8;
  color: #444;
  font-size: 14px;
  overflow-wrap: break-word;
}
.ann-content :deep(h1),
.ann-content :deep(h2),
.ann-content :deep(h3) {
  margin: 12px 0 8px;
  font-weight: 600;
}
.ann-content :deep(ul),
.ann-content :deep(ol) {
  padding-left: 24px;
  margin: 8px 0;
}
.ann-content :deep(p) {
  margin: 8px 0;
}
.ann-content :deep(img) {
  max-width: 100%;
  border-radius: 6px;
}

@keyframes pulse-dot {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.3); opacity: 0.7; }
}

.hero-section {
  display: flex;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  margin-bottom: 24px;
}
.hero-left {
  width: 320px;
}
.exam-cover {
  width: 100%;
  height: 240px;
  object-fit: cover;
}
.hero-right {
  flex: 1;
  padding: 32px 40px;
  display: flex;
  flex-direction: column;
}
.title-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}
.exam-title {
  font-size: 32px;
  font-weight: 600;
  margin: 0;
  color: #1a1a1a;
}
.course-name {
  color: #666;
  font-size: 16px;
  margin-bottom: 24px;
}
.quick-stats {
  margin-top: auto;
  display: flex;
  gap: 48px;
}
.stat-val {
  font-size: 24px;
  font-weight: bold;
  color: #1890ff;
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: #999;
}

.main-body {
  margin-top: 24px;
}
.info-card {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0,0,0,0.04);
}
.description-box {
  background: #fafafa;
  padding: 20px;
  border-radius: 8px;
  min-height: 120px;
  color: #444;
  line-height: 1.8;
  font-size: 15px;
}
.rules-section {
  margin-top: 32px;
}
.rules-section h3 {
  font-size: 17px;
  font-weight: 600;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.rules-section ul {
  padding-left: 20px;
  color: #666;
}
.rules-section li {
  margin-bottom: 10px;
}

.start-action {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}
.countdown-hint {
  text-align: center;
  color: #f5222d;
  margin-bottom: 16px;
  font-weight: 500;
}
.primary-start-btn {
  height: 52px;
  font-size: 18px;
  font-weight: 600;
  border-radius: 8px;
}

.loading-state {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
