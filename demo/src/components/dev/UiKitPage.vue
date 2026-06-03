<template>
  <div class="ds-page">
    <main class="ds-page-container ds-page-stack">
      <PageHeader
        eyebrow="Dev only"
        title="UI Kit"
        subtitle="Checklist giao diện cho SaaS vận hành: tokens, controls, states, bảng, empty/loading/error và mobile."
      >
        <template #actions>
          <BaseButton variant="secondary" icon="arrow_back" @click="$router.push('/login')">
            Đăng nhập
          </BaseButton>
          <BaseButton icon="check_circle">Primary action</BaseButton>
        </template>
      </PageHeader>

      <BaseCard>
        <div class="ui-section-head">
          <h2>Design tokens</h2>
          <p>Màu, surface, border, shadow và semantic states dùng `--ds-*`.</p>
        </div>
        <div class="ui-swatch-grid">
          <div v-for="swatch in swatches" :key="swatch.name" class="ui-swatch">
            <span class="ui-swatch__color" :style="{ background: swatch.value }" />
            <span>{{ swatch.name }}</span>
            <code>{{ swatch.token }}</code>
          </div>
        </div>
      </BaseCard>

      <BaseCard>
        <div class="ui-section-head">
          <h2>Buttons & status</h2>
          <p>Icon buttons, loading, disabled và trạng thái semantic.</p>
        </div>
        <div class="ui-inline">
          <BaseButton icon="plus">Tạo mới</BaseButton>
          <BaseButton variant="secondary" icon="filter_list">Lọc</BaseButton>
          <BaseButton variant="ghost" icon="download">Xuất file</BaseButton>
          <BaseButton variant="danger" icon="delete">Xóa</BaseButton>
          <BaseButton loading>Đang xử lý</BaseButton>
          <BaseButton disabled>Disabled</BaseButton>
        </div>
        <div class="ui-inline">
          <StatusChip status="live" dot label="Đang thi" />
          <StatusChip status="success" dot label="Đã xác minh" />
          <StatusChip status="warning" dot label="Cần xem xét" />
          <StatusChip status="error" dot label="Rủi ro cao" />
          <StatusChip status="neutral" label="Nháp" />
        </div>
      </BaseCard>

      <BaseCard>
        <div class="ui-section-head">
          <h2>Forms</h2>
          <p>Input, select, textarea, hint, error và disabled state.</p>
        </div>
        <form class="ui-form" @submit.prevent>
          <BaseField label="Tên kỳ thi" required hint="Tên ngắn, dễ nhận diện trong danh sách.">
            <template #default="{ inputId, hintId }">
              <BaseInput :id="inputId" v-model="form.title" :described-by="hintId" placeholder="Kiểm tra giữa kỳ Toán 10" />
            </template>
          </BaseField>

          <BaseField label="Chế độ giám sát">
            <template #default="{ inputId }">
              <BaseSelect
                :id="inputId"
                v-model="form.mode"
                :options="modeOptions"
                placeholder="Chọn chế độ"
              />
            </template>
          </BaseField>

          <BaseField label="Quy chế" hint="Dùng textarea cho nội dung dài, không làm layout nhảy khi nhập.">
            <template #default="{ inputId, hintId }">
              <BaseTextarea :id="inputId" v-model="form.rules" :described-by="hintId" rows="4" />
            </template>
          </BaseField>

          <BaseField label="Email nhận báo cáo" error="Email không hợp lệ.">
            <template #default="{ inputId, errorId }">
              <BaseInput :id="inputId" v-model="form.email" :described-by="errorId" invalid placeholder="teacher@example.edu" />
            </template>
          </BaseField>
        </form>
      </BaseCard>

      <BaseCard padding="none">
        <div class="ui-table-header">
          <div>
            <h2>Bảng dữ liệu</h2>
            <p>Loading, empty, selected và row actions giữ spacing nhất quán.</p>
          </div>
          <BaseButton variant="secondary" size="sm" icon="refresh_cw">Làm mới</BaseButton>
        </div>
        <DataTable :columns="columns" :data="rows" row-key="id">
          <template #cell-status="{ row }">
            <StatusChip :status="row.status" :label="row.statusLabel" dot />
          </template>
          <template #row-actions>
            <BaseButton variant="ghost" size="xs" icon="more_horiz" title="Tác vụ">Tác vụ</BaseButton>
          </template>
        </DataTable>
      </BaseCard>

      <div class="ui-grid">
        <BaseCard>
          <EmptyState
            icon="inbox"
            title="Chưa có dữ liệu"
            description="Dùng khi danh sách rỗng, có thể kèm hành động tạo mới."
            action-label="Tạo bản ghi"
          />
        </BaseCard>
        <BaseCard>
          <div class="ui-section-head">
            <h2>Responsive checklist</h2>
            <p>Không tràn chữ trong button/card/table; toolbar tự xuống dòng trên mobile.</p>
          </div>
          <div class="ds-toolbar">
            <BaseInput v-model="form.search" placeholder="Tìm kiếm..." />
            <div class="ds-control-row">
              <BaseButton variant="secondary" icon="filter_list">Bộ lọc</BaseButton>
              <BaseButton icon="save">Lưu</BaseButton>
            </div>
          </div>
        </BaseCard>
      </div>
    </main>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import PageHeader from '../ui/PageHeader.vue'
