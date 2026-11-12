<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import AppIcon from "../components/common/AppIcon.vue";
import { notify, useCampus } from "../composables/useCampus";
import type {
  ManagedResource,
  ResourceDraft,
} from "../services/types";

const { isDemo, loadManagedResources, saveResource } = useCampus();
const pageSize = 8;
const keyword = ref("");
const typeFilter = ref("");
const page = ref(1);
const items = ref<ManagedResource[]>([]);
const total = ref(0);
const loading = ref(false);
const failed = ref(false);
const dialog = ref<HTMLDialogElement | null>(null);
const editingId = ref<string | null>(null);
const submitting = ref(false);
const formError = ref("");
const form = reactive<ResourceDraft>({
  name: "",
  location: "",
  resourceType: "LIBRARY_SEAT",
  unitLabel: "",
  enabled: true,
});
const resourceTypes = [
  { value: "LIBRARY_SEAT", label: "图书馆座位" },
  { value: "CLASSROOM", label: "教室" },
  { value: "BADMINTON", label: "羽毛球场" },
  { value: "TABLE_TENNIS", label: "乒乓球台" },
  { value: "TENNIS", label: "网球场" },
  { value: "DISCUSSION_ROOM", label: "讨论室" },
];
const typeLabels = Object.fromEntries(
  resourceTypes.map((type) => [type.value, type.label]),
);
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

async function refresh() {
  loading.value = true;
  failed.value = false;
  try {
    const result = await loadManagedResources({
      page: page.value,
      size: pageSize,
      keyword: keyword.value.trim() || undefined,
      resourceType: typeFilter.value || undefined,
    });
    if (!result) {
      failed.value = true;
      items.value = [];
      total.value = 0;
      return;
    }
    items.value = result.items;
    total.value = result.total;
    page.value = result.page;
  } finally {
    loading.value = false;
  }
}
function search() {
  page.value = 1;
  return refresh();
}
function turn(delta: number) {
  page.value = Math.min(pageCount.value, Math.max(1, page.value + delta));
  return refresh();
}
function openCreate() {
  editingId.value = null;
  Object.assign(form, {
    name: "",
    location: "",
    resourceType: "LIBRARY_SEAT",
    unitLabel: "",
    enabled: true,
  });
  formError.value = "";
  dialog.value?.showModal();
}
function openEdit(item: ManagedResource) {
  editingId.value = item.id;
  Object.assign(form, {
    name: item.name,
    location: item.location,
    resourceType: item.resourceType,
    unitLabel: item.unitLabel,
    enabled: item.enabled,
  });
  formError.value = "";
  dialog.value?.showModal();
}
async function submit() {
  formError.value = "";
  if (!form.name.trim() || !form.location.trim() || !form.unitLabel.trim()) {
    formError.value = "请填写资源名称、位置和编号标签。";
    return;
  }
  submitting.value = true;
  try {
    const saved = await saveResource(editingId.value, {
      name: form.name.trim(),
      location: form.location.trim(),
      resourceType: form.resourceType,
      unitLabel: form.unitLabel.trim(),
      enabled: form.enabled,
    });
    if (!saved) {
      formError.value = "保存未成功，请查看错误提示后重试。";
      return;
    }
    dialog.value?.close();
    notify(editingId.value ? "资源已更新。" : "资源已创建。");
    await refresh();
  } finally {
    submitting.value = false;
  }
}
onMounted(refresh);
</script>

