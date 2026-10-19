<template>
  <div id="app" style="height: calc(100vh - 124px);">
    <el-form :inline="true" size="mini">
      <el-form-item label="Search">
        <el-input
          v-model="searchStr"
          style="margin-right: 1rem; width: auto;"
          placeholder="Keywords"
          prefix-icon="el-icon-search"
          clearable
          @input="initData"
        />
      </el-form-item>
      <el-form-item label="online status">
        <el-select
          v-model="online"
          style="width: 8rem; margin-right: 1rem;"
          placeholder="Please select"
          default-first-option
          @change="initData"
        >
          <el-option label="All" value="" />
          <el-option label="online" value="true" />
          <el-option label="Offline" value="false" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button icon="el-icon-plus" style="margin-right: 1rem;" type="primary" @click="add">Add device</el-button>
        <el-button icon="el-icon-info" style="margin-right: 1rem;" @click="showInfo()">access information
        </el-button>
      </el-form-item>
      <el-form-item style="float: right;">
        <el-button
          icon="el-icon-refresh-right"
          circle
          :loading="getDeviceListLoading"
          @click="getDeviceList()"
        />
      </el-form-item>
    </el-form>
    <!--Device list-->
    <el-table
      size="small"
      :data="deviceList"
      height="calc(100% - 64px)"
      header-row-class-name="table-header"
    >
      <el-table-column prop="name" label="Name" min-width="160" />
      <el-table-column prop="deviceId" label="Device number" min-width="160" />
      <el-table-column label="address" min-width="180">
        <template v-slot:default="scope">
          <div slot="reference" class="name-wrapper">
            <el-tag v-if="scope.row.hostAddress" size="medium">{{ scope.row.transport.toLowerCase() }}://{{ scope.row.hostAddress }}</el-tag>
            <el-tag v-if="!scope.row.hostAddress" size="medium">unknown</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="manufacturer" label="Manufacturer" min-width="100" />
      <el-table-column label="streaming mode" min-width="160">
        <template v-slot:default="scope">
          <el-select
            v-model="scope.row.streamMode"
            size="mini"
            placeholder="Please select"
            style="width: 120px"
            @change="transportChange(scope.row)"
          >
            <el-option key="UDP" label="UDP" value="UDP" />
            <el-option key="TCP-ACTIVE" label="TCPActive mode" value="TCP-ACTIVE" />
            <el-option key="TCP-PASSIVE" label="TCPpassive mode" value="TCP-PASSIVE" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="Number of channels" min-width="80">
        <template v-slot:default="scope">
          <span style="font-size: 1rem">{{ scope.row.channelCount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="Status" min-width="80">
        <template v-slot:default="scope">
          <div slot="reference" class="name-wrapper">
            <el-tag
              v-if="scope.row.onLine && myServerId !== scope.row.serverId"
              size="medium"
              style="border-color: #ecf1af"
            >online
            </el-tag>
            <el-tag v-if="scope.row.onLine && myServerId === scope.row.serverId" size="medium">online
            </el-tag>
            <el-tag v-if="!scope.row.onLine" size="medium" type="info">Offline</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="Subscribe" min-width="240">
        <template v-slot:default="scope">
          <el-checkbox
            label="Directory"
            :checked="scope.row.subscribeCycleForCatalog > 0"
            @change="(e)=>subscribeForCatalog(scope.row.id, e)"
          />
          <el-checkbox
            label="location"
            :checked="scope.row.subscribeCycleForMobilePosition > 0"
            @change="(e)=>subscribeForMobilePosition(scope.row.id, e)"
          />
          <el-checkbox
            label="Alarm"
            :checked="scope.row.subscribeCycleForAlarm > 0"
            @change="(e)=>subscribeForAlarm(scope.row.id, e)"
          />
        </template>
      </el-table-column>
      <el-table-column label="statistics" min-width="140">
        <template v-slot:default="scope">
          <el-button
            type="text"
            size="mini"
            :disabled="scope.row.online===0"
            icon="iconfont-14 icon-xintiao"
            title="Heartbeat time statistics"
            @click="getKeepaliveTimeStatistics(scope.row.deviceId)"
          >heartbeat
          </el-button>
          <el-button
            type="text"
            size="mini"
            :disabled="scope.row.online===0"
            icon="iconfont-14 icon-register"
            title="Registration time statistics"
            @click="getRegisterTimeStatistics(scope.row.deviceId)"
          >Register
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="Operation" min-width="260" fixed="right">
        <template v-slot:default="scope">
          <el-button
            type="text"
            size="medium"
            :disabled="scope.row.online===0"
            icon="el-icon-refresh"
            @click="refDevice(scope.row)"
            @mouseover="getTooltipContent(scope.row.deviceId)"
          >Refresh
          </el-button>
          <el-divider direction="vertical" />
          <el-button
            type="text"
            size="medium"
            icon="el-icon-video-camera"
            @click="showChannelList(scope.row)"
          >channel
          </el-button>
          <el-divider direction="vertical" />
          <el-button size="medium" icon="el-icon-edit" type="text" @click="edit(scope.row)">Edit</el-button>
          <el-divider direction="vertical" />
          <el-button size="medium" type="text" style="color: #f56c6c" @click="deleteDevice(scope.row)">Delete</el-button>
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
    <deviceEdit ref="deviceEdit" />
    <syncChannelProgress ref="syncChannelProgress" />
    <configInfo ref="configInfo" />
    <timeStatistics ref="timeStatistics" />
  </div>
</template>

<script>
import deviceEdit from './edit.vue'
import syncChannelProgress from './dialog/SyncChannelProgress.vue'
import configInfo from '../dialog/configInfo.vue'
import timeStatistics from './dialog/timeStatistics.vue'
import Vue from 'vue'

export default {
  name: 'App',
  components: {
    configInfo,
    deviceEdit,
    syncChannelProgress,
    timeStatistics
  },
  data() {
    return {
      deviceList: [], // Device list
      currentDevice: {}, // Current operating device object
      searchStr: '',
      online: null,
      videoComponentList: [],
      updateLooper: 0, // Data refresh rotation training flag
      currentDeviceChannelsLength: 0,
      currentPage: 1,
      count: 15,
      total: 0,
      getDeviceListLoading: false
    }
  },
  computed: {
    Vue() {
      return Vue
    },
    myServerId() {
      return this.$store.getters.serverId
    }
  },
  mounted() {
    this.initData()
    this.updateLooper = setInterval(this.getDeviceList, 10000)
  },
  destroyed() {
    this.$destroy('videojs')
    clearTimeout(this.updateLooper)
  },
  methods: {
    initData: function() {
      this.currentPage = 1
      this.total = 0
      this.getDeviceList()
    },
    currentChange: function(val) {
      this.currentPage = val
      this.getDeviceList()
    },
    handleSizeChange: function(val) {
      this.count = val
      this.getDeviceList()
    },
    getDeviceList: function() {
      this.getDeviceListLoading = true
      this.$store.dispatch('device/queryDevices', {
        page: this.currentPage,
        count: this.count,
        query: this.searchStr,
        status: this.online
      }).then((data) => {
        this.total = data.total
        this.deviceList = data.list
      }).catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        }).finally(() => {
        this.getDeviceListLoading = false
      })
    },
    deleteDevice: function(row) {
      let msg = 'Confirm to delete this device？'
      if (row.online !== 0) {
        msg = 'After the online device is deleted, it can still be brought online again through registration.。<br/>If you want to delete it completely, please take the device offline first.。<br/><strong>Confirm to delete this device？</strong>'
      }
      this.$confirm(msg, 'Tips', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        center: true,
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('device/deleteDevice', row.deviceId)
          .then((data) => {
            this.getDeviceList()
          })
          .catch((error) => {
            this.$message({
              showClose: true,
              message: error,
              type: 'error'
            })
          })
      })
    },
    showChannelList: function(row) {
      this.$emit('show-channel', row.deviceId)
      // this.$router.push(`/device/?deviceId=${row.deviceId}`)
    },
    showDevicePosition: function(row) {
      this.$router.push(`/map?deviceId=${row.deviceId}`)
    },

    // gb28181Platform docking
    // Refresh device information
    refDevice: function(itemData) {
      console.log('Refresh the corresponding device:' + itemData.deviceId)
      this.$store.dispatch('device/sync', itemData.deviceId)
        .then(data => {
          if (data && data.errorMsg) {
            this.$message({
              showClose: true,
              message: data.errorMsg,
              type: 'error'
            })
            return
          }

          this.$refs.syncChannelProgress.openDialog(itemData.deviceId, () => {
            this.getDeviceList()
          })
        })
        .catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        })
        .finally(() => {
          this.getDeviceList()
        })
    },

    getTooltipContent: async function(deviceId) {
      let result = ''
      await this.$store.dispatch('device/queryDeviceSyncStatus', deviceId)
        .then((data) => {
          if (data.errorMsg !== null) {
            result = data.errorMsg
          }
          result = `Synchronizing...[${data.current}/${data.total}]`
        }).catch(error => {
          result = error
        })
      return result
    },
    transportChange: function(row) {
      console.log(`Modify the transmission method to ${row.streamMode}：${row.deviceId} `)
      console.log(row.streamMode)
      this.$store.dispatch('device/updateDeviceTransport', [row.deviceId, row.streamMode])
    },
    edit: function(row) {
      this.$refs.deviceEdit.openDialog(row, () => {
        this.$refs.deviceEdit.close()
        this.$message({
          showClose: true,
          message: 'The device modification is successful and the channel character set will take effect in the next update.',
          type: 'success'
        })
        setTimeout(this.getDeviceList, 200)
      })
    },
    add: function() {
      this.$refs.deviceEdit.openDialog(null, () => {
        this.$refs.deviceEdit.close()
        this.$message({
          showClose: true,
          message: 'Added successfully',
          type: 'success'
        })
        setTimeout(this.getDeviceList, 200)
      })
    },
    showInfo: function() {
      this.$store.dispatch('server/getSystemConfig')
        .then((data) => {
          this.serverId = data.addOn.serverId
          this.$refs.configInfo.openDialog(data)
        })
    },

    subscribeForCatalog: function(data, value) {
      this.$store.dispatch('device/subscribeCatalog', {
        id: data,
        cycle: value ? 60 : 0
      }).then((data) => {
        this.$message.success({
          showClose: true,
          message: value ? 'Subscription successful' : 'Unsubscription successful'
        })
      }).catch((error) => {
        this.$message.error({
          showClose: true,
          message: error.message
        })
      })
    },
    subscribeForMobilePosition: function(data, value) {
      this.$store.dispatch('device/subscribeMobilePosition', {
        id: data,
        cycle: value ? 60 : 0,
        interval: value ? 5 : 0
      }).then((data) => {
        this.$message.success({
          showClose: true,
          message: value ? 'Subscription successful' : 'Unsubscription successful'
        })
      }).catch((error) => {
        this.$message.error({
          showClose: true,
          message: error.message
        })
      })
    },
    subscribeForAlarm: function(data, value) {
      this.$store.dispatch('device/subscribeForAlarm', {
        id: data,
        cycle: value ? 60 : 0
      }).then((data) => {
        this.$message.success({
          showClose: true,
          message: value ? 'Subscription successful' : 'Unsubscription successful'
        })
      }).catch((error) => {
        this.$message.error({
          showClose: true,
          message: error.message
        })
      })
    },
    getKeepaliveTimeStatistics: function(deviceId) {
      this.$refs.timeStatistics.openDialog('Heartbeat time statistics', 'device/getKeepaliveTimeStatistics', deviceId, 60)
    },
    getRegisterTimeStatistics: function(deviceId) {
      this.$refs.timeStatistics.openDialog('Registration time statistics', 'device/getRegisterTimeStatistics', deviceId, 10)
    }
  }
}
</script>
