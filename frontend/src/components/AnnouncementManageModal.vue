<script setup>
import { ref, watch, computed } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { getTeacherAnnouncements, createAnnouncement, updateAnnouncement, toggleAnnouncementPin, deleteAnnouncement } from '@/api';
import {
  PlusOutlined, EditOutlined, PushpinOutlined, PushpinFilled,
  DeleteOutlined, CloseOutlined, CheckOutlined, FormOutlined
} from '@ant-design/icons-vue';

const props = defineProps({
  open: Boolean,
  examId: [Number, String],
  examState: String
});

const emit = defineEmits(['update:open', 'success']);

const visible = computed({
  get: () => props.open,
  set: (v) => emit('update:open', v)
});

const loading = ref(false);
const announcements = ref([]);
const formModalVisible = ref(false);
const editingId = ref(null);
const form = ref({
  title: '',
  content: '',
  isPinned: false
});
const formSubmitting = ref(false);

const canCreate = computed(() => props.examState === 'PUBLISHED');

const loadAnnouncements = async () => {
  if (!props.examId) return;
  loading.value = true;
  try {
    const res = await getTeacherAnnouncements(props.examId);
    announcements.value = res.data;
  } catch (e) {
    message.error('加载公告列表失败');
  } finally {
    loading.value = false;
  }
};

watch(() => [props.open, props.examId], ([o]) => {
  if (o) {
    loadAnnouncements();
  }
}, { immediate: true });

const openCreateForm = () => {
  if (!canCreate.value) {
    message.warning('仅已发布的考试可发布公告');
    return;
  }
  if (announcements.value.length >= 50) {
    message.warning('每场考试最多保留 50 条公告');
    return;
  }
  editingId.value = null;
  form.value = { title: '', content: '', isPinned: false };
  formModalVisible.value = true;
};

const openEditForm = (item) => {
  editingId.value = item.id;
  form.value = {
    title: item.title,
    content: item.content,
    isPinned: item.isPinned
  };
  formModalVisible.value = true;
};

const submitForm = async () => {
  if (!form.value.title?.trim()) {
    message.error('请输入公告标题');
    return;
  }
  if (!form.value.content?.trim()) {
    message.error('请输入公告正文');
    return;
  }
  formSubmitting.value = true;
  try {
    if (editingId.value) {
      await updateAnnouncement(editingId.value, form.value);
      message.success('公告已更新');
    } else {
      await createAnnouncement(props.examId, form.value);
      message.success('公告已发布');
    }
    formModalVisible.value = false;
    loadAnnouncements();
    emit('success');
  } catch (e) {
    message.error(editingId.value ? '更新失败' : '发布失败');
  } finally {
    formSubmitting.value = false;
  }
};

const handleTogglePin = async (item) => {
  try {
    await toggleAnnouncementPin(item.id);
    message.success(item.isPinned ? '已取消置顶' : '已置顶');
    loadAnnouncements();
  } catch (e) {
    message.error('操作失败');
  }
};

const handleDelete = (item) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除公告「${item.title}」吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteAnnouncement(item.id);
        message.success('已删除');
        loadAnnouncements();
        emit('success');
      } catch (e) {
        message.error('删除失败');
      }
    }
  });
};

const formatTime = (t) => t ? new Date(t).toLocaleString() : '-';
</script>