<template>
  <section class="page-intro">
    <span class="eyebrow">CURATE EVERY SPACE</span>
    <h1>准确维护，<span class="gradient-text">共享资源。</span></h1>
    <p>集中维护资源名称、位置、类型和启用状态，供同学在发现空间页预约。</p>
  </section>
  <div class="section-heading">
    <div>
      <h2>资源管理</h2>
      <p class="muted">
        {{
          isDemo
            ? "管理员界面预览 · 保存只更新本次页面数据，刷新后重置"
            : "数据写入主后端，权限与字段由服务端校验"
        }}
      </p>
    </div>
    <button class="button primary" @click="openCreate">
      <AppIcon name="plus" :size="18" />新增资源
    </button>
  </div>
  <form class="admin-toolbar panel" @submit.prevent="search">
    <label
      >搜索资源<input
        v-model="keyword"
        class="input"
        type="search"
        placeholder="名称、位置或编号"
    /></label>
    <label
      >资源类型<select v-model="typeFilter" class="input">
        <option value="">全部类型</option>
        <option
          v-for="type in resourceTypes"
          :key="type.value"
          :value="type.value"
        >
          {{ type.label }}
        </option>
      </select></label>
    <button class="button secondary" type="submit" :disabled="loading">
      {{ loading ? "加载中…" : "查询" }}
    </button>
    <div class="admin-tip">
      <AppIcon name="info" :size="17" />开放安排与教学占用由时段管理维护。
    </div>
  </form>
  <div class="table-wrap panel">
    <table class="schedule-table">
      <thead>
        <tr>
          <th>资源 / 位置</th>
          <th>类型</th>
          <th>编号标签</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <td>
            <strong>{{ item.name }}</strong
            ><small>{{ item.location }}</small>
          </td>
          <td>{{ typeLabels[item.resourceType] ?? item.resourceType }}</td>
          <td class="tabular">{{ item.unitLabel }}</td>
          <td>
            <span
              class="badge"
              :class="item.enabled ? 'status-checked_in' : 'status-cancelled'"
              >{{ item.enabled ? "启用" : "已停用" }}</span
            >
          </td>
          <td>
            <button class="button secondary" @click="openEdit(item)">
              编辑
            </button>
          </td>
        </tr>
        <tr v-if="!items.length">
          <td colspan="5" class="table-empty">
            {{
              failed
                ? "资源列表加载失败，请检查登录状态或接口后重试。"
                : "没有匹配的资源。可新增一条，或调整筛选条件。"
            }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
  <div class="pagination">
    <span class="muted">共 {{ total }} 条 · 第 {{ page }} / {{ pageCount }} 页</span
    ><button
      class="button secondary"
      :disabled="loading || page <= 1"
      @click="turn(-1)"
    >
      上一页</button
    ><button
      class="button secondary"
      :disabled="loading || page >= pageCount"
      @click="turn(1)"
    >
      下一页
    </button>
  </div>
  <dialog ref="dialog" class="modal" aria-labelledby="resource-form-title">
    <form @submit.prevent="submit">
      <div class="modal-heading">
        <div>
          <span class="eyebrow">RESOURCE FORM</span>
          <h2 id="resource-form-title">
            {{ editingId ? "编辑资源" : "新增资源" }}
          </h2>
        </div>
        <button
          type="button"
          class="icon-button"
          aria-label="关闭表单"
          @click="dialog?.close()"
        >
          <AppIcon name="close" />
        </button>
      </div>
      <p class="muted form-description">
        {{
          isDemo
            ? "演示模式仅更新本次页面内存，不会写入数据库。"
            : "提交后读取服务端返回结果；停用资源不再出现在同学可预约列表中。"
        }}
      </p>
      <label class="form-field"
        >资源名称<input
          v-model="form.name"
          class="input"
          maxlength="100"
          placeholder="例如：图书馆A区03座"
          required
      /></label>
      <label class="form-field"
        >位置<input
          v-model="form.location"
          class="input"
          maxlength="100"
          placeholder="例如：图书馆一楼"
          required
      /></label>
      <div class="form-columns">
        <label class="form-field"
          >资源类型<select v-model="form.resourceType" class="input">
            <option
              v-for="type in resourceTypes"
              :key="type.value"
              :value="type.value"
            >
              {{ type.label }}
            </option>
          </select></label
        ><label class="form-field"
          >编号标签<input
            v-model="form.unitLabel"
            class="input"
            maxlength="50"
            placeholder="例如：A-03"
            required
        /></label>
      </div>
      <div class="form-field">
        <label class="available-filter"
          ><input v-model="form.enabled" type="checkbox" />启用该资源</label
        >
      </div>
      <p v-if="formError" class="error-message" role="alert">{{ formError }}</p>
      <div class="modal-actions">
        <button class="button secondary" type="button" @click="dialog?.close()">
          取消</button
        ><button class="button primary" type="submit" :disabled="submitting">
          {{ submitting ? "提交中…" : editingId ? "保存修改" : "创建资源" }}
        </button>
      </div>
    </form>
  </dialog>
</template>
