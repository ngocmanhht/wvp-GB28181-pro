<template>
  <div>
    <el-form label-width="120px" class="guard-form">
      <el-form-item label="enable">
        <el-switch v-model="enabled" />
      </el-form-item>
      <el-form-item label="Preset position">
        <el-select v-model="presetIndex" style="width: 180px" placeholder="Select preset position">
          <el-option v-for="p in allPresetList" :key="p.presetId"
                     :label="p.presetId + '-' + (p.presetName || ('preset point' + p.presetId))"
                     :value="Number(p.presetId)" />
        </el-select>
      </el-form-item>
      <el-form-item label="Automatic homing (seconds）">
        <el-input-number v-model="resetTime" :min="1" :max="999999" controls-position="right" style="width: 180px" />
      </el-form-item>
      <el-form-item>
        <div class="guard-actions">
          <el-button @click="loadPresets">Refresh</el-button>
          <el-button type="primary" :loading="submitting" :disabled="submitting" @click="confirmSave">save</el-button>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
export default {
  name: 'ChPtzGuardConfig',
  props: {
    channelId: { type: String, default: null }
  },
  data() {
    return {
      enabled: false,
      presetIndex: null,
      resetTime: 10,
      allPresetList: [],
      submitting: false
    }
  },
  created() {
    this.loadPresets()
  },
  methods: {
    loadPresets() {
      this.$store.dispatch('commonChanel/queryPreset', this.channelId)
        .then(data => {
          this.allPresetList = data || []
        })
        .catch(error => {
          console.log('[guard position] Failed to load preset point list', error)
        })
    },
    confirmSave() {
      if (!this.enabled && !this.presetIndex) {
        this.$message({ showClose: true, message: 'Please select a preset number', type: 'warning' })
        return
      }
      if (this.resetTime == null || this.resetTime < 1) {
        this.$message({ showClose: true, message: 'Please enter a valid return time', type: 'warning' })
        return
      }
      this.submitting = true
      const params = {
        channelId: this.channelId,
        enabled: this.enabled
      }
      if (this.presetIndex != null) {
        params.presetIndex = this.presetIndex
      }
      if (this.resetTime != null) {
        params.resetTime = this.resetTime
      }
      this.$store.dispatch('commonChanel/homePosition', params)
        .then(() => {
          this.$message({ showClose: true, message: 'Saved successfully', type: 'success' })
        })
        .catch(error => {
          this.$message({ showClose: true, message: error || 'Save failed', type: 'error' })
        })
        .finally(() => {
          this.submitting = false
        })
    }
  }
}
</script>

<style scoped>
.guard-form {
  padding: 16px 12px;
  max-width: 420px;
}
.guard-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
