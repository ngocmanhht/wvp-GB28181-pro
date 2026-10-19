<template>
  <div id="app" class="app-container">
    <div v-if="!platform" style="height: calc(100vh - 124px);">
      <el-form :inline="true" size="mini">
        <el-form-item label="Search">
          <el-input
            v-model="searchStr"
            style="margin-right: 1rem; width: auto;"
            size="mini"
            placeholder="Keywords"
            prefix-icon="el-icon-search"
            clearable
            @input="queryList"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            icon="el-icon-plus"
            size="mini"
            style="margin-right: 1rem;"
            type="primary"
            @click="addParentPlatform"
          >add
          </el-button>
        </el-form-item>
        <el-form-item style="float: right;">
          <el-button icon="el-icon-refresh-right" circle @click="refresh()" />
        </el-form-item>
      </el-form>
      <!--Device list-->
      <el-table
        size="small"
        :data="platformList"
        style="width: 100%"
        height="calc(100% - 64px)"
        :loading="loading"
      >
        <el-table-column prop="name" label="Name" />
        <el-table-column prop="serverGBId" label="Platform number" min-width="200" />
        <el-table-column label="Whether to enable" min-width="80">
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag v-if="scope.row.enable && myServerId !== scope.row.serverId" size="medium" style="border-color: #ecf1af">Enabled</el-tag>
              <el-tag v-if="scope.row.enable && myServerId === scope.row.serverId" size="medium">Enabled</el-tag>
              <el-tag v-if="!scope.row.enable" size="medium" type="info">Not enabled</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Status" min-width="80">
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag v-if="scope.row.status" size="medium">online</el-tag>
              <el-tag v-if="!scope.row.status" size="medium" type="info">Offline</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="address" min-width="160">
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag size="medium">{{ scope.row.serverIp }}:{{ scope.row.serverPort }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="deviceGBId" label="Equipment national standard number" min-width="200" />
        <el-table-column prop="transport" label="Signaling transmission mode" min-width="120" />
        <el-table-column prop="channelCount" label="Number of channels" min-width="120" />
        <el-table-column label="Subscription information" min-width="120" fixed="right">
          <template v-slot:default="scope">
            <i
              v-if="scope.row.alarmSubscribe"
              style="font-size: 20px"
              title="Alarm subscription"
              class="iconfont icon-gbaojings subscribe-on "
            />
            <i
              v-if="!scope.row.alarmSubscribe"
              style="font-size: 20px"
              title="Alarm subscription"
              class="iconfont icon-gbaojings subscribe-off "
            />
            <i v-if="scope.row.catalogSubscribe" title="directory subscription" class="iconfont icon-gjichus subscribe-on" />
            <i v-if="!scope.row.catalogSubscribe" title="directory subscription" class="iconfont icon-gjichus subscribe-off" />
            <i
              v-if="scope.row.mobilePositionSubscribe"
              title="location subscription"
              class="iconfont icon-gxunjians subscribe-on"
            />
            <i
              v-if="!scope.row.mobilePositionSubscribe"
              title="location subscription"
              class="iconfont icon-gxunjians subscribe-off"
            />
          </template>
        </el-table-column>

        <el-table-column label="Operation" min-width="260" fixed="right">
          <template v-slot:default="scope">
            <el-button size="medium" icon="el-icon-edit" type="text" @click="editPlatform(scope.row)">Edit</el-button>
            <el-button size="medium" icon="el-icon-share" type="text" @click="chooseChannel(scope.row)">channel sharing
            </el-button>
            <el-button
              size="medium"
              icon="el-icon-top"
              type="text"
              :loading="pushChannelLoading"
              @click="pushChannel(scope.row)"
            >push channel
            </el-button>
            <el-button
              size="medium"
              icon="el-icon-delete"
              type="text"
              style="color: #f56c6c"
              @click="deletePlatform(scope.row)"
            >Delete
            </el-button>
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

    <platformEdit
      v-if="platform"
      ref="platformEdit"
      v-model="platform"
      :close-edit="closeEdit"
      :device-ips="deviceIps"
    />
    <shareChannel ref="shareChannel" />
  </div>
</template>

<script>
import shareChannel from './dialog/shareChannel.vue'
import platformEdit from './edit.vue'
import Vue from 'vue'

export default {
  name: 'Platform',
  components: {
    shareChannel,
    platformEdit
  },
  data() {
    return {
      loading: false,
      platformList: [], // Device list
      deviceIps: [], // Device list
      defaultPlatform: null,
      platform: null,
      pushChannelLoading: false,
      searchStr: '',
      currentPage: 1,
      count: 15,
      total: 0
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
    this.updateLooper = setInterval(this.initData, 10000)
  },
  destroyed() {
    clearTimeout(this.updateLooper)
  },
  methods: {
    addParentPlatform: function() {
      this.platform = this.defaultPlatform
    },
    editPlatform: function(platform) {
      this.platform = platform
    },
    closeEdit: function() {
      this.platform = null
      this.getPlatformList()
    },
    deletePlatform: function(platform) {
      this.$confirm('Confirm deletion?', 'Tips', {
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        this.deletePlatformCommit(platform)
      })
    },
    deletePlatformCommit: function(platform) {
      this.loading = true
      this.$store.dispatch('platform/remove', platform.id)
        .then(() => {
          this.$message.success({
            showClose: true,
            message: 'Delete successfully'
          })
          this.initData()
        })
        .catch((error) => {
          this.loading = false
          this.$message.error({
            showClose: true,
            message: error
          })
        })
        .finally(() => {
          this.loading = false
        })
    },
    chooseChannel: function(platform) {
      this.$refs.shareChannel.openDialog(platform.id, this.initData)
    },
    pushChannel: function(row) {
      this.pushChannelLoading = true
      this.$store.dispatch('platform/pushChannel', row.id)
        .then((data) => {
          this.$message.success({
            showClose: true,
            message: 'Push successful'
          })
        })
        .catch((error) => {
          this.$message.error({
            showClose: true,
            message: error
          })
        })
        .finally(() => {
          this.pushChannelLoading = false
        })
    },
    initData: function() {
      this.$store.dispatch('platform/getServerConfig')
        .then((data) => {
          this.deviceIps = data.deviceIp.split(',')
          this.defaultPlatform = {
            id: null,
            enable: true,
            ptz: true,
            rtcp: false,
            asMessageChannel: false,
            autoPushChannel: false,
            name: null,
            serverGBId: null,
            serverGBDomain: null,
            serverIp: null,
            serverPort: null,
            deviceGBId: data.username,
            deviceIp: this.deviceIps[0],
            devicePort: data.devicePort,
            username: data.username,
            password: data.password,
            expires: 3600,
            keepTimeout: 60,
            transport: 'UDP',
            characterSet: 'GB2312',
            startOfflinePush: false,
            customGroup: false,
            catalogWithPlatform: 0,
            catalogWithGroup: 0,
            catalogWithRegion: 0,
            manufacturer: null,
            model: null,
            address: null,
            secrecy: 1,
            catalogGroup: 1,
            civilCode: null,
            sendStreamIp: data.sendStreamIp
          }
        })
      this.getPlatformList()
    },
    currentChange: function(val) {
      this.currentPage = val
      this.getPlatformList()
    },
    handleSizeChange: function(val) {
      this.count = val
      this.getPlatformList()
    },
    queryList: function() {
      this.currentPage = 1
      this.total = 0
      this.getPlatformList()
    },
    getPlatformList: function() {
      this.$store.dispatch('platform/query', {
        count: this.count,
        page: this.currentPage,
        query: this.searchStr
      })
        .then((data) => {
          this.total = data.total
          this.platformList = data.list
        })
        .catch(function(error) {
          console.log(error)
        })
    },
    refresh: function() {
      this.initData()
    }
  }
}
</script>
<style>
.subscribe-on {
  color: #409EFF;
  font-size: 18px;
}

.subscribe-off {
  color: #afafb3;
  font-size: 18px;
}
</style>
