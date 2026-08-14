<template>
  <div id="configInfo">
    <el-form :inline="true" size="mini" @submit.native.prevent>
      <el-form-item>
        <el-select
          v-model="type"
          style="width: 8rem; margin-right: 1rem;"
          placeholder="Please select type"
          default-first-option
        >
          <el-option label="image" :value="0" />
          <el-option label="Audio" :value="1" />
          <el-option label="video" :value="2" />
        </el-select>
        <el-select
          v-model="event"
          style="width: 8rem; margin-right: 1rem;"
          placeholder="Please select an event"
          default-first-option
        >
          <el-option label="Platform issues instructions" :value="0" />
          <el-option label="timed action" :value="1" />
          <el-option label="Robbery alarm triggered" :value="2" />
          <el-option label="Collision rollover alarm triggered" :value="3" />
        </el-select>
        <el-select
          v-model="chanelId"
          style="width: 8rem; margin-right: 1rem;"
          placeholder="Please select channel"
          default-first-option
        >
          <el-option label="all channels" :value="0" />
          <el-option v-for="item in channelList" :key="item.channelId" :label="item.name" :value="item.channelId" />
        </el-select>
        <el-date-picker
          v-model="timeRange"
          type="datetimerange"
          format="yyyy-MM-dd HH-mm-ss"
          value-format="yyyy-MM-dd HH:mm:ss"
          :picker-options="pickerOptions"
          range-separator="to"
          start-placeholder="start date"
          end-placeholder="end date"
          align="right">
        </el-date-picker>

      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="queryLoading" icon="el-icon-search" @click="search()" >
          Search
        </el-button>
      </el-form-item>
    </el-form>
    <el-table :data="mediaDataInfoList" :height="500" stripe style="width: 100%" empty-text="No data yet">
      <el-table-column prop="id" label="ID" />
      <el-table-column prop="type" label="Type" >
        <template v-slot:default="scope">
          {{typeLabel(scope.row.type)}}
        </template>
      </el-table-column>
      <el-table-column label="event" >
        <template v-slot:default="scope">
          {{eventCodeLabel(scope.row.eventCode)}}
        </template>
      </el-table-column>
      <el-table-column prop="channelId" label="channelID" />
      <el-table-column label="Operation" fixed="right">
        <template v-slot:default="scope">
          <el-button
            size="medium"
            type="text"
            icon="el-icon-location-information"
            :loading="scope.row.addRegionLoading"
            @click="showPositionInfo(scope.row)"
          >
            location report
          </el-button>
          <el-button
            size="medium"
            type="text"
            icon="el-icon-download"
            :loading="scope.row.addRegionLoading"
            @click="download(scope.row)"
          >
            Download
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <position ref="position"></position>
  </div>
</template>

<script>

import elDragDialog from '@/directive/el-drag-dialog'
import position from './position.vue'
import dayjs from 'dayjs'

export default {
  name: 'ConfigInfo',
  directives: { elDragDialog },
  components: { position },
  props: ['phoneNumber', 'deviceId', 'channelList'],
  data() {
    return {
      queryLoading: false,
      type: 0,
      chanelId: 0,
      event: 0,
      mediaDataInfoList: [],
      timeRange: '',
      pickerOptions: {
        shortcuts: [
          {
            text: 'last 24 hours',
            onClick(picker) {
              const end = new Date()
              const start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24)
              picker.$emit('pick', [start, end])
            }
          },
          {
            text: 'last week',
            onClick(picker) {
              const end = new Date()
              const start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
              picker.$emit('pick', [start, end])
            }
          },
          {
            text: 'last month',
            onClick(picker) {
              const end = new Date()
              const start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
              picker.$emit('pick', [start, end])
            }
          },
          {
            text: 'last three months',
            onClick(picker) {
              const end = new Date()
              const start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
              picker.$emit('pick', [start, end])
          }
        }]
      }
    }
  },
  computed: {},
  created() {

  },
  methods: {
    close: function() {
      this.mediaDataInfoList = []
      this.channelList = []
      this.type = 0
      this.chanelId = 0
      this.event = 0
    },
    typeLabel: function(type) {
      switch (type){
        case 0:
          return 'image'
        case 1:
          return 'Audio'
        case 2:
          return 'video'
        default:
          return type
      }
    },
    eventCodeLabel: function(eventCode) {
      switch (eventCode){
        case 0:
          return 'Platform issues instructions'
        case 1:
          return 'timed action'
        case 2:
          return 'Robbery alarm triggered'
        case 3:
          return 'Collision rollover alarm triggered'
        case 4:
          return 'Taking photos with the door open'
        case 5:
          return 'Taking photos after closing the door'
        case 6:
          return 'The door changes from open to closed, and the vehicle speed changes from less than 20km to more than 20km.20km'
        case 7:
          return 'Taking photos at a fixed distance'
        default:
          return eventCode
      }
    },
    search: function() {
      this.mediaDataInfoList = []
      this.queryLoading = true
      this.$store.dispatch('jtDevice/queryMediaData', {
        phoneNumber: this.phoneNumber,
        queryMediaDataCommand: {
          type: this.type,
          channelId: this.chanelId,
          event: this.event,
          startTime: this.timeRange === '' ? null : this.timeRange[0],
          endTime: this.timeRange === '' ? null : this.timeRange[1]
        }
      })
        .then(data => {
          console.log(data)
          if (data.length === 0) {
            this.$message.info('No relevant records found')
          }
          this.mediaDataInfoList = data
        })
        .finally(() => {
          this.queryLoading = false
        })
    },
    showPositionInfo: function(row) {
      this.$refs.position.openDialog(row.positionBaseInfo)
    },
    download: function(row) {
      this.$message.success('Download request sent', { closed: true })
      // File download address
      const baseUrl = window.baseUrl ? window.baseUrl : ''
      const fileUrl = ((process.env.NODE_ENV === 'development') ? process.env.VUE_APP_BASE_API : baseUrl) + `/api/jt1078/media/upload/one/upload?phoneNumber=${this.phoneNumber}&mediaId=${row.id}`
      let controller = new AbortController()
      let signal = controller.signal
      // Set request header
      const headers = new Headers()
      headers.append('access-token', this.$store.getters.token) // Set the authorization header and replace YourAccessToken with the actual access token
      // Make a request
      fetch(fileUrl, {
        method: 'GET',
        headers: headers,
        signal: signal
      })
        .then(response => response.blob())
        .then(blob => {
          console.log(blob)
          // Create a virtual link element to simulate a click to download
          const link = document.createElement('a')
          link.href = window.URL.createObjectURL(blob)
          let suffix = 'jpg'
          switch (row.type){
            case 0:
              suffix = 'jpg'
              break
            case 1:
              suffix = 'mp3'
              break
            case 2:
              suffix = 'mp4'
              break
          }
          link.download = `${row.id}.${suffix}` // Set the download file name and replace filename.ext with the actual file name and extension.
          document.body.appendChild(link)
          // simulate click
          link.click()
          // Remove virtual link element
          document.body.removeChild(link)
        })
        .catch(error => console.error('Download failed：', error))

      setTimeout(() => {
        this.$message.error('Download timeout', { closed: true })
        controller.abort('timeout')
      }, 15000)
    }
  }
}
</script>

<style scoped>
>>> .el-upload {
  width: 100% !important;
}
.el-slider__marks-text {
  margin-top: -36px;
  font-size: 12px;
  width: 2rem !important;
}
</style>