import StatusChip from '../ui/StatusChip.vue'
import DataTable from '../ui/DataTable.vue'
import EmptyState from '../ui/EmptyState.vue'
import BaseButton from '../shared/BaseButton.vue'
import BaseCard from '../shared/BaseCard.vue'
import BaseField from '../shared/BaseField.vue'
import BaseInput from '../shared/BaseInput.vue'
import BaseSelect from '../shared/BaseSelect.vue'
import BaseTextarea from '../shared/BaseTextarea.vue'

const form = reactive({
  title: '',
  mode: 'ai',
  rules: 'Camera luôn bật trong suốt bài thi.\nKhông chuyển tab hoặc trao đổi nội dung đề.',
  email: 'teacher@',
  search: ''
})

const modeOptions = [
  { label: 'Camera + AI', value: 'ai' },
  { label: 'Camera cơ bản', value: 'camera' },
  { label: 'Không giám sát', value: 'none' }
]

const swatches = [
  { name: 'Primary', token: '--ds-primary', value: 'var(--ds-primary)' },
  { name: 'Surface', token: '--ds-surface', value: 'var(--ds-surface)' },
  { name: 'Info', token: '--ds-info', value: 'var(--ds-info)' },
  { name: 'Success', token: '--ds-success', value: 'var(--ds-success)' },
  { name: 'Warning', token: '--ds-warning', value: 'var(--ds-warning)' },
  { name: 'Danger', token: '--ds-danger', value: 'var(--ds-danger)' }
]

const columns = [
  { key: 'name', label: 'Học sinh' },
  { key: 'exam', label: 'Kỳ thi' },
  { key: 'status', label: 'Trạng thái' },
  { key: 'score', label: 'Điểm', align: 'right' }
]

const rows = [
  { id: 1, name: 'Nguyễn Minh Anh', exam: 'Toán 10', status: 'live', statusLabel: 'Đang thi', score: '-' },
  { id: 2, name: 'Trần Quốc Bảo', exam: 'Vật lý 11', status: 'success', statusLabel: 'Đã nộp', score: '8.5' },
  { id: 3, name: 'Lê Hoàng Nam', exam: 'Hóa học 12', status: 'warning', statusLabel: 'Cần xem xét', score: '-' }
]
</script>

<style scoped>
.ui-section-head {
  margin-bottom: 1rem;
}

.ui-section-head h2,
.ui-table-header h2 {
  margin: 0;
  color: var(--ds-text);
  font-size: 1rem;
  font-weight: 800;
}

.ui-section-head p,
.ui-table-header p {
  margin: 0.25rem 0 0;
  color: var(--ds-text-secondary);
  font-size: 0.875rem;
  line-height: 1.5;
}

.ui-swatch-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 0.75rem;
}

.ui-swatch {
  display: grid;
  gap: 0.4rem;
  min-width: 0;
  border: 1px solid var(--ds-border);
  border-radius: var(--ds-radius-lg);
  padding: 0.75rem;
  background: var(--ds-surface-muted);
  color: var(--ds-text);
  font-size: 0.8rem;
  font-weight: 700;
}

.ui-swatch__color {
  height: 2.5rem;
  border-radius: var(--ds-radius-md);
  border: 1px solid var(--ds-border);
}

.ui-swatch code {
  color: var(--ds-text-secondary);
  font-size: 0.72rem;
  font-weight: 600;
}

.ui-inline {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 1rem;
}

.ui-form {
  display: grid;
  gap: 1rem;
  max-width: 680px;
}

.ui-table-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem;
}

.ui-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

@media (max-width: 768px) {
  .ui-grid,
  .ui-table-header {
    grid-template-columns: 1fr;
    display: grid;
  }
}
</style>
