<template>
  <div id="ptzCruiseConfig" style="height: 100%; display: flex; flex-direction: column;">
    <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px;">
      <div>
        <el-button type="primary" :disabled="formVisible" @click="openAdd">Add cruise group</el-button>
        <el-button :loading="clearing" :disabled="clearing" @click="clearCruiseTours">Clear</el-button>
      </div>
      <el-button icon="el-icon-refresh-right" circle @click="loadPresets" />
    </div>
    <div v-if="formVisible" style="margin-bottom: 6px; padding: 16px 8px; border: 1px solid #e6e6e6; border-radius: 4px;">
      <el-form inline size="small" style="display: flex; align-items: center; margin-top: 15px;">
        <el-form-item label="serial number" style="margin-bottom: 0;">
          <el-input-number v-model="formId" :min="0" :max="255" controls-position="right" style="width: 120px" />
        </el-form-item>
        <el-form-item label="Name" style="margin-bottom: 0;">
          <el-input v-model="formName" placeholder="Name" style="width: 140px" />
        </el-form-item>
        <el-form-item style="margin-bottom: 0;">
          <el-button type="primary" :loading="submitting" :disabled="submitting" @click="confirmSave">OK</el-button>
          <el-button @click="cancelForm">Cancel</el-button>
        </el-form-item>
      </el-form>
      <el-divider style="margin: 6px 0;" />
      <div style="margin-bottom: 4px;">
        <el-button size="mini" type="primary" @click="addPresetRow">Add preset point</el-button>
      </div>
      <el-table :data="formPresets" size="mini" stripe border max-height="200px">
        <el-table-column label="serial number" width="50">
          <template v-slot="{ $index }">{{ $index + 1 }}</template>
        </el-table-column>
        <el-table-column label="preset point" min-width="100">
          <template v-slot="{ row }">
            <el-select v-model="row.presetId" size="mini" style="width: 120px" placeholder="Select preset point">
              <el-option v-for="p in allPresetList" :key="p.presetId"
                         :label="p.presetName || ('preset point' + p.presetId)"
                         :value="p.presetId" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="Residence time (seconds）" min-width="100">
          <template v-slot="{ row }">
            <el-input-number v-model="row.dwellTime" :min="15" :max="300" size="mini" controls-position="right" style="width: 90px" />
          </template>
        </el-table-column>
        <el-table-column label="speed" min-width="100">
          <template v-slot="{ row }">
            <el-select v-model="row.speed" size="mini" style="width: 90px">
              <el-option v-for="s in 10" :key="s" :label="s" :value="s" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="Operation" width="60">
          <template v-slot="{ $index }">
            <el-button size="mini" type="text" style="color: #F56C6C" @click="removePresetRow($index)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div v-if="cruiseTours.length > 0" style="flex: 1; overflow: auto;">
      <el-table ref="cruiseTable" :data="cruiseTours" size="mini" max-height="100%" stripe border highlight-current-row>
        <el-table-column prop="id" label="ID" />
        <el-table-column prop="name" label="cruise name" />
        <el-table-column label="Operation" min-width="150">
          <template v-slot:default="scope">
            <el-button v-if="cruisingCruiseId === scope.row.id" size="mini" type="text" style="color: #F56C6C" :loading="operatingId === scope.row.id" :disabled="operatingId !== null" @click="stopCruise(scope.row)">deactivate</el-button>
            <el-button v-else size="mini" type="text" :disabled="cruisingCruiseId !== null || operatingId !== null" style="color: #409EFF" :loading="operatingId === scope.row.id" @click="startCruise(scope.row)">enable</el-button>
            <el-button size="mini" type="text" style="color: #409EFF" :disabled="operatingId !== null" @click="openEdit(scope.row)">Edit</el-button>
            <el-button size="mini" type="text" style="color: #F56C6C" :loading="deletingId === scope.row.id" :disabled="operatingId !== null || deletingId !== null" @click="deleteCruise(scope.row)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div v-else style="color: #909399; font-size: 12px; margin-bottom: 8px;">No cruise route yet</div>
  </div>
</template>

