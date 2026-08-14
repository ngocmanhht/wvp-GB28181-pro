<template>
  <div id="channelList" style="height: calc(100vh - 124px);">
    <div v-if="!editId && !ptzConfigChannelDeviceId && !cameraConfigDeviceId" style="height: 100%">
      <el-form :inline="true" size="mini">
        <el-form-item style="margin-right: 2rem">
          <el-page-header content="Channel list" @back="showDevice" />
        </el-form-item>
        <el-form-item label="Search">
          <el-input
            v-model="searchStr"
            style="margin-right: 1rem; width: auto;"
            placeholder="Keywords"
            prefix-icon="el-icon-search"
            clearable
            @input="search"
          />
        </el-form-item>
        <el-form-item label="Channel type">
          <el-select
            v-model="channelType"
            style="width: 8rem; margin-right: 1rem;"
            placeholder="Please select"
            default-first-option
            @change="search"
          >
            <el-option label="All" value="" />
            <el-option label="Equipment" value="false" />
            <el-option label="subdirectory" value="true" />
          </el-select>
        </el-form-item>
        <el-form-item label="online status">
          <el-select
            v-model="online"
            style="width: 8rem; margin-right: 1rem;"
            placeholder="Please select"
            default-first-option
            @change="search"
          >
            <el-option label="All" value="" />
            <el-option label="online" value="true" />
            <el-option label="Offline" value="false" />
          </el-select>
        </el-form-item>
        <el-form-item label="Stream type reset">
          <el-select
            v-model="subStream"
            style="width: 16rem; margin-right: 1rem;"
            placeholder="Please select stream type"
            default-first-option
            @change="subStreamChange"
          >
            <el-option label="stream:0(main stream)" value="stream:0" />
            <el-option label="stream:1(substream)" value="stream:1" />
            <el-option label="streamnumber:0(main stream-2022)" value="streamnumber:0" />
            <el-option label="streamnumber:1(substream-2022)" value="streamnumber:1" />
            <el-option label="streamprofile:0(main stream-Dahua)" value="streamprofile:0" />
            <el-option label="streamprofile:1(substream-Dahua)" value="streamprofile:1" />
            <el-option label="streamMode:MAIN(main stream-Mercury+TP-LINK)" value="streamMode:MAIN" />
            <el-option label="streamMode:SUB(substream-Mercury+TP-LINK)" value="streamMode:SUB" />
          </el-select>
        </el-form-item>
        <el-form-item style="float: right;">
          <el-button icon="el-icon-refresh-right" circle @click="refresh()" />
        </el-form-item>
      </el-form>
      <el-table
        ref="channelListTable"
        size="small"
        :data="deviceChannelList"
        height="calc(100% - 64px)"
        style="width: 100%; font-size: 12px;"
        header-row-class-name="table-header"
      >
        <el-table-column prop="name" label="Name" min-width="180" />
        <el-table-column prop="deviceId" label="No." min-width="180" />
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
        <!--          <el-table-column prop="subCount" label="Number of child nodes" min-width="100">-->
        <!--          </el-table-column>-->
        <el-table-column prop="manufacturer" label="Manufacturer" min-width="100" />
        <el-table-column label="location information" min-width="150">
          <template v-slot:default="scope">
            <span v-if="scope.row.longitude && scope.row.latitude">{{ scope.row.longitude }}<br>{{ scope.row.latitude }}</span>
            <span v-if="!scope.row.longitude || !scope.row.latitude">None</span>
          </template>
        </el-table-column>
        <el-table-column prop="ptzType" label="Camera type" min-width="100">
          <template v-slot:default="scope">
            <div>{{ scope.row.ptzTypeText }}</div>
          </template>
        </el-table-column>
        <el-table-column label="Turn on audio" min-width="100">
          <template v-slot:default="scope">
            <el-switch v-model="scope.row.hasAudio" active-color="#409EFF" @change="updateChannel(scope.row)" />
          </template>
        </el-table-column>
        <el-table-column label="Stream type" min-width="180">
          <template v-slot:default="scope">
            <el-select
              v-model="scope.row.streamIdentification"
              size="mini"
              style="margin-right: 1rem;"
              placeholder="Please select stream type"
              default-first-option
              @change="channelSubStreamChange(scope.row)"
            >
              <el-option label="stream:0(main stream)" value="stream:0" />
              <el-option label="stream:1(substream)" value="stream:1" />
              <el-option label="streamnumber:0(main stream-2022)" value="streamnumber:0" />
              <el-option label="streamnumber:1(substream-2022)" value="streamnumber:1" />
              <el-option label="streamprofile:0(main stream-Dahua)" value="streamprofile:0" />
              <el-option label="streamprofile:1(substream-Dahua)" value="streamprofile:1" />
              <el-option label="streamMode:MAIN(main stream-Mercury+TP-LINK)" value="streamMode:MAIN" />
              <el-option label="streamMode:SUB(substream-Mercury+TP-LINK)" value="streamMode:SUB" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="Status" min-width="100">
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag v-if="scope.row.status === 'ON'" size="medium">online</el-tag>
              <el-tag v-if="scope.row.status !== 'ON'" size="medium" type="info">Offline</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Operation" min-width="340" fixed="right">
          <template v-slot:default="scope">
            <el-button
              size="medium"
              :disabled="device == null || device.online === 0"
              icon="el-icon-video-play"
              type="text"
              :loading="scope.row.playLoading"
              @click="sendDevicePush(scope.row)"
            >play
            </el-button>
            <el-button
              v-if="!!scope.row.streamId"
              size="medium"
              :disabled="device == null || device.online === 0"
              icon="el-icon-switch-button"
              type="text"
              style="color: #f56c6c"
              @click="stopDevicePush(scope.row)"
            >stop
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
            <el-button
              v-if="scope.row.subCount > 0 || scope.row.parental === 1 || scope.row.deviceId.length <= 8"
              size="medium"
              icon="el-icon-s-open"
              type="text"
              @click="changeSubchannel(scope.row)"
            >View
            </el-button>
            <el-divider v-if="scope.row.subCount > 0 || scope.row.parental === 1 || scope.row.deviceId.length <= 8" direction="vertical" />
            <el-dropdown @command="(command)=>{moreClick(command, scope.row)}">
              <el-button size="medium" type="text">
                More<i class="el-icon-arrow-down el-icon--right" />
              </el-button>
              <el-dropdown-menu>
                <el-dropdown-item command="audioTalk" :disabled="device == null || device.online === 0">
                  Voice intercom</el-dropdown-item>
                <el-dropdown-item command="records" :disabled="device == null || device.online === 0">
                  Equipment video</el-dropdown-item>
                <el-dropdown-item command="ptzConfig" :disabled="device == null || device.online === 0">
                  PTZ configuration</el-dropdown-item>
                <el-dropdown-item command="cameraConfig" :disabled="device == null || device.online === 0">
                  Camera configuration</el-dropdown-item>
                <el-dropdown-item command="cloudRecords" :disabled="device == null || device.online === 0">
                  Cloud recording</el-dropdown-item>
              </el-dropdown-menu>

            </el-dropdown>
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

    <devicePlayer ref="devicePlayer" />
    <channel-edit v-if="editId" :id="editId" :close-edit="closeEdit" />
    <ptzConfig v-if="ptzConfigChannelDeviceId" :device-id="ptzConfigDeviceId" :channel-device-id="ptzConfigChannelDeviceId" @close="closePtzConfig" />
    <cameraConfig v-if="cameraConfigDeviceId" :device-id="deviceId" :channel-device-id="cameraConfigDeviceId" @close="cameraConfigDeviceId = null" />
    <audioTalk ref="audioTalk" />

  </div>
