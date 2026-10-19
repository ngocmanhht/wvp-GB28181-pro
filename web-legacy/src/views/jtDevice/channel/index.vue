<template>
  <div id="channelList" style="height: calc(100vh - 124px);">
    <div v-if="!jtChannel">
      <el-form :inline="true" size="mini">
        <el-form-item style="margin-right: 2rem">
          <el-page-header content="Channel list" @back="showDevice" />
        </el-form-item>
        <el-form-item label="Search">
          <el-input
            v-model="searchSrt"
            style="margin-right: 1rem; width: auto;"
            placeholder="Keywords"
            prefix-icon="el-icon-search"
            clearable
            @input="search"
          />
        </el-form-item>
        <el-form-item>
          <el-button icon="el-icon-plus" size="mini" style="margin-right: 1rem;" type="primary" @click="add">add channel</el-button>
        </el-form-item>
        <el-form-item style="float: right;">
          <el-button icon="el-icon-refresh-right" circle @click="refresh()" />
        </el-form-item>
      </el-form>
      <el-container v-loading="isLoging" style="height: 82vh;">
        <el-main style="padding: 5px;">
          <el-table
            ref="channelListTable"
            :data="deviceChannelList"
            :height="winHeight"
            style="width: 100%"
            header-row-class-name="table-header"
          >
            <el-table-column prop="channelId" label="Channel number" min-width="180" />
            <el-table-column prop="name" label="Name" min-width="180" />
            <el-table-column label="Snapshot" min-width="100">
              <template v-slot:default="scope">
                <el-image
                  :src="getSnap(scope.row)"
                  :preview-src-list="getBigSnap(scope.row)"
                  :fit="'contain'"
                  style="width: 60px"
                  @error="getSnapErrorEvent(scope.row.deviceId, scope.row.channelId)"
                >
                  <div slot="error" class="image-slot">
                    <i class="el-icon-picture-outline" />
                  </div>
                </el-image>
              </template>
            </el-table-column>
            <el-table-column label="Turn on audio" min-width="100">
              <template slot-scope="scope">
                <el-switch v-model="scope.row.hasAudio" active-color="#409EFF" @change="updateChannel(scope.row)" />
              </template>
            </el-table-column>
            <el-table-column label="Operation" min-width="340" fixed="right">
              <template slot-scope="scope">
                <el-button
                  size="medium"
                  :disabled="device == null || !device.status"
                  icon="el-icon-video-play"
                  type="text"
                  @click="sendDevicePush(scope.row)"
                >play
                </el-button>
                <el-button
                  v-if="!!scope.row.stream"
                  size="medium"
                  :disabled="device == null || !device.status"
                  icon="el-icon-switch-button"
                  type="text"
                  style="color: #f56c6c"
                  @click="stopDevicePush(scope.row)"
                >stop
                </el-button>
                <el-button
                  size="medium"
                  :disabled="device == null || !device.status"
                  icon="el-icon-camera"
                  type="text"
                  @click="shooting(scope.row)"
                >Snapshot
                </el-button>
                <el-divider direction="vertical" />
                <el-button
                  size="medium"
                  type="text"
                  icon="el-icon-edit"
                  @click="handleEdit(scope.row)"
                >
                  Edit
                </el-button>
                <el-divider direction="vertical" />
                <el-dropdown @command="(command)=>{moreClick(command, scope.row)}">
                  <el-button size="medium" type="text">
                    More features<i class="el-icon-arrow-down el-icon--right" />
                  </el-button>
                  <el-dropdown-menu slot="dropdown">
                    <el-dropdown-item command="records" :disabled="device == null || !device.status">
                      Equipment video</el-dropdown-item>
                    <el-dropdown-item command="cloudRecords" :disabled="device == null || !device.status">
                      Cloud recording</el-dropdown-item>
                  </el-dropdown-menu>
                </el-dropdown>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            style="float: right"
            :current-page="currentPage"
            :page-size="count"
            :page-sizes="[15, 25, 35, 50]"
            layout="total, sizes, prev, pager, next"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="currentChange"
          />
        </el-main>
      </el-container>
    </div>
    <devicePlayer ref="devicePlayer" />
    <channelEdit v-if="jtChannel" ref="channelEdit" :jt-channel="jtChannel" :close-edit="closeEdit" />
  </div>
</template>

<script>
import devicePlayer from '../dialog/jtDevicePlayer.vue'
import channelEdit from './edit.vue'
import dayjs from 'dayjs'

