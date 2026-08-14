<template>
  <div id="alarmManage" class="app-container">
    <div style="height: calc(100vh - 124px);">
      <el-form :inline="true" size="mini">
        <el-form-item label="start time">
          <el-date-picker
            v-model="beginTime"
            type="datetime"
            placeholder="start time"
            value-format="yyyy-MM-dd HH:mm:ss"
            style="width: 180px;"
            clearable
          />
        </el-form-item>
        <el-form-item label="end time">
          <el-date-picker
            v-model="endTime"
            type="datetime"
            placeholder="end time"
            value-format="yyyy-MM-dd HH:mm:ss"
            style="width: 180px;"
            clearable
          />
        </el-form-item>
        <el-form-item label="Alarm type">
          <el-select
            v-model="selectedAlarmTypes"
            multiple
            collapse-tags
            placeholder="All types"
            style="width: 200px;"
            clearable
          >
            <el-option
              v-for="item in alarmTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button size="mini" type="primary" icon="el-icon-search" @click="search">
            Query
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button
            size="mini"
            type="danger"
            icon="el-icon-delete"
            :disabled="selectedRows.length === 0"
            @click="deleteSelected"
          >
            Remove selected
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button
            size="mini"
            type="danger"
            plain
            icon="el-icon-delete-solid"
            @click="clearByCondition"
          >
            Clear
          </el-button>
        </el-form-item>
        <el-form-item style="float: right;">
          <el-button icon="el-icon-refresh-right" circle size="mini" @click="getAlarmList()" />
        </el-form-item>
      </el-form>
      <el-table
        ref="alarmTable"
        size="small"
        :data="alarmList"
        height="calc(100% - 64px)"
        style="width: 100%"
        header-row-class-name="table-header"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="alarmType" label="Alarm type" width="160">
          <template v-slot:default="scope">
            <el-tag size="mini" :type="getAlarmTypeTagType(scope.row.alarmType)">
              {{ getAlarmTypeLabel(scope.row.alarmType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Snapshot" width="100">
          <template v-slot:default="scope">
            <el-image
              v-if="scope.row.snapPath"
              :src="getSnapUrl(scope.row.id)"
              :preview-src-list="[getSnapUrl(scope.row.id)]"
              fit="cover"
              style="width: 64px; height: 48px; cursor: pointer;"
              lazy
            >
              <div slot="error" style="width: 64px; height: 48px; line-height: 48px; text-align: center; color: #c0c4cc; font-size: 12px;">
                <i class="el-icon-picture-outline" />
              </div>
            </el-image>
            <span v-else style="color: #c0c4cc; font-size: 12px;">None</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="Alarm description" show-overflow-tooltip />
        <el-table-column prop="channelName" label="Channel name" width="150" />
        <el-table-column prop="channelDeviceId" label="Channel number" width="180" />
        <el-table-column prop="longitude" label="longitude" width="110" />
        <el-table-column prop="latitude" label="Latitude" width="110" />
        <el-table-column label="Alarm time" width="170">
          <template v-slot:default="scope">
            {{ formatTime(scope.row.alarmTime) }}
          </template>
        </el-table-column>
        <el-table-column label="Operation" width="150" fixed="right">
          <template v-slot:default="scope">
            <el-button
              size="medium"
              icon="el-icon-video-play"
              type="text"
              @click="openPlayback(scope.row)"
            >Playback</el-button>
            <el-button
              size="medium"
              icon="el-icon-delete"
              style="color: #f56c6c"
              type="text"
              @click="deleteSingle(scope.row)"
            >Delete</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="text-align: right"
        :current-page="currentPage"
        :page-size="count"
        :page-sizes="[15, 25, 35, 50]"
        layout="total, sizes, prev, pager, next"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="currentChange"
      />
    </div>

    <!-- Video playback dialog box -->
    <el-dialog
      :title="playbackTitle"
      :visible.sync="playbackDialogVisible"
      width="800px"
      :before-close="closePlayback"
      destroy-on-close
    >
      <div v-if="playbackLoading" style="text-align: center; padding: 40px 0;">
        <i class="el-icon-loading" style="font-size: 32px;" />
        <div style="margin-top: 10px; color: #606266;">Loading playback...</div>
      </div>
      <div v-else-if="playbackError" style="text-align: center; padding: 40px 0; color: #f56c6c;">
        <i class="el-icon-warning-outline" style="font-size: 32px;" />
        <div style="margin-top: 10px;">{{ playbackError }}</div>
      </div>
      <div v-else-if="playbackStreamInfo" style="height: 400px;">
        <playerTabs
          ref="playbackPlayer"
          :show-button="false"
          :showTab="true"
          :has-audio="true"
        />
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button size="mini" @click="closePlayback">Close</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import playerTabs from '../common/playerTabs.vue'

const ALARM_TYPE_OPTIONS = [
  { value: 'VideoLoss', label: 'Video loss alarm' },
  { value: 'DeviceTamper', label: 'Equipment anti-tamper alarm' },
  { value: 'StorageFull', label: 'Storage device disk full alarm' },
  { value: 'DeviceHighTemperature', label: 'Equipment high temperature alarm' },
  { value: 'DeviceLowTemperature', label: 'Equipment low temperature alarm' },
  { value: 'ManualVideo', label: 'Manual video alarm' },
  { value: 'MotionDetection', label: 'Moving target detection alarm' },
  { value: 'LeftObjectDetection', label: 'Remaining object detection alarm' },
  { value: 'ObjectRemovalDetection', label: 'Object removal detection alarm' },
  { value: 'TripwireDetection', label: 'Tripwire detection alarm' },
  { value: 'IntrusionDetection', label: 'Intrusion detection alarm' },
  { value: 'MobileDetection', label: 'Motion detection alarm' },
  { value: 'VideoOcclusion', label: 'Video occlusion alarm' },
  { value: 'ReverseDetection', label: 'Retrograde detection alarm' },
  { value: 'LoiteringDetection', label: 'Wandering detection alarm' },
  { value: 'FlowStatistics', label: 'Traffic statistics alarm' },
  { value: 'DensityDetection', label: 'Density detection alarm' },
  { value: 'VideoAbnormal', label: 'Video anomaly detection and alarm' },
  { value: 'RapidMovement', label: 'Fast moving alarm' },
  { value: 'StorageFault', label: 'Storage device disk failure alarm' },
  { value: 'StorageFanFault', label: 'Storage device fan failure alarm' },
  { value: 'SoundAbnormal', label: 'Abnormal sound alarm' },
  { value: 'SignalAbnormal', label: 'Semaphore exception alarm' },
  { value: 'IllegalAccess', label: 'Illegal access alarm' },
  { value: 'Defocus', label: 'Virtual focus alarm' },
  { value: 'SceneChange', label: 'Scene change alarm' },
  { value: 'CrowdGathering', label: 'Call the police when people gather' },
  { value: 'ParkingDetection', label: 'Parking detection alarm' },
  { value: 'Other', label: 'Other alarms' }
]

function formatDatetime(ts) {
  if (!ts) return null
  const date = new Date(ts)
  const pad = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
         `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export default {
  name: 'AlarmManage',
  components: { playerTabs },
  data() {
    return {
      alarmList: [],
      beginTime: null,
      endTime: null,
      selectedAlarmTypes: [],
      alarmTypeOptions: ALARM_TYPE_OPTIONS,
      currentPage: 1,
      count: 15,
      total: 0,
      selectedRows: [],
      // Replay related
      playbackDialogVisible: false,
      playbackLoading: false,
      playbackError: null,
      playbackStreamInfo: null,
      playbackTitle: 'Video playback',
      currentPlaybackChannelId: null
    }
  },
  created() {
    this.getAlarmList()
  },
  methods: {
    currentChange(val) {
      this.currentPage = val
      this.getAlarmList()
    },
    handleSizeChange(val) {
      this.count = val
      this.getAlarmList()
    },
    handleSelectionChange(rows) {
      this.selectedRows = rows
    },
    search() {
      this.currentPage = 1
      this.total = 0
      this.getAlarmList()
    },
    getAlarmList() {
      this.$store.dispatch('alarm/getAlarmList', {
        page: this.currentPage,
        count: this.count,
        alarmType: this.selectedAlarmTypes.length > 0 ? this.selectedAlarmTypes : undefined,
        beginTime: this.beginTime || undefined,
        endTime: this.endTime || undefined
      }).then(data => {
        this.total = data.total
        this.alarmList = data.list
        this.$nextTick(() => {
          this.$refs.alarmTable.doLayout()
        })
      }).catch(error => {
        console.log(error)
      })
    },
    getSnapUrl(id) {
      const baseUrl = window.baseUrl ? window.baseUrl : ''
      return ((process.env.NODE_ENV === 'development') ? process.env.VUE_APP_BASE_API : baseUrl) + `/api/alarm/snap/${id}`
    },
    openPlayback(row) {
      if (!row.channelId) {
        this.$message({ showClose: true, message: 'This alarm has no associated channel and cannot be played back.', type: 'warning' })
        return
      }
      this.playbackTitle = `Video playback - ${row.channelName || row.channelDeviceId} (${this.formatTime(row.alarmTime)})`
      this.playbackDialogVisible = true
      this.playbackLoading = true
      this.playbackError = null
      this.playbackStreamInfo = null
      this.playbackVideoUrl = null
      this.currentPlaybackChannelId = row.channelId

      // Start time: 10 seconds before the alarm time, End time: 10 seconds after the alarm time (20 seconds in total）
      const alarmTs = row.alarmTime
      const startTime = formatDatetime(alarmTs - 10 * 1000)
      const endTime = formatDatetime(alarmTs + 10 * 1000)

      this.$store.dispatch('commonChanel/playback', {
        channelId: row.channelId,
        startTime: startTime,
        endTime: endTime
      }).then(data => {
        this.playbackStreamInfo = data
        this.playbackLoading = false
        this.$nextTick(() => {
          if (this.$refs.playbackPlayer) {
            this.$refs.playbackPlayer.setStreamInfo(data)
          }
        })
      }).catch(err => {
        this.playbackLoading = false
        this.playbackError = (err && err.msg) ? err.msg : 'The playback request failed, please check whether the channel has recordings during this period.'
        console.log(err)
      })
    },
    closePlayback() {
      if (this.$refs.playbackPlayer) {
        this.$refs.playbackPlayer.stop()
      }
      if (this.playbackStreamInfo && this.currentPlaybackChannelId) {
        this.$store.dispatch('commonChanel/stopPlayback', {
          channelId: this.currentPlaybackChannelId,
          stream: this.playbackStreamInfo.stream
        }).catch(err => {
          console.log(err)
        })
      }
      this.playbackDialogVisible = false
      this.playbackStreamInfo = null
      this.playbackError = null
      this.currentPlaybackChannelId = null
    },
    deleteSingle(row) {
      this.$confirm('Confirm to delete the alarm record?', 'Tips', {
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('alarm/deleteAlarms', [row.id])
          .then(() => {
            this.$message({ showClose: true, message: 'Delete successfully', type: 'success' })
            this.getAlarmList()
          })
          .catch(error => {
            this.$message({ showClose: true, message: error, type: 'error' })
          })
      }).catch(() => {})
    },
    deleteSelected() {
      this.$confirm(`Confirm to delete selected ${this.selectedRows.length} alarm records?`, 'Tips', {
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        const ids = this.selectedRows.map(r => r.id)
        this.$store.dispatch('alarm/deleteAlarms', ids)
          .then(() => {
            this.$message({ showClose: true, message: 'Delete successfully', type: 'success' })
            this.getAlarmList()
          })
          .catch(error => {
            this.$message({ showClose: true, message: error, type: 'error' })
          })
      }).catch(() => {})
    },
    clearByCondition() {
      const hasFilter = this.beginTime || this.endTime || this.selectedAlarmTypes.length > 0
      const filterDesc = hasFilter
        ? [
          this.beginTime ? `start time：${this.beginTime}` : null,
          this.endTime ? `end time：${this.endTime}` : null,
          this.selectedAlarmTypes.length > 0 ? `Alarm type：${this.selectedAlarmTypes.map(v => {
            const opt = this.alarmTypeOptions.find(o => o.value === v)
            return opt ? opt.label : v
          }).join('、')}` : null
        ].filter(Boolean).join('；')
        : 'All'
      this.$confirm(
        `All alarm records matching the current filter conditions will be deleted（${filterDesc}），This operation is irreversible, confirm to continue？`,
        'Clear alarm',
        {
          confirmButtonText: 'Confirm deletion',
          cancelButtonText: 'Cancel',
          type: 'warning'
        }
      ).then(() => {
        this.$store.dispatch('alarm/clearAlarms', {
          alarmType: this.selectedAlarmTypes.length > 0 ? this.selectedAlarmTypes : undefined,
          beginTime: this.beginTime || undefined,
          endTime: this.endTime || undefined
        }).then(count => {
          this.$message({ showClose: true, message: `Cleared ${count != null ? count : ''} alarm records`, type: 'success' })
          this.currentPage = 1
          this.getAlarmList()
        }).catch(error => {
          this.$message({ showClose: true, message: error, type: 'error' })
        })
      }).catch(() => {})
    },
    getAlarmTypeLabel(value) {
      const option = ALARM_TYPE_OPTIONS.find(o => o.value === value)
      return option ? option.label : value
    },
    getAlarmTypeTagType(value) {
      const dangerTypes = ['VideoLoss', 'IntrusionDetection', 'IllegalAccess', 'ManualVideo', 'TripwireDetection']
      const warningTypes = ['MotionDetection', 'MobileDetection', 'ReverseDetection', 'CrowdGathering', 'RapidMovement']
      if (dangerTypes.includes(value)) return 'danger'
      if (warningTypes.includes(value)) return 'warning'
      return 'info'
    },
    formatTime(timestamp) {
      if (!timestamp) return '-'
      return formatDatetime(timestamp)
    }
  }
}
</script>