</template>

<script>
import devicePlayer from '../dialog/devicePlayer.vue'
import audioTalk from '../dialog/audioTalk.vue'
import Edit from './edit.vue'
import ptzConfig from '@/views/device/channel/ptzConfig.vue'
import cameraConfig from './cameraConfig.vue'

export default {
  name: 'ChannelList',
  components: {
    devicePlayer,
    audioTalk,
    ChannelEdit: Edit,
    ptzConfig,
    cameraConfig
  },
  props: {
    defaultPage: {
      type: Number,
      default: 1
    },
    defaultCount: {
      type: Number,
      default: 15
    },
    deviceId: {
      type: String,
      default: null
    },
    parentChannelId: {
      type: String || null,
      default: null
    }
  },
  data() {
    return {
      device: null,
      deviceChannelList: [],
      videoComponentList: [],
      currentPlayerInfo: {}, // Current playback object
      updateLooper: 0, // Data refresh rotation training flag
      searchStr: '',
      channelType: '',
      online: '',
      subStream: '',
      winHeight: window.innerHeight - 200,
      currentPage: this.defaultPage | 1,
      count: this.defaultCount | 15,
      total: 0,
      beforeUrl: '/device',
      editId: null,
      ptzConfigDeviceId: null,
      ptzConfigChannelDeviceId: null,
      cameraConfigDeviceId: null,
      loadSnap: {},
      ptzTypes: {
        0: 'unknown',
        1: 'ball machine',
        2: 'hemisphere',
        3: 'Fixed bolt',
        4: 'remote control gun'
      }
    }
  },
  watch: {
    deviceId: function(val) {
      this.$store.dispatch('device/queryDeviceOne', this.deviceId)
        .then(data => {
          this.device = data
        })
      this.initData()
    }
  },
  mounted() {
    if (this.deviceId) {
      this.$store.dispatch('device/queryDeviceOne', this.deviceId)
        .then(data => {
          this.device = data
        })
    }
    this.initData()
  },
  destroyed() {
    this.$destroy('videojs')
    clearTimeout(this.updateLooper)
  },
  methods: {
    initData: function() {
      if (this.parentChannelId === null || typeof (this.parentChannelId) === 'undefined' || this.parentChannelId === 0) {
        this.getDeviceChannelList()
      } else {
        this.showSubChannels()
      }
    },
    initParam: function() {
      this.deviceId = this.$route.params.deviceId
      this.parentChannelId = this.$route.params.parentChannelId
      this.currentPage = 1
      this.count = 15
      if (this.parentChannelId === '' || this.parentChannelId === 0) {
        this.beforeUrl = '/device/list'
      }
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
      console.log(this.deviceId)
      if (typeof (this.deviceId) === 'undefined') return
      this.$store.dispatch('device/queryChannels', [this.deviceId, {
        page: this.currentPage,
        count: this.count,
        query: this.searchStr,
        online: this.online,
        channelType: this.channelType
      }]).then(data => {
        this.total = data.total
        this.deviceChannelList = data.list
        this.deviceChannelList.forEach(e => {
          e.ptzType = e.ptzType + ''
          this.$set(e, 'playLoading', false)
        })
        // Prevent form misalignment
        this.$nextTick(() => {
          this.$refs.channelListTable.doLayout()
        })
      })
    },

    // Notify device to upload media stream
    sendDevicePush: function(itemData) {
      const deviceId = this.deviceId
      const channelId = itemData.deviceId
      itemData.playLoading = true
      console.log('Notification device push1：' + deviceId + ' : ' + channelId)
      this.$store.dispatch('play/play', [deviceId, channelId])
        .then((data) => {
          setTimeout(() => {
            const snapId = deviceId + '_' + channelId
            this.loadSnap[deviceId + channelId] = 0
            this.getSnapErrorEvent(snapId)
          }, 5000)
          itemData.streamId = data.stream
          this.$refs.devicePlayer.openDialog('media', deviceId, channelId, {
            streamInfo: data,
            hasAudio: itemData.hasAudio
          })
          setTimeout(() => {
            this.initData()
          }, 1000)
        })
        .catch((error) => {
          console.log(error)
          this.$message.error({
            showClose: true,
            message: error
          })
        })
        .finally(() => {
          itemData.playLoading = false
        })
    },
    closePtzConfig: function() {
      this.ptzConfigDeviceId = null
      this.ptzConfigChannelDeviceId = null
    },
    moreClick: function(command, itemData) {
      if (command === 'records') {
        this.queryRecords(itemData)
      } else if (command === 'cloudRecords') {
        this.queryCloudRecords(itemData)
      } else if (command === 'ptzConfig') {
        console.log(itemData.channelId)
        this.ptzConfigDeviceId = this.deviceId
        this.ptzConfigChannelDeviceId = itemData.deviceId
      } else if (command === 'audioTalk') {
        this.$refs.audioTalk.openDialog(this.deviceId, itemData.deviceId)
      } else if (command === 'cameraConfig') {
        this.cameraConfigDeviceId = itemData.deviceId
      }
    },
    queryRecords: function(itemData) {
      const deviceId = this.deviceId
      const channelId = itemData.deviceId

      this.$router.push(`/device/record/${deviceId}/${channelId}`)
    },
    queryCloudRecords: function(itemData) {
      const deviceId = this.deviceId
      const channelId = itemData.deviceId

      this.$router.push(`/cloudRecord/detail/rtp/${deviceId}_${channelId}`)
    },
    stopDevicePush: function(itemData) {
      this.$store.dispatch('play/stop', {
        deviceId: this.deviceId,
        channelId: itemData.deviceId
      }).then(data => {
        this.initData()
      }).catch((error) => {
        if (error.response.status === 402) { // has stopped
          this.initData()
        } else {
          console.log(error)
        }
      })
    },
    getSnap: function(row) {
      const baseUrl = window.baseUrl ? window.baseUrl : ''
      return ((process.env.NODE_ENV === 'development') ? process.env.VUE_APP_BASE_API : baseUrl) + '/api/device/query/snap/' + this.deviceId + '/' + row.deviceId
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
          const url = (process.env.NODE_ENV === 'development' ? 'debug' : '') + '/api/device/query/snap/' + deviceId + '/' + channelId
          this.loadSnap[deviceId + channelId]++
          document.getElementById(deviceId + channelId).setAttribute('src', url + '?' + new Date().getTime())
        }, 1000)
      }
    },
    showDevice: function() {
      // this.$router.push(this.beforeUrl).then(() => {
      //   this.initParam()
      //   this.initData()
      // })
      this.$emit('show-device')
    },
    changeSubchannel(itemData) {
      this.beforeUrl = this.$router.currentRoute.path

      var url = `/${this.$router.currentRoute.name}/${this.$router.currentRoute.params.deviceId}/${itemData.deviceId}`
      this.$router.push(url).then(() => {
        this.searchStr = ''
        this.channelType = ''
        this.online = ''
        this.initParam()
        this.initData()
      })
    },
    showSubChannels: function() {
      this.$store.dispatch('device/querySubChannels', [
        {
          page: this.currentPage,
          count: this.count,
          query: this.searchStr,
          online: this.online,
          channelType: this.channelType
        },
        this.deviceId,
        this.parentChannelId
      ])
        .then(data => {
          this.total = data.total
          this.deviceChannelList = data.list
          this.deviceChannelList.forEach(e => {
            e.ptzType = e.ptzType + ''
          })
          // Prevent form misalignment
          this.$nextTick(() => {
            this.$refs.channelListTable.doLayout()
          })
        })
    },
    search: function() {
      this.currentPage = 1
      this.total = 0
      this.initData()
    },
    updateChannel: function(row) {
      this.$store.dispatch('device/changeChannelAudio', {
        channelId: row.id,
        audio: row.hasAudio
      })
    },
    subStreamChange: function() {
      this.$confirm('Determine to reset the code stream type of all channels?', 'Tips', {
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('device/updateChannelStreamIdentification', {
          deviceDbId: this.device.id,
          streamIdentification: this.subStream
        })
          .then(data => {
            this.initData()
          })
          .finally(() => {
            this.subStream = ''
          })
      }).catch(() => {
        this.subStream = ''
      })
    },
    channelSubStreamChange: function(row) {
      this.$store.dispatch('device/updateChannelStreamIdentification', {
        deviceDbId: row.deviceDbId,
        id: row.id,
        streamIdentification: row.streamIdentification
      })
        .then(data => {
          this.initData()
        })
        .finally(() => {
          this.subStream = ''
        })
    },
    refresh: function() {
      this.initData()
    },
    // Edit
    handleEdit(row) {
      this.editId = row.id
    },
    // End editing
    closeEdit: function() {
      this.editId = null
      this.getDeviceChannelList()
    }

  }
}
</script>