<script>
export default {
  name: 'PtzCruiseConfig',
  props: {
    deviceId: { type: String, default: null },
    channelDeviceId: { type: String, default: null }
  },
  data() {
    return {
      cruiseTours: [],
      cruisingCruiseId: null,
      formVisible: false,
      editingTourId: null,
      submitting: false,
      clearing: false,
      operatingId: null,
      deletingId: null,
      formId: 1,
      formName: '',
      formPresets: [],
      allPresetList: []
    }
  },
  created() {
    this.loadPresets()
  },
  methods: {
    loadPresets() {
      this.$store.dispatch('frontEnd/queryPreset', [this.deviceId, this.channelDeviceId])
        .then(data => {
          this.allPresetList = data || []
        })
        .catch(error => {
          console.log('[cruise] Failed to load preset point list', error)
        })
    },
    getNextAvailableId() {
      const used = new Set((this.cruiseTours || []).map(t => t.id))
      for (let i = 0; i <= 255; i++) {
        if (!used.has(i)) return i
      }
      return 0
    },
    openAdd() {
      this.editingTourId = null
      this.formId = this.getNextAvailableId()
      this.formName = 'Cruise group' + this.formId
      this.formPresets = []
      this.formVisible = true
    },
    openEdit(tour) {
      this.editingTourId = tour.id
      this.formId = tour.id
      this.formName = tour.name
      this.formPresets = (tour.presets || []).map(p => ({
        presetId: p.presetId,
        dwellTime: p.dwellTime,
        speed: p.speed
      }))
      if (this.formPresets.length === 0) {
        this.formPresets.push({ presetId: this.getFirstPresetId(), dwellTime: 15, speed: 7 })
      }
      this.formVisible = true
    },
    cancelForm() {
      this.formVisible = false
      this.editingTourId = null
      this.formPresets = []
    },
    getFirstPresetId() {
      const first = this.allPresetList[0]
      return first ? first.presetId : 1
    },
    addPresetRow() {
      this.formPresets.push({
        presetId: this.getFirstPresetId(),
        dwellTime: 15,
        speed: 7
      })
    },
    removePresetRow(index) {
      this.formPresets.splice(index, 1)
    },
    confirmSave() {
      if (!this.formName.trim()) {
        this.$message({ showClose: true, message: 'Please enter the cruise group name', type: 'warning' })
        return
      }
      if (this.formId == null || this.formId < 0 || this.formId > 255) {
        this.$message({ showClose: true, message: 'The cruise serial number must be in0-255between', type: 'warning' })
        return
      }
      this.submitting = true
      let chain = Promise.resolve()
      if (this.editingTourId !== null) {
        chain = chain.then(() => this.$store.dispatch('frontEnd/deletePointForCruise', [this.deviceId, this.channelDeviceId, this.formId, 0]))
      }
      this.formPresets.forEach(p => {
        chain = chain.then(() => this.$store.dispatch('frontEnd/addPointForCruise', [this.deviceId, this.channelDeviceId, this.formId, p.presetId]))
      })
      const speed = this.formPresets.length > 0 ? this.formPresets[0].speed : 7
      const dwellTime = this.formPresets.length > 0 ? this.formPresets[0].dwellTime : 15
      chain = chain.then(() => this.$store.dispatch('frontEnd/setCruiseSpeed', [this.deviceId, this.channelDeviceId, this.formId, speed]))
      chain = chain.then(() => this.$store.dispatch('frontEnd/setCruiseTime', [this.deviceId, this.channelDeviceId, this.formId, dwellTime]))
      chain.then(() => {
        const idx = this.cruiseTours.findIndex(t => t.id === this.formId)
        const presets = this.formPresets.map(p => ({
          presetId: p.presetId,
          dwellTime: p.dwellTime,
          speed: p.speed
        }))
        const tour = { id: this.formId, name: this.formName, presets }
        if (idx !== -1) {
          this.$set(this.cruiseTours, idx, tour)
        } else {
          this.cruiseTours.push(tour)
        }
        this.cancelForm()
        this.$message({ showClose: true, message: 'Saved successfully', type: 'success' })
      }).catch(error => {
        this.$message({ showClose: true, message: error || 'Save failed', type: 'error' })
      }).finally(() => {
        this.submitting = false
      })
    },
    clearCruiseTours() {
      if (this.cruiseTours.length === 0) return
      this.$confirm('Confirm to clear all cruise groups?', 'Tips', {
        confirmButtonText: 'OK', cancelButtonText: 'Cancel', type: 'warning'
      }).then(() => {
        this.clearing = true
        let chain = Promise.resolve()
        this.cruiseTours.forEach(tour => {
          chain = chain.then(() => this.$store.dispatch('frontEnd/deletePointForCruise', [this.deviceId, this.channelDeviceId, tour.id, 0]))
        })
        chain.then(() => {
          this.cruiseTours = []
          this.cruisingCruiseId = null
          this.$message({ showClose: true, message: 'Cleared successfully', type: 'success' })
        }).catch(() => {
          this.$message({ showClose: true, message: 'Clearing failed', type: 'error' })
        }).finally(() => {
          this.clearing = false
        })
      }).catch(() => {})
    },
    startCruise(row) {
      this.operatingId = row.id
      this.$store.dispatch('frontEnd/startCruise', [this.deviceId, this.channelDeviceId, row.id])
        .then(() => {
          this.cruisingCruiseId = row.id
          this.$message({ showClose: true, message: 'Activated successfully', type: 'success' })
        }).catch(() => {
          this.$message({ showClose: true, message: 'Failed to enable', type: 'error' })
        }).finally(() => {
          this.operatingId = null
        })
    },
    stopCruise(row) {
      this.operatingId = row.id
      this.$store.dispatch('frontEnd/stopCruise', [this.deviceId, this.channelDeviceId, row.id])
        .then(() => {
          this.cruisingCruiseId = null
          this.$message({ showClose: true, message: 'stop successfully', type: 'success' })
        }).catch(() => {
          this.$message({ showClose: true, message: 'Stop failed', type: 'error' })
        }).finally(() => {
          this.operatingId = null
        })
    },
    deleteCruise(row) {
      this.$confirm('Confirm to delete this cruise group?', 'Tips', {
        confirmButtonText: 'OK', cancelButtonText: 'Cancel', type: 'warning'
      }).then(() => {
        this.deletingId = row.id
        this.$store.dispatch('frontEnd/deletePointForCruise', [this.deviceId, this.channelDeviceId, row.id, 0])
          .then(() => {
            const idx = this.cruiseTours.indexOf(row)
            if (idx !== -1) this.cruiseTours.splice(idx, 1)
            if (this.cruisingCruiseId === row.id) this.cruisingCruiseId = null
            this.$message({ showClose: true, message: 'Delete successfully', type: 'success' })
          }).catch(() => {
            this.$message({ showClose: true, message: 'Delete failed', type: 'error' })
          }).finally(() => {
            this.deletingId = null
          })
      }).catch(() => {})
    }
  }
}
</script>