<template>
  <a-modal
    v-model:open="visible"
    :title="`公告管理 - #${examId}`"
    :footer="null"
    width="900px"
    :maskClosable="false"
    destroyOnClose
  >
    <div class="announcement-manage-wrapper">
      <div class="toolbar">
        <div class="toolbar-left">
          <a-tag color="blue" v-if="canCreate">已发布考试，可发布公告</a-tag>
          <a-tag color="default" v-else>草稿/已结束考试，不可发布公告</a-tag>
          <span class="count-info">共 {{ announcements.length }} / 50 条</span>
        </div>
        <a-button type="primary" :disabled="!canCreate" @click="openCreateForm">
          <PlusOutlined /> 发布公告
        </a-button>
      </div>

      <a-spin :spinning="loading">
        <div v-if="announcements.length === 0" class="empty-state">
          <a-empty description="暂无公告，点击上方按钮发布第一条公告" />
        </div>
        <a-list v-else :dataSource="announcements" item-layout="vertical" split class="announcement-list">
          <template #renderItem="{ item }">
            <a-list-item class="announcement-item" :class="{ pinned: item.isPinned }">
              <div class="item-header">
                <div class="title-row">
                  <span v-if="item.isPinned" class="pin-badge">
                    <PushpinFilled /> 置顶
                  </span>
                  <span class="item-title">{{ item.title }}</span>
                </div>
                <div class="item-meta">
                  <span class="creator">
                    <FormOutlined /> {{ item.creator?.fullName || item.creator?.username }}
                  </span>
                  <span class="time">{{ formatTime(item.createdAt) }}</span>
                  <span v-if="item.updatedAt && item.updatedAt !== item.createdAt" class="time-edit">
                    (编辑于 {{ formatTime(item.updatedAt) }})
                  </span>
                </div>
              </div>
              <div class="item-content" v-html="item.content"></div>
              <div class="item-actions">
                <a-button type="link" size="small" @click="handleTogglePin(item)">
                  <template #icon>
                    <PushpinFilled v-if="item.isPinned" />
                    <PushpinOutlined v-else />
                  </template>
                  {{ item.isPinned ? '取消置顶' : '置顶' }}
                </a-button>
                <a-divider type="vertical" />
                <a-button type="link" size="small" @click="openEditForm(item)">
                  <template #icon><EditOutlined /></template>
                  编辑
                </a-button>
                <a-divider type="vertical" />
                <a-button type="link" size="small" danger @click="handleDelete(item)">
                  <template #icon><DeleteOutlined /></template>
                  删除
                </a-button>
              </div>
            </a-list-item>
          </template>
        </a-list>
      </a-spin>
    </div>

    <a-modal
      v-model:open="formModalVisible"
      :title="editingId ? '编辑公告' : '发布公告'"
      @ok="submitForm"
      @cancel="formModalVisible = false"
      :confirmLoading="formSubmitting"
      okText="确认提交"
      cancelText="取消"
      width="720px"
      destroyOnClose
    >
      <a-form layout="vertical" class="announcement-form">
        <a-form-item label="公告标题" required>
          <a-input
            v-model:value="form.title"
            placeholder="请输入公告标题（建议不超过 50 字）"
            maxlength="100"
            showCount
          />
        </a-form-item>
        <a-form-item label="公告正文" required>
          <a-textarea
            v-model:value="form.content"
            placeholder="请输入公告正文内容，支持 HTML 格式（如 &lt;b&gt;粗体&lt;/b&gt;、&lt;ul&gt;&lt;li&gt;列表&lt;/li&gt;&lt;/ul&gt; 等）"
            :rows="10"
            maxlength="10000"
          />
          <div class="form-tip">
            提示：支持基本 HTML 标签，JS/危险标签将被自动过滤。
          </div>
        </a-form-item>
        <a-form-item>
          <a-checkbox v-model:checked="form.isPinned">
            <CheckOutlined v-if="form.isPinned" /> 置顶此公告（置顶公告将优先显示在最上方）
          </a-checkbox>
        </a-form-item>
      </a-form>
    </a-modal>
  </a-modal>
</template>

<style scoped>
.announcement-manage-wrapper {
  min-height: 400px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 8px;
  margin-bottom: 16px;
}
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.count-info {
  font-size: 12px;
  color: #999;
}
.empty-state {
  padding: 60px 0;
}
.announcement-list {
  background: white;
  border-radius: 8px;
}
.announcement-item {
  padding: 20px 24px !important;
  border-left: 3px solid transparent;
  transition: all 0.2s;
}
.announcement-item.pinned {
  background: #fffbe6;
  border-left-color: #faad14;
}
.item-header {
  margin-bottom: 12px;
}
.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}
.pin-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background: #faad14;
  color: white;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}
.item-title {
  font-size: 17px;
  font-weight: 600;
  color: #1a1a1a;
}
.item-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 12px;
  color: #999;
}
.creator {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.item-content {
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 8px;
  line-height: 1.8;
  color: #444;
  font-size: 14px;
  margin-bottom: 12px;
  overflow-wrap: break-word;
}
.item-content :deep(h1),
.item-content :deep(h2),
.item-content :deep(h3) {
  margin: 12px 0 8px;
  font-weight: 600;
}
.item-content :deep(ul),
.item-content :deep(ol) {
  padding-left: 24px;
  margin: 8px 0;
}
.item-content :deep(p) {
  margin: 8px 0;
}
.item-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
.announcement-form {
  margin-top: 8px;
}
.form-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #999;
}
</style>
