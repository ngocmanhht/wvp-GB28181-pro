<template>
  <div id="channelList" class="app-container" style="height: calc(100vh - 124px);">
    <div v-if="!editId && !showPtzConfig" style="height: 100%">
      <el-form :inline="true" size="mini">
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
        <el-form-item label="Type">
          <el-select
            v-model="channelType"
            style="width: 8rem; margin-right: 1rem;"
            placeholder="Please select"
            default-first-option
            @change="getChannelList"
          >
            <el-option label="All" value="" />
            <el-option v-for="item in Object.values($channelTypeList)" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item >
          <el-input placeholder="Please select an administrative division" v-model="civilCodeName" readonly style="width: 12rem; margin-right: 1rem;">
            <span slot="suffix" v-show="civilCodeName" style="height: 100%; display: flex; align-items: center; width: 22px;"
                  @click="civilCodeClear">
               <i class="el-icon-circle-close" style="margin-left: 5px;cursor: pointer;"></i>
            </span>
            <el-button slot="append" @click="civilCodeFilter">Choose</el-button>
          </el-input>
        </el-form-item>
        <el-form-item >
          <el-input placeholder="Please select a business group" v-model="groupName" readonly style="width: 12rem; margin-right: 1rem;">
            <span slot="suffix" v-show="groupName" style="height: 100%; display: flex; align-items: center; width: 22px;"
                  @click="groupClear">
               <i class="el-icon-circle-close" style="margin-left: 5px;cursor: pointer;"></i>
            </span>
            <el-button slot="append" @click="groupFilter">Choose</el-button>
          </el-input>
        </el-form-item>
        <el-form-item >
          <el-dropdown >
            <el-button type="primary">
              Batch operations<i class="el-icon-arrow-down el-icon--right"></i>
            </el-button>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item @click.native="batchChangeRegion">Administrative division</el-dropdown-item>
              <el-dropdown-item @click.native="batchChangeGroup">business grouping</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </el-form-item>
        <el-form-item style="float: right;">
          <el-button icon="el-icon-refresh-right" circle @click="refresh()" title="Refresh table"/>
        </el-form-item>
      </el-form>
      <el-table
        ref="channelListTable"
        size="small"
        :data="channelList"
        height="calc(100% - 64px)"
        style="width: 100%; font-size: 12px;"
        header-row-class-name="table-header"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="gbName" label="Name" min-width="180" />
        <el-table-column prop="gbDeviceId" label="No." min-width="180" />
        <el-table-column prop="gbManufacturer" label="Manufacturer" min-width="100" />
        <el-table-column label="Type" min-width="100">
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag size="medium" effect="plain" type="success" :style="$channelTypeList[scope.row.dataType].style">{{ $channelTypeList[scope.row.dataType].name }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="location information" min-width="150">
          <template v-slot:default="scope">
            <span v-if="scope.row.gbLongitude && scope.row.gbLatitude">{{ scope.row.gbLongitude }}<br>{{ scope.row.gbLatitude }}</span>
            <span v-if="!scope.row.gbLongitude || !scope.row.gbLatitude">None</span>
          </template>
        </el-table-column>
        <el-table-column prop="ptzType" label="Camera type" min-width="100">
          <template v-slot:default="scope">
            <div>{{ scope.row.ptzTypeText }}</div>
          </template>
        </el-table-column>
        <el-table-column label="Status" min-width="100">
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag v-if="scope.row.gbStatus === 'ON'" size="medium">online</el-tag>
              <el-tag v-if="scope.row.gbStatus !== 'ON'" size="medium" type="info">Offline</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Operation" min-width="210" fixed="right">
          <template v-slot:default="scope">
            <el-button
              size="medium"
              :disabled="scope.row.gbStatus !== 'ON'"
              icon="el-icon-video-play"
              type="text"
              :loading="scope.row.playLoading"
              @click="sendDevicePush(scope.row)"
            >play
            </el-button>
            <el-button
              v-if="!!scope.row.streamId"
              size="medium"
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
              v-if="$store.getters.authority !== 2"
              @click="handleEdit(scope.row)"
            >
              Edit
            </el-button>
            <el-divider direction="vertical" />
            <el-dropdown @command="(command)=>{moreClick(command, scope.row)}">
              <el-button size="medium" type="text">
                More<i class="el-icon-arrow-down el-icon--right" />
              </el-button>
              <el-dropdown-menu>
                <el-dropdown-item command="records" :disabled="scope.row.gbStatus !== 'ON'">
                  Equipment video</el-dropdown-item>
                <el-dropdown-item command="cloudRecords" :disabled="scope.row.gbStatus !== 'ON'">
                  Cloud recording</el-dropdown-item>
                <el-dropdown-item command="ptzConfig" :disabled="scope.row.gbStatus !== 'ON'">
                  PTZ configuration</el-dropdown-item>
                <el-dropdown-item command="audioTalk" :disabled="scope.row.gbStatus !== 'ON'">
                  Voice intercom</el-dropdown-item>
              </el-dropdown-menu>

            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="text-align: right"
        :current-page="currentPage"
        :page-size="count"
        :page-sizes="[15, 25, 35, 50, 100, 500, 1000]"
        layout="total, sizes, prev, pager, next"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="currentChange"
      />
    </div>

    <devicePlayer ref="devicePlayer" />
    <audioTalk ref="audioTalk" />
    <ptzConfig v-if="showPtzConfig" :channel-id="ptzConfigChannelId" @close="showPtzConfig = false" />
    <channel-edit v-if="editId" :id="editId" :close-edit="closeEdit" />
    <chooseCivilCode ref="chooseCivilCode" />
    <chooseGroup ref="chooseGroup" />

  </div>
</template>

<script>
import devicePlayer from './player.vue'
import audioTalk from './audioTalk.vue'
import ptzConfig from './ptzConfig.vue'
import Edit from './edit.vue'
import ChooseCivilCode from '../dialog/chooseCivilCode.vue'
import ChooseGroup from '@/views/dialog/chooseGroup.vue'

export default {
  name: 'ChannelList',
  components: {
    ChooseGroup,
    devicePlayer,
    audioTalk,
    ptzConfig,
    ChooseCivilCode,
    ChannelEdit: Edit
  },
  props: {
    defaultPage: {
      type: Number,
      default: 1
    },
    defaultCount: {
      type: Number,
      default: 15
    }
  },
  computed: {
    excelName(){
      return 'Channel list-' + this.currentPage
    }
  },
  data() {
    return {
      device: null,
      channelList: [],
      excelFields: {
        Name: 'gbName',
        No.: 'gbDeviceId',
        Manufacturer: 'gbManufacturer',
        Type: {
          field: 'dataType',
          callback: (value) => {
            return this.$channelTypeList[value].name
          }
        },
        longitude: 'gbLongitude',
        Latitude: 'gbLatitude',
        Camera type: 'ptzTypeText',
        Status: {
          field: 'gbStatus',
          callback: (value) => {
            return value === 'ON' ? 'online' : 'Offline'
          }
        }
      },
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
      showPtzConfig: false,
      ptzConfigChannelId: null,
      civilCodeName: null,
      civilCodeDeviceId: null,

      groupName: null,
      groupDeviceId: null,
      groupBusiness: null,

      multipleSelection: []
    }
  },
  mounted() {
    this.initData()
  },
  destroyed() {
    this.$destroy('videojs')
    clearTimeout(this.updateLooper)
  },
  methods: {
    handleSelectionChange: function(val) {
      this.multipleSelection = val
    },
    initData: function() {
      this.getChannelList()
    },
    initParam: function() {
      this.currentPage = 1
      this.count = 15
    },
    currentChange: function(val) {
      this.currentPage = val
      this.initData()
    },
    handleSizeChange: function(val) {
      this.count = val
      this.getChannelList()
    },
    getChannelList: function() {
      this.channelList = []
      this.$store.dispatch('commonChanel/getList', {
        page: this.currentPage,
        count: this.count,
        query: this.searchStr,
        online: this.online,
        channelType: this.channelType,
        civilCode: this.civilCodeDeviceId,
        parentDeviceId: this.groupDeviceId
      }).then(data => {
        this.total = data.total
        this.channelList = data.list
        this.channelList.forEach(e => {
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
      itemData.playLoading = true
      this.$store.dispatch('commonChanel/playChannel', itemData.gbId)
        .then((data) => {
          itemData.streamId = data.stream
          this.$refs.devicePlayer.openDialog('media', itemData.gbId, {
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
          itemData.playLoading = false
        })
    },
    queryRecords: function(itemData) {
      const channelId = itemData.gbId
      this.$router.push(`/channel/record/${channelId}`)
    },
    queryCloudRecords: function(itemData) {
      const deviceId = this.deviceId
      const channelId = itemData.deviceId

      this.$router.push(`/cloudRecord/detail/rtp/${deviceId}_${channelId}`)
    },
    stopDevicePush: function(itemData) {
      this.$store.dispatch('commonChanel/stopPlayChannel', itemData.gbId).then(data => {
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
    search: function() {
      this.currentPage = 1
      this.total = 0
      this.initData()
    },
    refresh: function() {
      this.initData()
    },
    // Edit
    handleEdit(row) {
      console.log(row)
      this.editId = row.gbId
    },
    // End editing
    closeEdit: function() {
      this.editId = null
      this.getChannelList()
    },
    moreClick: function(command, itemData) {
      if (command === 'records') {
        this.queryRecords(itemData)
      } else if (command === 'cloudRecords') {
        this.queryCloudRecords(itemData)
      } else if (command === 'ptzConfig') {
        this.ptzConfigChannelId = itemData.gbId
        this.showPtzConfig = true
      } else if (command === 'audioTalk') {
        this.$refs.audioTalk.openDialog(itemData.gbId)
      }
    },
    getCheckIds: function() {
      const channelIds = []
      for (let i = 0; i < this.multipleSelection.length; i++) {
        channelIds.push(this.multipleSelection[i].gbId)
      }
      if (channelIds.length === 0) {
        this.$message.warning({
          showClose: true,
          message: 'Please select channel'
        })
        return []
      }
      return channelIds
    },
    batchChangeRegion: function() {
      let ids = this.getCheckIds()
      if (ids.length === 0) {
        return
      }
      this.$refs.chooseCivilCode.openDialog((code, name) => {
        this.$confirm(`Confirm to add${ids.length}channel to${name}?`, 'Batch operations', {
          confirmButtonText: 'Confirm',
          cancelButtonText: 'Cancel',
          type: 'warning'
        }).then(() => {
          this.$store.dispatch('commonChanel/addToRegion', {
            civilCode: code,
            channelIds: ids
          })
            .then(data => {
              this.$message.success({
                showClose: true,
                message: 'Saved successfully'
              })
            })
            .catch((error) => {
              this.$message.error({
                showClose: true,
                message: error
              })
            })
            .finally(() => {
              this.loading = false
            })
        })
      })
    },
    batchChangeGroup: function() {
      let ids = this.getCheckIds()
      if (ids.length === 0) {
        return
      }
      this.$refs.chooseGroup.openDialog((code, businessGroupId, name) => {
        this.$confirm(`Confirm to add${ids.length}channel to${name}?`, 'Batch operations', {
          confirmButtonText: 'Confirm',
          cancelButtonText: 'Cancel',
          type: 'warning'
        }).then(() => {
          this.$store.dispatch('commonChanel/addToGroup', {
            parentId: code,
            businessGroup: businessGroupId,
            channelIds: ids
          })
            .then(data => {
              this.$message.success({
                showClose: true,
                message: 'Saved successfully'
              })
              this.getChannelList()
            })
            .catch((error) => {
              this.$message.error({
                showClose: true,
                message: error
              })
            })
            .finally(() => {
              this.loading = false
            })
        })
      })
    },
    civilCodeFilter() {
      this.$refs.chooseCivilCode.openDialog((code, name) => {
        this.civilCodeName = name
        this.civilCodeDeviceId = code
        this.getChannelList()
      })
    },
    groupFilter() {
      this.$refs.chooseGroup.openDialog((code, businessGroupId, name) => {
        this.groupDeviceId = code
        this.groupBusiness = businessGroupId
        this.groupName = name
        this.getChannelList()
      })
    },
    civilCodeClear(){
      this.civilCodeDeviceId = null
      this.civilCodeName = null
      this.getChannelList()
    },
    groupClear(){
      this.groupName = null
      this.groupDeviceId = null
      this.groupBusiness = null
      this.getChannelList()
    }
  }
}
</script>
