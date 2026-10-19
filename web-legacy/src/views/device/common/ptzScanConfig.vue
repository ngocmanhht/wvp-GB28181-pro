<template>
  <div id="ptzScanConfig" style="height: 100%; display: flex; flex-direction: column;">
    <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px;">
      <div>
        <el-button type="primary" :loading="adding" :disabled="adding" @click="addLineScan">Add line scan</el-button>
        <el-button @click="clearAll">Clear</el-button>
      </div>
      <el-button icon="el-icon-refresh-right" circle />
    </div>
    <div v-if="scanAreas.length > 0" style="flex: 1; overflow: auto;">
      <el-table :data="scanAreas" max-height="100%" stripe border highlight-current-row height="100%">
        <el-table-column label="serial number" min-width="50">
          <template v-slot="{ row }">{{ row.index }}</template>
        </el-table-column>
        <el-table-column label="Name" min-width="80">
          <template v-slot="{ row }">{{ row.name }}</template>
        </el-table-column>
        <el-table-column label="left border" min-width="90">
          <template v-slot="{ row }">
            <el-button type="text"
                       :style="{ color: row.leftBoundary ? '#67C23A' : '#409EFF' }"
                       :loading="boundaryLoading.index === row.index && boundaryLoading.side === 'Left'"
                       :disabled="operatingId !== null"
                       @click="setBoundary(row, 'Left')">
              {{ row.leftBoundary ? 'Resave' : 'To be saved' }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="right border" min-width="90">
          <template v-slot="{ row }">
            <el-button type="text"
                       :style="{ color: row.rightBoundary ? '#67C23A' : '#409EFF' }"
                       :loading="boundaryLoading.index === row.index && boundaryLoading.side === 'Right'"
                       :disabled="operatingId !== null"
                       @click="setBoundary(row, 'Right')">
              {{ row.rightBoundary ? 'Resave' : 'To be saved' }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="speed" min-width="90">
          <template v-slot="{ row }">
            <el-select v-model="row.speed" :disabled="speedSaving === row.index" @change="onSpeedChange(row)">
              <el-option v-for="s in 8" :key="s" :label="s" :value="s" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="Operation" min-width="120">
          <template v-slot="{ row, $index }">
            <el-button v-if="$index === cruisingScanIndex" type="text" style="color: #F56C6C" :loading="operatingId === row.index" :disabled="operatingId !== null" @click="stopScan(row)">deactivate</el-button>
            <el-button v-else type="text" style="color: #409EFF" :disabled="operatingId !== null" :loading="operatingId === row.index" @click="startScan(row, $index)">enable</el-button>
            <el-button type="text" style="color: #F56C6C" :disabled="operatingId !== null" @click="deleteScan(row)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div v-else style="color: #909399; font-size: 12px; margin-bottom: 8px;">Temporarily unscanned area</div>
  </div>
</template>

<script>
export default {
  name: 'PtzScanConfig',
  props: {
    deviceId: { type: String, default: null },
    channelDeviceId: { type: String, default: null }
  },
  data() {
    return {
      scanAreas: [],
      cruisingScanIndex: null,
      operatingId: null,
      adding: false,
      boundaryLoading: { index: null, side: null },
      speedSaving: null
    }
  },
  methods: {
    getNextAvailableIndex() {
      const used = new Set(this.scanAreas.filter(a => a.name && a.name.trim()).map(a => a.index))
      for (let i = 0; i <= 255; i++) {
        if (!used.has(i)) return i
      }
      return 0
    },
    addLineScan() {
      const nextIndex = this.getNextAvailableIndex()
      const name = 'Line scan' + nextIndex
      this.adding = true
      this.scanAreas.push({
        index: nextIndex,
        name: name,
        leftBoundary: false,
        rightBoundary: false,
        speed: 5
      })
      this.$nextTick(() => { this.adding = false })
    },
    setBoundary(row, boundary) {
      this.boundaryLoading = { index: row.index, side: boundary }
      const action = boundary === 'Left' ? 'setLeftForScan' : 'setRightForScan'
      this.$store.dispatch('frontEnd/' + action, [this.deviceId, this.channelDeviceId, row.index])
        .then(() => {
          this.$message({ showClose: true, message: (boundary === 'Left' ? 'left' : 'right') + 'Boundary set successfully', type: 'success' })
          if (boundary === 'Left') {
            row.leftBoundary = true
          } else {
            row.rightBoundary = true
          }
        }).catch(() => {
          this.$message({ showClose: true, message: 'Boundary setup failed', type: 'error' })
        }).finally(() => {
          this.boundaryLoading = { index: null, side: null }
        })
    },
    onSpeedChange(row) {
      this.speedSaving = row.index
      this.$store.dispatch('frontEnd/setSpeedForScan', [this.deviceId, this.channelDeviceId, row.index, row.speed])
        .then(() => {
          this.$message({ showClose: true, message: 'speed saved', type: 'success' })
        }).catch(() => {
          this.$message({ showClose: true, message: 'Speed saving failed', type: 'error' })
        }).finally(() => {
          this.speedSaving = null
        })
    },
    startScan(row, index) {
      this.operatingId = row.index
      this.$store.dispatch('frontEnd/startScan', [this.deviceId, this.channelDeviceId, row.index])
        .then(() => {
          this.$message({ showClose: true, message: 'Activated successfully', type: 'success' })
          this.cruisingScanIndex = index
        }).catch(() => {
          this.$message({ showClose: true, message: 'Failed to enable', type: 'error' })
        }).finally(() => {
          this.operatingId = null
        })
    },
    stopScan(row) {
      this.operatingId = row.index
      this.$store.dispatch('frontEnd/stopScan', [this.deviceId, this.channelDeviceId, row.index])
        .then(() => {
          this.$message({ showClose: true, message: 'Deactivation successful', type: 'success' })
          this.cruisingScanIndex = null
        }).catch(() => {
          this.$message({ showClose: true, message: 'Deactivation failed', type: 'error' })
        }).finally(() => {
          this.operatingId = null
        })
    },
    deleteScan(row) {
      this.$confirm('Confirm delete line scan ' + row.index + '?', 'Tips', {
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        const idx = this.scanAreas.indexOf(row)
        if (idx !== -1) this.scanAreas.splice(idx, 1)
        if (this.cruisingScanIndex !== null && this.scanAreas[this.cruisingScanIndex] === undefined) {
          this.cruisingScanIndex = null
        }
        this.$message({ showClose: true, message: 'Deletion successful (only local list, device configuration needs to be cleared manually）', type: 'success' })
      }).catch(() => {})
    },
    clearAll() {
      if (this.scanAreas.length === 0) return
      this.$confirm('Make sure to clear all line scan areas?', 'Tips', {
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        this.scanAreas = []
        this.cruisingScanIndex = null
        this.$message({ showClose: true, message: 'Cleared successfully (only local list, device configuration needs to be cleared manually）', type: 'success' })
      }).catch(() => {})
    }
  }
}
</script>
