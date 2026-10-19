<template>
  <div id="streamProxyList" class="app-container">
    <div v-if="!streamProxy" style="height: calc(100vh - 124px);">
      <el-form :inline="true" size="mini">
        <el-form-item label="Search">
          <el-input
            v-model="searchStr"
            style="margin-right: 1rem; width: auto;"
            placeholder="Keywords"
            prefix-icon="el-icon-search"
            clearable
            @input="queryList"
          />
        </el-form-item>
        <el-form-item label="streaming media">
          <el-select
            v-model="mediaServerId"
            style="margin-right: 1rem;"
            placeholder="Please select"
            default-first-option
            @change="queryList"
          >
            <el-option label="All" value="" />
            <el-option
              v-for="item in mediaServerList"
              :key="item.id"
              :label="item.id"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Pull state">
          <el-select
            v-model="pulling"
            style="margin-right: 1rem;"
            placeholder="Please select"
            default-first-option
            @change="queryList"
          >
            <el-option label="All" value="" />
            <el-option label="Streaming" value="true" />
            <el-option label="Not yet streamed" value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button icon="el-icon-plus" size="mini" style="margin-right: 1rem;" type="primary" @click="addStreamProxy">Add proxy</el-button>
        </el-form-item>
        <el-form-item style="float: right;">
          <el-button icon="el-icon-refresh-right" circle @click="refresh()" />
        </el-form-item>
      </el-form>
      <streamProxyPlayer ref="streamProxyPlayer" />
      <el-table size="small" :data="streamProxyList" style="width: 100%" height="calc(100% - 64px)">
        <el-table-column prop="app" label="Streaming application name" min-width="120" show-overflow-tooltip />
        <el-table-column prop="stream" label="flowID" min-width="120" show-overflow-tooltip />
        <el-table-column label="stream address" min-width="250" show-overflow-tooltip>
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag v-clipboard="scope.row.srcUrl" size="medium" @success="$message({type:'success', message:'Successfully copied to clipboard'})">
                <i class="el-icon-document-copy" title="Click to copy" />
                {{ scope.row.srcUrl }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="mediaServerId" label="streaming media" min-width="180" />
        <el-table-column label="Agency method" width="100">
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              {{ scope.row.type === "default"? "Default":"FFMPEGagent" }}
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="gbDeviceId" label="National standard code" min-width="180" show-overflow-tooltip />
        <el-table-column label="Pull state" min-width="100">
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag v-if="scope.row.pulling && myServerId !== scope.row.serverId" size="medium" style="border-color: #ecf1af">Streaming</el-tag>
              <el-tag v-if="scope.row.pulling && myServerId === scope.row.serverId" size="medium">Streaming</el-tag>
              <el-tag v-if="!scope.row.pulling" size="medium" type="info">Not yet streamed</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="enable" min-width="100">
          <template v-slot:default="scope">
            <div slot="reference" class="name-wrapper">
              <el-tag v-if="scope.row.enable && myServerId !== scope.row.serverId" size="medium" style="border-color: #ecf1af">Enabled</el-tag>
              <el-tag v-if="scope.row.enable && myServerId === scope.row.serverId" size="medium">Enabled</el-tag>
              <el-tag v-if="!scope.row.enable" size="medium" type="info">Not enabled</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="creation time" min-width="150" show-overflow-tooltip />
        <el-table-column label="Operation" width="370" fixed="right">
          <template v-slot:default="scope">
            <el-button size="medium" :loading="scope.row.playLoading" icon="el-icon-video-play" type="text" @click="play(scope.row)">play</el-button>
            <el-divider direction="vertical" />
            <el-button v-if="scope.row.pulling" size="medium" icon="el-icon-switch-button" style="color: #f56c6c" type="text" @click="stopPlay(scope.row)">stop</el-button>
            <el-divider v-if="scope.row.pulling" direction="vertical" />
            <el-button size="medium" icon="el-icon-edit" type="text" @click="edit(scope.row)">
              Edit
            </el-button>
            <el-divider direction="vertical" />
            <el-button size="medium" icon="el-icon-cloudy" type="text" @click="queryCloudRecords(scope.row)">Cloud recording</el-button>
            <el-divider direction="vertical" />
            <el-button size="medium" icon="el-icon-delete" type="text" style="color: #f56c6c" @click="deleteStreamProxy(scope.row)">Delete</el-button>
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
    <StreamProxyEdit v-if="streamProxy" v-model="streamProxy" :close-edit="closeEdit" />
  </div>
</template>

<script>
import streamProxyPlayer from './player.vue'
import StreamProxyEdit from './edit.vue'
import Vue from 'vue'

export default {
  name: 'Proxy',
  components: {
    streamProxyPlayer,
    StreamProxyEdit
  },
  data() {
    return {
      streamProxyList: [],
      currentPusher: {}, // Current operating device object
      updateLooper: 0, // Data refresh rotation training flag
      currentDeviceChannelsLenth: 0,
      currentPage: 1,
      count: 15,
      total: 0,
      streamProxy: null,
      searchStr: '',
      mediaServerId: '',
      pulling: '',
      mediaServerList: []
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
    this.startUpdateList()
  },
  destroyed() {
    this.$destroy('videojs')
    clearTimeout(this.updateLooper)
  },
  methods: {
    initData: function() {
      this.getStreamProxyList()
      this.$store.dispatch('server/getOnlineMediaServerList')
        .then((data) => {
          this.mediaServerList = data
        })
    },
    startUpdateList: function() {
      this.updateLooper = setInterval(() => {
        if (!this.streamProxy) {
          this.getStreamProxyList()
        }
      }, 1000)
    },
    currentChange: function(val) {
      this.currentPage = val
      this.getStreamProxyList()
    },
    handleSizeChange: function(val) {
      this.count = val
      this.getStreamProxyList()
    },
    queryList: function() {
      this.currentPage = 1
      this.total = 0
      this.getStreamProxyList()
    },
    getStreamProxyList: function() {
      this.$store.dispatch('streamProxy/queryList', {
        page: this.currentPage,
        count: this.count,
        query: this.searchStr,
        pulling: this.pulling,
        mediaServerId: this.mediaServerId
      })
        .then(data => {
          this.total = data.total
          for (let i = 0; i < data.list.length; i++) {
            data.list[i]['playLoading'] = false
          }
          this.streamProxyList = data.list
        })
    },
    addStreamProxy: function() {
      this.streamProxy = {
        type: 'default',
        dataType: 3,
        noneReader: 1,
        enable: true,
        enableAudio: true,
        mediaServerId: '',
        timeout: 10
      }
    },
    edit: function(row) {
      if (row.enableDisableNoneReader) {
        this.$set(row, 'noneReader', 1)
      } else {
        this.$set(row, 'noneReader', 0)
      }
      this.streamProxy = row
      this.$set(this.streamProxy, 'rtspType', row.rtspType)
    },
    closeEdit: function(row) {
      this.streamProxy = null
    },
    play: function(row) {
      row.playLoading = true
      this.$store.dispatch('streamProxy/play', row.id)
        .then((data) => {
          this.$refs.streamProxyPlayer.openDialog(data, true)
        })
        .catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        })
        .finally(() => {
          row.playLoading = false
        })
    },
    stopPlay: function(row) {
      this.$store.dispatch('streamProxy/stopPlay', row.id)
        .then(() => {
          this.getStreamProxyList()
        })
        .catch((error) => {
          this.$message({
            showClose: true,
            message: error,
            type: 'error'
          })
        })
    },
    queryCloudRecords: function(row) {
      this.$router.push(`/cloudRecord/detail/${row.app}/${row.stream}`)
    },
    deleteStreamProxy: function(row) {
      this.$confirm('Are you sure you want to delete this agent?？', 'Tips', {
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('streamProxy/remove', row.id)
          .then((data) => {
            this.$message.success({
              showClose: true,
              message: 'Delete successfully'
            })
            this.initData()
          })
          .catch((error) => {
            this.$message({
              showClose: true,
              message: error,
              type: 'error'
            })
          })
      }).catch(() => {
      })
    },
    refresh: function() {
      this.initData()
    }
  }
}
</script>