export default {
  name: 'ChannelList',
  components: {
    channelEdit,
    devicePlayer
  },
  props: {
    deviceId: {
      type: String,
      default: null
    }
  },
  data() {
    return {
      device: null,
      deviceChannelList: [],
      updateLooper: 0, // Data refresh rotation training flag
      searchSrt: '',
      channelType: '',
      online: '',
      winHeight: window.innerHeight - 200,
      currentPage: 1,
      count: 15,
      total: 0,
      beforeUrl: '/jtDeviceList',
      isLoging: false,
      loadSnap: {},
      jtChannel: null
    }
  },

  mounted() {
    this.initParam()
    this.initData()
  },
  destroyed() {
    this.$destroy('videojs')
    clearTimeout(this.updateLooper)
  },
  methods: {
    initData: function() {
      this.getDeviceChannelList()
    },
    initParam: function() {
      this.currentPage = 1
      this.count = 15
      this.$store.dispatch('jtDevice/queryDeviceById', this.deviceId)
        .then(data => {
          this.device = data
        })
        .catch(err => {
          console.error(err)
        })
    },
    currentChange: function(val) {
      this.currentPage = val
      this.initData()
    },
    handleSizeChange: function(val) {
      this.count = val
      this.getDeviceChannelList()
    },
    getDeviceChannelList: function() {
      if (typeof (this.deviceId) === 'undefined') return
      this.$store.dispatch('jtDevice/queryChannels', {
        page: this.currentPage,
        count: this.count,
        query: this.searchSrt,
        deviceId: this.deviceId
      })
        .then(data => {
          this.total = data.total
          this.deviceChannelList = data.list
          // Prevent form misalignment
          this.$nextTick(() => {
            this.$refs.channelListTable.doLayout()
          })
        })
    },

    // Notify device to upload media stream
    sendDevicePush: function(itemData) {
      this.isLoging = true
      const channelId = itemData.channelId
      console.log('Notification device push1：' + this.device.phoneNumber + ' : ' + channelId)

      this.$store.dispatch('jtDevice/play', {
        phoneNumber: this.device.phoneNumber,
        channelId: channelId,
        type: 0
      })
        .then(data => {
          setTimeout(() => {
            const snapId = this.device.phoneNumber + '_' + channelId
            this.loadSnap[this.device.phoneNumber + channelId] = 0
            this.getSnapErrorEvent(snapId)
          }, 5000)
          itemData.streamId = data.stream
          this.$refs.devicePlayer.openDialog('media', this.device.phoneNumber, channelId, {
            streamInfo: data,
            hasAudio: itemData.hasAudio
          })
          setTimeout(() => {
            this.initData()
          }, 1000)
        })
        .catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        })
        .finally(() => {
          this.isLoging = false
        })
    },
    moreClick: function(command, itemData) {
      if (command === 'records') {
        this.queryRecords(itemData)
      } else if (command === 'cloudRecords') {
        this.queryCloudRecords(itemData)
      } else {
        this.$message.info('Not supported yet')
      }
    },
    queryRecords: function(itemData) {
      this.$router.push(`/jtDevice/record/${this.device.phoneNumber}/${itemData.channelId}`)
    },
    queryCloudRecords: function(itemData) {
      const deviceId = this.device.phoneNumber
      const channelId = itemData.channelId
      this.$router.push(`/cloudRecord/detail/rtp/jt_${deviceId}_${channelId}`)
    },
    stopDevicePush: function(itemData) {
      this.$store.dispatch('jtDevice/stopPlay', {
        phoneNumber: this.device.phoneNumber,
        channelId: itemData.channelId
      })
        .then((data) => {
          this.initData()
        })
        .catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        })
    },
    getSnap: function(row) {
      const baseUrl = window.baseUrl ? window.baseUrl : ''
      return ((process.env.NODE_ENV === 'development') ? process.env.VUE_APP_BASE_API : baseUrl) + '/api/device/query/snap/' + this.device.phoneNumber + '/' + row.channelId
    },
    getBigSnap: function(row) {
      return [this.getSnap(row)]
    },
    getSnapErrorEvent: function(deviceId, channelId) {
      if (typeof (this.loadSnap[deviceId + channelId]) !== 'undefined') {
        console.log('Download screenshot' + this.loadSnap[deviceId + channelId])
        if (this.loadSnap[deviceId + channelId] > 5) {
          delete this.loadSnap[deviceId + channelId]
          return
        }
        setTimeout(() => {
          const baseUrl = window.baseUrl ? window.baseUrl : ''
          const url = (process.env.NODE_ENV === 'development' ? process.env.VUE_APP_BASE_API : baseUrl) + '/api/device/query/snap/' + deviceId + '/' + channelId
          this.loadSnap[deviceId + channelId]++
          document.getElementById(deviceId + channelId).setAttribute('src', url + '?' + new Date().getTime())
        }, 1000)
      }
    },
    showDevice: function() {
      this.$emit('show-device')
    },
    search: function() {
      this.currentPage = 1
      this.total = 0
      this.initData()
    },
    updateChannel: function(row) {
      this.$store.dispatch('jtDevice/updateChannel', row)
        .catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        })
    },
    refresh: function() {
      this.initData()
    },
    add: function() {
      this.jtChannel = {
        terminalDbId: this.deviceId
      }
    },
    // Edit
    handleEdit(row) {
      this.jtChannel = row
    },
    // Edit
    closeEdit(row) {
      this.jtChannel = null
    },
    // Edit
    shooting(row) {
      this.$message.success('Screenshot has been requested and will be downloaded automatically after completion', { closed: true })
      // File download address
      const baseUrl = window.baseUrl ? window.baseUrl : ''
      const fileUrl = ((process.env.NODE_ENV === 'development') ? process.env.VUE_APP_BASE_API : baseUrl) + `/api/jt1078/snap?phoneNumber=${this.device.phoneNumber}&channelId=${row.channelId}`
      let controller = new AbortController()
      let signal = controller.signal
      // Set request header
      const headers = new Headers()
      headers.append('access-token', this.$store.getters.token) // Set the authorization header and replace YourAccessToken with the actual access token

      let timer = setTimeout(() => {
        this.$message.error('Timeout waiting for screenshot', { closed: true })
        controller.abort('timeout')
      }, 15000)

      // Make a request
      fetch(fileUrl, {
        method: 'GET',
        headers: headers,
        signal: signal
      })
        .then(response => response.blob())
        .then(blob => {
          window.clearTimeout(timer)
          // Create a virtual link element to simulate a click to download
          const link = document.createElement('a')
          link.href = window.URL.createObjectURL(blob)
          link.download = `${this.device.phoneNumber}-${row.channelId}-${dayjs().format('YYYYMMDDHHmmss')}.jpg` // Set the download file name and replace filename.ext with the actual file name and extension.
          document.body.appendChild(link)
          // simulate click
          link.click()
          // Remove virtual link element
          document.body.removeChild(link)
        })
        .catch(error => {
          window.clearTimeout(timer)
          console.error('Download failed：', error)
        })
    }
  }
}
</script>
